#!/usr/bin/env python3
'''
Sends registrations to the securezone notification API so that they restart from
the securezone stage. Registrations come from the query in config.py (--db), a
file (--file) or the command line (--registration).

Exit codes:
  0  every registration was forwarded (or there was nothing to process)
  1  run stopped: network error, authentication failure, too many consecutive
     failures, or an unexpected error
  2  run finished, but some registrations failed (see failed_<timestamp>.txt)
See README.md for details.
'''

import sys
import traceback
import argparse
import time
import datetime as dt
import psycopg2
import config as conf
from api import *
from db import *
from utils import *

EXIT_SUCCESS = 0
EXIT_STOPPED = 1
EXIT_PARTIAL = 2

RUN_TS = dt.datetime.now().strftime('%Y%m%d_%H%M%S')


class Stats:
    def __init__(self):
        self.picked = 0
        self.success = 0
        self.failed = 0
        self.batches = 0

    def __str__(self):
        return 'batches=%d picked=%d success=%d failed=%d' % (self.batches, self.picked, self.success, self.failed)


class OutputFiles:
    '''
    failed_<ts>.txt   : registrations that failed, in --file format, each preceded by a "# reason" line
    remaining_<ts>.txt: registrations not yet sent when a --file / --registration run stops
    Files are created only when there is something to write.
    '''
    def __init__(self):
        self.failed_path = '%s_%s.txt' % (conf.failed_file.rsplit('.txt', 1)[0], RUN_TS)
        self.remaining_path = '%s_%s.txt' % (conf.remaining_file.rsplit('.txt', 1)[0], RUN_TS)

    def add_failed(self, item, reason):
        with open(self.failed_path, 'a') as f:
            f.write('# %s\n' % ' '.join(str(reason).split()))
            f.write('%s\n' % format_registration(item))

    def write_remaining(self, items):
        if not items:
            return
        with open(self.remaining_path, 'w') as f:
            for item in items:
                f.write('%s\n' % format_registration(item))
        myprint('Registrations not sent were written to %s' % self.remaining_path)


def format_registration(item):
    '''
    Write a registration (tuple or raw text line) in --file format.
    '''
    if isinstance(item, str):
        return item
    return ', '.join('' if v is None else str(v) for v in item)


def parse_registration(item):
    '''
    Normalise a registration to (rid, process, workflow_instance_id, iteration, source).

    item: a DB row tuple, or a text line "rid, process, workflow_instance_id, iteration, source".
    Every input (--db, --file, --registration) must have exactly these 5 fields, in this order.
    rid, process, workflow_instance_id and iteration must have a value. source may be empty
    (sent as null), but its field must still be present, e.g. "rid, NEW, wf-id, 1, ".
    Quotes and spaces around each field are removed.
    Raises ValueError for a malformed line or row.
    '''
    if isinstance(item, str):
        fields = [f.strip().strip('\'"').strip() for f in item.split(',')]
    else:
        fields = list(item)

    if len(fields) != 5:
        raise ValueError('Expected 5 fields (rid, process, workflow_instance_id, iteration, source), got %d: %s'
                         % (len(fields), item))

    rid, process, workflow_instance_id, iteration, source = fields
    empty = (None, '', 'None', 'null')
    if rid in empty or process in empty or workflow_instance_id in empty or iteration in empty:
        raise ValueError('rid, process, workflow_instance_id and iteration must have a value: %s' % (item,))

    try:
        iteration = int(iteration)
    except (TypeError, ValueError):
        raise ValueError('iteration must be a number, got %r: %s' % (iteration, item))

    source = None if source in empty else str(source)

    return (str(rid), str(process), str(workflow_instance_id), iteration, source)


def read_file(filename):
    '''
    One registration per line. Empty lines and lines starting with # are ignored.
    Lines are parsed later, one by one, so a bad line is marked failed instead of stopping the run.
    '''
    with open(filename, 'rt') as f:
        lines = [l.strip() for l in f.readlines()]
    return [l for l in lines if l and not l.startswith('#')]


def describe(item):
    try:
        rid, process, workflow_instance_id, iteration, source = parse_registration(item)
        return '%s | %s | %s | iteration=%s | source=%s' % (rid, process, workflow_instance_id, iteration, source)
    except Exception:
        return str(item)


def process_batch(session, items, delay, stats, out, write_remaining_on_stop):
    '''
    Send each registration to securezone.
    Per-registration failures are recorded and the run continues.
    FatalError (network, authentication, too many consecutive failures) stops the run.
    '''
    consecutive_failures = 0
    total = len(items)
    for i, item in enumerate(items):
        stats.picked += 1
        try:
            registration = parse_registration(item)
            status, message = session.notify_securezone(registration)
        except FatalError:
            if write_remaining_on_stop:
                out.write_remaining(items[i:])
            raise
        except Exception as e:  # Bad row or unexpected error for this registration only
            status, message = FAILED, 'Error: %r' % e

        myprint('%d/%d | %s | %s | %s' % (i + 1, total, describe(item), status, message))

        if status == SUCCESS:
            stats.success += 1
            consecutive_failures = 0
        else:
            stats.failed += 1
            consecutive_failures += 1
            out.add_failed(item, message)
            if consecutive_failures >= conf.max_consecutive_failures:
                if write_remaining_on_stop:
                    out.write_remaining(items[i + 1:])
                raise FatalError('%d consecutive registrations failed. Check the server and the last errors.'
                                 % consecutive_failures)

        if i < total - 1:
            time.sleep(delay)


def dry_run(items):
    myprint('DRY RUN: %d registration(s) would be sent. Nothing is sent.' % len(items))
    for i, item in enumerate(items):
        myprint('%d/%d | %s' % (i + 1, len(items), describe(item)))


def new_session():
    return MosipSession(conf.server, conf.client_id, conf.client_pwd, appid=conf.app_id,
                        login_with_client_secret=conf.login_with_client_secret, ssl_verify=conf.ssl_verify,
                        timeout=conf.timeout, max_retries=conf.max_retries, backoff=conf.backoff)


def fetch_batch():
    '''
    Open a new database connection, run the query, and close the connection.
    Each batch is independent: no connection is held while the batch is sent.
    A database error stops the run (exit code 1). Rerunning continues where it stopped.
    '''
    try:
        db = DB(conf.db_user, conf.db_pwd, conf.db_host, conf.db_port, conf.db_name,
                connect_timeout=conf.db_connect_timeout)
    except psycopg2.Error as e:
        raise FatalError('Database connection failed: %s' % str(e).strip())
    try:
        return db.get_rids(conf.query)
    except psycopg2.Error as e:
        raise FatalError('Query failed: %s' % str(e).strip())
    finally:
        db.close()


def run_db(args, stats, out):
    if args.dry_run:
        dry_run(fetch_batch())
        return

    session = new_session()
    attempted = set()  # workflow_instance_ids already sent in this run
    while True:
        rows = fetch_batch()
        new_rows = [r for r in rows if r[2] not in attempted]
        stats.batches += 1
        myprint('Batch %d: query returned %d row(s), %d not yet sent in this run'
                % (stats.batches, len(rows), len(new_rows)))

        if not rows:
            myprint('Query returned no rows. Done.')
            return
        if not new_rows:
            myprint('All rows returned by the query were already sent in this run. Stopping. '
                    'If they were forwarded, the query still matches restarted packets: change it so it no '
                    'longer does. If they failed, handle the failures and run again.')
            return

        attempted.update(r[2] for r in new_rows)
        process_batch(session, new_rows, conf.delay, stats, out, write_remaining_on_stop=False)

        if not args.loop:
            return


def run_list(items, args, stats, out):
    if args.dry_run:
        dry_run(items)
        return
    session = new_session()
    stats.batches += 1
    process_batch(session, items, conf.delay, stats, out, write_remaining_on_stop=True)


class ArgumentParser(argparse.ArgumentParser):
    '''Exit with EXIT_STOPPED on invalid arguments, so exit code 2 only means "some registrations failed".'''
    def error(self, message):
        self.print_usage(sys.stderr)
        sys.stderr.write('%s: error: %s\n' % (self.prog, message))
        sys.exit(EXIT_STOPPED)


def args_parse():
    parser = ArgumentParser(
        description='Restart registrations from the securezone stage. See README.md.')
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--registration', type=str,
                       help='One registration: "rid, process, workflow_instance_id, iteration, source"')
    group.add_argument('--file', type=str,
                       help='File with one registration per line, same format as --registration')
    group.add_argument('--db', action='store_true', help='Fetch registrations with the query in config.py')
    parser.add_argument('--loop', action='store_true',
                        help='With --db: run the query again until it returns no rows')
    parser.add_argument('--dry-run', action='store_true', help='Print the registrations without sending them')
    parser.add_argument('--server', type=str,
                        help='Full url of the server. Overrides server in config.py')
    parser.add_argument('--disable_ssl_verify', action='store_true',
                        help='Disable ssl cert verification while connecting to server')
    args = parser.parse_args()
    if args.loop and not args.db:
        parser.error('--loop can only be used with --db')
    return args


def main():
    args = args_parse()
    if args.server:
        conf.server = args.server  # Override
    if args.disable_ssl_verify:
        conf.ssl_verify = False

    init_logger('full', 'a', './out.log', level=logging.INFO)  # Append mode
    init_logger('last', 'w', './last.log', level=logging.INFO, stdout=False)  # Log of last run only

    stats = Stats()
    out = OutputFiles()
    myprint('Run started. Mode: %s%s%s' % ('db' if args.db else 'file' if args.file else 'registration',
                                            ' loop' if args.loop else '', ' dry-run' if args.dry_run else ''))
    try:
        if args.db:
            run_db(args, stats, out)
        elif args.file:
            run_list(read_file(args.file), args, stats, out)
        else:
            run_list([args.registration], args, stats, out)
    except FatalError as e:
        myprint('RUN STOPPED: %s' % e)
        myprint('Summary: %s' % stats)
        sys.exit(EXIT_STOPPED)
    except Exception:
        myprint('RUN STOPPED: unexpected error\n%s' % traceback.format_exc())
        myprint('Summary: %s' % stats)
        sys.exit(EXIT_STOPPED)

    myprint('Summary: %s' % stats)
    if stats.failed:
        myprint('Failed registrations were written to %s' % out.failed_path)
        sys.exit(EXIT_PARTIAL)
    sys.exit(EXIT_SUCCESS)


if __name__ == '__main__':
    main()

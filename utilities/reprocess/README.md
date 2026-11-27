# Reprocess packets from securezone

## Purpose
Registration Processor has a Reprocessor stage that resumes stuck packets automatically on a schedule. This script is for restarting packets **manually**: it sends registrations to the Registration Processor **securezone notification API**, so that they restart from the securezone stage.

The script is generic. It sends every registration returned by the query in `config.py` (`--db`), listed in a file (`--file`) or given on the command line (`--registration`). Which packets to restart is decided by the query or the input, not by the script.

## Prerequisites
### Install
Requires Python 3.6 or later. The only extra packages are `requests` and `psycopg2-binary` (`requirements.txt`).

**Linux and macOS:**
```
$ ./preinstall.sh
$ source venv/bin/activate
```
`preinstall.sh` creates a virtual environment in `./venv` and installs the packages into it. Nothing is installed system-wide. If Python 3 or its `venv` module is missing, it stops and prints the install command for your system. On Ubuntu/Debian this is usually `sudo apt-get install -y python3 python3-venv`.

Activate the environment (`source venv/bin/activate`) in every new shell before running the script.

**Windows** (Command Prompt):
```
> py -m venv venv
> venv\Scripts\activate
> pip install -r requirements.txt
```

### Environment variables

| Variable | Description |
|---|---|
| `REGPROC_CLIENT_PWD` | Secret of the `mosip-regproc-client` client |
| `DB_USER`, `DB_PWD`, `DB_HOST`, `DB_PORT` | Access to the `mosip_regprc` database (needed for `--db` only) |

Set them in the same shell that runs the script. They are lost when the shell is closed.

**Linux and macOS:**
```
$ export REGPROC_CLIENT_PWD='<mosip-regproc-client secret>'
$ export DB_USER='<db user>'
$ export DB_PWD='<db password>'
$ export DB_HOST='<db host>'
$ export DB_PORT='5432'
$ ./reprocess.py --db --dry-run
```

**Windows** (Command Prompt, no quotes around the values):
```
> set REGPROC_CLIENT_PWD=<mosip-regproc-client secret>
> set DB_USER=<db user>
> set DB_PWD=<db password>
> set DB_HOST=<db host>
> set DB_PORT=5432
> python reprocess.py --db --dry-run
```

## Configuration (`config.py`)
| Setting | Default | Description |
|---|---|---|
| `server` | `''` | Base URL, e.g. `https://api-internal.sandbox.mosip.net`. Can be overridden with `--server`. |
| `ssl_verify` | `True` | Set `False` for self-signed certificates, or use `--disable_ssl_verify`. |
| `client_id` | `mosip-regproc-client` | Client used to log in. Its secret comes from `REGPROC_CLIENT_PWD`. |
| `app_id` | `admin` | Application id sent with the login request. |
| `login_with_client_secret` | `True` | Login method. `True`: client id and secret (`/v1/authmanager/authenticate/clientidsecretkey`). `False`: user name and password (`/v1/authmanager/authenticate/useridPwd`), with `client_id` as the user name and `client_pwd` as the password. |
| `query` | See the **Query** section below | SQL used by `--db`. |
| `delay` | `1` | Seconds between two registrations. Lower to go faster, raise to reduce load. |
| `timeout` | `60` | HTTP timeout per call, in seconds. |
| `db_connect_timeout` | `30` | Seconds to wait for a database connection (`--db`). A new connection is opened for each batch. |
| `max_retries` | `3` | Retries for requests that did not reach the server and for HTTP 500/503. See **Error handling**. |
| `backoff` | `[5, 10, 20]` | Seconds to wait before retry 1, 2, 3. |
| `max_consecutive_failures` | `10` | Stop the run after this many failures in a row. |
| `failed_file`, `remaining_file` | `./failed.txt`, `./remaining.txt` | Output files. A timestamp is added to each name per run. |

## Query
### Contract
Any query can be used. It must return these columns, **in this order**:

| Column | Sent to securezone as |
|---|---|
| `reg_id` | `rid` |
| `process` | `reg_type` |
| `workflow_instance_id` | `workflowInstanceId` |
| `iteration` | `iteration` |
| `source` | `source` |

### Default query
The query is the `query` setting in `config.py`. By default it is the **draft upgrade query**: it selects the in-progress packets that must restart from securezone so that a new draft is created (packets after Quality Classifier not yet published to ID Repo, packets at Packet Receiver, and packets that failed before Create Draft). All other packets are left to the Reprocessor.

Before running, replace the two placeholders in the query (UTC, e.g. `2026-11-20 18:30:00`):

| Placeholder | Value |
|---|---|
| `FROM_TS` | Oldest last-activity time (`latest_trn_dtimes`) of the packets to restart |
| `CUTOVER_TS` | Time at which traffic and all Registration Processor stages were stopped |

Until they are replaced, the query fails with `invalid input syntax for type timestamp`.

### Changing the query
Validate the query before use, and change it as per your requirement (for example custom stages), as long as it returns the columns in the **Contract** above.

To use a different query:
1. Edit `query` in `config.py`. Keep the 5 columns above, in the same order.
2. Count the rows: run it with `count(*)` in place of the column list and without `LIMIT`.
3. See the rows: `./reprocess.py --db --dry-run`.
4. On a large table, check the plan: `ANALYZE regprc.registration;` then `EXPLAIN (ANALYZE, BUFFERS) <query>`. Avoid `Seq Scan on registration`.
5. Run `./reprocess.py --db` (or `--db --loop`).

**Note:** always keep a `LIMIT` in the query (e.g. `LIMIT 1000`). The script loads every row the query returns into memory before sending, so a query without `LIMIT` can load the whole table. Use `--loop` to go through all matching packets batch by batch.

## Options
| Option | Description |
|---|---|
| `--db` | Fetch registrations with `config.query`. |
| `--file <path>` | Text file that you create, any name, one registration per line: `rid, process, workflow_instance_id, iteration, source`. All 5 fields are required, in this order (the same as the query columns). Take the values from `regprc.registration`. `source` may be empty but its field must be present (e.g. `..., 1, `). Quotes are optional. Empty lines and lines starting with `#` are ignored. A `failed_<ts>.txt` or `remaining_<ts>.txt` from an earlier run is already in this format and can be passed directly. |
| `--registration "<line>"` | One registration given directly on the command line, no file needed. Same 5 fields as a `--file` line, in double quotes. |
| `--loop` | With `--db` only. Repeats until the query returns no rows: run the query, send the rows, run the query again. <br><br>**Tracking:** the script remembers every packet it sends in the run (success or failure), using `workflow_instance_id` as the unique identifier. If a later batch returns a packet with the same `workflow_instance_id`, it is skipped, so a packet is sent at most once per run. If a batch contains only packets already sent, the run stops. The tracking is in memory only: if the script is run again (a new run), the list starts empty, and a packet the query still returns is sent again. <br><br>**Query:** must not return a packet once it is sent, otherwise the run stops after the first batch. Use `latest_trn_dtimes < '<run start time>'`, e.g. `latest_trn_dtimes < '2026-11-10 10:00'`: securezone sets `latest_trn_dtimes` to the current time on a sent packet, so it drops out of the next batch. |
| `--dry-run` | Prints the registrations and sends nothing. With `--db`: runs the query once and prints the rows it returns (at most the query's `LIMIT`, e.g. 1000), even with `--loop`. With `--file` / `--registration`: prints every registration given. |
| `--server <url>` | Overrides `server` in `config.py`. |
| `--disable_ssl_verify` | Disables SSL certificate verification. |

`--db`, `--file` and `--registration` are mutually exclusive, and one of them is required.

## Examples
```
# See what would be sent
$ ./reprocess.py --db --dry-run

# One batch
$ ./reprocess.py --db

# All batches until the query returns no rows
$ ./reprocess.py --db --loop

# Single registration
$ ./reprocess.py --registration "10001101040000520220809113748, NEW, e6e944b1-5c4b-42aa-b749-b39428cb7512, 1, REGISTRATION_CLIENT"

# Single registration without source (source is NULL in regprc.registration):
# keep the comma after iteration and leave the last field empty
$ ./reprocess.py --registration "10001101040000520220809113748, NEW, e6e944b1-5c4b-42aa-b749-b39428cb7512, 1, "

# Registrations from a file
$ ./reprocess.py --file registrations.txt

# Retry the failures of a run
$ ./reprocess.py --file failed_20261106_101500.txt
```

Example `registrations.txt`:
```
# rid, process, workflow_instance_id, iteration, source
10001101040000520220809113748, NEW, e6e944b1-5c4b-42aa-b749-b39428cb7512, 1, REGISTRATION_CLIENT
'10001101040000520220809113730', 'UPDATE', 'e6e944b1-5c4b-42aa-b749-b39428cb7333', 1, 
```

**Empty `source`:** when `source` is NULL in `regprc.registration`, keep all 5 fields and leave the last one empty, e.g. `"<rid>, NEW, <workflow_instance_id>, 1, "`. The trailing comma is required: `"<rid>, NEW, <workflow_instance_id>, 1"` has only 4 fields and is rejected. An empty `source` is sent to securezone as null.

## Error handling
| Error | Action |
|---|---|
| Securezone says the packet was not forwarded (e.g. "has not been uploaded to file System") | Marked failed. Run continues. |
| HTTP 4xx (other than 401) | Marked failed. Run continues. |
| HTTP 401 (token expired) | Logs in again and retries once. If the login fails, the run stops. |
| HTTP 500, 503 | Retried `max_retries` times with `backoff`. Then marked failed. Run continues. |
| HTTP 502, 504 (gateway), read timeout, connection reset | **Not resent**: securezone may already have forwarded the packet. Marked failed with `Network or gateway error (...). Check DB status and retry.` Run continues. See **Checking the status before a retry**. |
| Malformed line or row (not exactly 5 fields, a required value missing, `iteration` not a number) | Marked failed. Run continues. |
| Network error before the request is sent (connect timeout, connection refused, DNS) | Retried `max_retries` times with `backoff`. Then **the run stops**. |
| SSL error | **The run stops.** See **Certificate verification failure**. |
| Authentication fails at start | **The run stops.** |
| Database connection or query fails (`--db`) | **The run stops.** A new connection is opened for each batch and closed straight after the query, so nothing is held open while a batch is sent. Rerunning continues where it stopped. |
| `max_consecutive_failures` failures in a row | **The run stops.** This usually means a system-wide problem. |

A registration is successful only when securezone answers "has been forwarded to next stage". Securezone returns HTTP 200 for both outcomes, so the script checks the message text.

### Checking the status before a retry
Securezone forwards a packet again if the same registration is sent twice. After a `Network or gateway error`, the request may have been processed even though no answer came back, so check the registration before retrying it:
```sql
select reg_id, workflow_instance_id, latest_trn_type_code, latest_trn_status_code, upd_dtimes
from regprc.registration
where workflow_instance_id = '<workflow_instance_id>';
```
- `latest_trn_type_code` = `SECUREZONE_NOTIFICATION` and `latest_trn_status_code` = `SUCCESS`, updated at the time of the run: the packet was forwarded. Do not retry it.
- Otherwise: retry it with `--file` or `--registration`.

## Troubleshooting
### TLS/SSL handshake failure
**Symptom:** the run stops at login with a network error that mentions an SSL handshake or protocol, for example:
```
RUN STOPPED: Authentication failed. Network error: ... SSLError(... SSLV3_ALERT_HANDSHAKE_FAILURE ...)
RUN STOPPED: Authentication failed. Network error: ... SSLError(... TLSV1_ALERT_PROTOCOL_VERSION ...)
RUN STOPPED: Authentication failed. Network error: ... SSLError(... UNSUPPORTED_PROTOCOL ...)
```

**Cause:** the script connects over HTTPS using the TLS library built into the Python that runs it. If that library does not support the TLS versions or ciphers the server allows, the server refuses the connection. This can happen on any operating system, usually with an older Python or older OS packages. It is not a certificate problem, so `--disable_ssl_verify` does not help.

**Steps:**
1. Check the TLS library of the Python used by the script (venv activated):
   ```
   python -c "import ssl; print(ssl.OPENSSL_VERSION)"
   ```
2. Check that the machine can reach the server over HTTPS with another client, for example `curl -v https://<server>/`. If `curl` succeeds and the script fails, the problem is the Python TLS library.
3. Install a newer Python 3 on the machine, from python.org or the operating system's package manager. Its `ssl` module should use OpenSSL 1.1.1 or later.
4. Recreate the virtual environment with that Python: delete the `venv` folder and install again as in **Install**. Make sure the new Python is the one used to create the venv.
5. Repeat step 1 to confirm the new TLS library, then run the script again.

If Python cannot be changed on the machine, run the script from another machine that has a newer Python and can reach the server.

### Server not reachable
**Symptom:** the run stops at login with a connection error, for example:
```
RUN STOPPED: Authentication failed. Network error: ... Connection timed out
RUN STOPPED: Authentication failed. Network error: ... Name or service not known
RUN STOPPED: Authentication failed. Network error: ... Connection refused
```

**Cause:** the server URL is wrong, or the server cannot be reached from this machine. The internal API gateway (`api-internal.<domain>`) is usually reachable only from inside the deployment's network.

**Steps:**
1. Check `server` in `config.py` or the `--server` value: it must be the base URL, with `https://` and no trailing slash.
2. Check the connection from the same machine, for example `curl -v https://<server>/`.
3. If it cannot connect, run the script from a machine inside the deployment's network, or connect this machine to that network (for example over a VPN), then run the script again.

### Certificate verification failure
**Symptom:** the run stops at login with an error such as:
```
RUN STOPPED: Authentication failed. Network error: ... CERTIFICATE_VERIFY_FAILED ...
```

**Cause:** the server's certificate is self-signed or issued by an internal certificate authority that this machine does not trust.

**Steps:**
1. Confirm that the server URL is the expected one.
2. Run with `--disable_ssl_verify`, or set `ssl_verify = False` in `config.py`.

Prefer trusting the deployment CA (`REQUESTS_CA_BUNDLE=/path/to/ca.pem`) over disabling verification.

### Login rejected
**Symptom:** the run stops at login with an HTTP error, for example:
```
RUN STOPPED: Authentication failed. HTTP 401 ...
RUN STOPPED: Authentication failed. HTTP 403 ...
```

**Cause:** the server was reached, but the credentials were not accepted.

**Steps:**
1. Check that `REGPROC_CLIENT_PWD` is set in the same shell and holds the secret of `client_id`.
2. Check `client_id`, `app_id` and `login_with_client_secret` in `config.py`.
3. Run the script again.

## Exit codes
| Code | Meaning | What to do |
|---|---|---|
| `0` | Every registration was forwarded, or there was nothing to process. | Nothing. |
| `1` | Run stopped: network error, authentication failure, too many consecutive failures, invalid arguments or unexpected error. | Read the `RUN STOPPED` line in the log and fix the cause. Then rerun: with `--db`, the same command (unsent rows are still returned by the query); with `--file`, use `--file remaining_<ts>.txt`. Also retry `failed_<ts>.txt` if it exists: `remaining_<ts>.txt` does not include registrations that already failed. |
| `2` | Run finished, but some registrations failed. | Check `failed_<ts>.txt` (each line is preceded by the reason) and rerun with `--file failed_<ts>.txt` once fixed. |

## Output
| File | Content |
|---|---|
| `out.log` | All runs (appended). One line per registration: `n/total | rid | process | workflow_instance_id | iteration | source | SUCCESS/FAILED | message`, then a summary. Also printed on the console. |
| `last.log` | Last run only. |
| `failed_<ts>.txt` | Failed registrations in `--file` format, each preceded by `# <reason>`. Created only if something failed. |
| `remaining_<ts>.txt` | Registrations not yet sent when a `--file` / `--registration` run stops. In `--db` mode it is not needed: unsent rows are returned by the query again. |

Summary line: `batches=<n> picked=<n> success=<n> failed=<n>`.

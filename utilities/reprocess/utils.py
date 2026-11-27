import datetime as dt
import requests
import json
import hashlib
import os
import shutil
import pprint
import logging
import glob
import traceback

class FatalError(Exception):
    '''
    Error after which the run cannot continue (network down, authentication failed,
    too many consecutive failures). The run stops with exit code 1.
    '''
    pass

def read_token(response):
    '''
    Read the Authorization cookie from an authentication response, whatever the order of its cookies.
    Raises FatalError when there is no token, because nothing can be sent without one.
    '''
    token = response.cookies.get('Authorization')
    if token:
        return token

    raise FatalError('Authentication failed. HTTP %s. Response: %s' % (response.status_code, response.text[:500]))

def myprint(msg, name='full', name2='last'):
    '''
    name: Logger name. 
    name2: Optional name of second logger
    '''
    text = msg if isinstance(msg, str) else pprint.pformat(msg, width=80)
    log1 = logging.getLogger(name)
    log1.info(text)

    if name2 is not None:  # Same info repeated here
        log2 = logging.getLogger(name2)
        log2.info(text)

def get_timestamp(seconds_offset=None):
    '''
    Current TS.
    Format: 2019-02-14T12:40:59.768Z  (UTC)
    '''
    delta = dt.timedelta(days=0)
    if seconds_offset is not None:
        delta = dt.timedelta(seconds=seconds_offset)

    ts = dt.datetime.now(dt.timezone.utc) + delta
    ms = ts.strftime('%f')[0:3]
    s = ts.strftime('%Y-%m-%dT%H:%M:%S') + '.%sZ' % ms
    return s

def sha256_hash(data):
    '''
    data assumed as type bytes
    '''
    m = hashlib.sha256()
    m.update(data)
    h = m.hexdigest().upper()
    return h

def response_to_json(r):
    try:
        #myprint('Response: <%d>' % r.status_code)
        r = r.content.decode() # to str 
        r = json.loads(r)
    except:
        r = traceback.format_exc()

    return r

def print_response(r):
    print('Status code =  %s' % r.status_code)
    print('Headers =  %s' % r.headers)
    print('Links =  %s' % r.links)
    print('Encoding = %s' % r.encoding)
    print('Response Data = %s' % r.content)
    print('Size = %s' % len(r.content))
 
def zip_packet(regid, base_path, out_dir):
    '''
    Args:
        regid: Registration id - this will be the name of the packet
        base_path:  Zip will cd into this dir and archive from here
        out_dir: Dir in which zip file will be written
    Returns:
        path of zipped packet
    '''
    out_path = os.path.join(out_dir, regid)
    shutil.make_archive(out_path, 'zip', base_path)
    return out_path + '.zip'

def init_logger(logger_name, mode, log_file, level=logging.INFO, stdout=True):
    l = logging.getLogger(logger_name)
    formatter = logging.Formatter('\n[%(asctime)s]\n%(message)s')
    fileHandler = logging.FileHandler(log_file, mode=mode)
    fileHandler.setFormatter(formatter)
    streamHandler = logging.StreamHandler()
    streamHandler.setFormatter(formatter)
   
    l.setLevel(level)
    l.addHandler(fileHandler)
    if stdout:
        l.addHandler(streamHandler) 
    
def path_to_files(path):
   '''
   Given a path determine if its a directory in which case return all files in the dir. 
   '''
   if os.path.isdir(path):
      files = glob.glob(os.path.join(path, '**'), recursive=True)
      files = [f for f in files if not os.path.isdir(f)]  # Remove directories
   else:
      files = [path]
   
   return files

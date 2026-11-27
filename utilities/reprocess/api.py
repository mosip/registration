import time
import requests
import sys
from urllib3.exceptions import NewConnectionError
sys.path.insert(0, '../')
from utils import *

SUCCESS = 'SUCCESS'
FAILED = 'FAILED'

# Securezone returns HTTP 200 for both outcomes. Only this text means the packet was accepted.
FORWARDED_TEXT = 'has been forwarded to next stage'

# Gateway errors (502 Bad Gateway: connection to securezone failed; 504 Gateway Timeout: securezone
# did not answer in time). Securezone may still have forwarded the packet, so it is not resent.
GATEWAY_ERRORS = (502, 504)

UNCERTAIN_MSG = 'Network or gateway error (%s). Check DB status and retry.'


def request_not_sent(e):
    '''
    True when the request surely did not reach the server, so resending is safe:
    connect timeout, connection refused, DNS failure.
    '''
    if isinstance(e, requests.ConnectTimeout):
        return True
    reason = getattr(e.args[0], 'reason', None) if e.args else None
    return isinstance(reason, NewConnectionError)

class MosipSession:
    def __init__(self, server, user, pwd, appid, login_with_client_secret, ssl_verify, timeout, max_retries, backoff):
        self.server = server
        self.user = user
        self.pwd = pwd
        self.appid = appid
        self.ssl_verify = ssl_verify
        self.login_with_client_secret = login_with_client_secret
        self.timeout = timeout
        self.max_retries = max_retries
        self.backoff = list(backoff)
        self.token = self.authenticate()

    def authenticate(self):
        '''
        Get a new token. Any failure raises FatalError.
        '''
        try:
            if self.login_with_client_secret:
                return self.auth_get_client_token(self.appid, self.user, self.pwd)
            return self.auth_get_token(self.appid, self.user, self.pwd)
        except requests.RequestException as e:
            raise FatalError('Authentication failed. Network error: %s' % e)

    def auth_get_token(self, appid, username, pwd):
        url = '%s/v1/authmanager/authenticate/useridPwd' % self.server
        ts = get_timestamp()
        j = {
            "id": "mosip.io.userId.pwd",
            "metadata" : {},
                "version":"1.0",
                "requesttime": ts,
                "request": {
                    "appId" : appid,
                    "userName": username,
                    "password": pwd
            }
        }
        r = requests.post(url, json = j, verify=self.ssl_verify, timeout=self.timeout)
        token = read_token(r)
        return token

    def auth_get_client_token(self, appid, client_id, pwd):
        url = '%s/v1/authmanager/authenticate/clientidsecretkey' % self.server
        ts = get_timestamp()
        j = {
            'id': 'string',
            'metadata': {},
            'request': {
                'appId': appid,
                'clientId': client_id,
                'secretKey': pwd
            },
            'requesttime': ts,
            'version': '1.0'
        }

        r = requests.post(url, json = j, verify=self.ssl_verify, timeout=self.timeout)
        token = read_token(r)
        return token

    def _wait(self, attempt):
        time.sleep(self.backoff[min(attempt, len(self.backoff) - 1)])

    def notify_securezone(self, registration):
        '''
        registration: tuple (rid, process, workflow_instance_id, iteration, source)

        Returns (SUCCESS | FAILED, message).
        A request is resent only when it surely did not change anything on the server.
          - SSL error: FatalError (run stops). Retrying does not help.
          - Request not sent (connect timeout, connection refused, DNS): retried max_retries times,
            then FatalError (run stops).
          - Request may have been processed (read timeout, connection reset, HTTP 502/504):
            not resent. FAILED, to be retried after checking the status in the database.
          - HTTP 401: one re-authentication and retry. Re-authentication failure raises FatalError.
          - Other HTTP 5xx (500, 503): retried max_retries times, then FAILED.
          - Any other HTTP status, or a response without the "forwarded" text: FAILED.
        '''
        rid, process, workflow_instance_id, iteration, source = registration
        url = '%s/registrationprocessor/v1/securezone/notification' % self.server
        j = {
            'rid': rid,
            'reg_type': process,
            'workflowInstanceId': workflow_instance_id,
            'isValid': True,
            'internalError': False,
            'messageBusAddress': None,
            'retryCount': None,
            'iteration': iteration,
            'source': source
        }

        attempt = 0
        reauthenticated = False
        while True:
            try:
                r = requests.post(url, cookies={'Authorization': self.token}, json=j,
                                  verify=self.ssl_verify, timeout=self.timeout)
            except requests.exceptions.SSLError as e:  # Caught first: SSLError is a ConnectionError
                raise FatalError('SSL error: %s' % e)
            except (requests.ConnectionError, requests.Timeout) as e:
                if not request_not_sent(e):
                    return FAILED, UNCERTAIN_MSG % e
                if attempt >= self.max_retries:
                    raise FatalError('Network error after %d retries: %s' % (attempt, e))
                self._wait(attempt)
                attempt += 1
                continue

            if r.status_code == 401 and not reauthenticated:
                self.token = self.authenticate()
                reauthenticated = True
                continue

            if r.status_code in GATEWAY_ERRORS:
                return FAILED, UNCERTAIN_MSG % ('HTTP %d' % r.status_code)

            if r.status_code >= 500:
                if attempt >= self.max_retries:
                    return FAILED, 'HTTP %d after %d retries: %s' % (r.status_code, attempt, r.text[:500])
                self._wait(attempt)
                attempt += 1
                continue

            if r.status_code != 200:
                return FAILED, 'HTTP %d: %s' % (r.status_code, r.text[:500])

            response = response_to_json(r)
            if FORWARDED_TEXT in str(response):
                return SUCCESS, response
            return FAILED, response

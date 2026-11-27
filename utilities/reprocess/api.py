import time
import requests
import sys
sys.path.insert(0, '../')
from utils import *

SUCCESS = 'SUCCESS'
FAILED = 'FAILED'

# Securezone returns HTTP 200 for both outcomes. Only this text means the packet was accepted.
FORWARDED_TEXT = 'has been forwarded to next stage'

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
          - Network error: retried max_retries times, then FatalError (run stops).
          - HTTP 401: one re-authentication and retry. Re-authentication failure raises FatalError.
          - HTTP 5xx: retried max_retries times, then FAILED.
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
            except (requests.ConnectionError, requests.Timeout) as e:  # SSLError is a ConnectionError
                if attempt >= self.max_retries:
                    raise FatalError('Network error after %d retries: %s' % (attempt, e))
                self._wait(attempt)
                attempt += 1
                continue

            if r.status_code == 401 and not reauthenticated:
                self.token = self.authenticate()
                reauthenticated = True
                continue

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

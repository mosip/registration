import psycopg2

class DB:
    '''
    One connection per batch: open, run the query, close.
    Nothing stays open while the batch is being sent, so a run of several hours holds no
    database connection or transaction, and firewalls have no idle connection to drop.
    '''
    def __init__(self, user, pwd, host, port, db_name, connect_timeout):
        self.conn = psycopg2.connect(user = user, password = pwd, host = host, port = port, database = db_name,
                                     connect_timeout = connect_timeout)

    def close(self):
        self.conn.close()  # Also ends the read-only transaction opened by the query

    def get_rids(self, query):
        cur = self.conn.cursor()
        cur.execute(query)
        registrations = cur.fetchall()
        cur.close()
        return registrations

CREATE DATABASE mosip_regprc
	ENCODING = 'UTF8'
	LC_COLLATE = 'en_US.UTF-8'
	LC_CTYPE = 'en_US.UTF-8'
	TABLESPACE = pg_default
	OWNER = postgres
	TEMPLATE  = template0;
COMMENT ON DATABASE mosip_regprc IS 'The data related to Registration process flows and transaction will be maintained in this database. This database also maintains data that is needed to perform deduplication.';

\c mosip_regprc postgres

DROP SCHEMA IF EXISTS regprc CASCADE;
CREATE SCHEMA regprc;
ALTER SCHEMA regprc OWNER TO postgres;
ALTER DATABASE mosip_regprc SET search_path TO regprc,pg_catalog,public;

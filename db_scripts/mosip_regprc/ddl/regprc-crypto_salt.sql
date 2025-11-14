

-- object: regprc.crypto_salt | type: TABLE --
-- DROP TABLE IF EXISTS regprc.crypto_salt CASCADE;
CREATE TABLE regprc.crypto_salt(
	id integer NOT NULL,
	salt character varying(36) NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256) ,
	upd_dtimes timestamp ,
	CONSTRAINT pk_rides PRIMARY KEY (id));
-- ddl-end --


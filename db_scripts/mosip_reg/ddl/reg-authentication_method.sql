
-- object: reg.authentication_method | type: TABLE --
-- DROP TABLE IF EXISTS reg.authentication_method CASCADE;
CREATE TABLE reg.authentication_method(
	code character varying(36) NOT NULL,
	method_seq smallint,
	lang_code character varying(3) NOT NULL,
	is_active boolean NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean,
	del_dtimes timestamp,
	CONSTRAINT pk_authm_code PRIMARY KEY (code,lang_code)

);
-- ddl-end --

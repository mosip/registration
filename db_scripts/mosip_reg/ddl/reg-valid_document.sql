
-- object: reg.valid_document | type: TABLE --
-- DROP TABLE IF EXISTS reg.valid_document CASCADE;
CREATE TABLE reg.valid_document(
	doccat_code character varying(36) NOT NULL,
	doctyp_code character varying(36) NOT NULL,
	lang_code character varying(3) NOT NULL,
	is_active boolean NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean,
	del_dtimes timestamp,
	CONSTRAINT pk_valdoc_code PRIMARY KEY (doccat_code,doctyp_code)

);
-- ddl-end --



-- object: reg.registration_transaction | type: TABLE --
-- DROP TABLE IF EXISTS reg.registration_transaction CASCADE;
CREATE TABLE reg.registration_transaction(
	id character varying(36) NOT NULL,
	reg_id character varying(39) NOT NULL,
	trn_type_code character varying(36) NOT NULL,
	remarks character varying(1024),
	parent_regtrn_id character varying(36),
	status_code character varying(36) NOT NULL,
	lang_code character varying(3) NOT NULL,
	status_comment character varying(1024),
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean,
	del_dtimes timestamp,
	CONSTRAINT pk_regtrn_id PRIMARY KEY (id)

);
-- ddl-end --


-- object: reg.user_pwd | type: TABLE --
-- DROP TABLE IF EXISTS reg.user_pwd CASCADE;
CREATE TABLE reg.user_pwd(
	usr_id character varying(256) NOT NULL,
	pwd character varying(512) NOT NULL,
	pwd_expiry_dtimes timestamp,
	status_code character varying(64) NOT NULL,
	lang_code character varying(3) NOT NULL,
	is_active boolean NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean,
	del_dtimes timestamp,
	CONSTRAINT pk_usrpwd_usr_id PRIMARY KEY (usr_id)

);
-- ddl-end --

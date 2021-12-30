
-- object: reg.screen_authorization | type: TABLE --
-- DROP TABLE IF EXISTS reg.screen_authorization CASCADE;
CREATE TABLE reg.screen_authorization(
	screen_id character varying(36) NOT NULL,
	role_code character varying(36) NOT NULL,
	lang_code character varying(3) NOT NULL,
	is_permitted boolean NOT NULL,
	is_active boolean NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean,
	del_dtimes timestamp,
	CONSTRAINT pk_scrauth_screen_id PRIMARY KEY (screen_id,role_code)

);
-- ddl-end --

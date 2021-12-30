
-- object: reg.screen_detail | type: TABLE --
-- DROP TABLE IF EXISTS reg.screen_detail CASCADE;
CREATE TABLE reg.screen_detail(
	id character varying(36) NOT NULL,
	app_id character varying(36) NOT NULL,
	name character varying(64) NOT NULL,
	descr character varying(256),
	lang_code character varying(3) NOT NULL,
	is_active boolean NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean,
	del_dtimes timestamp,
	CONSTRAINT pk_scrdtl_id PRIMARY KEY (id,lang_code)

);
-- ddl-end --

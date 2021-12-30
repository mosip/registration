
-- object: reg.sync_control | type: TABLE --
-- DROP TABLE IF EXISTS reg.sync_control CASCADE;
CREATE TABLE reg.sync_control(
	id character varying(36) NOT NULL,
	syncjob_id character varying(36) NOT NULL,
	machine_id character varying(10),
	regcntr_id character varying(10),
	synctrn_id character varying(36) NOT NULL,
	last_sync_dtimes timestamp NOT NULL,
	lang_code character varying(3),
	is_active boolean NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean,
	del_dtimes timestamp,
	CONSTRAINT pk_synctrl_id PRIMARY KEY (id)

);
-- ddl-end --

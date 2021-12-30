

-- object: reg.sync_transaction | type: TABLE --
-- DROP TABLE IF EXISTS reg.sync_transaction CASCADE;
CREATE TABLE reg.sync_transaction(
	id character varying(36) NOT NULL,
	syncjob_id character varying(36) NOT NULL,
	sync_dtimes timestamp NOT NULL,
	status_code character varying(36) NOT NULL,
	status_comment character varying(256),
	trigger_point character varying(32),
	sync_from character varying(32),
	sync_to character varying(32),
	machine_id character varying(10),
	regcntr_id character varying(10),
	ref_id_type character varying(64),
	ref_id character varying(64),
	sync_param character varying(2048),
	lang_code character varying(3),
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean,
	del_dtimes timestamp,
	CONSTRAINT pk_synctrn_id PRIMARY KEY (id)

);
-- ddl-end --

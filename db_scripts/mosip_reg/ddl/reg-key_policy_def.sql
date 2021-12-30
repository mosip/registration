

-- object: reg.key_policy_def | type: TABLE --
-- DROP TABLE IF EXISTS reg.key_policy_def CASCADE;
CREATE TABLE reg.key_policy_def(
	app_id character varying(36) NOT NULL,
	key_validity_duration smallint,
	is_active boolean NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean,
	del_dtimes timestamp,
	CONSTRAINT pk_keypdef_id PRIMARY KEY (app_id)

);

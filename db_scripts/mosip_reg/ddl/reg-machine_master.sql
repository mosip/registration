

-- object: reg.machine_master | type: TABLE --
-- DROP TABLE IF EXISTS reg.machine_master CASCADE;
CREATE TABLE reg.machine_master(
	id character varying(10) NOT NULL,
	name character varying(64) NOT NULL,
	mac_address character varying(64),
	serial_num character varying(64),
	ip_address character varying(17),
	public_key  character varying(1024),
	key_index character varying(128),
	sign_public_key  character varying(1024),
	sign_key_index character varying(128),
	validity_end_dtimes timestamp,
	mspec_id character varying(36) NOT NULL,
	lang_code character varying(3) NOT NULL,
	is_active boolean NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean,
	del_dtimes timestamp,
	CONSTRAINT pk_machm_id PRIMARY KEY (id,lang_code)

);
-- ddl-end --


-- object: reg.reg_device_sub_type | type: TABLE --
-- DROP TABLE IF EXISTS reg.reg_device_sub_type CASCADE;
CREATE TABLE reg.reg_device_sub_type(
	code character varying(36) NOT NULL,
	dtyp_code character varying(36) NOT NULL,
	name character varying(64) NOT NULL,
	descr character varying(512),
	is_active boolean NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean,
	del_dtimes timestamp,
	CONSTRAINT pk_rdstyp_code PRIMARY KEY (code),
	CONSTRAINT uk_rdstyp UNIQUE (dtyp_code,name)

);

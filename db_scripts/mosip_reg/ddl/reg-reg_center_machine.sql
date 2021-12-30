

-- object: reg.reg_center_machine | type: TABLE --
-- DROP TABLE IF EXISTS reg.reg_center_machine CASCADE;
CREATE TABLE reg.reg_center_machine(
	regcntr_id character varying(10) NOT NULL,
	machine_id character varying(10) NOT NULL,
	lang_code character varying(3) NOT NULL,
	is_active boolean NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean,
	del_dtimes timestamp,
	CONSTRAINT pk_cntrmac_usr_id PRIMARY KEY (machine_id,regcntr_id)

);
-- ddl-end --

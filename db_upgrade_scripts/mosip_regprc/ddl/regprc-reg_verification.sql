-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_regprc
-- Table Name 	: regprc.reg_manual_verification
-- Purpose    	: Verification: Records marked for verification will be present in this table.
-- Create By   	: Monobikash
-- Created Date	: Nov-2021
--
-- ------------------------------------------------------------------------------------------
--
-- ------------------------------------------------------------------------------------------

-- object: regprc.reg_verification | type: TABLE --
-- DROP TABLE IF EXISTS regprc.reg_verification CASCADE;
CREATE TABLE regprc.reg_verification(
    workflow_instance_id character varying(36) NOT NULL,
	reg_id character varying(39) NOT NULL,
	verification_req_id character varying(39) NOT NULL,
	matched_type character varying(36),
	verification_usr_id character varying(256),
	response_text character varying(512),
	status_code character varying(36),
	reason_code character varying(36),
	status_comment character varying(256),
	is_active boolean NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean,
	del_dtimes timestamp,
	CONSTRAINT pk_reg_ver_id PRIMARY KEY (workflow_instance_id)
);
-- ddl-end --
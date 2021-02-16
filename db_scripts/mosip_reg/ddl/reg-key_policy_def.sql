-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_reg
-- Table Name 	: reg.key_policy_def
-- Purpose    	: Key Policy Defination: Policy related to encryption key management is defined here. For eg. Expiry duration of a key generated.
--           
-- Create By   	: Sadanandegowda DM
-- Created Date	: Sep-2020
-- 
-- Modified Date        Modified By         Comments / Remarks
-- ------------------------------------------------------------------------------------------
-- Jan-2021		Ram Bhatt	    Set is_deleted flag to not null and default false
-- ------------------------------------------------------------------------------------------

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
	is_deleted boolean NOT NULL DEFAULT FALSE,
	del_dtimes timestamp,
	CONSTRAINT pk_keypdef_id PRIMARY KEY (app_id)

);

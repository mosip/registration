-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_reg
-- Table Name 	: reg.key_alias
-- Purpose    	: Key Alias: To maintain a system generated key as alias for the encryption key that will be stored in key-store devices like HSM.
--           
-- Create By   	: Sadanandegowda DM
-- Created Date	: Sep-2020
-- 
-- Modified Date        Modified By         Comments / Remarks
-- ------------------------------------------------------------------------------------------
-- Jan-2021		Ram Bhatt	    Set is_deleted flag to not null and default false
-- ------------------------------------------------------------------------------------------

-- object: reg.key_alias | type: TABLE --
-- DROP TABLE IF EXISTS reg.key_alias CASCADE;
CREATE TABLE reg.key_alias(
	id character varying(36) NOT NULL,
	app_id character varying(36) NOT NULL,
	ref_id character varying(128),
	key_gen_dtimes timestamp,
	key_expire_dtimes timestamp,
	status_code character varying(36),
	lang_code character varying(3),
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean NOT NULL DEFAULT FALSE,
	del_dtimes timestamp,
	CONSTRAINT pk_keymals_id PRIMARY KEY (id)

);

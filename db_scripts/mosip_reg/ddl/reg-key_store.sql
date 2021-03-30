-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_reg
-- Table Name 	: reg.key_store
-- Purpose    	: Key Store: In MOSIP, data related to an individual in stored in encrypted form. This table is to manage all the keys(private and public keys) used.
--           
-- Create By   	: Sadanandegowda DM
-- Created Date	: Sep-2020
-- 
-- Modified Date        Modified By         Comments / Remarks
-- ------------------------------------------------------------------------------------------
-- Sep-2020             Sadanandegowda DM   Update private_key data type and added certificate_data attribute
-- Jan-2021		Ram Bhatt	    Set is_deleted flag to not null and default false
-- ------------------------------------------------------------------------------------------

-- object: reg.key_store | type: TABLE --
-- DROP TABLE IF EXISTS reg.key_store CASCADE;
CREATE TABLE reg.key_store(
	id character varying(36) NOT NULL,
	master_key character varying(36) NOT NULL,
	private_key character varying(2500) NOT NULL,
	certificate_data character varying(2500) NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean NOT NULL DEFAULT FALSE,
	del_dtimes timestamp,
	CONSTRAINT pk_keystr_id PRIMARY KEY (id)

);

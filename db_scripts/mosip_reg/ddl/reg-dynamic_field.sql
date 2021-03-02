-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_reg
-- Table Name 	: 
-- Purpose    	: 
--           
-- Create By   	: Sadanandegowda DM
-- Created Date	: 22-Apr-2019
-- 
-- Modified Date        Modified By         Comments / Remarks
-- ------------------------------------------------------------------------------------------
-- Jan-2021		Ram Bhatt	    Set is_deleted flag to not null and default false
-- ------------------------------------------------------------------------------------------
-- object: reg.dynamic_field | type: TABLE --
-- DROP TABLE IF EXISTS reg.dynamic_field CASCADE;
CREATE TABLE reg.dynamic_field(
	id character varying(36) NOT NULL,
	name character varying(36) NOT NULL,
	description character varying(256),
	data_type character varying(16),
	value_json character varying(4086),
	lang_code character varying(3),
	is_active boolean NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean NOT NULL DEFAULT FALSE,
	del_dtimes timestamp,
	CONSTRAINT pk_schfld_id PRIMARY KEY (id),
	CONSTRAINT uk_schfld_name UNIQUE (name,lang_code)

);

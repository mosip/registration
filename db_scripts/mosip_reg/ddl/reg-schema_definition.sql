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
-- object: reg.schema_definition | type: TABLE --
-- DROP TABLE IF EXISTS reg.schema_definition CASCADE;
CREATE TABLE reg.schema_definition(
	id character varying(36) NOT NULL,
	def_type character varying(16),
	def_name character varying(36),
	add_props boolean,
	def_json character varying(4086),
	is_active boolean NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean NOT NULL DEFAULT FALSE,
	del_dtimes timestamp,
	CONSTRAINT pk_schdef_id PRIMARY KEY (id)

);

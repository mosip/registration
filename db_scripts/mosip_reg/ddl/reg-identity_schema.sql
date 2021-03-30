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
-- object: reg.identity_schema | type: TABLE --
-- DROP TABLE IF EXISTS reg.identity_schema CASCADE;
CREATE TABLE reg.identity_schema(
	id character varying(36) NOT NULL,
	id_version character varying(8),
	title character varying(64),
	description character varying(256),
	id_attr_json character varying(12288),
	schema_json character varying(10240),
	status_code character varying(36),
	add_props boolean,
	effective_from timestamp,
	lang_code character varying(3),
	is_active boolean NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean NOT NULL DEFAULT FALSE,
	del_dtimes timestamp,
	CONSTRAINT pk_idsch_id PRIMARY KEY (id)

);

-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_reg
-- Table Name 	: reg.title
-- Purpose    	: 
--           
-- Create By   	: Nasir Khan / Sadanandegowda
-- Created Date	: 15-Jul-2019
-- 
-- Modified Date        Modified By         Comments / Remarks
-- ------------------------------------------------------------------------------------------
-- Jan-2021		Ram Bhatt	    Set is_deleted flag to not null and default false
-- ------------------------------------------------------------------------------------------

-- object: reg.title | type: TABLE --
-- DROP TABLE IF EXISTS reg.title CASCADE;
CREATE TABLE reg.title(
	code character varying(16) NOT NULL,
	name character varying(64) NOT NULL,
	descr character varying(128),
	lang_code character varying(3) NOT NULL,
	is_active boolean NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean NOT NULL DEFAULT FALSE,
	del_dtimes timestamp,
	CONSTRAINT pk_ttl_code PRIMARY KEY (code,lang_code)

);
-- ddl-end --

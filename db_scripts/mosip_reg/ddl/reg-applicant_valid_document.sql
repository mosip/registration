-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_reg
-- Table Name 	: reg.applicant_valid_document
-- Purpose    	: 
--           
-- Create By   	: Nasir Khan / Sadanandegowda
-- Created Date	: 15-Jul-2019
-- 
-- Modified Date        Modified By         Comments / Remarks
-- ------------------------------------------------------------------------------------------
-- Jan-2021		Ram Bhatt	    Set is_deleted flag to not null and default false
-- ------------------------------------------------------------------------------------------

-- object: reg.applicant_valid_document | type: TABLE --
-- DROP TABLE IF EXISTS reg.applicant_valid_document CASCADE;
CREATE TABLE reg.applicant_valid_document(
	apptyp_code character varying(36) NOT NULL,
	doccat_code character varying(36) NOT NULL,
	doctyp_code character varying(36) NOT NULL,
	lang_code character varying(3) NOT NULL,
	is_active boolean NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean NOT NULL DEFAULT FALSE,
	del_dtimes timestamp,
	CONSTRAINT pk_avaldoc_code PRIMARY KEY (apptyp_code,doccat_code,doctyp_code)

);
-- ddl-end --

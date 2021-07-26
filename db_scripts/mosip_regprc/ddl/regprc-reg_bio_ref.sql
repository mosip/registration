-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_regprc
-- Table Name 	: regprc.reg_bio_ref
-- Purpose    	: Registration Biometric Reference: Mapping table to store the bio reference id for an registration id
--           
-- Create By   	: Nasir Khan / Sadanandegowda
-- Created Date	: 15-Jul-2019
-- 
-- Modified Date        Modified By         Comments / Remarks
-- ------------------------------------------------------------------------------------------
-- Jan-2021		Ram Bhatt	    Set is_deleted flag to not null and default false
-- Mar-2021		Ram Bhatt	    Reverting is_deleted not null changes for 1.1.5
-- Jul-2021		Ram Bhatt	    Index Creation and added new column
-- ------------------------------------------------------------------------------------------

-- object: regprc.reg_bio_ref | type: TABLE --
-- DROP TABLE IF EXISTS regprc.reg_bio_ref CASCADE;
CREATE TABLE regprc.reg_bio_ref(
	reg_id character varying(39) NOT NULL,
	bio_ref_id character varying(36) NOT NULL,
	is_active boolean NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean DEFAULT FALSE,
	del_dtimes timestamp,
	workflow_instance_id character varying(36) NOT NULL,
	source character varying,
	iteration integer DEFAULT 1,
	CONSTRAINT pk_regbref_id PRIMARY KEY (bio_ref_id,workflow_instance_id)

);
-- indexes section -------------------------------------------------
create index idx_rbioref_crdtimes on regprc.reg_bio_ref (cr_dtimes);

-- ddl-end --

COMMENT ON TABLE regprc.reg_bio_ref IS 'Registration Biometric Reference: Mapping table to store the bio reference id for an registration id';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_bio_ref.reg_id IS 'Registration ID: ID of the registration request';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_bio_ref.bio_ref_id IS 'Biometric Reference ID: Biometric Reference ID of the host registration id for which requests are being sent to ABIS application.';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_bio_ref.is_active IS 'IS_Active : Flag to mark whether the record is Active or In-active';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_bio_ref.cr_by IS 'Created By : ID or name of the user who create / insert record.';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_bio_ref.cr_dtimes IS 'Created DateTimestamp : Date and Timestamp when the record is created/inserted';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_bio_ref.upd_by IS 'Updated By : ID or name of the user who update the record with new values';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_bio_ref.upd_dtimes IS 'Updated DateTimestamp : Date and Timestamp when any of the fields in the record is updated with new values.';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_bio_ref.is_deleted IS 'IS_Deleted : Flag to mark whether the record is Soft deleted.';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_bio_ref.del_dtimes IS 'Deleted DateTimestamp : Date and Timestamp when the record is soft deleted with is_deleted=TRUE';
-- ddl-end --


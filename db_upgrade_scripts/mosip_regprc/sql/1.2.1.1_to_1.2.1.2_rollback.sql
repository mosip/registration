DROP INDEX IF EXISTS idx_registration_workflow_instance_id ON regprc.registration;
DROP INDEX IF EXISTS idx_registration_sts_resume ON regprc.registration;
DROP INDEX IF EXISTS idx_abis_search ON regprc.abis_request;
DROP INDEX IF EXISTS idx_rbioref_wfid ON  regprc.reg_bio_ref;
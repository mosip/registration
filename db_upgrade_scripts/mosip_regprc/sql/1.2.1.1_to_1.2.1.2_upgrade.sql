\c mosip_regprc

CREATE INDEX IF NOT EXISTS idx_registration_workflow_instance_id ON regprc.registration(workflow_instance_id desc);
CREATE INDEX IF NOT EXISTS idx_registration_sts_resume ON regprc.registration(status_code, resume_timestamp, default_resume_action);
CREATE INDEX IF NOT EXISTS idx_abis_search on regprc.abis_request(bio_ref_id, ref_regtrn_id);
CREATE INDEX IF NOT EXISTS idx_rbioref_wfid on  regprc.reg_bio_ref(workflow_instance_id);
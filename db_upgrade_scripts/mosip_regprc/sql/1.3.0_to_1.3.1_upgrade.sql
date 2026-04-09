CREATE INDEX idx_registration_reg_id_is_active ON regprc.registration (reg_id) WHERE is_deleted = false AND is_active = true;
CREATE INDEX idx_registration_reprocessor ON regprc.registration (latest_trn_status_code, latest_trn_dtimes, reg_process_retry_count, status_code);
CREATE INDEX idx_registration_paused_actionable ON regprc.registration (status_code, resume_timestamp, upd_dtimes) WHERE default_resume_action IS NOT NULL;
CREATE INDEX idx_reg_transaction_reg_id ON regprc.registration_transaction (reg_id);
-- UPGRADE FOR PERFORMANCE OPTIMIZATION INDEXES

CREATE INDEX idx_bio_regtrn_reqtype ON regprc.abis_request USING btree (bio_ref_id, ref_regtrn_id, request_type);
CREATE INDEX idx_bio_reqtype_status ON regprc.abis_request USING btree (bio_ref_id, request_type, status_code);
CREATE INDEX idx_refregtrn_reqtype ON regprc.abis_request USING btree (ref_regtrn_id, request_type);
CREATE INDEX idx_req_batch_id ON regprc.abis_request USING btree (req_batch_id);

CREATE INDEX idx_abis_resp_id ON regprc.abis_response USING btree (id);
CREATE INDEX idx_abis_response_reqid ON regprc.abis_response USING btree (abis_req_id);

CREATE INDEX idx_addlinforeq_regid ON regprc.additional_info_request USING btree (reg_id);
CREATE INDEX idx_addlinforeq_regid_proc_iterdesc ON regprc.additional_info_request USING btree (reg_id, additional_info_process, additional_info_iteration DESC);
CREATE INDEX idx_addlinforeq_reqid ON regprc.additional_info_request USING btree (additional_info_req_id);

CREATE UNIQUE INDEX pk_anonymous_id ON regprc.anonymous_profile USING btree (id);

CREATE UNIQUE INDEX pk_rides ON regprc.crypto_salt USING btree (id);

CREATE INDEX idx_idemogd_namedobgender_lang_active ON regprc.individual_demographic_dedup USING btree (name, dob, gender, lang_code, is_active);
CREATE INDEX idx_individual_demographic_dedup_regid ON regprc.individual_demographic_dedup USING btree (reg_id);

CREATE INDEX idx_regbio_bio_created ON regprc.reg_bio_ref USING btree (bio_ref_id, cr_dtimes DESC);
CREATE INDEX idx_regbio_regid ON regprc.reg_bio_ref USING btree (reg_id);
CREATE INDEX idx_regbio_wf_created ON regprc.reg_bio_ref USING btree (workflow_instance_id, cr_dtimes DESC);

CREATE INDEX idx_verification_reqid ON regprc.reg_verification USING btree (verification_req_id);
CREATE INDEX idx_verification_wf ON regprc.reg_verification USING btree (workflow_instance_id);

CREATE INDEX idx_paused_actionable ON regprc.registration USING btree (status_code, resume_timestamp, upd_dtimes) WHERE (default_resume_action IS NOT NULL);
CREATE INDEX idx_regid_active_not_deleted ON regprc.registration USING btree (reg_id) WHERE ((is_deleted = false) AND (is_active = true));
CREATE INDEX idx_registration_reg_id ON regprc.registration USING btree (reg_id);
CREATE INDEX idx_resumable_packets ON regprc.registration USING btree (status_code, upd_dtimes);

CREATE INDEX idx_additional_info_req_id ON regprc.registration_list USING btree (additional_info_req_id);
CREATE INDEX idx_packet_id ON regprc.registration_list USING btree (packet_id);
CREATE INDEX idx_reglist_regid_aireqid_active ON regprc.registration_list USING btree (reg_id, additional_info_req_id) WHERE (is_deleted = false);
CREATE INDEX idx_workflow_instance_id ON regprc.registration_list USING btree (workflow_instance_id);

CREATE INDEX idx_registration_transaction_status ON regprc.registration_transaction USING btree (status_code);

---END UPGRADE FOR PERFORMANCE OPTIMIZATION INDEXES--

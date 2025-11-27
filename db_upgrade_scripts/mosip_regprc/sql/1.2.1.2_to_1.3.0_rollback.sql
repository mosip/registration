-- Below scripts are required to rollback from 1.3.0 to 1.3.0-beta.1

\c mosip_regprc

-- ROLLBACK FOR PERFORMANCE OPTIMIZATION INDEXES

DROP INDEX IF EXISTS regprc.idx_bio_regtrn_reqtype;
DROP INDEX IF EXISTS regprc.idx_bio_reqtype_status;
DROP INDEX IF EXISTS regprc.idx_refregtrn_reqtype;
DROP INDEX IF EXISTS regprc.idx_req_batch_id;

DROP INDEX IF EXISTS regprc.idx_abis_resp_id;
DROP INDEX IF EXISTS regprc.idx_abis_response_reqid;

DROP INDEX IF EXISTS regprc.idx_addlinforeq_regid;
DROP INDEX IF EXISTS regprc.idx_addlinforeq_regid_proc_iterdesc;
DROP INDEX IF EXISTS regprc.idx_addlinforeq_reqid;

DROP INDEX IF EXISTS regprc.idx_idemogd_namedobgender_lang_active;
DROP INDEX IF EXISTS regprc.idx_individual_demographic_dedup_regid;

DROP INDEX IF EXISTS regprc.idx_regbio_bio_created;
DROP INDEX IF EXISTS regprc.idx_regbio_regid;
DROP INDEX IF EXISTS regprc.idx_regbio_wf_created;

DROP INDEX IF EXISTS regprc.idx_verification_reqid;
DROP INDEX IF EXISTS regprc.idx_verification_wf;

DROP INDEX IF EXISTS regprc.idx_paused_actionable;
DROP INDEX IF EXISTS regprc.idx_regid_active_not_deleted;
DROP INDEX IF EXISTS regprc.idx_registration_reg_id;
DROP INDEX IF EXISTS regprc.idx_resumable_packets;

DROP INDEX IF EXISTS regprc.idx_additional_info_req_id;
DROP INDEX IF EXISTS regprc.idx_packet_id;
DROP INDEX IF EXISTS regprc.idx_reglist_regid_aireqid_active;
DROP INDEX IF EXISTS regprc.idx_workflow_instance_id;

DROP INDEX IF EXISTS regprc.idx_registration_transaction_status;

ALTER TABLE regprc.abis_request RESET (autovacuum_vacuum_scale_factor, autovacuum_vacuum_threshold, autovacuum_analyze_scale_factor, autovacuum_analyze_threshold);
ALTER TABLE regprc.registration_list RESET (autovacuum_vacuum_scale_factor, autovacuum_vacuum_threshold, autovacuum_analyze_scale_factor, autovacuum_analyze_threshold);
ALTER TABLE regprc.reg_demo_dedupe_list RESET (autovacuum_vacuum_scale_factor, autovacuum_vacuum_threshold, autovacuum_analyze_scale_factor, autovacuum_analyze_threshold);

-- END ROLLBACK FOR PERFORMANCE OPTIMIZATION INDEXES

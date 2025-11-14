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

-- END ROLLBACK FOR PERFORMANCE OPTIMIZATION INDEXES

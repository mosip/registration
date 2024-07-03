\c mosip_regprc

REASSIGN OWNED BY postgres TO sysadmin;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA regprc TO sysadmin;

DROP TABLE IF EXISTS regprc.batch_job_execution CASCADE;
DROP TABLE IF EXISTS regprc.batch_job_execution_context CASCADE;
DROP TABLE IF EXISTS regprc.batch_job_execution_params CASCADE;
DROP TABLE IF EXISTS regprc.batch_job_instance CASCADE;
DROP TABLE IF EXISTS regprc.batch_step_execution CASCADE;
DROP TABLE IF EXISTS regprc.batch_step_execution_context CASCADE;

DROP SEQUENCE BATCH_STEP_EXECUTION_SEQ;
DROP SEQUENCE BATCH_JOB_EXECUTION_SEQ;
DROP SEQUENCE BATCH_JOB_SEQ;
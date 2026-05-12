DROP INDEX IF EXISTS regprc.idx_rgstrnlst_supervisor_status;

ALTER TABLE regprc.registration_list
	DROP COLUMN IF EXISTS supervisor_id;

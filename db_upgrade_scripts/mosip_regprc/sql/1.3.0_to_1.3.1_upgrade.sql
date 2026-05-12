ALTER TABLE regprc.registration_list
	ADD COLUMN IF NOT EXISTS supervisor_id character varying;

COMMENT ON COLUMN regprc.registration_list.supervisor_id IS 'Supervisor ID: ID of the supervisor or admin who approved/rejected the registration packet';

CREATE INDEX IF NOT EXISTS idx_rgstrnlst_supervisor_status
	ON regprc.registration_list (supervisor_id, source, client_status_code)
	WHERE (supervisor_id IS NOT NULL);

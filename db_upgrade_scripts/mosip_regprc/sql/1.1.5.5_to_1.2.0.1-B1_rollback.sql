\c mosip_regprc sysadmin

ALTER TABLE regprc.registration DROP COLUMN IF EXISTS resume_timestamp;
ALTER TABLE regprc.registration DROP COLUMN IF EXISTS default_resume_action;

--------------------------------------------------------------------------------------------
ALTER TABLE regprc.registration DROP COLUMN IF EXISTS resume_remove_tags;
----------------------------------------------------------------------------------------------------

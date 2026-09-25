\c mosip_regprc

INSERT INTO regprc.transaction_type (code, descr, lang_code, is_active, cr_by, cr_dtimes)
VALUES ('CREATE_DRAFT', 'transaction done', 'eng', TRUE, 'MOSIP_SYSTEM', now())
ON CONFLICT (code, lang_code) DO NOTHING;

ALTER TABLE regprc.registration_list ADD COLUMN IF NOT EXISTS packet_meta_data jsonb;
ALTER TABLE regprc.registration_list ADD COLUMN IF NOT EXISTS packet_operations_data jsonb;
ALTER TABLE regprc.registration_list ADD COLUMN IF NOT EXISTS packet_captured_devices jsonb;

COMMENT ON COLUMN regprc.registration_list.packet_meta_data IS 'Packet metaData: JSON array of label/value pairs from packet manager metaInfo metaData attribute, stored so post-ABIS stages can read it without calling Packet Manager.';
COMMENT ON COLUMN regprc.registration_list.packet_operations_data IS 'Packet operationsData: JSON array of label/value pairs from packet manager metaInfo operationsData attribute.';
COMMENT ON COLUMN regprc.registration_list.packet_captured_devices IS 'Packet capturedRegisteredDevices: JSON array of registered capture devices from packet manager metaInfo capturedRegisteredDevices attribute.';
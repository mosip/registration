CREATE TABLE regprc.registration_list(
	workflow_instance_id character varying(36) NOT NULL,
	reg_id character varying(39) NOT NULL,
	process character varying(64),
	packet_checksum character varying(128) NOT NULL DEFAULT '0',
	packet_size bigint NOT NULL DEFAULT 0,
	client_status_code character varying(36),
	client_status_comment character varying(256),
	additional_info bytea,
	status_code character varying(36),
	status_comment character varying(256),
	lang_code character varying(3) NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean DEFAULT FALSE,
	del_dtimes timestamp,
	name character varying,
	phone character varying,
	email character varying,
	center_id character varying,
	registration_date date,
	location_code character varying,
	additional_info_req_id character varying(256),
	packet_id character varying,
	ref_id character varying(512),
	source	character varying,
	CONSTRAINT pk_reglist_id PRIMARY KEY (workflow_instance_id)
);

create index idx_rgstrnlst_pcktid on regprc.registration_list (packet_id);
create index idx_rgstrnlst_aireqid on regprc.registration_list (additional_info_req_id);

CREATE INDEX IF NOT EXISTS idx_reglist_reg_id ON regprc.registration_list USING btree (reg_id);
COMMENT ON TABLE regprc.registration_list IS 'Registration Lists: List of Registration packets details received (to be received) from registration client applications. These details are used to validate the actuall packets received for processing.';
COMMENT ON COLUMN regprc.registration_list.reg_id IS 'Registration ID: ID of the registration request';
COMMENT ON COLUMN regprc.registration_list.packet_checksum IS 'packet Checksum: Checksum value of the packet which id used to validate the packet when actual packate is received for processing';
COMMENT ON COLUMN regprc.registration_list.packet_size IS 'Packet Size: Size of the packate when it is crated at registration client';
COMMENT ON COLUMN regprc.registration_list.client_status_code IS 'Client Status Code: Client status code of the registration packet which is updated berefore sending for registration process';
COMMENT ON COLUMN regprc.registration_list.client_status_comment IS 'Client Status Comment: Client status comment on the registration packet which is updated berefore sending for registration process';
COMMENT ON COLUMN regprc.registration_list.additional_info IS 'Additional Information: Futuristic field to capture any additional information shared by registration client application. The information can be stored in json format as flat structure.';
COMMENT ON COLUMN regprc.registration_list.status_code IS 'Status Code:  Status code of the registration packet which is updated berefore sending for registration process';
COMMENT ON COLUMN regprc.registration_list.status_comment IS 'Status Comment: Status comment on the registration packet which is updated berefore sending for registration process';
COMMENT ON COLUMN regprc.registration_list.lang_code IS 'Language Code : For multilanguage implementation this attribute Refers master.language.code. The value of some of the attributes in current record is stored in this respective language.';
COMMENT ON COLUMN regprc.registration_list.cr_by IS 'Created By : ID or name of the user who create / insert record.';
COMMENT ON COLUMN regprc.registration_list.cr_dtimes IS 'Created DateTimestamp : Date and Timestamp when the record is created/inserted';
COMMENT ON COLUMN regprc.registration_list.upd_by IS 'Updated By : ID or name of the user who update the record with new values';
COMMENT ON COLUMN regprc.registration_list.upd_dtimes IS 'Updated DateTimestamp : Date and Timestamp when any of the fields in the record is updated with new values.';
COMMENT ON COLUMN regprc.registration_list.is_deleted IS 'IS_Deleted : Flag to mark whether the record is Soft deleted.';
COMMENT ON COLUMN regprc.registration_list.del_dtimes IS 'Deleted DateTimestamp : Date and Timestamp when the record is soft deleted with is_deleted=TRUE';

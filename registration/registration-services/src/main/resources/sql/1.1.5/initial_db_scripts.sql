CREATE TABLE "REG"."CA_CERT_STORE"("CERT_ID" VARCHAR(36) NOT NULL, "CERT_SUBJECT" VARCHAR(500) NOT NULL, "CERT_ISSUER" VARCHAR(500) NOT NULL, "ISSUER_ID" VARCHAR(36) NOT NULL, "CERT_NOT_BEFORE" TIMESTAMP, "CERT_NOT_AFTER" TIMESTAMP, "CRL_URI" VARCHAR(120), "CERT_DATA" VARCHAR(3000), "CERT_THUMBPRINT" VARCHAR(100), "CERT_SERIAL_NO" VARCHAR(50),    "PARTNER_DOMAIN" VARCHAR(36), "CR_BY" VARCHAR(256) NOT NULL, "CR_DTIMES" TIMESTAMP NOT NULL, "UPD_BY" VARCHAR(256), "UPD_DTIMES" TIMESTAMP, "IS_DELETED" BOOLEAN, "DEL_DTIMES" TIMESTAMP);

ALTER TABLE "REG"."CA_CERT_STORE" ADD CONSTRAINT "PK_CACS_ID" PRIMARY KEY ("CERT_ID");

ALTER TABLE "REG"."SYNC_JOB_DEF" ADD COLUMN "JOB_TYPE" VARCHAR(128);

INSERT INTO "REG"."GLOBAL_PARAM" VALUES ('mosip.registration.last_software_update','mosip.registration.last_software_update','-','CONFIGURATION','eng',true,'SYSTEM',current timestamp, 'SYSTEM',current timestamp, false, current timestamp);

INSERT INTO "REG"."GLOBAL_PARAM" VALUES ('mosip.registration.regclient_installed_time','mosip.registration.regclient_installed_time',current timestamp,'CONFIGURATION','eng',true,'SYSTEM',current timestamp, 'SYSTEM',current timestamp, false, current timestamp);

UPDATE "REG"."GLOBAL_PARAM" SET val='false' WHERE name='mosip.registration.machinecenterchanged';
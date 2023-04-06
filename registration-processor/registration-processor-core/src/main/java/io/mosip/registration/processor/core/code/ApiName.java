package io.mosip.registration.processor.core.code;

/**
 * The Enum ApiName.
 * 
 * @author Rishabh Keshari
 */
public enum ApiName {

	/** The auth. */
	AUTH,

	/** The authinternal. */
	AUTHINTERNAL,

	/** The master data. */
	MASTER,

	/** The iam. */
	IAM,

	/** The audit. */
	AUDIT,

	/** The ida. */
	IDA,

	/** The regstatus. */
	REGSTATUS,

	/** The regsync. */
	REGSYNC,

	/** The machine history. */
	MACHINEHISTORY,

	/** The center history. */
	CENTERHISTORY,

	/** The center-machine-user history. */
	CENTERUSERMACHINEHISTORY,

	/** The sms notifier. */
	SMSNOTIFIER,

	/** The email notifier. */
	EMAILNOTIFIER,

	/** The biodedupeinsert. */
	BIODEDUPEINSERT,

	/** The biodedupepotential. */
	BIODEDUPEPOTENTIAL,

	/** The biodedupe. */
	BIODEDUPE,

	/** The idrepository. */
	IDREPOSITORY,

	/** The idrepository get id by uin. */
	IDREPOGETIDBYUIN,

	/** The uingenerator. */
	UINGENERATOR,

	/** The idrepodev. */
	IDREPODEV,

	/** The Cryptomaanger *. */
	CRYPTOMANAGERDECRYPT,

	ENCRYPTURL,

	IDAUTHENCRYPTION,

	IDAUTHPUBLICKEY,

	IDAUTHCERTIFICATE,

	/** The ReverseDataSync *. */
	REVERSEDATASYNC,

	/** The Device history *. */
	DEVICESHISTORIES,

	/** The Reg center device history *. */
	REGISTRATIONCENTERDEVICEHISTORY,

	/** The registration center timestamp *. */
	REGISTRATIONCENTERTIMESTAMP,

	/** The registration connector *. */
	REGISTRATIONCONNECTOR,

	/** The fullname. */
	FULLNAME,

	/** The residencestatus. */
	RESIDENCESTATUS,

	/** The phone. */
	PHONE,

	/** The email. */
	EMAIL,

	/** The proofofidentity. */
	PROOFOFIDENTITY,

	/** The proofofaddress. */
	PROOFOFADDRESS,

	/** The proofofrelationship. */
	PROOFOFRELATIONSHIP,

	/** The proofofdateofbirth. */
	PROOFOFDATEOFBIRTH,

	/** The introducerbiometrics. */
	INTRODUCERBIOMETRICS,

	/** The idschemaversion. */
	IDSCHEMAVERSION,

	/** The encryptionservice. */
	ENCRYPTIONSERVICE,

	/** The centerdetails. */
	CENTERDETAILS,

	/** The machinedetails. */
	MACHINEDETAILS,

	/** The external service. */
	EISERVICE,

	/** The retrieveidentity. */
	RETRIEVEIDENTITY,

	/** The retrieveidentity using rid. */
	RETRIEVEIDENTITYFROMRID,

	/** The digitalsignature. */
	DIGITALSIGNATURE,
	
	/** The Language. */
	LANGUAGE,

	/** The Vid creation. */
	CREATEVID,

	/** The user details. */
	USERDETAILS,

	/** get operator rid from id. */
	GETRIDFROMUSERID,

	/** get individualId from userid. */
	GETINDIVIDUALIDFROMUSERID,

	/** The internalauth. */
	INTERNALAUTH,

	/** The templates. */
	TEMPLATES,

	GETUINBYVID,

	DEVICEVALIDATEHISTORY,

	NGINXDMZURL,

	IDSCHEMAURL,

	PACKETMANAGER_SEARCH_FIELD,
	PACKETMANAGER_SEARCH_FIELDS,
	PACKETMANAGER_SEARCH_METAINFO,
	PACKETMANAGER_INFO,
	PACKETMANAGER_VALIDATE,
	PACKETMANAGER_SEARCH_DOCUMENT,
	PACKETMANAGER_SEARCH_BIOMETRICS,
	PACKETMANAGER_SEARCH_AUDITS,
	PACKETMANAGER_UPDATE_TAGS,
	PACKETMANAGER_DELETE_TAGS,
	PACKETMANAGER_GET_TAGS,
	DATASHARECREATEURL,
	PMS,
	PARTNERGETBIOEXTRACTOR,
	DATASHAREGETEURL,
	CREDENTIALREQUEST, GETVIDSBYUIN,
	JWTVERIFY,
	DEVICEHOTLIST,
	IDREPOHASDRAFT,
	IDREPOGETDRAFT,
	IDREPOCREATEDRAFT,
	IDREPOUPDATEDRAFT,
	IDREPOPUBLISHDRAFT,
	IDREPOEXTRACTBIOMETRICS;

}


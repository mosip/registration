# registration-processor-printing-stage

This stage creates a pdf format UIN card and a text format acknowledgement, send it to MOSIP queues where consumers (Printing & Postal Service Provider) can consume them for their uses.

## Design

[Design - Approach for Printing Stage](https://github.com/mosip/registration/blob/master/design/registration-processor/Approach_for_printing_stage.md)

## Default Port and Context Path
  
  * server.port=8099
  * eventbus.port=5722
  * server.servlet.path=/registrationprocessor/v1/print-stage


## URL

 * https://{dns-name}:8099/registrationprocessor/v1/print-stage/swagger-ui.html
 
## Information related to uin card template and placeholders

We need to store templates in master table.
The parameter FileText for a template will be sent as a message in sms or email.

FileText for templates can contain parameters with $ prefix which will be replaced by their actual values present in ID Json file. These variables has the same meaning as in identity object. 

The name of the placeholder depends on two things:
1.> key value used in RegistrationProcessorIdentity.json(mapper json file present in config)
2.> Two languanges present in the id json file.

All values present in mapper json can be used as placeholder with proper languange suffix.

User Case 1: 
There are parameters in id json with single value like phone, postalcode etc.
For them, placeholder can be $FieldName
eg: $phone, $UIN, $RID

User Case 2:
There are parameters with values in two languages like name, gender etc.
Suppose languange codes are eng(English) and ara(Arabic)
For them, placeholders will be like $name_eng for name in english , $ name_ara for name in arabic.

For present mapper json, and languange codes be eng and ara, following placeholders can be used.

	 $name_eng
	 @name_ara
	 $phone
	 $email
	 $dob
	 $age
	 $gender_eng
	 $gender_ara
	 $addressLine1_eng
	 $addressLine1_ara
	 $addressLine2_eng
	 $addressLine2_ara
	 $addressLine3_eng
	 $addressLine3_ara
	 $region_eng
	 $region_ara
	 $province_eng
	 $province_ara
	 $city_eng
	 $city_ara
	 $postalCode
	 $UIN
	 $RID
	 
eg: FileText for UIN Generation SMS notification
"Hi $name_eng,
	Your UIN for the Registration $RID has been successfully generated and will reach soon at your Postal Address."
	
Here $name_eng will be replaced with actual english name, and $RID will be replaced with actual registration id.


## API Dependencies
	
|Dependent Module |  Dependent Services  | API |
| ------------- | ------------- | ------------- |
| commons/kernel  | kernel-auditmanager-service | /audits|
| commons/id-repository | id-repository-identity-service | /uin/{uin} |
| |  | /rid/{rid}|


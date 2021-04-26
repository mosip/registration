#Below are the stage groups:
````
stage group 1:
	regproc-packet-receiver-stage
stage group 2:
	regproc-securezone-notification-stage
	regproc-quality-checker-stage
	regproc-message-sender-stage
stage group 3:
	regproc-bio-dedupe-stage
	regproc-abis-handler-stage
	regproc-abis-middleware-stage
	regproc-manual-verification-stage
stage group 4:
	regproc-biometric-authentication-stage
	regproc-demo-dedupe-stage
stage group 5:
	regproc-packet-validator-stage
	regproc-osi-validator-stage
stage group 6:
	regproc-packet-uploader-stage
	regproc-packet-classifier-stage
stage group 7:
	regproc-uin-generator-stage
    regproc-printing-stage
````

The `regproc-reprocessor-stage` and `regproc-external-stage` are not grouped into any stage group.


# Sample Command to build:
````
sudo docker build -t  registration-processor-stage-group-1:1.2.0-SNAPSHOT .
````

# Sample Command to run:
````
sudo docker run --rm -it -e active_profile_env=dmz -e spring_config_url_env=http://localhost:51000/config -e spring_config_label_env=master  registration-processor-stage-group-1:1.2.0-SNAPSHOT
````
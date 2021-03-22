# Registration Processor 
This repository contains the source code and design documents for MOSIP Registration Processor module. 

# Dependencies
Registration Processor services' dependencies are mentioned below.  For all Kernel services refer to [commons repo](https://github.com/mosip/commons)

* registration-processor-packet-receiver-stage
    * kernel-cryptomanager-service
    * kernel-auditmanager-service
    * kernel-auth-service
    * kernel-config-service

* registration-processor-packet-uploader-stage
    * kernel-cryptomanager-service
    * kernel-auditmanager-service
    * kernel-auth-service
    * kernel-config-service

* registration-processor-packet-validator-stage
    * kernel-masterdata-service
    * id-repository-identity-service
    * kernel-auditmanager-service
    * pre-registration-datasync-service
    * kernel-auth-service
    * kernel-config-service

* registration-processor-osi-validator-stage
    * kernel-masterdata-service
    * kernel-auditmanager-service
    * authentication-internal-service
    * id-repository-identity-service
    * kernel-auth-service
    * kernel-config-service

* registration-processor-manual-verification-stage
    * kernel-masterdata-service
    * kernel-signature-service
    * kernel-auth-service
    * kernel-config-service

* registration-processor-biometric-authentication-stage
    * kernel-auditmanager-service
    * authentication-internal-service
    * id-repository-identity-service
    * kernel-auth-service
    * kernel-config-service

* registration-processor-uin-generator-stage
    * kernel-uingenerator-service
    * kernel-auditmanager-service
    * id-repository-identity-service
    * id-repository-vid-service
    * kernel-auth-service
    * kernel-config-service

* registration-processor-message-sender-stage
    * kernel-masterdata-service
    * kernel-smsnotification-service
    * kernel-emailnotification-service
    * kernel-auditmanager-service
    * id-repository-identity-service
    * kernel-auth-service
    * kernel-config-service

* registration-processor-printing-stage
   * kernel-auditmanager-service
   * id-repository-identity-service
    * kernel-auth-service
    * kernel-config-service

* registration-processor-registration-status-service
    * kernel-cryptomanager-service
    * kernel-signature-service
    * kernel-auth-service
    * kernel-config-service

* registration-processor-request-handler-service
    * kernel-signature-service
    * kernel-ridgenerator-service
    * kernel-masterdata-service
    * id-repository-identity-service
    * kernel-keymanager-service
    * id-repository-vid-service
    * kernel-auth-service
    * kernel-config-service

* registration-processor-demo-dedupe-stage
    * id-repository-identity-service
    * kernel-auth-service
    * kernel-config-service

# Build
The project requires JDK 1.8. 
1. To build jars:
    ```
    $ mvn clean install 
    ```
1. To skip JUnit tests and Java Docs:
    ```
    $ mvn install -DskipTests=true -Dmaven.javadoc.skip=true
    ```
1. To build Docker for a service:
    ```
    $ cd <service/stage folder>
    $ docker build -f Dockerfile
    ```

# Deploy
1. To run all services as Dockers using MiniKube run [sandbox installer](https://githbu.com/mosip/mosip-infra/deployment/sandbox/) scripts.

1. To run a Service jar individually:
    ```
    `java -Dspring.profiles.active=<profile> -Dspring.cloud.config.uri=<config-url> -Dspring.cloud.config.label=<config-label> -jar <jar-name>.jar`
    ```
    Example:  
        _profile_: `env` (extension used on configuration property files*)    
        _config_label_: `master` (git branch of config repo*)  
        _config-url_: `http://localhost:51000` (Url of the config server*)  
	
	\* Refer to [kernel-config-server](https://github.com/mosip/commons/tree/master/kernel/kernel-config-server) for details


1. Note that you will have to run the dependent services like kernel-config-server to run any service successfully.
1. To run a Docker image individually:
    ``` 
    $ docker run -it -p <host-port>:<container-port> -e active_profile_env={profile} -e spring_config_label_env= {branch} -e spring_config_url_env={config_server_url} <docker-registry-IP:docker-registry-port/<dcker-image>`
    ```

# Test
Automated functaionl tests available in [Functional Tests repo](https://github.com/mosip/mosip-functional-tests)

# APIs
API documentation available on Wiki: [Registration Processor APIs](https://github.com/mosip/documentation/wiki/Registration-Processor-APIs)

# Documentation
MOSIP documentation is available on [Wiki](https://github.com/mosip/documentation/wiki)

# License
This project is licensed under the terms of [Mozilla Public License 2.0](https://github.com/mosip/mosip-platform/blob/master/LICENSE)

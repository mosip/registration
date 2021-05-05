# MOSIP Stage Executor

This is the executor application that can load one or more registration processor stages as a group.

## Steps to convert stage as LIB and execute the same:

1) Ceate a POM for stage-group, if not done already. Set the 'stage-groups' as parent in the POM. Add this stage group as module to the stages parent pom.
2) Name the stage group like `registration-processor-stage-group-1` , which will be passed as below VM arg for the stage-group execution command line: `-Dstage-group-name=stage-group-1`
3) Add mosip-stage-executor dependency to the stage group POM, if not done already.
4) Add the stage to the Stage group POM as dependency.
5) Remove the Stage's Springboot build in the POM, make sure it is built as jar (but not springboot).
6) Convert StageBean to configuration bean(`@Configuration`).
7) Add component scans (`@ComponentScans`) as per the stage application.
8) Remove any existing bean definition of the stage bean in any configuration file of the stage.
9) The default stages bean base package is defined in registration-processor-mz.properties as below. This is used in auto discovery of stage beans for the stage group.

````
mosip.regproc.stage-groups.stage-beans-base-packages.default=io.mosip.registration.processor,io.mosip.registrationprocessor
````

If the stage group does not contain any stage bean in the default base packages, add below stage-group specific property to registration-processor-mz.properties
mosip.regproc.stage-groups.stage-beans-base-packages.<stage-groupe-name>=<comma seperated list of stage bean base packages>

For example:

````
mosip.regproc.stage-groups.stage-beans-base-packages.stage-group-1=${mosip.regproc.stage-groups.stage-beans-base-packages.default},my.stage.bean.basepackage
````

10) Update the stage property for server.port, server.servlet.path, eventbus.port and kafka properties defined in step 7.
11) From the Stag's bootstrap properties, copy `server.port`, `server.servlet.path`, `eventbus.port` and kafka properties to **registration-processor.(d)mz.properties**, and apply the stage specific prefix to them.
12) Remove the getPort and getEventbusPort methods from stage bean and implement getPropertyPrefix to return the appropriate prefix used in the stage specific configuration properties.
13) Build the Stage and Stage-Group
14) Run the stage with below command:

````
java -Dapplication.base.url=http://localhost:8090 -Dspring.profiles.active=mz -Dspring.cloud.config.uri=http://localhost:51000/config -Dspring.cloud.config.label=master  -Dstage-group-name=stage-group-1 -jar stage-group-1-1.2.0-SNAPSHOT.jar
````

15) Rename the existing stage Dockerfile to Dockerfile-not-used, so that docker image is not created for the stage as part of github workflow build.

# Note:
1) The mosip-stage-executor also loads the bootstrap.properties. This file should not be removed, as some stage configuration refer to load this file.
2) Add any common dependency of all stage groups to *stage-groups>pom.xml* .
3) Add any common dependency of a specific stage group to *stage-group pom.xml* .


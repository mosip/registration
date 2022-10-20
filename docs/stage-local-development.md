## Steps to run registration processor stages in local development machine:

1. Copy the below content and create a new file called docker-compose.yml 
```
version: "2"

services:
  zookeeper:
    image: docker.io/bitnami/zookeeper:3.7.0-debian-10-r0
    ports:
      - "2181:2181"
    volumes:
      - "zookeeper_data:/bitnami"
    environment:
      - ALLOW_ANONYMOUS_LOGIN=yes
  kafka:
    image: docker.io/bitnami/kafka:2.8.0-debian-10-r0
    ports:
      - "9092:9092"
      - "29092:29092"
    volumes:
      - "kafka_data:/bitnami"
    environment:
      - KAFKA_CFG_ZOOKEEPER_CONNECT=zookeeper:2181
      - ALLOW_PLAINTEXT_LISTENER=yes
      - KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      - KAFKA_LISTENERS=PLAINTEXT://:29092,PLAINTEXT_HOST://0.0.0.0:9092
      - KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
    depends_on:
      - zookeeper
  kafka-ui:
    image: provectuslabs/kafka-ui
    container_name: kafka-ui
    ports:
      - "8080:8080"
    restart: always
    environment:
      - KAFKA_CLUSTERS_0_NAME=local
      - KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS=kafka:29092
      - KAFKA_CLUSTERS_0_ZOOKEEPER=zookeeper:2181
    depends_on:
      - kafka
volumes:
  zookeeper_data:
    driver: local
  kafka_data:
    driver: local

```
2. Use the below command to start the docker compose to run kafka, zookeeper and kafka-ui dockers together. Docker compose and docker should be pre-installed.
```
docker-compose up
```
3. Edit the below property in registration-processor-default.properties file to connect to the kafka running in the docker compose.
```
mosip.regproc.eventbus.kafka.bootstrap.servers=localhost:9092
```
4. Edit registration-processor-mz.properties to update depdendent service URLs, DB, keycloak and relevant credentials to point to a particular cloud environment 
5. Run the kernel-config server pointing to local copy of property files (Refer https://github.com/mosip/mosip-config/tree/develop1-v3#readme)
6. If stages to be run from console
    1. Change directory to any stage group file and update the pom to file to add the relevant stage and comment the remaining stages
    ```
    cd registration-processor/stage-groups/registration-processor-stage-group-2/
    ```
    2. Build the stage group using the below command
    ```
    mvn clean install -Dgpg.skip -DskipTests
    ```
    3. Create a new folder called 'additional-jars' and download the latest kernel adapter jar file to this folder
    4. Run the below command to start the stage
    ```
    java -Dspring.profiles.active=default -Dspring.cloud.config.uri=http://localhost:51000/config -Dloader.path=./additional-jars/ -jar target/registration-processor-stage-group-2-1.2.1-SNAPSHOT.jar
    ```
7. If stages to be run on IDE
    1. Open IDE and configure the entire code base of registration processor
    2. Add the relevant stage as depdendent module to mosip-stage-executor module
    3. Download the latest version of kernel auth adapter jar file and add as jar dependency to mosip-stage-executor module
    4. Edit run configuration of MosipStageExecutorApplication class file of mosip-stage-executor module to add the below VM arguments
    ```
    -Dspring.profiles.active=default -Dspring.cloud.config.uri=http://localhost:51000/config
    ```
    5. Run MosipStageExecutorApplication class file

8. Open http://localhost:8080 in browser to open kafka-ui application and navigate to topics link, open the stage bus-in topic and click on "Produce Message" and add the below JSON message.
```
{
	"reg_type": "NEW",
	"rid": "10002100010001020220928084430",
	"isValid": true,
	"internalError": false,
	"messageBusAddress": {
		"address": "packet-validator-bus-in"
	},
	"retryCount": null,
	"tags": {},
	"lastHopTimestamp": "2022-10-20T10:48:29.355Z",
	"source": null,
	"iteration": 1,
	"workflowInstanceId": "ff3063de-f563-46c7-b529-b07daf1f2fca"
}
```
Ensure the rid and workflowInstanceId is filled in as per a valid packet that is avaialble in the connected cloud environment. Also ensure lastHopTimestamp is the current time.

This step will trigger the message processing in stage.

9. Once the message processing is completed, stage will add another message with status back into the out bus of the stage.

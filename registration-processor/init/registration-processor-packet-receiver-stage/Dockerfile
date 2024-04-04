FROM openjdk:11

#Uncomment below and Comment above line(i.e. FROM openjdk:8) for OS specific (e.g. Alpine OS ) docker base image
#FROM openjdk:8-jdk-alpine

ARG SOURCE
ARG COMMIT_HASH
ARG COMMIT_ID
ARG BUILD_TIME
LABEL source=${SOURCE}
LABEL commit_hash=${COMMIT_HASH}
LABEL commit_id=${COMMIT_ID}
LABEL build_time=${BUILD_TIME}

# setting labels for extra properties needed for this Docker image, so that it can be used at run time with docker inspect
LABEL network=host
LABEL privileged=true
# can be passed during Docker build as build time environment for github branch to pickup configuration from | for spring profiles active and config server URL respectively
ARG spring_config_label  
ARG active_profile 
ARG spring_config_url

# environment variable to pass active profile such as DEV, QA etc at docker runtime
ENV active_profile_env=${active_profile}
# environment variable to pass github branch to pickup configuration from, at docker runtime
ENV spring_config_label_env=${spring_config_label}

ARG container_user=mosip
ARG container_user_group=mosip
ARG container_user_uid=1001
ARG container_user_gid=1001

# change volume to whichever storage directory you want to use for this container.
VOLUME /home/ftp1/ARCHIVE_PACKET_LOCATION /home/ftp1/LANDING_ZONE /home/Glowroot
  
COPY ./target/registration-processor-packet-receiver-stage-*.jar registration-processor-packet-receiver-stage.jar
EXPOSE 8081
EXPOSE 4001

WORKDIR /home/${container_user}

RUN chown -R ${container_user}:${container_user} /home/${container_user}

USER ${container_user_uid}:${container_user_gid}

CMD if [ "$active_profile_env" = "preprod" ]; then \
    wget 'http://13.71.87.138:8040/artifactory/libs-release-local/io/mosip/testing/glowroot.zip' ; \
    apt-get update && apt-get install -y unzip ; \
    unzip glowroot.zip ; \
    rm -rf glowroot.zip ; \
    
    sed -i 's/<service_name>/registration-processor-packet-receiver-stage/g' glowroot/glowroot.properties ; \
    java -jar -javaagent:glowroot/glowroot.jar -Dspring.cloud.config.label="${spring_config_label_env}" -Dspring.profiles.active="${active_profile_env}" -Dspring.cloud.config.uri="${spring_config_url_env}" registration-processor-packet-receiver-stage.jar ; \
    else \
    java -jar -Dspring.cloud.config.label="${spring_config_label_env}" -Dspring.profiles.active="${active_profile_env}" -Dspring.cloud.config.uri="${spring_config_url_env}" registration-processor-packet-receiver-stage.jar ; \
    fi
#CMD ["java","-Dspring.cloud.config.label=${spring_config_label_env}","-Dspring.profiles.active=${active_profile_env}","-Dspring.cloud.config.uri=${spring_config_url_env}","-jar","-javaagent:/home/Glowroot/glowroot.jar","registration-processor-packet-receiver-stage.jar"]

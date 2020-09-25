FROM openjdk:11

#Uncomment below and Comment above line(i.e. FROM openjdk:8) for OS specific (e.g. Alpine OS ) docker base image
#FROM openjdk:8-jdk-alpine

# can be passed during Docker build as build time environment for github branch to pickup configuration from.
ARG spring_config_label

# can be passed during Docker build as build time environment for spring profiles active 
ARG active_profile

# can be passed during Docker build as build time environment for config server URL 
ARG spring_config_url

# can be passed during Docker build as build time environment management rmi server hostname
ARG management_rmi_server_hostname

# can be passed during Docker build as build time environment management rmi server port
ARG management_jmxremote_rmi_port

# environment variable to pass active profile such as DEV, QA etc at docker runtime
ENV active_profile_env=${active_profile}

# environment variable to pass github branch to pickup configuration from, at docker runtime
ENV spring_config_label_env=${spring_config_label}

# environment variable to pass github branch to pickup configuration from, at docker runtime
ENV spring_config_label_env=${spring_config_label}

# environment variable to pass iam_adapter url, at docker runtime
ENV iam_adapter_url_env=${iam_adapter_url}

# change volume to whichever storage directory you want to use for this container.
VOLUME /home/logs /home/Glowroot

COPY ./target/registration-processor-registration-status-service-*.jar registration-processor-registration-status-service.jar

EXPOSE 8083
EXPOSE 4000

CMD if [ "$active_profile_env" = "preprod" ]; then \
    wget 'http://13.71.87.138:8040/artifactory/libs-release-local/io/mosip/testing/glowroot.zip' ; \
     wget "${iam_adapter_url_env}" -O kernel-auth-adapter.jar; \
    java -Dloader.path=./kernel-auth-adapter.jar -jar -Dspring.cloud.config.label="${spring_config_label_env}" -Dspring.profiles.active="${active_profile_env}"  -Dspring.cloud.config.uri="${spring_config_url_env}" registration-processor-registration-status-service.jar; \    
    apt-get update && apt-get install -y unzip ; \
    unzip glowroot.zip ; \
    rm -rf glowroot.zip ; \
    
    sed -i 's/<service_name>/registration-processor-registration-status-service/g' glowroot/glowroot.properties ; \
    java -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:MaxRAMFraction=1 -XX:+HeapDumpOnOutOfMemoryError -XX:+UseG1GC -XX:+UseStringDeduplication -jar -javaagent:glowroot/glowroot.jar -Dspring.cloud.config.label="${spring_config_label_env}" -Dspring.profiles.active="${active_profile_env}" -Dspring.cloud.config.uri="${spring_config_url_env}" registration-processor-registration-status-service.jar ; \
   else \
   wget "${iam_adapter_url_env}" -O kernel-auth-adapter.jar; \
    java -Dloader.path=./kernel-auth-adapter.jar -jar -Dspring.cloud.config.label="${spring_config_label_env}" -Dspring.profiles.active="${active_profile_env}"  -Dspring.cloud.config.uri="${spring_config_url_env}" registration-processor-registration-status-service.jar; \   
   fi

#CMD ["java","-Dspring.cloud.config.label=${spring_config_label_env}","-Dspring.profiles.active=${active_profile_env}","-Dspring.cloud.config.uri=${spring_config_url_env}","-jar","-javaagent:/home/Glowroot/glowroot.jar","registration-processor-registration-status-service.jar"]

FROM openjdk:11

#Uncomment below and Comment above line(i.e. FROM openjdk:8) for OS specific (e.g. Alpine OS ) docker base image
#FROM openjdk:8-jdk-alpine

# can be passed during Docker build as build time environment for github branch to pickup configuration from.
ARG spring_config_label

# can be passed during Docker build as build time environment for spring profiles active 
ARG active_profile

# can be passed during Docker build as build time environment for config server URL 
ARG spring_config_url

# can be passed during Docker build as build time environment for glowroot 
ARG is_glowroot

# can be passed during Docker build as build time environment for artifactory URL
ARG artifactory_url

# environment variable to pass active profile such as DEV, QA etc at docker runtime
ENV active_profile_env=${active_profile}

# environment variable to pass github branch to pickup configuration from, at docker runtime
ENV spring_config_label_env=${spring_config_label}

# environment variable to pass github branch to pickup configuration from, at docker runtime
ENV spring_config_label_env=${spring_config_label}

# environment variable to pass glowroot, at docker runtime
ENV is_glowroot_env=${is_glowroot}

# environment variable to pass artifactory url, at docker runtime
ENV artifactory_url_env=${artifactory_url}

# change volume to whichever storage directory you want to use for this container.
VOLUME /home/logs

COPY ./target/registration-processor-reprocessor-stage-*.jar registration-processor-reprocessor-stage.jar

#Below 4 lines is added only as a temporary fix to downloaded the ceylon dependencies for chime scheduler
#later this chime to be replaced with something else
CMD apt-get update && apt-get install -y unzip ; \
    wget "${artifactory_url_env}"/artifactory/libs-release-local/io/mosip/testing/regproc-reprocessor-ceylon-cache-repo.zip ; \
    unzip regproc-reprocessor-ceylon-cache-repo.zip ; \
    rm -rf regproc-reprocessor-ceylon-cache-repo.zip ; \

    if [ "$is_glowroot_env" = "present" ]; then \
    wget "${artifactory_url_env}"/artifactory/libs-release-local/io/mosip/testing/glowroot.zip ; \
    unzip glowroot.zip ; \
    rm -rf glowroot.zip ; \
    
    sed -i 's/<service_name>/registration-processor-reprocessor-stage/g' glowroot/glowroot.properties ; \
    java -jar -javaagent:glowroot/glowroot.jar -Dspring.cloud.config.label="${spring_config_label_env}" -Dspring.profiles.active="${active_profile_env}" -Dspring.cloud.config.uri="${spring_config_url_env}" -Dceylon.cache.repo=./regproc-reprocessor-ceylon-cache-repo registration-processor-reprocessor-stage.jar ; \
    else \
    java -jar -Dspring.cloud.config.label="${spring_config_label_env}" -Dspring.profiles.active="${active_profile_env}" -Dspring.cloud.config.uri="${spring_config_url_env}" -Dceylon.cache.repo=./regproc-reprocessor-ceylon-cache-repo registration-processor-reprocessor-stage.jar ; \
    fi

#CMD ["java","-Dspring.cloud.config.label=${spring_config_label_env}","-Dspring.profiles.active=${active_profile_env}","-Dspring.cloud.config.uri=${spring_config_url_env}","-jar","registration-processor-reprocessor-stage.jar"]

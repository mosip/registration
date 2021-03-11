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

# can be passed during Docker build as build time environment for artifactory URL
ARG application_base_url

# environment variable to pass active profile such as DEV, QA etc at docker runtime
ENV active_profile_env=${active_profile}

# environment variable to pass github branch to pickup configuration from, at docker runtime
ENV spring_config_label_env=${spring_config_label}

# environment variable to pass glowroot, at docker runtime
ENV is_glowroot_env=${is_glowroot}

# environment variable to pass artifactory url, at docker runtime
ENV artifactory_url_env=${artifactory_url}

# environment variable to pass artifactory url, at docker runtime
ENV application_base_url=${application_base_url}

# change volume to whichever storage directory you want to use for this container.
VOLUME /home/logs

ADD additional_properties additional_properties

ADD additional_jars additional_jars

ADD mosip-stage-executor.jar mosip-stage-executor.jar

CMD java -Dapplication.base.url="${application_base_url}" \
  -Dspring.profiles.active="${active_profile_env}" \
  -Dspring.cloud.config.uri="${spring_config_url_env}" \
  -Dspring.cloud.config.label="${spring_config_label_env}" \
  -classpath "mosip-stage-executor.jar;.\additional_jars\*" -jar mosip-stage-executor.jar
FROM openjdk:8

RUN apt-get update

RUN apt install -y zip nginx

ARG client_version

ARG crypto_key

ARG healthcheck_url

ARG client_upgrade_server

ARG cert_server_URL

ARG tpm_enabled

ARG db_bootpwd

ENV client_version_env=${client_version}

ENV crypto_key_env=${crypto_key}

ENV healthcheck_url_env=${healthcheck_url}

ENV client_upgrade_server_env=${client_upgrade_server}

ENV client_repo_env=${client_repo_url}

ENV client_certificate_env=${client_certificate}

ENV tpm_enabled_env=${tpm_enabled}

ENV db_bootpwd_env=${db_bootpwd}

ADD registration-client/target /registration-client/target

ADD registration-libs/target /registration-libs/target

ADD registration-libs/src/main/resources registration-libs/resources

ADD configure.sh configure.sh

RUN chmod a+x configure.sh \
 && rm -f /registration-client/target/registration-client-*-javadoc.jar \
 && rm -f  /registration-client/target/registration-client-*-sources.jar \
 && rm -f  /registration-libs/target/registration-libs-*-javadoc.jar \
 && rm  -f /registration-libs/target/registration-libs-*-sources.jar

ENTRYPOINT ["./configure.sh" ]

#CMD [ "${client_version_env}", "${crypto_key_env}", "${db_bootpwd_env}", "${tpm_enabled_env}", "${client_certificate_env}", "${client_upgrade_server_env}" ]

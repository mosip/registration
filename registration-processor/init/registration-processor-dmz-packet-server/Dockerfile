FROM nginx


ARG SOURCE
ARG COMMIT_HASH
ARG COMMIT_ID
ARG BUILD_TIME
LABEL source=${SOURCE}
LABEL commit_hash=${COMMIT_HASH}
LABEL commit_id=${COMMIT_ID}
LABEL build_time=${BUILD_TIME}

ARG container_user=mosip
ARG container_user_group=mosip
ARG container_user_uid=1001
ARG container_user_gid=1001

WORKDIR /home/${container_user}

RUN chown -R ${container_user}:${container_user} /home/${container_user}

VOLUME /home/mosip
COPY nginx.conf /etc/nginx/nginx.conf
COPY healthcheck.txt /home/mosip/landing/healthcheck.txt
EXPOSE 8082

USER ${container_user_uid}:${container_user_gid}

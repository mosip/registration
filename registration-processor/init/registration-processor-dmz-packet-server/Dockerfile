FROM nginx

ARG container_user=mosip
ARG container_user_group=mosip
ARG container_user_uid=1001
ARG container_user_gid=1001

WORKDIR /home/${container_user}

RUN chown -R ${container_user}:${container_user} /home/${container_user}\


USER ${container_user_uid}:${container_user_gid}


VOLUME /home/mosip
COPY nginx.conf /etc/nginx/nginx.conf
COPY healthcheck.txt /home/mosip/landing/healthcheck.txt
EXPOSE 8082

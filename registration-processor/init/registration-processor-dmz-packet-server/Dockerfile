FROM nginx

ARG SOURCE
ARG COMMIT_HASH
ARG COMMIT_ID
ARG BUILD_TIME
LABEL source=${SOURCE}
LABEL commit_hash=${COMMIT_HASH}
LABEL commit_id=${COMMIT_ID}
LABEL build_time=${BUILD_TIME}

VOLUME /home/mosip

COPY nginx.conf /etc/nginx/nginx.conf

COPY healthcheck.txt /home/mosip/landing/healthcheck.txt

EXPOSE 8082

CMD ["nginx", "-g", "daemon off;"]

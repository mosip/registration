FROM nginx

ARG SOURCE
ARG COMMIT_HASH
ARG COMMIT_ID
ARG BUILD_TIME
LABEL source=${SOURCE}
LABEL commit_hash=${COMMIT_HASH}
LABEL commit_id=${COMMIT_ID}
LABEL build_time=${BUILD_TIME}

# Define arguments for user and group
ARG container_user=mosip
ARG container_user_group=mosip
ARG container_user_uid=1001
ARG container_user_gid=1001

# Expose port 8082
EXPOSE 8082

# Set working directory
WORKDIR /home/${container_user}

# Copy nginx configuration file
COPY nginx.conf /etc/nginx/nginx.conf

# Ensure the user and group exist, copy files, set permissions
RUN groupadd -g ${container_user_gid} ${container_user_group} \
    && useradd -u ${container_user_uid} -g ${container_user_group} -m ${container_user} \
    && mkdir -p /home/mosip/landing/ \
    && cp /home/mosip/landing/healthcheck.txt /home/mosip/landing/healthcheck.txt.bak \
    && chown -R ${container_user}:${container_user} /home/${container_user} \
    && mv /etc/nginx/nginx.conf /etc/nginx/nginx.conf.bak \
    && mv /home/mosip/landing/healthcheck.txt /home/mosip/landing/healthcheck.txt.bak

# Start Nginx with daemon off
CMD ["nginx", "-g", "daemon off;"]

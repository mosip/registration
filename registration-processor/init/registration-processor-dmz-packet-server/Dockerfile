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

# Copy nginx configuration file
COPY nginx.conf /etc/nginx/nginx.conf

# Copy healthcheck.txt to the specified directory
COPY healthcheck.txt /home/mosip/landing/healthcheck.txt

# Expose port 8082
EXPOSE 8082

# Set working directory and change ownership to the specified user
WORKDIR /home/${container_user}
RUN chown -R ${container_user}:${container_user} /home/${container_user}

# Switch to the specified user
USER ${container_user_uid}:${container_user_gid}

# Start Nginx with daemon off
CMD ["nginx", "-g", "daemon off;"]

FROM nginx

ARG SOURCE
ARG COMMIT_HASH
ARG COMMIT_ID
ARG BUILD_TIME
LABEL source=${SOURCE}
LABEL commit_hash=${COMMIT_HASH}
LABEL commit_id=${COMMIT_ID}
LABEL build_time=${BUILD_TIME}

# Define arguments for container user
ARG container_user=mosip
ARG container_user_group=mosip
ARG container_user_uid=1001
ARG container_user_gid=1001

# Set working directory and change ownership to container user
WORKDIR /home/${container_user}
RUN chown -R ${container_user}:${container_user} /home/${container_user}

# Switch to non-root user
USER ${container_user_uid}:${container_user_gid}

# Copy nginx configuration and healthcheck file
COPY nginx.conf /etc/nginx/nginx.conf
COPY healthcheck.txt /home/mosip/landing/healthcheck.txt

# Expose port
EXPOSE 8082

# Start nginx
CMD ["nginx", "-g", "daemon off;"]


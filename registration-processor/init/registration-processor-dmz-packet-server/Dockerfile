FROM nginx

# Define arguments
ARG container_user=mosip
ARG container_user_group=mosip
ARG container_user_uid=1001
ARG container_user_gid=1001

# Create a user and group inside the container
RUN addgroup --gid ${container_user_gid} ${container_user_group} && \
    adduser --disabled-password --gecos '' --uid ${container_user_uid} --gid ${container_user_gid} ${container_user}

# Set the working directory
WORKDIR /home/${container_user}

# Copy nginx.conf file
COPY nginx.conf /etc/nginx/nginx.conf

# Copy healthcheck.txt file
COPY healthcheck.txt /home/${container_user}/landing/healthcheck.txt

# Change ownership of /home/mosip directory
RUN chown -R ${container_user}:${container_user} /home/${container_user}

# Change user and group to the created user and group
USER ${container_user_uid}:${container_user_gid}

# Expose port 8082
EXPOSE 8082

# Run nginx
VOLUME /home/mosip

COPY nginx.conf /etc/nginx/nginx.conf
  
COPY healthcheck.txt /home/mosip/landing/healthcheck.txt

EXPOSE 8082

CMD ["nginx", "-g", "daemon off;"]

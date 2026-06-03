#!/bin/bash

#installs the pre-requisites.
set -e

echo "Downloading pre-requisites install scripts"

wget "${iam_adapter_url_env}" -O "${loader_path_env}"/kernel-auth-adapter.jar; \
wget --no-parent "${artifactory_url_env}"/"${regproc_jars_env}" --directory-prefix "${loader_path_env}"/ ; \

echo "Installating pre-requisites completed."

exec "$@"
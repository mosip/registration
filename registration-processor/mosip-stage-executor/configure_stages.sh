#!/bin/bash

#installs the pre-requisites.
set -e

# return executable jar path
get_exec_file() {
  local exec_jar=$(find -name '*.jar' -type f | grep -Ev "javadoc|sources")
  echo $(realpath $exec_jar)
}

# Loop through all stage folders
for stage_dir in ../stage_groups/*; do
  # get the stage folder name
  stage_name=$(basename $stage_dir)

  # create applicational properties folder
  mkdir $stage_dir/additional_properties

  # create additional jars folder
  mkdir $stage_dir/additional_jars

  # backup of old delimiter
  old_ifs=$IFS

  # copy stage executor jar into stage folder
  cp $(get_exec_file) $stage_dir/mosip-stage-executor.jar

  # read first line of .env file
  stages=$(head $stage_dir/build.properties)

  # split stages based on =
  IFS='=' read -r -a stage_list <<<$stages

  # split stages based on comma
  IFS=',' read -r -a stage_list <<<"${stage_list[@]:1}"

  # assigning old delimiter back
  IFS=$old_ifs

  # Looping through all stages provided in build.properties
  # and copying the properties into stage folder
  for i in "${stage_list[@]:0}"; do
    file=../pre-processor/$i/src/main/resources/$i.properties
    cd ../pre-processor/$i
    if [ -e "$file" ]; then
      cp ./src/main/resources/$i.properties ../$stage_dir/additional_properties
    else
      echo $i "property file not found"
    fi
    if [ -e "$jar_file"]; then
      cp $(get_exec_file) ../$stage_dir/additional_jars
    else
      echo $i "jar file not found"
    fi
  done
done
# Sample Command to build:
````
sudo docker build -t stage-group-1:1.2.0-SNAPSHOT --build-arg stage_group_jar_name=stage-group-1 --build-arg stage_group_name=stage-group-1 .
````

# Sample Command to run:
````
sudo docker run --rm -it -e active_profile_env=mz -e spring_config_url_env=http://localhost:51000/config -e spring_config_label_env=master  stage-group-1:1.2.0-SNAPSHOT
````
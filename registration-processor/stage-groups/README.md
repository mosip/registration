# Sample Command to build:
````
sudo docker build -t  my-stage-group:1.2.0-SNAPSHOT .
````

# Sample Command to run:
````
sudo docker run --rm -it -e active_profile_env=mz -e spring_config_url_env=http://localhost:51000/config -e spring_config_label_env=master  my-stage-group:1.2.0-SNAPSHOT
````
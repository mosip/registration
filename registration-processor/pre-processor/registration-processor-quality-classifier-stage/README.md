# Quality Classifier Stage

## About
 Quality classifier stage performs the following functions:
* Checks for multiple packets against same application id.
* Notifies packet uploader stage to pick the packets from landing zone.
* Optionally, validates quality of biometrics using [biometric SDK service]().

## Default context-path and port
Refer [`bootstrap.properties`](src/main/resources/bootstrap.properties)


# CMD Validator Stage

## About
CMD stands for Center, Machine, Device.  Validates the following: 
* Presence of location coordinates of registration center (does not validate the actual center coordinates).
* Match of center and machine id against master data db.  
* Packet creation time within centre's working hours.
* Trust validation of devices

## Default context-path and port
Refer [`bootstrap.properties`](src/main/resources/bootstrap.properties)


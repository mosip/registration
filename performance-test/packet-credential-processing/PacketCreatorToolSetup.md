# PacketCreator Setup Guide

PacketCreator is a support tool developed to generate simulated identity packets for the **MOSIP Registration Processor** module to consume.

The Jmeter scripts uses this tool to generate new packets that can later be synced and uploaded via Registration Processor APIs.

This document guides users through the installation and setup of the Packet Creator tool for the use of  *`PacketCreator_and_Upload_Test_Script.jmx`*  and *`Regproc_Syncdata_Test_Script.jmx`* scripts.

---

## 1. PacketCreator Installation

### 1.1 Clone the Repository

1. Go to the repository: [https://github.com/mosip/mosip-automation-tests](https://github.com/mosip/mosip-automation-tests)
2. Clone the  repository to your local machine.
    
### 1.2: Build the Code

1. Navigate to the cloned repository on your local machine.
2. Open a terminal (or Git Bash) and run the following Maven commands to build the code (Java Version 11 may be required ):
```
mvn clean install -Dgpg.skip
```
3. This will build the project and install the necessary dependencies.
4. After the successful build, you will get the necessary JAR file for the PacketCreator within "target" folder:
dslrig-packetcreator-<x.x.x.x>.jar (tested with version 1.3.0.1)
5. The Packet Utility is used to create and upload packets for end-to-end automation/performance testing.

### 1.3: Install Packet Creator Utility

1. Download the 'centralized' folder from src/main/resources/dockersupport and save it to a new path.
2. Place the  dslrig-packetcreator-<x.x.x.x>.jar in the same new path.


## 2. Packet Creator Setup

### 2.1: Configure the executable file
1. Within the newly created installation folder, Open run_centralized_packet_creator.bat in text editor
2. edit the file paths and filenames as per the new installation
```
    Eg: java -Xss8m  -Dfile.encoding=UTF-8 -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=9999,suspend=n -jar <<dslrig-packetcreator-versionxx>>.jar --spring.config.location=file:///D:\centralized\mosip-packet-creator\config\application.properties>>D:\centralized\mosip-packet-creator\PacketUtilityRunlog.txt
```

### 2.2: Configuration
Edit the `application.properties` file in the `config` directory. Key settings include:
* mountPath=/path/to/mountVolume
* authCertsPath=/path/to/authCerts
* user.id=<User_ID> (Master data)
* machine.id=<Machine_ID> (Master data)
* center.id=<Center_ID> (Master data)

Ensure all paths and keys match the specific environment.

### 2.3: Starting the Packet Creator
1. Execute run_centralized_packet_creator.bat
2. Verify the Packet Creator Tool is running, visit:
    http://localhost:8080/v1/packetcreator/swagger-ui.html#/

Note: *`>> /path/to/PacketUtilityRunlog.txt`* is optional logging command.


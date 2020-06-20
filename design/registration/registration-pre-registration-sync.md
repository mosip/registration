# Design - Pre-Registration Sync

## Background
As part of the registration process, the resident can provide the pre-registration ID so that the system can fetch the resident's details from the pre-registration application. As part of the sync process the operator can also trigger retrival of the pre-registration IDs and the subsequent pre-registration packets for their center from the pre-registration application.

## Target Users
- System (Registration Client) using the Pre-registration ID
- Manual trigger by Operator (Officer/Supervisor)

## Key Requirements
- As part of the registration process, the resident can provide the pre-registration id to fetch the details.
    - Enter the pre-registration ID in the new registration screen
    - System should verifies if the pre-registration ID is present in the DB
    - If the data is not available and system is online, system should fetch the resident's details from pre-registration application:
        - System should get the pre-registration service URL which was received as part of the admin configuration
        - System should fetch the pre-registration ID call to get the pre-registration ID's as a response and save the list in the DB
        - System should fetch the each packet \[ZIP\] file WRT the pre-registration ID and save to desire location and same thing should be updated to the DB
        - While saving the pre-registration packet, the system need to encrypt using the symmetric key and save the packet
        - System should fetch the data based on the pre-registration ID and populate the same in the registration screen
        - Resident should be able to modify the data and save the information
        - Once the registration ID is generated, the system need to delete the pre-registration packet and updated the status in DB
    - If the data is not available and system is offline, system should display an alert, "**The Details are not available**".
    - If the data is not available in pre-registration application, system should display an alert, "**The Details are not available**".
- As part of manual trigger by operator to download the pre-registration data for the center,
    - System should check if it is online
    - System should get the pre-registration service URL which was received as part of the admin configuration
    - System should fetch the list of pre-registration ID's as a response and save the list in the DB
    - System should fetch the each packet \[ZIP\] file WRT the Pre-Registration ID and save to desire location and same thing should be updated to the DB
    - While saving the pre-registration packet, the system need to encrypt using the symmetric key and save the packet

## Key Non-Functional Requirements
- Security:
    - We should not store the operator's plain text credentials or any sensitive information
    - The password should be not stored as raw data. It should be stored in hashed format
    - The session key should be stored in the DB for each pre-registration packet
    - The data resided in the database should be encrypted
- Network:
    - URL should be communicated using the SSL mode
    - As a security measures the UIN or any sensitive individual information should not be logged
- Other standard NFR, need to be taken care:
    - Logging, audit, exception handling

## Solution

1. Create the **PreRegistrtaionIDSyncher** with following methods as:
    - List\<String\> getPreRegIDs - ( centerID, fromDate,toDate)
    - PreregistrtaionEntity getPreRegId - (preRegID).
2. Call the configured URL to the Pre-registration system to get the list of ID's.
3. Call the Kernel API to get the Symmetric session key for each packet and encrypt using it.
4. Encrypt the pre-registration packet raw data and Save it to the desired location \[which is configured\].
5. Call the PreRegistrtaionDAO with the follows methods to do the operation WRT the packet.
    - save(Preregistrtaion entity)
    - update(Preregistrtaion entity)
    - find(String preRegId) \[We need to validate against the status\]
6. Call the PreregistrtaionSyncRepositry to persist to the data into the DB.
    - write the respective queries using the JPA query language.

## Class Diagram

![Pre-Registrtaion Sync Class Diagram](_images/PreRegistrationSyncClassDgm.png)

## Sequence Diagram

![Pre-Registrtaion Sync Seq Diagram](_images/PreRegistrationSeqDgm.png)

# Design - Registration User Role Sync

## Background
As part of the application launch, the user information along with the roles should be loaded before launch of the application. The user related information consists of user names, passwords, roles, process names, method sequence and priority information. These all information will be downloaded from the server to client using sync.

## Target Users
- User Role Sync Server
- Operator (Officer/Supervisor)
- System (Registration Client Application)

## Key Requirements
- Server sends only the user information of users mapped to the specific Registration Center that the machine belongs to
- Data of existing users in the Registration Client is updated and data of new users is added

## Key Non-Functional Requirements
- Server calls should be https mode
	
## Solution
1. Registration user mapping information should be downloaded from server from client.
	- Frequency of execution – configurable based on the job frequency [Automatic], "Sync Data" menu will be listed in the Menu section provided to the user
2. The configuration information should be fetched from the SyncJOBDef table
3. While downloading the information based on the center ID, the user information details should be pulled
4. The user information should pick from the user detail tables. User role table will be used to store the user roles table

## Sequence and Class Diagram

![Registration user role sync Sequence diagram](_images/reg_center_user_role_config_sync.png)

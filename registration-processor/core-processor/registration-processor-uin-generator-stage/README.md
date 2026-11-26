# UIN Generator Stage

> **Note:** This stage is no longer used in the registration processor pipeline. The create/update draft functionality now runs in the [Create Draft stage](../../pre-processor/registration-processor-create-draft-stage). See [Create Draft Stage Flow](../../../docs/create-draft-stage-flow.md) for details.

## About
* Decides what packet data should be sent to ID Repo based on ID schema.
* Fetches unused UIN from ID Generator service and associates it with above data.
* Creates draft version of above data in ID Repo.

## Default context, path, port
Refert to [bootstrap properties](src/main/resources/bootstrap.properties)

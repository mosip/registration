# Update Packet With Unregistered Biometric Fix — Design

## Overview
This document describes the design changes implemented in the MOSIP Registration Processor to correctly handle **update packets** where the biometric data is **not found in ABIS** (i.e., no biometric match exists for the UIN holder).

This fix enhances the decision-making logic in the **Bio-Dedupe stage**, preventing unauthorized biometric updates and enabling correct fallback handling for **infant** and **biometric-exception** scenarios.

---

## Approach Flow

### 1. Identify Update Packet
The system first determines whether the incoming packet is an **update** packet by checking the registration type.

---

### 2. Biometric Match Check
In the Bio-Dedupe stage, biometrics extracted from the update packet are checked against ABIS.  
If **no match is found**, the system considers the update biometric as **unregistered** and applies fallback logic.

---

### 3. Check for Infant Scenario
The system checks whether the applicant was an **infant** during the last interaction with MOSIP (registration or previous update).

If the applicant qualifies as an infant, biometric match in ABIS is **not required**.

---

### 3.1 Infant Fallback Logic
If the last interaction occurred while the applicant was still an infant, the system **allows biometric update**.

To determine this:

---

### Step 1 — Fetch Date of Birth (DOB)
- Retrieve **date of birth** from the ID Repository for the UIN.

---

### Step 2 — Determine Applicant's Last Interaction with MOSIP
Age is calculated based on the last interaction date.  
The system determines this using the following fallback sequence:

---

#### **Fallback Order to Determine Last Interaction Timestamp**

##### a) Use `packetCreatedOn` field from `/idvid` API
If **_packetCreatedOn_** field available in the `/idvid` response, use it directly.  
**_packetCreatedOn_** will be updated to ID Repository during the NEW and UPDATE operations. This field will be null only for legacy UINs created before the MOSIP version that introduced this field.

---

##### b) If not available, use `/idvid-metadata/search` API
This API returns metadata for the provided UIN such as:
- `registrationId` : The registrationId associated with the latest interaction of the UIN.
- `createdOn` : DateTime when the UIN was created.
- `updatedOn` : DateTime when the UIN was last updated.

Using these, the system performs:

1. **Fetch packetId using registrationId from registration_list table**
   - Query `registration_list` using registrationId to obtain the packetId.
   - **_packetId_** is used for obtaining the **_packetCreatedOn_** as it contains the packet creation timestamp. Example: _10018301560378920240729070828-10219_10200-**20240729070828**_. Here last 14 digits represent the packetCreatedOn in **yyyyMMddHHmmss**.  
     
2. **If unable to find packetCreatedOn from packetId, derive the same from registrationId**
   - **_registrationId_** is used to determine the last interaction date. Example: _100183015603789**20240729070828**_ where last 14 digits represent the packetCreatedOn in **yyyyMMddHHmmss**.

3. **If unable to obtain from registrationId, approximate using createdOn/updatedOn**  
   **_createdOn_** or **_updatedOn_** are used to determine the last interaction. As the createdOn and updatedOn are the time when the UIN is created or updated respectively, includes the processing time taken for the packet i.e. equals to **packetCreatedOn** + **processing time**. In order to approximately determine the last interaction, following configuration is introduced:<br><br>
   **_registration.processor.expected-packet-processing-duration_** : This configuration holds the expected maximum duration taken for processing a packet in hours. This value is subtracted from createdOn or updatedOn to approximately determine the last interaction date. By default, this value is set to 0. The approximate packetCreatedOn is determined as follows:<br>
   ```packetCreatedOn = createdOn/updatedOn - expected-packet-processing-duration (in hours)```<br><br>
   **Note :** This property is kept for considering the time taken for processing the packet as createdOn and updatedOn includes that time. Country can configure this property based on their requirements.
---

### Step 3 — Calculate Age at Last Interaction
```age = packetCreatedOn - dateOfBirth```

---

### Step 4 — Compare Against Effective Age Limit
If the calculated age is less than the effective age limit, the applicant is considered an infant during last interaction. The effective age limit is obtained as given below:<br>
```effective age limit = configured age limit (mosip.kernel.applicant.type.age.limit) + age limit buffer (registration.processor.applicant.type.age.limit.buffer)```<br><br>
**Note :** **_registration.processor.applicant.type.age.limit.buffer_** is introduced to provide some buffer over the configured age limit to handle edge case scenarios in age calculation. Increasing this value provides a safety margin for age calculations near the eligibility boundary. By default, this value is set to 0.

---

## 4. Biometric Exception Fallback
If the applicant was **not an infant**, the system checks if **all biometrics are marked as exception**, based on CBEFF biometric data stored in ID Repository.

If **all biometrics are exception**, the packet is forwarded to **Manual Verification (MV)**.  
MV becomes the final decision-maker for the update.

---

## 5. Non-Infant and Not All Biometric Exception Scenario
If:
- The applicant was **not an infant**
- The applicant does **not** have all biometrics as exception
- ABIS returns **no match**

Then the following configuration controls the behavior:

**_mosip.regproc.bio.dedupe.non-infant-not-all-biometric-exception-decision_**

Accepted values:
- `REJECTED`
- `MANUAL_VERIFICATION`

Default: **REJECTED**

- `REJECTED` → update packet is rejected.
- `MANUAL_VERIFICATION` → packet sent to MV stage.

---

## 6. Biometric Match Found
If ABIS returns a **successful match**, the system proceeds with the **normal update flow**.

---

## Notes

1. If `packetCreatedOn` cannot be determined, the system throws **_PacketDateComputationException_** and sends the packet to the MV stage for manual verification.
2. If a `BiometricClassificationException` occurs during biometric exception evaluation, the packet is forwarded to MV.

---

## Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant RC as Registration Client
    participant RP as Biodedupe Stage <Br> (Registration Processor)
    participant ABIS as ABIS
    participant IDR as ID Repository
    participant MV as Manual Verification

    RC->>RP: Submit Update Packet
    RP->>RP: Identify registrationType == UPDATE

    RP->>RP: Execute Bio-Dedupe Stage
    RP->>ABIS: Perform biometric match
    ABIS-->>RP: No Match Found

    RP->>RP: Check Infant Scenario

    RP->>IDR: Fetch DOB
    IDR-->>RP: Date of Birth

    RP->>IDR: /idvid (Fetch packetCreatedOn)
    alt packetCreatedOn available
        IDR-->>RP: packetCreatedOn
    else packetCreatedOn not available
        RP->>IDR: /idvid-metadata/search
        IDR-->>RP: registrationId, createdOn, updatedOn

        RP->>RP: Query registration_list using registrationId
        alt packetId found
            RP->>RP: Derive packetCreatedOn from packetId
        else packetId not found
            RP->>RP: Derive packetCreatedOn from registrationId
        end

        alt Still not determined
            RP->>RP: Approximate packetCreatedOn <Br> (createdOn/updatedOn - expected duration)
        end
    end

    alt packetCreatedOn cannot be determined
        RP->>MV: Send to Manual Verification <BR> (PacketDateComputationException)
    end

    RP->>RP: Calculate age at last interaction
    RP->>RP: Compare with effective age limit

    alt Applicant was Infant
        RP->>RP: Allow biometric update
        RP->>RP: Continue normal update flow
    else Applicant not Infant
        RP->>IDR: Fetch biometric exception status (CBEFF)
        alt All biometrics are exception
            RP->>MV: Send to Manual Verification
        else Not all biometrics exception
            RP->>RP: Read config <Br> mosip.regproc.bio.dedupe.non-infant-not-all-biometric-exception-decision
            alt Decision = REJECTED
                RP-->>RC: Reject update packet
            else Decision = MANUAL_VERIFICATION
                RP->>MV: Send to Manual Verification
            end
        end
    end

    alt ABIS Match Found
        ABIS-->>RP: Match Success
        RP->>RP: Proceed with normal update flow
    end


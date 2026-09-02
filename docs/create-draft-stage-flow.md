# Create Draft Stage Flow

## Overview

Draft creation is moved earlier in the pipeline. A dedicated **Create Draft** stage (stage-group-2) creates and populates the ID Repository draft **before ABIS**. Post-ABIS stages then use that draft instead of Packet Manager. **UIN Generator is no longer deployed** in stage-group-7.

Camel route XML lives in the config server (`mosip-config`), not in this repository. Create Draft is placed **after Quality Classifier**.

---

## Problem

Before this change, the ID Repository draft was created late — in **UIN Generator**, after ABIS. Later stages still read identity, documents, and biometrics from **Packet Manager**.

Packet Manager decrypts the packet and caches the result. If ABIS is slow or down for a long time, that cache expires before post-ABIS stages run. Each expired post-ABIS packet then needs a **fresh decryption**. At the same time, new packets in pre-ABIS stages also need decryption. Packet Manager is then loaded with both:

- **Fresh packets** still before ABIS (first decryption)
- **Post-ABIS packets** whose cache has expired (decrypt again)

That overload slows Packet Manager and the rest of the pipeline, not only the packet that waited on ABIS.

---

## Approach

1. **Create Draft stage** reads the packet once (identity, documents, biometrics) and writes an ID Repo draft **before ABIS**.
2. **ID Repository** provides a new `/v2` API version for get, create, update, extract, and publish draft operations.
3. **Post-ABIS stages** read or update the draft and avoid Packet Manager where possible. Finalization mainly publishes the draft that was already filled.
4. **LOST** creates a draft without UIN. Bio Dedupe stamps the matched UIN after ABIS, or discards the draft if there is no match.
5. **Workflow Manager** discards the draft only when the packet is **REJECTED**. FAILED and REPROCESS keep the draft so the packet can continue without re-running Create Draft.

---

## Packet-type behaviour in Create Draft

| Packet type | Draft create | UIN |
|-------------|--------------|-----|
| **NEW** | Create draft, then update the draft with identity / documents / biometrics | ID Repo allocates UIN |
| **UPDATE / RES_UPDATE** | Create draft for the existing UIN, then update the draft with identity / documents / biometrics | UIN taken from the packet |
| **ACTIVATED / DEACTIVATED** | Create draft for the existing UIN with the corresponding status, then update the draft | UIN taken from the packet |
| **LOST** | Create a bare draft with allowed demographic fields only | No UIN at create. Bio Dedupe stamps UIN after a unique ABIS match |

If a draft already exists for the RID, Create Draft **discards it and recreates** it.

---

## ID Repository draft APIs used

ID Repository exposes a `/v2` API version for get, create, update, extract, and publish draft.

| API | HTTP | When used |
|-----|------|-----------|
| `/idrepository/v1/identity/draft` | HEAD | Check whether a draft exists for the RID (200 = exists, 204 = none) |
| `/idrepository/v1/identity/draft/v2/create` | POST | Create draft |
| `/idrepository/v1/identity/draft/v2` | GET | Read draft. Optional `type` (for example `demographics` in Finalization) |
| `/idrepository/v1/identity/draft/v2/update` | PATCH | Update the draft with identity / documents / biometrics |
| `/idrepository/v1/identity/draft/uindata` | PATCH | Stamp resolved UIN on a LOST draft |
| `/idrepository/v1/identity/draft/v2/extractbiometrics/` | PUT | Biometric extraction on the existing draft |
| `/idrepository/v1/identity/draft/v2/publish` | GET | Publish draft to ID Repo (Finalization) |
| `/idrepository/v1/identity/draft/v2/discard/` | DELETE | Discard the draft |

---

## Stage flow (before vs after)

```mermaid
flowchart LR
    subgraph before [Before Create Draft stage]
        QC1[Quality Classifier] --> REST1[Validators / Dedupe / ABIS]
        REST1 --> UG[UIN Generator<br/>create draft]
        UG --> BE1[Biometric Extraction]
        BE1 --> FIN1[Finalization<br/>publish draft]
    end

    subgraph after [After Create Draft stage]
        QC2[Quality Classifier] --> CD[Create Draft<br/>create + update draft]
        CD --> REST2[Validators / Dedupe / ABIS]
        REST2 --> BE2[Biometric Extraction]
        BE2 --> FIN2[Finalization<br/>read draft + publish]
    end
```

Create Draft is bundled in **stage-group-2**. Stage-group-7 now contains Biometric Extraction, Finalization, and Credential Requestor only.

---

## Sequence: NEW / UPDATE (happy path)

```mermaid
sequenceDiagram
    autonumber
    participant QC as Quality Classifier
    participant CD as Create Draft
    participant PM as Packet Manager
    participant IDR as ID Repository
    participant ABIS as ABIS / Bio Dedupe
    participant BE as Biometric Extraction
    participant FIN as Finalization

    QC->>CD: Packet after quality classification
    alt Draft already exists
        CD->>IDR: HEAD /identity/draft
        CD->>IDR: DELETE /identity/draft/v2/discard
    end
    CD->>PM: Read identity, documents, biometrics
    CD->>IDR: Get last committed RID for the UIN
    CD->>CD: Stale check — is this the latest packet?

    alt Older packet
        CD->>CD: Fail packet — do not create draft
    else Latest packet
        CD->>IDR: POST /identity/draft/v2/create
        Note over CD,IDR: NEW: ID Repo allocates UIN<br/>UPDATE: existing UIN
        CD->>IDR: PATCH /identity/draft/v2/update
        Note over CD,IDR: Update the draft with identity / documents / biometrics
        CD->>ABIS: Continue pipeline
        ABIS->>BE: Post-ABIS
        BE->>IDR: PUT /identity/draft/v2/extractbiometrics
        FIN->>IDR: HEAD /identity/draft
        FIN->>IDR: GET /identity/draft/v2 type=demographics
        FIN->>FIN: Resolve UIN and stale check
        FIN->>IDR: GET /identity/draft/v2/publish
    end
```

---

## Sequence: LOST packet

```mermaid
sequenceDiagram
    autonumber
    participant CD as Create Draft
    participant IDR as ID Repository
    participant BD as Bio Dedupe
    participant ABIS as ABIS
    participant FIN as Finalization

    CD->>IDR: POST /identity/draft/v2/create
    CD->>IDR: PATCH /identity/draft/v2/update
    Note over CD,IDR: Bare draft — no UIN
    CD->>BD: Continue pipeline
    BD->>ABIS: Identify
    alt No match
        BD->>IDR: DELETE /identity/draft/v2/discard
        BD->>BD: Reject packet
    else Unique match
        BD->>IDR: PATCH /identity/draft/uindata
        BD->>FIN: Continue
        FIN->>IDR: GET /identity/draft/v2/publish
    else Multiple matches
        BD->>BD: Manual adjudication
        Note over BD,IDR: Draft is kept
    end
```

---

## Draft lifecycle

The draft is created once before ABIS, carried through the rest of the flow, and then either **published** (success) or **discarded** (REJECTED, LOST no-match, or older packet at Finalization). An **older packet at Create Draft never gets a draft**. FAILED and REPROCESS do not discard an existing draft.

```mermaid
flowchart TD
    A([No draft]) --> B[Create Draft stage]
    B -->|Older packet| N([Draft not created])
    B -->|Latest packet| C[Create draft, then update with<br/>identity / documents / biometrics]
    C --> D[Draft exists — packet continues<br/>through validators, dedupe, ABIS]
    D --> E[Biometric Extraction]
    E --> F[Finalization]
    F -->|Latest packet| G([Draft published — identity committed])

    D -->|LOST: unique ABIS match| H[Update draft with matched UIN]
    H --> E

    D -->|REJECTED| X([Draft discarded])
    D -->|LOST: no match| X
    F -->|Older packet| X

    D -->|FAILED or REPROCESS| D
    B -->|Latest packet reprocessed| C
```

Read the diagram as:

- **Down the centre:** happy path — create → ABIS → extract → publish.
- **Left branch:** LOST unique match stamps UIN, then the same extract/publish path.
- **Create Draft + older packet:** draft is **not created**.
- **Finalization + older packet, REJECTED, or LOST no-match:** draft is **discarded**.
- **FAILED / REPROCESS:** stay on the existing draft so the packet can resume.

---

## Reprocessing behaviour change

### Latest packet is allowed for reprocessing

Only the **latest packet** for a UIN is allowed to continue (or to be reprocessed). Create Draft may run again for that packet: any existing draft is discarded and a new draft is created from the packet.

FAILED and REPROCESS **keep** the draft so the latest packet can resume after ABIS without calling Packet Manager again and without re-running Create Draft.

### Older packet is not allowed to reprocess

An **older packet** for the same UIN is not allowed to create, update, or publish a draft. If a newer packet is already committed in ID Repository, the older packet is marked obsolete. Create Draft does not create a draft; Finalization discards the draft that already exists and does not publish it.

### How latest vs older is determined

Create Draft (before creating the draft) and Finalization (before publishing) use the same check:

```mermaid
flowchart TD
    S([Start stale check]) --> U[Read this packet UIN and packet created-on]
    U --> M[Get last committed RID for that UIN<br/>from ID Repository idvid-metadata]
    M --> C1{Committed identity<br/>exists?}
    C1 -->|No — typical NEW| L([Latest packet])
    C1 -->|Yes| C2{This RID is the<br/>last committed RID?}
    C2 -->|Yes| L
    C2 -->|No| T[Resolve last committed packet created-on<br/>from packetId in registration_list<br/>or from the RID timestamp]
    T --> C3{Created-on<br/>resolved?}
    C3 -->|No| R([Reprocess — retry the check])
    C3 -->|Yes| C4{Last committed packet<br/>is strictly newer?}
    C4 -->|No| L
    C4 -->|Yes| O([Older packet])
```

- **Latest packet:** Create Draft creates and updates the draft; Finalization publishes it.
- **Older packet:** Create Draft does **not** create a draft; Finalization **discards** the existing draft and does not publish.
- **Reprocess:** created-on could not be resolved — retry the check.

---

## Post-ABIS draft usage guideline

Packet Manager usage **after ABIS must be restricted**. Decrypting the packet again after a long ABIS wait (cache expiry) competes with fresh pre-ABIS packets and slows the whole system.

Guideline:

- **Before ABIS:** put all identity, documents, biometrics, and other data the later stages need into the ID Repo draft (Create Draft, and any later pre-ABIS enrichment).
- **After ABIS:** prefer **publish** of that draft. Do not call Packet Manager for data that is already on the draft.
- **If a post-ABIS stage needs data:** read it from the draft (`/identity/draft/v2`, with `type` when only a subset is needed).
- **If a new field is required post-ABIS:** add it to the draft in Create Draft (or another pre-ABIS stage) instead of introducing a new Packet Manager read after ABIS.

Finalization follows this: UIN comes from the draft (`type=demographics`); packet created-on for the stale check comes from RID / packetId, not Packet Manager.

---

## Configuration (Create Draft)

These properties are read by Create Draft from the config server:

| Property | Role |
|----------|------|
| `mosip.regproc.create.draft.message.expiry-time-limit` | Event-bus message expiry |
| `mosip.regproc.create.draft.lost.packet.allowed.update.fields` | Demographic fields allowed on a LOST draft |
| `mosip.regproc.create.draft.trim-whitespaces.simpleType-value` | Trim simple-type values before writing the draft |

---

## Notes

1. The **UIN Generator** module is still in this repository but is **not** a dependency of stage-group-7. Runtime draft creation is Create Draft.
2. Exact Camel route XML for inserting `create-draft-bus-in` / `create-draft-bus-out` is maintained in **mosip-config**.
3. Create Draft discards an existing draft before recreate so a reprocess of the latest packet does not merge into an old draft.

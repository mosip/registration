# Sync V3 Supervisor Traceability

## Problem

Packet approval and rejection currently persist supervisor status, comments, and timestamps, but not the acting supervisor/admin identity. Reporting teams therefore cannot reliably map approved or rejected packets back to the person who performed the action.

## MVP Solution

This change adds Sync V3 support for supervisor traceability in Registration Processor:

- `/syncV3` accepts `supervisorId` and `source` in each sync request item.
- `registration_list.supervisor_id` stores the supervisor ID or admin login ID.
- Existing `registration_list.source` marks packet origin as `SUPERVISOR_UPLOAD` or `ADMIN_UPLOAD`.
- Admin uploads skip supervisor validation and are logged as skipped.
- Supervisor uploads compare packet metadata supervisor ID with the Sync V3 `supervisorId` before running the existing supervisor validator.
- `supervisor_id` is nullable so V2 and legacy records remain backward compatible.

## Registration Client Contract

The Registration Client/Admin Portal should include these fields in the encrypted Sync V3 metadata payload:

```json
{
  "registrationId": "27847657360002520181208183052",
  "packetId": "27847657360002520181208183052",
  "registrationType": "NEW",
  "packetHashValue": "packet-hash",
  "packetSize": 12345,
  "supervisorStatus": "APPROVED",
  "supervisorComment": "Verified",
  "supervisorId": "supervisor-or-admin-login-id",
  "source": "SUPERVISOR_UPLOAD",
  "langCode": "eng"
}
```

For admin uploads, the admin login ID should be sent as `supervisorId` and `source` should be `ADMIN_UPLOAD`.

## Reporting Proof

The stored fields support supervisor/admin vs packet-status analytics directly:

```sql
SELECT
  supervisor_id,
  source,
  client_status_code,
  COUNT(*) AS packet_count
FROM regprc.registration_list
WHERE supervisor_id IS NOT NULL
GROUP BY supervisor_id, source, client_status_code
ORDER BY supervisor_id, source, client_status_code;
```

## Error Handling

Sync V3 follows the existing MOSIP sync response envelope. Per-packet validation failures are returned in the sync response error list, including missing supervisor ID, invalid Sync V3 source, and supervisor ID mismatch during processing.

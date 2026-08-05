package io.mosip.registration.processor.packet.storage.utils;

/**
 * Tri-state result for {@link StaleReprocessChecker#checkStaleReprocess}.
 */
public enum StaleCheckResult {
    /** A newer committed packet for the same UIN was processed after the current packet — discard draft and mark obsolete. */
    STALE,
    /** Confirmed that no newer committed packet exists — safe to proceed. */
    NOT_STALE,
    /** Lookup failed (API error, null response, etc.) — staleness cannot be determined — trigger reprocess. */
    UNAVAILABLE
}

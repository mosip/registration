package io.mosip.registration.processor.manual.verification.dto;

import lombok.Data;

@Data
public class ManualVerificationDecisionDto {
	private String matchedRefType;
	private String mvUsrId;
	private String reasonCode;
	private String regId;
	private String statusCode;
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((matchedRefType == null) ? 0 : matchedRefType.hashCode());
		result = prime * result + ((mvUsrId == null) ? 0 : mvUsrId.hashCode());
		result = prime * result + ((reasonCode == null) ? 0 : reasonCode.hashCode());
		result = prime * result + ((regId == null) ? 0 : regId.hashCode());
		result = prime * result + ((statusCode == null) ? 0 : statusCode.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ManualVerificationDecisionDto other = (ManualVerificationDecisionDto) obj;
		if (mvUsrId == null) {
			if (other.mvUsrId != null)
				return false;
		} else if (!mvUsrId.equals(other.mvUsrId)) {
			return false;
		}
		if (regId == null) {
			if (other.regId != null)
				return false;
		} else if (!regId.equals(other.regId)) {
			return false;
		}
		if (matchedRefType == null) {
			if (other.matchedRefType != null)
				return false;
		} else if (!matchedRefType.equals(other.matchedRefType)) {
			return false;
		}
		if (statusCode == null) {
			if (other.statusCode != null)
				return false;
		} else if (!statusCode.equals(other.statusCode)) {
			return false;
		}
		if (reasonCode == null) {
			if (other.reasonCode != null)
				return false;
		} else if (!reasonCode.equals(other.reasonCode)) {
			return false;
		}
		
		return true;
	}
}

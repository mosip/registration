package io.mosip.registration.processor.verification.dto;

import java.io.Serializable;

public class MatchDetail implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -9149403347431263664L;

	private String matchedRegId;
	
	private String url;
	
	/** The matched ref type. */
	private String matchedRefType;
	
	/** The reason code. */
	private String reasonCode;

	public MatchDetail() {
		super();
		
	}

	/**
	 * @return the matchedRegId
	 */
	public String getMatchedRegId() {
		return matchedRegId;
	}

	/**
	 * @param matchedRegId the matchedRegId to set
	 */
	public void setMatchedRegId(String matchedRegId) {
		this.matchedRegId = matchedRegId;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param url the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}
	
	/**
	 * Gets the matched ref type.
	 *
	 * @return the matchedRefType
	 */
	public String getMatchedRefType() {
		return matchedRefType;
	}

	/**
	 * Sets the matched ref type.
	 *
	 * @param matchedRefType the matchedRefType to set
	 */
	public void setMatchedRefType(String matchedRefType) {
		this.matchedRefType = matchedRefType;
	}
	
	/**
	 * Gets the reason code.
	 *
	 * @return the reasonCode
	 */
	public String getReasonCode() {
		return reasonCode;
	}

	/**
	 * Sets the reason code.
	 *
	 * @param reasonCode the reasonCode to set
	 */
	public void setReasonCode(String reasonCode) {
		this.reasonCode = reasonCode;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((matchedRegId == null) ? 0 : matchedRegId.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		result = prime * result + ((matchedRefType == null) ? 0 : matchedRefType.hashCode());
		result = prime * result + ((reasonCode == null) ? 0 : reasonCode.hashCode());
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
		MatchDetail other = (MatchDetail) obj;
		if (matchedRegId == null) {
			if (other.matchedRegId != null)
				return false;
		} else if (!matchedRegId.equals(other.matchedRegId)) {
			return false;
		}
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url)) {
			return false;
		}
		
		if (matchedRefType == null) {
			if (other.matchedRefType != null)
				return false;
		} else if (!matchedRefType.equals(other.matchedRefType)) {
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

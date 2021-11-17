package io.mosip.registration.processor.adjudication.dto;

import java.io.Serializable;
import java.util.List;
	
/**
 * The {@link ManualVerificationDTO} class.
 *
 * @author Pranav Kumar
 * @since 0.0.1
 */
public class ManualVerificationDTO implements Serializable {
	
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The reg id. */
	private String regId;
	
	/** The url. */
	private String url;
	
	/** The mv usr id. */
	private String mvUsrId;
	
	/** The status code. */
	private String statusCode;
	
	/** The matched ref id. */
	private List<MatchDetail> gallery;
	/**
	 * Gets the reg id.
	 *
	 * @return the regId
	 */
	public String getRegId() {
		return regId;
	}

	/**
	 * Sets the reg id.
	 *
	 * @param regId the regId to set
	 */
	public void setRegId(String regId) {
		this.regId = regId;
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
	 * Gets the mv usr id.
	 *
	 * @return the mvUsrId
	 */
	public String getMvUsrId() {
		return mvUsrId;
	}

	/**
	 * Sets the mv usr id.
	 *
	 * @param mvUsrId the mvUsrId to set
	 */
	public void setMvUsrId(String mvUsrId) {
		this.mvUsrId = mvUsrId;
	}

	/**
	 * Gets the status code.
	 *
	 * @return the statusCode
	 */
	public String getStatusCode() {
		return statusCode;
	}

	/**
	 * Sets the status code.
	 *
	 * @param statusCode the statusCode to set
	 */
	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	

	/**
	 * @return the gallery
	 */
	public List<MatchDetail> getGallery() {
		return gallery;
	}

	/**
	 * @param gallery the gallery to set
	 */
	public void setGallery(List<MatchDetail> gallery) {
		this.gallery = gallery;
	}

	/**
	 * Gets the serialversionuid.
	 *
	 * @return the serialversionuid
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((gallery == null) ? 0 : gallery.hashCode());
		
		result = prime * result + ((mvUsrId == null) ? 0 : mvUsrId.hashCode());
		result = prime * result + ((regId == null) ? 0 : regId.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		result = prime * result + ((statusCode == null) ? 0 : statusCode.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ManualVerificationDTO other = (ManualVerificationDTO) obj;
		if (gallery == null) {
			if (other.gallery != null)
				return false;
		} else {
			long count=gallery.size();
			long othercount=other.gallery.size();
			if(count!=othercount) {
				return false;
			}
			long sum=0;
			for(MatchDetail detail:gallery) {
				for(MatchDetail otherdetail:other.gallery) {
					if(detail.equals(otherdetail)) {
						sum=sum+1;
					}
				}
			}
			if(sum!=count) {
				return false;
			}
		}
		
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url)) {
			return false;
		}
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
		if (statusCode == null) {
			if (other.statusCode != null)
				return false;
		} else if (!statusCode.equals(other.statusCode)) {
			return false;
		}
		return true;
	}

	
	
}

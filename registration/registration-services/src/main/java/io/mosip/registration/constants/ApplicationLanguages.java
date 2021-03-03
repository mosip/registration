package io.mosip.registration.constants;

public enum ApplicationLanguages {

	ENG("eng", "English"),
	ARA("ara", "Arabic"),
	FRA("fra", "French");

	private String langCode;
	private String language;

	ApplicationLanguages(String langCode, String language) {
		this.langCode = langCode;
		this.language = language;
	}

	public String getLangCode() {
		return langCode;
	}

	public void setLangCode(String langCode) {
		this.langCode = langCode;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}
	
	public static String getLanguageByLangCode(String langCode) {
		for (ApplicationLanguages applicationLanguages : ApplicationLanguages.values()) {
			if (applicationLanguages.getLangCode().equalsIgnoreCase(langCode)) {
				return applicationLanguages.getLanguage();
			}
		}
		/** If language is not available, return the same langCode back */
		return langCode;
	}
	
	public static String getLangCodeByLanguage(String language) {
		for (ApplicationLanguages applicationLanguages : ApplicationLanguages.values()) {
			if (applicationLanguages.getLanguage().equalsIgnoreCase(language)) {
				return applicationLanguages.getLangCode();
			}
		}
		/** If langCode is not available, return the substring of given language */
		return language.substring(0, 3).toLowerCase();
	}
	
	
}

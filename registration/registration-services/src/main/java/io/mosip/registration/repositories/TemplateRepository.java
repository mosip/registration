package io.mosip.registration.repositories;

import java.util.List;

import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
import io.mosip.registration.entity.Template;

/**
 * Repository for Template.
 *
 * @author Himaja Dhanyamraju
 */
public interface TemplateRepository<P> extends BaseRepository<Template, String>{
	/**
	 * This method returns the list of {@link Template} which are active have specified
	 * templateTypeCode
	 * 
	 * @param templateTypeCode
	 *            the required template type code
	 * @return the list of {@link Template}
	 */
	List<Template> findByIsActiveTrueAndTemplateTypeCode(String templateTypeCode);

	List<Template> findAllByIsActiveTrueAndTemplateTypeCodeLikeAndLangCodeOrderByIdAsc(String templateTypeCode, String langCode);
	
}

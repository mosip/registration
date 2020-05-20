package io.mosip.registration.validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.RequiredOnExpr;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.response.SchemaDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.IdentitySchemaService;

@Component
public class RequiredFieldValidator {
	
	@Autowired
	private IdentitySchemaService identitySchemaService;
	
	@SuppressWarnings("unchecked")
	public boolean isRequiredField(String fieldId, RegistrationDTO registrationDTO) throws RegBaseCheckedException {
		SchemaDto schema = identitySchemaService.getIdentitySchema(registrationDTO.getIdSchemaVersion());
		Optional<UiSchemaDTO> schemaField = schema.getSchema().stream().filter(field -> field.getId().equals(fieldId)).findFirst();
		if(!schemaField.isPresent())
			return false;
		
		return isRequiredField(schemaField.get(), registrationDTO);
	}
	
	@SuppressWarnings("unchecked")
	public boolean isRequiredField(UiSchemaDTO schemaField, RegistrationDTO registrationDTO) throws RegBaseCheckedException {
		boolean required = false;
		if(schemaField == null)
			return required;
		required = schemaField.isRequired();			
		if(schemaField.getRequiredOn() != null && !schemaField.getRequiredOn().isEmpty()) {
			Optional<RequiredOnExpr> expression = schemaField.getRequiredOn().stream().filter(field -> 
			"MVEL".equalsIgnoreCase(field.getEngine()) && field.getExpr() != null).findFirst();
			
			if(expression.isPresent()) {
				Map context = new HashMap();
				context.put("identity", registrationDTO.getMVELDataContext());
				VariableResolverFactory resolverFactory = new MapVariableResolverFactory(context);
				required = MVEL.evalToBoolean(expression.get().getExpr(), resolverFactory);
			}
		}		
		return required;
	}

}

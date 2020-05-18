
package io.mosip.registration.util.mastersync;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.MAPPER_UTILL;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.dto.mastersync.DynamicFieldDto;
import io.mosip.registration.entity.RegistrationCommonFields;
import io.mosip.registration.exception.RegBaseUncheckedException;


/**
 * MapperUtils class provides methods to map or copy values from source object
 * to destination object.
 * 
 * @author Sreekar Chukka
 * @since 1.0.0
 * @see MapperUtils
 *
 */
@SuppressWarnings("unchecked")
public class MapperUtils {

	/** Object for Logger. */
	private static final Logger LOGGER = AppConfig.getLogger(MapperUtils.class);
	
	private static final String FIELD_MISSING_ERROR_MESSAGE = "Field %s not found in data";
	
	private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";	
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat(DATE_TIME_FORMAT);
	
	private MapperUtils() {
		super();
	}

	private static final String SOURCE_NULL_MESSAGE = "source should not be null";
	private static final String DESTINATION_NULL_MESSAGE = "destination should not be null";
	
	private static final ObjectMapper mapper = new ObjectMapper();
	
	static {
		mapper.registerModule(new JavaTimeModule());
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
	}

	/**
	 * This flag is used to restrict copy null values.
	 */
	private static Boolean mapNullValues = Boolean.TRUE;

	/*
	 * #############Public method used for mapping################################
	 */

	/**
	 * This method map the values from <code>source</code> to
	 * <code>destination</code> if name and type of the fields inside the given
	 * parameters are same.If any of the parameters are <code>null</code> this
	 * method return <code>null</code>.This method internally check whether the
	 * source or destinationClass is DTO or an Entity type and map accordingly. If
	 * any {@link Collection} type or Entity type field is their then only matched
	 * name fields value will be set but not the embedded IDs and super class
	 * values.
	 * 
	 * @param               <S> is a type parameter
	 * @param               <D> is a type parameter
	 * @param source        which value is going to be mapped
	 * @param destination   where values is going to be mapped
	 * @param mapNullValues by default marked as true so, it will map null values
	 *                      but if marked as false then null values will be ignored
	 * @return the <code>destination</code> object
	 * @throws NullPointerException if either <code>source</code> or
	 *                              <code>destination</code> is null
	 */
	public static <S, D> D map(final S source, D destination, Boolean mapNullValues) {
		MapperUtils.mapNullValues = mapNullValues;
		return map(source, destination);
	}
	
	


	/**
	 * This method map the values from <code>source</code> to
	 * <code>destination</code> if name and type of the fields inside the given
	 * parameters are same.If any of the parameters are <code>null</code> this
	 * method return <code>null</code>.This method internally check whether the
	 * source or destinationClass is DTO or an Entity type and map accordingly. If
	 * any {@link Collection} type or Entity type field is their then only matched
	 * name fields value will be set but not the embedded IDs and super class
	 * values.
	 * 
	 * @param             <S> is a type parameter
	 * @param             <D> is a type parameter
	 * @param source      which value is going to be mapped
	 * @param destination where values is going to be mapped
	 * @return the <code>destination</code> object
	 * @throws NullPointerException if either <code>source</code> or
	 *                              <code>destination</code> is null
	 */
	public static <S, D> D map(final S source, D destination) {
		Objects.requireNonNull(source, SOURCE_NULL_MESSAGE);
		Objects.requireNonNull(destination, DESTINATION_NULL_MESSAGE);
		try {
			mapValues(source, destination);
		} catch (IllegalAccessException | InstantiationException exOperationException) {
			LOGGER.error(MAPPER_UTILL, APPLICATION_NAME, APPLICATION_ID, "Exception raised while mapping values form "
					+ source.getClass().getName() + " to " + destination.getClass().getName());
			throw new RegBaseUncheckedException(MAPPER_UTILL,
					exOperationException.getMessage() + ExceptionUtils.getStackTrace(exOperationException));
		}
		return destination;
	}
	
	
	/*
	 * public static <S, D> D mapLTE(final List<String> source, D destination) {
	 * Objects.requireNonNull(source, SOURCE_NULL_MESSAGE);
	 * Objects.requireNonNull(destination, DESTINATION_NULL_MESSAGE);
	 * mapJsonListToEntity(source, destination); return destination; }
	 */
	/**
	 * This method takes <code>source</code> and <code>destinationClass</code>, take
	 * all values from source and create an object of <code>destinationClass</code>
	 * and map all the values from source to destination if field name and type is
	 * same.This method internally check whether the source or destinationClass is
	 * DTO or an Entity type and map accordingly.If any {@link Collection} type or
	 * Entity type field is their then only matched name fields value will be set
	 * but not the embedded IDs and super class values.
	 * 
	 * @param                  <S> is a type parameter
	 * @param                  <D> is a type parameter
	 * @param source           which value is going to be mapped
	 * @param destinationClass where values is going to be mapped
	 * @return the object of <code>destinationClass</code>
	 * @throws DataAccessLayerException if exception occur during creating of
	 *                                  <code>destinationClass</code> object
	 * @throws NullPointerException     if either <code>source</code> or
	 *                                  <code>destinationClass</code> is null
	 */
	public static <S, D> D map(final S source, Class<D> destinationClass) {
		Objects.requireNonNull(source, SOURCE_NULL_MESSAGE);
		Objects.requireNonNull(destinationClass, "destination class should not be null");
		Object destination = null;
		try {
			destination = destinationClass.newInstance();
		} catch (InstantiationException | IllegalAccessException exOperationException) {
			LOGGER.error(MAPPER_UTILL, APPLICATION_NAME, APPLICATION_ID, "Exception in mapping vlaues from source : "
					+ source.getClass().getName() + " to destination : " + destinationClass.getClass().getName());
			throw new RegBaseUncheckedException(MAPPER_UTILL,
					exOperationException.getMessage() + ExceptionUtils.getStackTrace(exOperationException));
		}
		return (D) map(source, destination);
	}
	
	

	/**
	 * This method map values of <code>source</code> object to
	 * <code>destination</code> object. It will map field values having same name
	 * and same type for the fields. It will not map any field which is static or
	 * final.It will simply ignore those values.
	 * 
	 * @param             <S> is a type parameter
	 * 
	 * @param             <D> is a type parameter
	 * @param source      is any object which should not be null and have data which
	 *                    is going to be copied
	 * @param destination is an object in which source field values is going to be
	 *                    matched
	 * 
	 * @throws DataAccessLayerException if error raised during mapping values
	 * @throws NullPointerException     if either <code>source</code> or
	 *                                  <code>destination</code> is null
	 */
	public static <S, D> void mapFieldValues(S source, D destination) {

		Objects.requireNonNull(source, SOURCE_NULL_MESSAGE);
		Objects.requireNonNull(destination, DESTINATION_NULL_MESSAGE);
		Field[] sourceFields = source.getClass().getDeclaredFields();
		Field[] destinationFields = destination.getClass().getDeclaredFields();

		mapFieldValues(source, destination, sourceFields, destinationFields);

	}

	/*
	 * #############Private method used for mapping################################
	 */

	/**
	 * Map values from source object to destination object.
	 * 
	 * @param source      which value is going to be mapped
	 * @param destination where values is going to be mapped
	 * @throws InstantiationException if not able to create instance of field having
	 *                                annotation {@link EmbeddedId}
	 * @throws IllegalAccessException if provided fields are not accessible
	 */
	private static <S, D> void mapValues(S source, D destination)
			throws IllegalAccessException, InstantiationException {
		mapFieldValues(source, destination);// this method simply map values if field name and type are same

		if (source.getClass().isAnnotationPresent(Entity.class)) {
			mapEntityToDto(source, destination);
		} else {
			mapDtoToEntity(source, destination);
		}
	}

		 
	

	/**
	 * This method map source DTO to a class object which extends {@link BaseEntity}
	 * 
	 * @param source      which value is going to be mapped
	 * @param destination where values is going to be mapped
	 * @throws InstantiationException if not able to create instance of field having
	 *                                annotation {@link EmbeddedId}
	 * @throws IllegalAccessException if provided fields are not accessible
	 */
	private static <S, D> void mapDtoToEntity(S source, D destination)
			throws InstantiationException, IllegalAccessException {
		Field[] fields = destination.getClass().getDeclaredFields();
		setBaseFieldValue(source, destination);// map super class values
		for (Field field : fields) {
			/**
			 * Map DTO matching field values to super class field values
			 */
			if (field.isAnnotationPresent(EmbeddedId.class)) {
				Object id = field.getType().newInstance();
				mapFieldValues(source, id);
				field.setAccessible(true);
				field.set(destination, id);
				field.setAccessible(false);
				break;
			}
		}
	}
	
	
	
	private static boolean isIgnoreField(Field dfield) {
		return (Modifier.isStatic(dfield.getModifiers()) 
				|| Modifier.isFinal(dfield.getModifiers()) 
				|| dfield.isAnnotationPresent(ManyToMany.class)
				|| dfield.isAnnotationPresent(ManyToOne.class)
				|| dfield.getName().equals("crBy")
				|| dfield.getName().equals("crDtime")
				|| dfield.getName().equals("updBy")
				|| dfield.getName().equals("updDtimes")) ?
						true : false;		
	}

	/**
	 * Map source which extends {@link BaseEntity} to a DTO object.
	 * 
	 * @param source      which value is going to be mapped
	 * @param destination where values is going to be mapped
	 * @throws IllegalAccessException if provided fields are not accessible
	 */
	private static <S, D> void mapEntityToDto(S source, D destination) throws IllegalAccessException {
		Field[] sourceFields = source.getClass().getDeclaredFields();
		/*
		 * Here source is a Entity so we need to take values from Entity object and set
		 * the matching fields in the destination object mostly an DTO.
		 */
		boolean isIdMapped = false;// a flag to check if there any composite key is present and is mapped
		boolean isSuperMapped = false;// a flag to check is class extends the BaseEntity and is mapped
		for (Field sfield : sourceFields) {
			sfield.setAccessible(true);// mark accessible true because fields my be private, for safety
			if (!isIdMapped && sfield.isAnnotationPresent(EmbeddedId.class)) {
				/**
				 * Map the composite key values from source to destination if field name is same
				 */
				/**
				 * Take the field and get the composite key object and map all values to
				 * destination object
				 */
				mapFieldValues(sfield.get(source), destination);
				sfield.setAccessible(false);
				isIdMapped = true;// set flag so no need to check and map again
			} else if (!isSuperMapped) {
				setBaseFieldValue(source, destination);// this method check whether source is entity or destination
														// and maps values accordingly
				isSuperMapped = true;
			}
		}
	}

	/**
	 * Map values from {@link BaseEntity} class source object to destination or vice
	 * versa.
	 * 
	 * @param source      which value is going to be mapped
	 * @param destination where values is going to be mapped
	 */
	private static <S, D> void setBaseFieldValue(S source, D destination) {

		String sourceSupername = source.getClass().getSuperclass().getName();// super class of source object
		String destinationSupername = destination.getClass().getSuperclass().getName();// super class of destination
																						// object
		String baseEntityClassName = RegistrationCommonFields.class.getName();// base entity fully qualified name

		// if source is an entity
		if (sourceSupername.equals(baseEntityClassName)) {
			Field[] sourceFields = source.getClass().getSuperclass().getDeclaredFields();
			Field[] destinationFields = destination.getClass().getDeclaredFields();
			mapFieldValues(source, destination, sourceFields, destinationFields);
			return;
		}
		// if destination is an entity
		if (destinationSupername.equals(baseEntityClassName)) {
			Field[] sourceFields = source.getClass().getDeclaredFields();
			Field[] destinationFields = destination.getClass().getSuperclass().getDeclaredFields();
			mapFieldValues(source, destination, sourceFields, destinationFields);
		}

	}
	
	
	
	
	private static <S, D> void setBaseFieldValue(List<String> source, D destination) {

		String sourceSupername = source.getClass().getSuperclass().getName();// super class of source object
		String destinationSupername = destination.getClass().getSuperclass().getName();// super class of destination
																						// object
		String baseEntityClassName = RegistrationCommonFields.class.getName();// base entity fully qualified name

		// if source is an entity
		if (sourceSupername.equals(baseEntityClassName)) {
			Field[] sourceFields = source.getClass().getSuperclass().getDeclaredFields();
			Field[] destinationFields = destination.getClass().getDeclaredFields();
			mapFieldValues(source, destination, sourceFields, destinationFields);
			return;
		}
		// if destination is an entity
		if (destinationSupername.equals(baseEntityClassName)) {
			Field[] sourceFields = source.getClass().getDeclaredFields();
			Field[] destinationFields = destination.getClass().getSuperclass().getDeclaredFields();
			mapFieldValues(source, destination, sourceFields, destinationFields);
		}

	}

	/**
	 * Map values from source field to destination.
	 * 
	 * @param source      which value is going to be mapped
	 * @param destination where values is going to be mapped
	 * @param sf          source fields
	 * @param dtf         destination fields
	 */
	private static <D, S> void mapFieldValues(S source, D destination, Field[] sourceFields,
			Field[] destinationFields) {
		try {
			for (Field sfield : sourceFields) {
				// Do not set values either static or final
				if (Modifier.isStatic(sfield.getModifiers()) || Modifier.isFinal(sfield.getModifiers())) {
					continue;
				}

				// make field accessible possibly private
				sfield.setAccessible(true);

				for (Field dfield : destinationFields) {

					Class<?> sourceType = sfield.getType();
					Class<?> destinationType = dfield.getType();

					// map only those field whose name and type is same
					if (sfield.getName().equals(dfield.getName()) && sourceType.equals(destinationType)) {

						// for normal field values
						dfield.setAccessible(true);
						setFieldValue(source, destination, sfield, dfield);
						break;
					}
				}
			}
		} catch (IllegalAccessException exIllegalAccessException) {
			LOGGER.error(MAPPER_UTILL, APPLICATION_NAME, APPLICATION_ID, "Exception raised while mapping values form "
					+ source.getClass().getName() + " to " + destination.getClass().getName());
			throw new RegBaseUncheckedException("MapperUtils",
					exIllegalAccessException.getMessage() + ExceptionUtils.getStackTrace(exIllegalAccessException));
		}
	}
	 
		private static <D, S> void mapFieldValuesClient(S source, D destination, Field[] sourceFields,
				Field[] destinationFields) {
			try {
				for (Field sfield : sourceFields) {
					// Do not set values either static or final
					if (Modifier.isStatic(sfield.getModifiers()) || Modifier.isFinal(sfield.getModifiers())) {
						continue;
					}

					// make field accessible possibly private
					sfield.setAccessible(true);

					for (Field dfield : destinationFields) {

						Class<?> sourceType = sfield.getType();
						Class<?> destinationType = dfield.getType();

						// map only those field whose name and type is same
						if (sfield.getName().equals(dfield.getName()) && sourceType.equals(destinationType)) {

							// for normal field values
							dfield.setAccessible(true);
							setFieldValue(source, destination, sfield, dfield);
							break;
						}
					}
				}
			} catch (IllegalAccessException exIllegalAccessException) {
				LOGGER.error(MAPPER_UTILL, APPLICATION_NAME, APPLICATION_ID, "Exception raised while mapping values form "
						+ source.getClass().getName() + " to " + destination.getClass().getName());
				throw new RegBaseUncheckedException("MapperUtils",
						exIllegalAccessException.getMessage() + ExceptionUtils.getStackTrace(exIllegalAccessException));
			}
		}


	/**
	 * Take value from source field and insert value into destination field.
	 * 
	 * @param source      which value is going to be mapped
	 * @param destination where values is going to be mapped
	 * @param sf          source fields
	 * @param dtf         destination fields
	 * @throws IllegalAccessException if provided fields are not accessible
	 */
	private static <S, D> void setFieldValue(S source, D destination, Field sf, Field dtf)
			throws IllegalAccessException {
		// check whether user wants to map null values into destination object or not
		if (!mapNullValues && EmptyCheckUtils.isNullEmpty(sf.get(source))) {
			return;
		}
		dtf.set(destination, sf.get(source));
		dtf.setAccessible(false);
		sf.setAccessible(false);
	}
	
	/**
	 * Map values from {@link BaseEntity} class source object to destination or vice
	 * versa.
	 * 
	 * @param jsonObject      which value is going to be mapped
	 * @param destination where values is going to be mapped
	 * @throws ParseException 
	 * @throws JSONException 
	 * @throws SecurityException 
	 * @throws IllegalArgumentException 
	 */
	public static <D> D mapJSONObjectToEntity(final JSONObject jsonObject, Class<?> entityClass) throws IllegalAccessException, InstantiationException, ParseException, IllegalArgumentException, SecurityException, JSONException {
		LOGGER.debug(MAPPER_UTILL, APPLICATION_NAME, APPLICATION_ID, "Building entity of type : " + entityClass.getName());
		
		Objects.requireNonNull(jsonObject, SOURCE_NULL_MESSAGE);
		Objects.requireNonNull(entityClass, "destination class should not be null");
		Object destination = null;
		try {
			destination = entityClass.newInstance();
		} catch (InstantiationException | IllegalAccessException exOperationException) {
			LOGGER.error(MAPPER_UTILL, APPLICATION_NAME, APPLICATION_ID, "Exception in mapping vlaues from source : "
					+ jsonObject.getClass().getName() + " to destination : " + entityClass.getName());
			throw new RegBaseUncheckedException(MAPPER_UTILL,
					exOperationException.getMessage() + ExceptionUtils.getStackTrace(exOperationException));
		}
		Objects.requireNonNull(destination, DESTINATION_NULL_MESSAGE);
		setBaseFieldValueFromJsonObject(jsonObject, destination);// map super class values
		mapJsonToEntity(jsonObject, destination, destination.getClass().getDeclaredFields());
		return (D) destination;
	}
	
	
	/**
	 * Take value from source field and insert value into destination field.
	 * 
	 * @param jsonObject      which value is going to be mapped
	 * @param destination where values is going to be mapped
	 * @param fields         destination fields
	 * @throws IllegalAccessException if provided fields are not accessible
	 * @throws JSONException 
	 * @throws IllegalArgumentException 
	 */
	private static <D> void mapJsonToEntity(JSONObject jsonObject, D destination, Field[] fields)
			throws InstantiationException, IllegalAccessException, ParseException, IllegalArgumentException, JSONException {

		for (Field dfield : fields) {
			if (isIgnoreField(dfield)) {
				continue;
			}
			if (dfield.isAnnotationPresent(EmbeddedId.class)) {
				Object id = dfield.getType().newInstance();
				mapJsonToEntity(jsonObject, id, id.getClass().getDeclaredFields());
				dfield.setAccessible(true);
				dfield.set(destination, id);
				dfield.setAccessible(false);
				continue;
			}
			
			//avoids failure of complete sync on missing of non-mandatory field
			if(!jsonObject.has(dfield.getName()) || jsonObject.get(dfield.getName()) == JSONObject.NULL) {
				//throw new RegBaseUncheckedException(MAPPER_UTILL, String.format(FIELD_MISSING_ERROR_MESSAGE, dfield.getName()));
				LOGGER.warn(MAPPER_UTILL, APPLICATION_NAME, APPLICATION_ID, String.format(FIELD_MISSING_ERROR_MESSAGE, dfield.getName()));
				continue;
			}
			
			dfield.setAccessible(true);			
			
			switch (dfield.getType().getName()) {
			case "java.lang.Boolean":	
				dfield.set(destination, jsonObject.get(dfield.getName()));
				break;
			case "boolean":
				dfield.set(destination,  jsonObject.get(dfield.getName()));
				break;
			case "java.sql.Time":
				dfield.set(destination, java.sql.Time.valueOf(jsonObject.getString(dfield.getName())));
				break;
			case "[B" :
				dfield.set(destination, jsonObject.getString(dfield.getName()).getBytes());
				break;
			case "java.sql.Timestamp" :
				dfield.set(destination, getTimestampValue(jsonObject.getString(dfield.getName())));
				break;
			case "java.time.LocalDateTime":
				dfield.set(destination, getLocalDateTimeValue(jsonObject.getString(dfield.getName())));
				break;
			case "java.time.LocalDate":
				dfield.set(destination, getLocalDateValue(jsonObject.getString(dfield.getName())));
				break;
			default:
				dfield.set(destination, jsonObject.get(dfield.getName()));
				break;
			}			
		}
	}
	
	private static Timestamp getTimestampValue(String value) {
		Timestamp timestamp = null;
		try {
			timestamp = new Timestamp(SIMPLE_DATE_FORMAT.parse(value).getTime());
			return timestamp;
		} catch(ParseException ex) {
			LOGGER.error(MAPPER_UTILL, APPLICATION_NAME, APPLICATION_ID, "Failed to parse timestamp, invalid format >> " + value);
		}
		return timestamp;
	}
	
	private static LocalDateTime getLocalDateTimeValue(String value) {
		LocalDateTime timestamp = null;
		try {
			Instant instant = Instant.parse(value);
			timestamp = LocalDateTime.ofInstant(instant, ZoneId.of(ZoneOffset.UTC.getId()));
			return timestamp;
		} catch(DateTimeParseException ex) {
			LOGGER.error(MAPPER_UTILL, APPLICATION_NAME, APPLICATION_ID, "Failed to parse LocalDateTime, invalid format >> " + value);
		}
		return timestamp;
	}
	
	private static LocalDate getLocalDateValue(String value) {
		LocalDate timestamp = null;
		try {
			timestamp = LocalDate.parse(value);
			return timestamp;
		} catch(DateTimeParseException ex) {
			LOGGER.error(MAPPER_UTILL, APPLICATION_NAME, APPLICATION_ID, "Failed to parse LocalDate, invalid format >> " + value);
		}
		return timestamp;
	}
	
	
	private static <D> void setBaseFieldValueFromJsonObject(JSONObject source, D destination) throws InstantiationException, IllegalAccessException, ParseException, IllegalArgumentException, JSONException {

		String destinationSupername = destination.getClass().getSuperclass().getName();// super class of destination object
		String baseEntityClassName = RegistrationCommonFields.class.getName();// base entity fully qualified name
		// if destination is an entity
		if (destinationSupername.equals(baseEntityClassName)) {
			Field[] destinationFields = destination.getClass().getSuperclass().getDeclaredFields();
			mapJsonToEntity(source, destination, destinationFields);
		}
	}
	
	public static <T> T convertJSONStringToDto(final String jsonString, TypeReference<T> typeReference) throws IOException {
		return mapper.readValue(jsonString, typeReference);
	}
	
	public static String convertObjectToJsonString(final Object object) throws IOException {
		return mapper.writeValueAsString(object);
	}

}
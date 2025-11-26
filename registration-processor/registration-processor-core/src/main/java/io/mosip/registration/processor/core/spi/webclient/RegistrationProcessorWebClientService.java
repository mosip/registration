package io.mosip.registration.processor.core.spi.webclient;

import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import org.springframework.http.MediaType;

import java.util.List;

// TODO: Auto-generated Javadoc

/**
 * The Interface RegistrationProcessorWebClientService.
 *
 * @param <T> the generic type
 * @author Janrdhan B S
 *
 */
public interface RegistrationProcessorWebClientService<T> {

    /**
     * Gets the api.
     *
     * @param apiName         the api name
     * @param pathSegments    pathSegments of the uri
     * @param queryParam      the query param
     * @param queryParamValue the query param value
     * @param responseType    the response type
     * @return the api
     * @throws ApisResourceAccessException the apis resource access exception
     */
    public T getApi(ApiName apiName, List<String> pathSegments, String queryParam, String queryParamValue,
                    Class<?> responseType) throws ApisResourceAccessException;

    /**
     * Gets the api.
     *
     * @param apiName         the api name
     * @param pathSegments    the pathSegments
     * @param queryParam      the query param
     * @param queryParamValue the query param value
     * @param responseType    the response type
     * @return the api
     * @throws ApisResourceAccessException the apis resource access exception
     */
    public T getApi(ApiName apiName, List<String> pathSegments, List<String> queryParam, List<Object> queryParamValue,
                    Class<?> responseType) throws ApisResourceAccessException;

    /**
     * Post api.
     *
     * @param apiName         the api name
     * @param queryParam      the query param
     * @param queryParamValue the query param value
     * @param requestedData   the requested data
     * @param responseType    the response type
     * @return the t
     * @throws ApisResourceAccessException the apis resource access exception
     */
    public T postApi(ApiName apiName, String queryParam, String queryParamValue, T requestedData, Class<?> responseType)
            throws ApisResourceAccessException;

    /**
     * Post api.
     *
     * @param apiName         the api name
     * @param queryParamName  the query param
     * @param queryParamValue the query param value
     * @param requestedData   the requested data
     * @param responseType    the response type
     * @param mediaType       the content type
     * @return the t
     * @throws ApisResourceAccessException the apis resource access exception
     */
    public T postApi(ApiName apiName, String queryParamName, String queryParamValue, T requestedData,
                     Class<?> responseType, MediaType mediaType) throws ApisResourceAccessException;

    /**
     * Post api.
     *
     * @param apiName         the api name
     * @param pathSegments    the pathSegments
     * @param queryParam      the query param
     * @param queryParamValue the query param value
     * @param requestedData   the requested data
     * @param responseType    the response type
     * @return the t
     * @throws ApisResourceAccessException the apis resource access exception
     */

    public T postApi(ApiName apiName, List<String> pathSegments, String queryParam, String queryParamValue,
                     T requestedData, Class<?> responseType) throws ApisResourceAccessException;

    /**
     * Post Api.
     *
     * @param apiName         the api name
     * @param mediaType       the media type
     * @param pathSegments    the pathSegments
     * @param queryParam      the query param
     * @param queryParamValue the query param value
     * @param requestedData   the requested data
     * @param responseType    the response type
     * @return the t
     * @throws ApisResourceAccessException the apis resource access exception
     */
    public T postApi(ApiName apiName, MediaType mediaType, List<String> pathSegments, List<String> queryParam, List<Object> queryParamValue,
                     T requestedData, Class<?> responseType) throws ApisResourceAccessException;

    /**
     * Patch api.
     *
     * @param apiName         the api name
     * @param pathSegments    the pathSegments
     * @param queryParam      the query param
     * @param queryParamValue the query param value
     * @param requestedData   the requested data
     * @param responseType    the response type
     * @return the t
     * @throws ApisResourceAccessException the apis resource access exception
     */
    public T patchApi(ApiName apiName, List<String> pathSegments, String queryParam, String queryParamValue,
                      T requestedData, Class<?> responseType) throws ApisResourceAccessException;

    /**
     * Put api.
     *
     * @param apiName         the api name
     * @param pathSegments    the pathSegments
     * @param queryParam      the query param
     * @param queryParamValue the query param value
     * @param requestedData   the requested data
     * @param responseType    the response type
     * @param mediaType       the media type
     * @return the t
     * @throws ApisResourceAccessException the apis resource access exception
     */
    public T putApi(ApiName apiName, List<String> pathSegments, String queryParam, String queryParamValue,
                    T requestedData, Class<?> responseType, MediaType mediaType) throws ApisResourceAccessException;

    /**
     * Post api.
     *
     * @param url             the url
     * @param mediaType       the media type
     * @param pathSegments    the pathSegments
     * @param queryParam      the query param
     * @param queryParamValue the query param value
     * @param requestedData   the requested data
     * @param responseType    the response type
     * @return the t
     * @throws ApisResourceAccessException the apis resource access exception
     */
    public T postApi(String url, MediaType mediaType, List<String> pathSegments, List<String> queryParam, List<Object> queryParamValue,
                     T requestedData, Class<?> responseType) throws ApisResourceAccessException;

    public Integer headApi(ApiName apiName, List<String> pathSegments, List<String> queryParamName, List<Object> queryParamValue) throws ApisResourceAccessException;

    public T deleteApi(ApiName apiName, List<String> pathSegments, String queryParam, String queryParamValue,
                       Class<?> responseType) throws ApisResourceAccessException;
}

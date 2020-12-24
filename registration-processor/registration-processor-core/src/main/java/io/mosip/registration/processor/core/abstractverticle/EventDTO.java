package io.mosip.registration.processor.core.abstractverticle;

import java.io.Serializable;

import io.vertx.core.json.JsonObject;

/**
 * DTO class that is used to pass event data between handlers
 *
 * @author Vishwanath V
 */
public class EventDTO implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new Event DTO.
	 */
	public EventDTO() {
		super();
	}

	/** The Json object body of an event. */
	private JsonObject body;

	
	/**
	 * Gets the body .
	 *
	 * @return the body
	 */

	public JsonObject getBody() {
		return this.body;
	}

	/**
	 * Sets the body.
	 *
	 * @param body the new body JsonObject
	 */
	public void setBody(JsonObject body) {
		this.body = body;
	}

	
}

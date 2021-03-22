package io.mosip.registration.processor.core.spi.eventbus;

/**
 * The generic Event handler that be used for implementing handlers passing an 
 * event and successive handle
 * 
 * @author Vishwanath V
 */
public interface EventHandler<E, H> {

    /**
    * Some Event has occured, so handle the same.
    *
    * @param event  The event to handle
    * @param handle The handle that should be called after completion of handling of the event
    */
    void handle(E event, H handle);
    
}

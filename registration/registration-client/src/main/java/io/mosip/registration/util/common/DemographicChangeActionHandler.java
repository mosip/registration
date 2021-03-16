package io.mosip.registration.util.common;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

@Component
public class DemographicChangeActionHandler {

    private static final Logger LOGGER = AppConfig.getLogger(DemographicChangeActionHandler.class);

    @Autowired
    private List<ChangeActionHandler> handlerList;

    /**
     *
     * @param handleStr "action:arg1,arg2"
     */
    public void actionHandle(Pane parentFlowPane, String sourceId, String handleStr) {
        if(handleStr != null) {
            LOGGER.debug("Invoking external action handler for .... {} ", sourceId);
            String[] parts = handleStr.split(":");
            ChangeActionHandler changeActionHandler = getChangeActionHandler(parts[0]);
            if(changeActionHandler != null) {
                changeActionHandler.handle(parentFlowPane, sourceId, parts.length > 1 ? parts[1].split(",") : new String[]{});
            }
        }
    }

    private boolean executePrecondition(String preconditionStr) {
        return true;
    }

    private ChangeActionHandler getChangeActionHandler(String actionClassName) {
       Optional<ChangeActionHandler> result = handlerList.stream()
               .filter(h-> h.getActionClassName().equals(actionClassName)).findFirst();
       return result.isPresent() ? result.get() : null;
    }

}

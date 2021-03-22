package io.mosip.registration.util.control.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.templatemanager.spi.TemplateManager;
import io.mosip.kernel.core.templatemanager.spi.TemplateManagerBuilder;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.template.TemplateService;
import io.mosip.registration.util.control.FxControl;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;

/**
 * Builds HTML content based on the provided templateName in the first language
 * select in the order of selection.
 *
 */
public class HtmlFxControl extends FxControl {

    private static final Logger LOGGER = AppConfig.getLogger(HtmlFxControl.class);

    private TemplateManagerBuilder templateManagerBuilder;
    private TemplateService templateService;

    public HtmlFxControl() {
        org.springframework.context.ApplicationContext applicationContext = Initialization.getApplicationContext();
        auditFactory = applicationContext.getBean(AuditManagerService.class);
        templateManagerBuilder = applicationContext.getBean(TemplateManagerBuilder.class);
        templateService = applicationContext.getBean(TemplateService.class);
    }

    @Override
    public FxControl build(UiSchemaDTO uiSchemaDTO) {
        this.uiSchemaDTO = uiSchemaDTO;
        this.control = this;

        try {
            HBox hBox = new HBox();
            hBox.setId(uiSchemaDTO.getId()+"_PARENT");
            hBox.setSpacing(20);
            WebView webView = new WebView();
            webView.setId(uiSchemaDTO.getId());
            webView.getEngine().loadContent(getContent());
            webView.getEngine()
                    .documentProperty()
                    .addListener((observableValue, oldValue, document) -> addListeners(document));
            hBox.getChildren().add(webView);
            //TODO - We can have toolbar to display print / downloadPDF options
            this.node = hBox;
            
            auditFactory.audit(AuditEvent.REG_HTML_FX_CONTROL, Components.REG_DEMO_DETAILS, SessionContext.userId(),
					AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
            
            return this.control;

        } catch (RegBaseCheckedException | IOException e) {
            LOGGER.error("Failed to build HTMLFxControl", e);
        }
        return null;
    }


    @Override
    public void setData(Object data) {

    }

    @Override
    public void fillData(Object data) {

    }

    @Override
    public Object getData() {
        return null;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public List<GenericDto> getPossibleValues(String langCode) {
        return null;
    }

    @Override
    public void setListener(Node node) {
    }

    @Override
    public void selectAndSet(Object data) {
    }

    private String getContent() throws RegBaseCheckedException, IOException {
        String templateText = this.uiSchemaDTO.getTemplateName() == null ?
                "<!DOCTYPE html><html><body><h1>Template Name not set !!</h1></body></html>" :
                templateService.getHtmlTemplate(this.uiSchemaDTO.getTemplateName(),  getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));
        Writer writer = new StringWriter();
        TemplateManager templateManager = templateManagerBuilder.build();
        InputStream inputStream = templateManager.merge(new ByteArrayInputStream(templateText == null ?
                                "<!DOCTYPE html><html><body><h1>Empty template Content</h1></body></html>".getBytes(StandardCharsets.UTF_8) :
                                templateText.getBytes(StandardCharsets.UTF_8)),
                getRegistrationDTo().getMVELDataContext());
        IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);
        return writer.toString();
    }

    private void addListeners(Document document) {
        //TODO
    }
}

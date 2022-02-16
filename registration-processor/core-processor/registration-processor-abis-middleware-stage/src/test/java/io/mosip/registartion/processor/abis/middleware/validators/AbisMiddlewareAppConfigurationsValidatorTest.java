package io.mosip.registartion.processor.abis.middleware.validators;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import io.mosip.registration.processor.abis.queue.dto.AbisQueueDetails;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
public class AbisMiddlewareAppConfigurationsValidatorTest {

    @InjectMocks
    AbisMiddlewareAppConfigurationsValidator validator;

    @Mock
    private Utilities utility;

    @Mock
    ContextRefreshedEvent event;

    @Before
    public void setup() {
        ReflectionTestUtils.setField(validator, "reprocessorElapseTime", 1000);
        ReflectionTestUtils.setField(validator, "reprocessBufferTime", 1000);

    }

    @Test
    public void validateConfigurationsTest() throws RegistrationProcessorCheckedException {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        final Appender<ILoggingEvent> mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);

        AbisQueueDetails abisQueueDetail = new AbisQueueDetails();
        abisQueueDetail.setInboundMessageTTL(1000);

        Mockito.when(utility.getAbisQueueDetails()).thenReturn(Lists.newArrayList(abisQueueDetail));

        validator.validateConfigurations(event);

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher<ILoggingEvent>() {

            @Override
            public boolean matches(ILoggingEvent argument) {
                return ((LoggingEvent) argument).getFormattedMessage()
                        .contains("registration.processor.reprocess.elapse.time config 1000 is invalid");
            }
        }));
    }

    @Test
    public void validateConfigurationsExceptionTest() throws RegistrationProcessorCheckedException {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        final Appender<ILoggingEvent> mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);

        AbisQueueDetails abisQueueDetail = new AbisQueueDetails();
        abisQueueDetail.setInboundMessageTTL(1000);

        Mockito.when(utility.getAbisQueueDetails()).thenThrow(new RegistrationProcessorCheckedException("Error", "Error"));

        validator.validateConfigurations(event);

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher<ILoggingEvent>() {

            @Override
            public boolean matches(ILoggingEvent argument) {
                return ((LoggingEvent) argument).getFormattedMessage()
                        .contains("Abis queue details invalid");
            }
        }));
    }
}

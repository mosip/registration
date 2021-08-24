package io.mosip.registration.processor.camel.bridge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Endpoint;
import org.apache.camel.ErrorHandlerFactory;
import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.NoFactoryAvailableException;
import org.apache.camel.PollingConsumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.ServiceStatus;
import org.apache.camel.ShutdownRoute;
import org.apache.camel.ShutdownRunningTask;
import org.apache.camel.StartupListener;
import org.apache.camel.TypeConverter;
import org.apache.camel.api.management.mbean.ManagedCamelContextMBean;
import org.apache.camel.api.management.mbean.ManagedProcessorMBean;
import org.apache.camel.api.management.mbean.ManagedRouteMBean;
import org.apache.camel.builder.ErrorHandlerBuilder;
import org.apache.camel.health.HealthCheckRegistry;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.model.HystrixConfigurationDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.model.cloud.ServiceCallConfigurationDefinition;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.camel.model.rest.RestsDefinition;
import org.apache.camel.model.transformer.TransformerDefinition;
import org.apache.camel.model.validator.ValidatorDefinition;
import org.apache.camel.runtimecatalog.RuntimeCamelCatalog;
import org.apache.camel.spi.AsyncProcessorAwaitManager;
import org.apache.camel.spi.CamelContextNameStrategy;
import org.apache.camel.spi.ClassResolver;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.DataFormatResolver;
import org.apache.camel.spi.DataType;
import org.apache.camel.spi.Debugger;
import org.apache.camel.spi.EndpointRegistry;
import org.apache.camel.spi.EndpointStrategy;
import org.apache.camel.spi.ExecutorServiceManager;
import org.apache.camel.spi.ExecutorServiceStrategy;
import org.apache.camel.spi.FactoryFinder;
import org.apache.camel.spi.FactoryFinderResolver;
import org.apache.camel.spi.HeadersMapFactory;
import org.apache.camel.spi.InflightRepository;
import org.apache.camel.spi.Injector;
import org.apache.camel.spi.InterceptStrategy;
import org.apache.camel.spi.Language;
import org.apache.camel.spi.LifecycleStrategy;
import org.apache.camel.spi.LogListener;
import org.apache.camel.spi.ManagementMBeanAssembler;
import org.apache.camel.spi.ManagementNameStrategy;
import org.apache.camel.spi.ManagementStrategy;
import org.apache.camel.spi.MessageHistoryFactory;
import org.apache.camel.spi.ModelJAXBContextFactory;
import org.apache.camel.spi.NodeIdFactory;
import org.apache.camel.spi.PackageScanClassResolver;
import org.apache.camel.spi.ProcessorFactory;
import org.apache.camel.spi.Registry;
import org.apache.camel.spi.ReloadStrategy;
import org.apache.camel.spi.RestConfiguration;
import org.apache.camel.spi.RestRegistry;
import org.apache.camel.spi.RouteController;
import org.apache.camel.spi.RoutePolicyFactory;
import org.apache.camel.spi.RouteStartupOrder;
import org.apache.camel.spi.RuntimeEndpointRegistry;
import org.apache.camel.spi.ServicePool;
import org.apache.camel.spi.ShutdownStrategy;
import org.apache.camel.spi.StreamCachingStrategy;
import org.apache.camel.spi.Transformer;
import org.apache.camel.spi.TransformerRegistry;
import org.apache.camel.spi.TypeConverterRegistry;
import org.apache.camel.spi.UnitOfWorkFactory;
import org.apache.camel.spi.UuidGenerator;
import org.apache.camel.spi.Validator;
import org.apache.camel.spi.ValidatorRegistry;
import org.apache.camel.util.LoadPropertiesException;
import org.apache.camel.util.ValueHolder;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.registration.processor.camel.bridge.intercepter.WorkflowCommandPredicate;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.WorkflowInternalActionDTO;
import io.mosip.registration.processor.core.code.WorkflowInternalActionCode;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(VertxUnitRunner.class)
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*" })

@AutoConfigureMockMvc
public class WorkflowCommandPredicateTest {

	private WorkflowCommandPredicate workflowCommandPredicate;

	private ObjectMapper objectMapper;

	private Exchange exchange;


	@Before
	public void setup() {
		objectMapper = new ObjectMapper();
		workflowCommandPredicate = new WorkflowCommandPredicate();
		ReflectionTestUtils.setField(workflowCommandPredicate, "objectMapper", objectMapper);
		exchange = new DefaultExchange(new CamelContext() {

			@Override
			public void setTracing(Boolean tracing) {
			}

			@Override
			public void setStreamCaching(Boolean cache) {

			}

			@Override
			public void setShutdownRunningTask(ShutdownRunningTask shutdownRunningTask) {
			}

			@Override
			public void setShutdownRoute(ShutdownRoute shutdownRoute) {
			}

			@Override
			public void setMessageHistory(Boolean messageHistory) {
			}

			@Override
			public void setLogMask(Boolean logMask) {
			}

			@Override
			public void setLogExhaustedMessageBody(Boolean logExhaustedMessageBody) {
			}

			@Override
			public void setHandleFault(Boolean handleFault) {
			}

			@Override
			public void setDelayer(Long delay) {
			}

			@Override
			public void setAutoStartup(Boolean autoStartup) {


			}

			@Override
			public void setAllowUseOriginalMessage(Boolean allowUseOriginalMessage) {
			}

			@Override
			public Boolean isTracing() {
				return null;
			}

			@Override
			public Boolean isStreamCaching() {
				return null;
			}

			@Override
			public Boolean isMessageHistory() {
				return null;
			}

			@Override
			public Boolean isLogMask() {
				return null;
			}

			@Override
			public Boolean isLogExhaustedMessageBody() {
				return null;
			}

			@Override
			public Boolean isHandleFault() {
				return null;
			}

			@Override
			public Boolean isAutoStartup() {
				return null;
			}

			@Override
			public Boolean isAllowUseOriginalMessage() {
				return null;
			}

			@Override
			public ShutdownRunningTask getShutdownRunningTask() {
				return null;
			}

			@Override
			public ShutdownRoute getShutdownRoute() {
				return null;
			}

			@Override
			public Long getDelayer() {
				return null;
			}

			@Override
			public void suspend() throws Exception {
			}

			@Override
			public void resume() throws Exception {
			}

			@Override
			public boolean isSuspended() {
				return false;
			}

			@Override
			public void suspendRoute(String routeId, long timeout, TimeUnit timeUnit) throws Exception {
			}

			@Override
			public void suspendRoute(String routeId) throws Exception {
			}

			@Override
			public boolean stopRoute(String routeId, long timeout, TimeUnit timeUnit, boolean abortAfterTimeout)
					throws Exception {
				return false;
			}

			@Override
			public void stopRoute(String routeId, long timeout, TimeUnit timeUnit) throws Exception {
			}

			@Override
			public void stopRoute(String routeId) throws Exception {
			}

			@Override
			public void stopRoute(RouteDefinition route) throws Exception {
			}

			@Override
			public void stop() throws Exception {

			}

			@Override
			public void startRoute(String routeId) throws Exception {
			}

			@Override
			public void startRoute(RouteDefinition route) throws Exception {
			}

			@Override
			public void startAllRoutes() throws Exception {
			}

			@Override
			public void start() throws Exception {
			}

			@Override
			public void shutdownRoute(String routeId, long timeout, TimeUnit timeUnit) throws Exception {
			}

			@Override
			public void shutdownRoute(String routeId) throws Exception {
			}

			@Override
			public void setupRoutes(boolean done) {
			}

			@Override
			public void setValidators(List<ValidatorDefinition> validators) {
			}

			@Override
			public void setUuidGenerator(UuidGenerator uuidGenerator) {
			}

			@Override
			public void setUseMDCLogging(Boolean useMDCLogging) {
			}

			@Override
			public void setUseDataType(Boolean useDataType) {
			}

			@Override
			public void setUseBreadcrumb(Boolean useBreadcrumb) {
			}

			@Override
			public void setUnitOfWorkFactory(UnitOfWorkFactory unitOfWorkFactory) {
			}

			@Override
			public void setTypeConverterStatisticsEnabled(Boolean typeConverterStatisticsEnabled) {
			}

			@Override
			public void setTransformers(List<TransformerDefinition> transformers) {
			}

			@Override
			public void setStreamCachingStrategy(StreamCachingStrategy streamCachingStrategy) {
			}

			@Override
			public void setShutdownStrategy(ShutdownStrategy shutdownStrategy) {
			}

			@Override
			public void setServiceCallConfigurations(List<ServiceCallConfigurationDefinition> configurations) {
			}

			@Override
			public void setServiceCallConfiguration(ServiceCallConfigurationDefinition configuration) {
			}

			@Override
			public void setSSLContextParameters(SSLContextParameters sslContextParameters) {
			}

			@Override
			public void setRuntimeEndpointRegistry(RuntimeEndpointRegistry runtimeEndpointRegistry) {

			}

			@Override
			public void setRouteController(RouteController routeController) {

			}

			@Override
			public void setRestRegistry(RestRegistry restRegistry) {

			}

			@Override
			public void setRestConfiguration(RestConfiguration restConfiguration) {

			}

			@Override
			public void setReloadStrategy(ReloadStrategy reloadStrategy) {
			}

			@Override
			public void setProperties(Map<String, String> properties) {
			}

			@Override
			public void setProducerServicePool(ServicePool<Endpoint, Producer> servicePool) {

			}

			@Override
			public void setProcessorFactory(ProcessorFactory processorFactory) {
			}

			@Override
			public void setPollingConsumerServicePool(ServicePool<Endpoint, PollingConsumer> servicePool) {
			}

			@Override
			public void setPackageScanClassResolver(PackageScanClassResolver resolver) {
			}

			@Override
			public void setNodeIdFactory(NodeIdFactory factory) {

			}

			@Override
			public void setNameStrategy(CamelContextNameStrategy nameStrategy) {
			}

			@Override
			public void setModelJAXBContextFactory(ModelJAXBContextFactory modelJAXBContextFactory) {
			}

			@Override
			public void setMessageHistoryFactory(MessageHistoryFactory messageHistoryFactory) {
			}

			@Override
			public void setManagementStrategy(ManagementStrategy strategy) {
			}

			@Override
			public void setManagementNameStrategy(ManagementNameStrategy nameStrategy) {
			}

			@Override
			public void setLoadTypeConverters(Boolean loadTypeConverters) {
			}

			@Override
			public void setLazyLoadTypeConverters(Boolean lazyLoadTypeConverters) {
			}

			@Override
			public void setInflightRepository(InflightRepository repository) {
			}

			@Override
			public void setHystrixConfigurations(List<HystrixConfigurationDefinition> configurations) {
			}

			@Override
			public void setHystrixConfiguration(HystrixConfigurationDefinition configuration) {

			}

			@Override
			public void setHealthCheckRegistry(HealthCheckRegistry healthCheckRegistry) {


			}

			@Override
			public void setHeadersMapFactory(HeadersMapFactory factory) {


			}

			@Override
			public void setGlobalOptions(Map<String, String> globalOptions) {


			}

			@Override
			public void setFactoryFinderResolver(FactoryFinderResolver resolver) {


			}

			@Override
			public void setExecutorServiceManager(ExecutorServiceManager executorServiceManager) {


			}

			@Override
			public void setErrorHandlerBuilder(ErrorHandlerFactory errorHandlerBuilder) {


			}

			@Override
			public void setDefaultTracer(InterceptStrategy tracer) {


			}

			@Override
			public void setDefaultBacklogTracer(InterceptStrategy backlogTracer) {


			}

			@Override
			public void setDefaultBacklogDebugger(InterceptStrategy backlogDebugger) {


			}

			@Override
			public void setDebugger(Debugger debugger) {


			}

			@Override
			public void setDataFormats(Map<String, DataFormatDefinition> dataFormats) {


			}

			@Override
			public void setDataFormatResolver(DataFormatResolver dataFormatResolver) {


			}

			@Override
			public void setClassResolver(ClassResolver resolver) {


			}

			@Override
			public void setAsyncProcessorAwaitManager(AsyncProcessorAwaitManager manager) {


			}

			@Override
			public void setApplicationContextClassLoader(ClassLoader classLoader) {


			}

			@Override
			public void resumeRoute(String routeId) throws Exception {


			}

			@Override
			public Validator resolveValidator(DataType type) {

				return null;
			}

			@Override
			public Transformer resolveTransformer(DataType from, DataType to) {

				return null;
			}

			@Override
			public Transformer resolveTransformer(String model) {

				return null;
			}

			@Override
			public String resolvePropertyPlaceholders(String text) throws Exception {

				return null;
			}

			@Override
			public Language resolveLanguage(String language) {

				return null;
			}

			@Override
			public DataFormatDefinition resolveDataFormatDefinition(String name) {

				return null;
			}

			@Override
			public DataFormat resolveDataFormat(String name) {

				return null;
			}

			@Override
			public String resolveComponentDefaultName(String javaType) {

				return null;
			}

			@Override
			public boolean removeService(Object object) throws Exception {

				return false;
			}

			@Override
			public void removeRouteDefinitions(Collection<RouteDefinition> routeDefinitions) throws Exception {


			}

			@Override
			public void removeRouteDefinition(RouteDefinition routeDefinition) throws Exception {


			}

			@Override
			public boolean removeRoute(String routeId) throws Exception {

				return false;
			}

			@Override
			public Collection<Endpoint> removeEndpoints(String pattern) throws Exception {

				return null;
			}

			@Override
			public void removeEndpoint(Endpoint endpoint) throws Exception {


			}

			@Override
			public Component removeComponent(String componentName) {

				return null;
			}

			@Override
			public RoutesDefinition loadRoutesDefinition(InputStream is) throws Exception {

				return null;
			}

			@Override
			public RestsDefinition loadRestsDefinition(InputStream is) throws Exception {

				return null;
			}

			@Override
			public boolean isVetoStarted() {

				return false;
			}

			@Override
			public Boolean isUseMDCLogging() {

				return null;
			}

			@Override
			public Boolean isUseDataType() {

				return true;
			}

			@Override
			public Boolean isUseBreadcrumb() {

				return null;
			}

			@Override
			public Boolean isTypeConverterStatisticsEnabled() {

				return null;
			}

			@Override
			public boolean isStartingRoutes() {

				return false;
			}

			@Override
			public boolean isSetupRoutes() {

				return false;
			}

			@Override
			public Boolean isLoadTypeConverters() {

				return null;
			}

			@Override
			public Boolean isLazyLoadTypeConverters() {

				return null;
			}

			@Override
			public <T> Set<T> hasServices(Class<T> type) {

				return null;
			}

			@Override
			public <T> T hasService(Class<T> type) {

				return null;
			}

			@Override
			public boolean hasService(Object object) {

				return false;
			}

			@Override
			public Endpoint hasEndpoint(String uri) {

				return null;
			}

			@Override
			public Component hasComponent(String componentName) {

				return null;
			}

			@Override
			public String getVersion() {

				return null;
			}

			@Override
			public List<ValidatorDefinition> getValidators() {

				return null;
			}

			@Override
			public ValidatorRegistry<? extends ValueHolder<String>> getValidatorRegistry() {

				return null;
			}

			@Override
			public UuidGenerator getUuidGenerator() {

				return null;
			}

			@Override
			public long getUptimeMillis() {

				return 0;
			}

			@Override
			public String getUptime() {

				return null;
			}

			@Override
			public UnitOfWorkFactory getUnitOfWorkFactory() {

				return null;
			}

			@Override
			public TypeConverterRegistry getTypeConverterRegistry() {

				return null;
			}

			@Override
			public TypeConverter getTypeConverter() {

				return null;
			}

			@Override
			public List<TransformerDefinition> getTransformers() {

				return null;
			}

			@Override
			public TransformerRegistry<? extends ValueHolder<String>> getTransformerRegistry() {

				return null;
			}

			@Override
			public StreamCachingStrategy getStreamCachingStrategy() {

				return null;
			}

			@Override
			public ServiceStatus getStatus() {

				return null;
			}

			@Override
			public ShutdownStrategy getShutdownStrategy() {

				return null;
			}

			@Override
			public ServiceCallConfigurationDefinition getServiceCallConfiguration(String serviceName) {

				return null;
			}

			@Override
			public SSLContextParameters getSSLContextParameters() {

				return null;
			}

			@Override
			public RuntimeEndpointRegistry getRuntimeEndpointRegistry() {

				return null;
			}

			@Override
			public RuntimeCamelCatalog getRuntimeCamelCatalog() {

				return null;
			}

			@Override
			public List<Route> getRoutes() {

				return null;
			}

			@Override
			public ServiceStatus getRouteStatus(String routeId) {

				return null;
			}

			@Override
			public List<RouteStartupOrder> getRouteStartupOrder() {

				return null;
			}

			@Override
			public List<RoutePolicyFactory> getRoutePolicyFactories() {

				return null;
			}

			@Override
			public List<RouteDefinition> getRouteDefinitions() {

				return null;
			}

			@Override
			public RouteDefinition getRouteDefinition(String id) {

				return null;
			}

			@Override
			public RouteController getRouteController() {

				return null;
			}

			@Override
			public Route getRoute(String id) {

				return null;
			}

			@Override
			public RestRegistry getRestRegistry() {

				return null;
			}

			@Override
			public List<RestDefinition> getRestDefinitions() {

				return null;
			}

			@Override
			public Collection<RestConfiguration> getRestConfigurations() {

				return null;
			}

			@Override
			public RestConfiguration getRestConfiguration(String component, boolean defaultIfNotFound) {

				return null;
			}

			@Override
			public RestConfiguration getRestConfiguration() {

				return null;
			}

			@Override
			public ReloadStrategy getReloadStrategy() {

				return null;
			}

			@Override
			public <T> T getRegistry(Class<T> type) {

				return null;
			}

			@Override
			public Registry getRegistry() {

				return null;
			}

			@Override
			public String getPropertySuffixToken() {

				return null;
			}

			@Override
			public String getPropertyPrefixToken() {

				return null;
			}

			@Override
			public String getProperty(String key) {

				return null;
			}

			@Override
			public Map<String, String> getProperties() {

				return null;
			}

			@Override
			public ServicePool<Endpoint, Producer> getProducerServicePool() {

				return null;
			}

			@Override
			public ProcessorFactory getProcessorFactory() {

				return null;
			}

			@Override
			public <T extends ProcessorDefinition> T getProcessorDefinition(String id, Class<T> type) {

				return null;
			}

			@Override
			public ProcessorDefinition getProcessorDefinition(String id) {

				return null;
			}

			@Override
			public <T extends Processor> T getProcessor(String id, Class<T> type) {

				return null;
			}

			@Override
			public Processor getProcessor(String id) {

				return null;
			}

			@Override
			public ServicePool<Endpoint, PollingConsumer> getPollingConsumerServicePool() {

				return null;
			}

			@Override
			public PackageScanClassResolver getPackageScanClassResolver() {

				return null;
			}

			@Override
			public NodeIdFactory getNodeIdFactory() {

				return null;
			}

			@Override
			public CamelContextNameStrategy getNameStrategy() {

				return null;
			}

			@Override
			public String getName() {

				return null;
			}

			@Override
			public ModelJAXBContextFactory getModelJAXBContextFactory() {

				return null;
			}

			@Override
			public MessageHistoryFactory getMessageHistoryFactory() {

				return null;
			}

			@Override
			public ManagementStrategy getManagementStrategy() {

				return null;
			}

			@Override
			public ManagementNameStrategy getManagementNameStrategy() {

				return null;
			}

			@Override
			public String getManagementName() {

				return null;
			}

			@Override
			public ManagementMBeanAssembler getManagementMBeanAssembler() {

				return null;
			}

			@Override
			public <T extends ManagedRouteMBean> T getManagedRoute(String routeId, Class<T> type) {

				return null;
			}

			@Override
			public <T extends ManagedProcessorMBean> T getManagedProcessor(String id, Class<T> type) {

				return null;
			}

			@Override
			public ManagedCamelContextMBean getManagedCamelContext() {

				return null;
			}

			@Override
			public Set<LogListener> getLogListeners() {

				return null;
			}

			@Override
			public List<LifecycleStrategy> getLifecycleStrategies() {

				return null;
			}

			@Override
			public String getLanguageParameterJsonSchema(String languageName) throws IOException {

				return null;
			}

			@Override
			public List<String> getLanguageNames() {

				return null;
			}

			@Override
			public List<InterceptStrategy> getInterceptStrategies() {

				return null;
			}

			@Override
			public Injector getInjector() {

				return null;
			}

			@Override
			public InflightRepository getInflightRepository() {

				return null;
			}

			@Override
			public HystrixConfigurationDefinition getHystrixConfiguration(String id) {

				return null;
			}

			@Override
			public HealthCheckRegistry getHealthCheckRegistry() {

				return null;
			}

			@Override
			public HeadersMapFactory getHeadersMapFactory() {

				return new HeadersMapFactory() {

					@Override
					public Map<String, Object> newMap(Map<String, Object> map) {

						return null;
					}

					@Override
					public Map<String, Object> newMap() {

						return new HashMap<String, Object>();
					}

					@Override
					public boolean isInstanceOf(Map<String, Object> map) {

						return false;
					}

					@Override
					public boolean isCaseInsensitive() {

						return false;
					}
				};
			}

			@Override
			public Map<String, String> getGlobalOptions() {
				return null;
			}

			@Override
			public String getGlobalOption(String key) {
				return null;
			}

			@Override
			public FactoryFinder getFactoryFinder(String path) throws NoFactoryAvailableException {
				return null;
			}

			@Override
			public ExecutorServiceStrategy getExecutorServiceStrategy() {
				return null;
			}

			@Override
			public ExecutorServiceManager getExecutorServiceManager() {
				return null;
			}

			@Override
			public ScheduledExecutorService getErrorHandlerExecutorService() {
				return null;
			}

			@Override
			public ErrorHandlerBuilder getErrorHandlerBuilder() {
				return null;
			}

			@Override
			public Collection<Endpoint> getEndpoints() {
				return null;
			}

			@Override
			public EndpointRegistry<? extends ValueHolder<String>> getEndpointRegistry() {

				return null;
			}

			@Override
			public Map<String, Endpoint> getEndpointMap() {
				return null;
			}

			@Override
			public <T extends Endpoint> T getEndpoint(String name, Class<T> endpointType) {

				return null;
			}

			@Override
			public Endpoint getEndpoint(String uri) {
				return null;
			}

			@Override
			public String getEipParameterJsonSchema(String eipName) throws IOException {
				return null;
			}

			@Override
			public InterceptStrategy getDefaultTracer() {

				return null;
			}

			@Override
			public FactoryFinder getDefaultFactoryFinder() {
				return null;
			}

			@Override
			public InterceptStrategy getDefaultBacklogTracer() {
				return null;
			}

			@Override
			public InterceptStrategy getDefaultBacklogDebugger() {
				return null;
			}

			@Override
			public Debugger getDebugger() {
				return null;
			}

			@Override
			public Map<String, DataFormatDefinition> getDataFormats() {
				return null;
			}

			@Override
			public DataFormatResolver getDataFormatResolver() {
				return null;
			}

			@Override
			public String getDataFormatParameterJsonSchema(String dataFormatName) throws IOException {
				return null;
			}

			@Override
			public String getComponentParameterJsonSchema(String componentName) throws IOException {
				return null;
			}

			@Override
			public List<String> getComponentNames() {
				return null;
			}

			@Override
			public String getComponentDocumentation(String componentName) throws IOException {
				return null;
			}

			@Override
			public Component getComponent(String name, boolean autoCreateComponents, boolean autoStart) {
				return null;
			}

			@Override
			public <T extends Component> T getComponent(String name, Class<T> componentType) {
				return null;
			}

			@Override
			public Component getComponent(String name, boolean autoCreateComponents) {
				return null;
			}

			@Override
			public Component getComponent(String componentName) {
				return null;
			}

			@Override
			public ClassResolver getClassResolver() {
				return null;
			}

			@Override
			public AsyncProcessorAwaitManager getAsyncProcessorAwaitManager() {
				return null;
			}

			@Override
			public ClassLoader getApplicationContextClassLoader() {
				return null;
			}

			@Override
			public Map<String, Properties> findEips() throws LoadPropertiesException, IOException {
				return null;
			}

			@Override
			public Map<String, Properties> findComponents() throws LoadPropertiesException, IOException {
				return null;
			}

			@Override
			public String explainEndpointJson(String uri, boolean includeAllOptions) {
				return null;
			}

			@Override
			public String explainEipJson(String nameOrId, boolean includeAllOptions) {
				return null;
			}

			@Override
			public String explainDataFormatJson(String dataFormatName, DataFormat dataFormat,
					boolean includeAllOptions) {
				return null;
			}

			@Override
			public String explainComponentJson(String componentName, boolean includeAllOptions) {
				return null;
			}

			@Override
			public void disableJMX() throws IllegalStateException {
			}

			@Override
			public void deferStartService(Object object, boolean stopOnShutdown) throws Exception {
			}

			@Override
			public String createRouteStaticEndpointJson(String routeId, boolean includeDynamic) {
				return null;
			}

			@Override
			public String createRouteStaticEndpointJson(String routeId) {
				return null;
			}

			@Override
			public ProducerTemplate createProducerTemplate(int maximumCacheSize) {
				return null;
			}

			@Override
			public ProducerTemplate createProducerTemplate() {
				return null;
			}

			@Override
			public FluentProducerTemplate createFluentProducerTemplate(int maximumCacheSize) {
				return null;
			}

			@Override
			public FluentProducerTemplate createFluentProducerTemplate() {
				return null;
			}

			@Override
			public DataFormat createDataFormat(String name) {
				return null;
			}

			@Override
			public ConsumerTemplate createConsumerTemplate(int maximumCacheSize) {
				return null;
			}

			@Override
			public ConsumerTemplate createConsumerTemplate() {
          return null;
			}

			@Override
			public void addStartupListener(StartupListener listener) throws Exception {
			}

			@Override
			public void addServiceCallConfiguration(String serviceName,
					ServiceCallConfigurationDefinition configuration) {
			}

			@Override
			public void addService(Object object, boolean stopOnShutdown, boolean forceStart) throws Exception {
			}

			@Override
			public void addService(Object object, boolean stopOnShutdown) throws Exception {
			}

			@Override
			public void addService(Object object) throws Exception {
			}

			@Override
			public void addRoutes(RoutesBuilder builder) throws Exception {
			}

			@Override
			public void addRoutePolicyFactory(RoutePolicyFactory routePolicyFactory) {
			}

			@Override
			public void addRouteDefinitions(Collection<RouteDefinition> routeDefinitions) throws Exception {
			}

			@Override
			public void addRouteDefinition(RouteDefinition routeDefinition) throws Exception {
			}

			@Override
			public void addRestDefinitions(Collection<RestDefinition> restDefinitions) throws Exception {
			}

			@Override
			public void addRestConfiguration(RestConfiguration restConfiguration) {
			}

			@Override
			public void addRegisterEndpointCallback(EndpointStrategy strategy) {

			}

			@Override
			public void addLogListener(LogListener listener) {

			}

			@Override
			public void addLifecycleStrategy(LifecycleStrategy lifecycleStrategy) {
			}

			@Override
			public void addInterceptStrategy(InterceptStrategy interceptStrategy) {

			}

			@Override
			public void addHystrixConfiguration(String id, HystrixConfigurationDefinition configuration) {
			}

			@Override
			public Endpoint addEndpoint(String uri, Endpoint endpoint) throws Exception {
				return null;
			}

			@Override
			public void addComponent(String componentName, Component component) {

			}

			@Override
			public <T extends CamelContext> T adapt(Class<T> type) {

				return null;
			}
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testRoutePredicatePositiveForCompleteAsProcessed() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10002100741000120201231071308");
		messageDTO.setReg_type("NEW");
		messageDTO.setIteration(1);
		messageDTO.setSource("REGISTRATION_CLIENT");
		exchange.getMessage().setBody(objectMapper.writeValueAsString(messageDTO));
		exchange.getMessage().setHeader(Exchange.INTERCEPTED_ENDPOINT, "workflow-cmd://complete-as-processed");
		assertTrue(workflowCommandPredicate.matches(exchange));
		;
		WorkflowInternalActionDTO workflowInternalActionDTO=objectMapper.readValue(exchange.getMessage()
				.getBody().toString(), WorkflowInternalActionDTO.class);
		assertEquals(WorkflowInternalActionCode.COMPLETE_AS_PROCESSED.toString(),
				workflowInternalActionDTO.getActionCode());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testRoutePredicatePositiveForCompleteAsRejected() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10002100741000120201231071308");
		messageDTO.setReg_type("NEW");
		messageDTO.setIteration(1);
		messageDTO.setSource("REGISTRATION_CLIENT");
		exchange.getMessage().setBody(objectMapper.writeValueAsString(messageDTO));
		exchange.getMessage().setHeader(Exchange.INTERCEPTED_ENDPOINT, ("workflow-cmd://complete-as-rejected"));
		assertTrue(workflowCommandPredicate.matches(exchange));
		WorkflowInternalActionDTO workflowInternalActionDTO = objectMapper
				.readValue(exchange.getMessage().getBody().toString(), WorkflowInternalActionDTO.class);
		assertEquals(WorkflowInternalActionCode.COMPLETE_AS_REJECTED.toString(),
				workflowInternalActionDTO.getActionCode());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testRoutePredicatePositiveForMarkAsReprocess() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10002100741000120201231071308");
		messageDTO.setReg_type("NEW");
		messageDTO.setIteration(1);
		messageDTO.setSource("REGISTRATION_CLIENT");
		exchange.getMessage().setBody(objectMapper.writeValueAsString(messageDTO));
		exchange.getMessage().setHeader(Exchange.INTERCEPTED_ENDPOINT, "workflow-cmd://mark-as-reprocess");
		assertTrue(workflowCommandPredicate.matches(exchange));
		WorkflowInternalActionDTO workflowInternalActionDTO = objectMapper
				.readValue(exchange.getMessage().getBody().toString(), WorkflowInternalActionDTO.class);
		assertEquals(WorkflowInternalActionCode.MARK_AS_REPROCESS.toString(),
				workflowInternalActionDTO.getActionCode());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testRoutePredicatePositiveForCompleteAsFailed() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10002100741000120201231071308");
		messageDTO.setReg_type("NEW");
		messageDTO.setIteration(1);
		messageDTO.setSource("REGISTRATION_CLIENT");
		exchange.getMessage().setBody(objectMapper.writeValueAsString(messageDTO));
		exchange.getMessage().setHeader(Exchange.INTERCEPTED_ENDPOINT, "workflow-cmd://complete-as-failed");
		assertTrue(workflowCommandPredicate.matches(exchange));
		WorkflowInternalActionDTO workflowInternalActionDTO = objectMapper
				.readValue(exchange.getMessage().getBody().toString(), WorkflowInternalActionDTO.class);
		assertEquals(WorkflowInternalActionCode.COMPLETE_AS_FAILED.toString(),
				workflowInternalActionDTO.getActionCode());
	}

	@SuppressWarnings("unchecked")
	@Test(expected = BaseUncheckedException.class)
	public void testRoutePredicateNegative() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10002100741000120201231071308");
		messageDTO.setReg_type("NEW");
		messageDTO.setIteration(1);
		messageDTO.setSource("REGISTRATION_CLIENT");
		exchange.getMessage().setBody(objectMapper.writeValueAsString(messageDTO));
		exchange.getMessage().setHeader(Exchange.INTERCEPTED_ENDPOINT, null);
		workflowCommandPredicate.matches(exchange);
	}

	@SuppressWarnings("unchecked")
	@Test(expected = BaseUncheckedException.class)
	public void testRoutePredicateDefault() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10002100741000120201231071308");
		messageDTO.setReg_type("NEW");
		messageDTO.setIteration(1);
		messageDTO.setSource("REGISTRATION_CLIENT");
		exchange.getMessage().setBody(objectMapper.writeValueAsString(messageDTO));
		exchange.getMessage().setHeader(Exchange.INTERCEPTED_ENDPOINT, "workflow-cmd://complete-as-faled");
		workflowCommandPredicate.matches(exchange);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testRoutePredicatePositiveForPauseAndRequestAdditionalInfo() throws Exception
	{
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10002100741000120201231071308");
		messageDTO.setReg_type("NEW");
		messageDTO.setIteration(1);
		messageDTO.setSource("REGISTRATION_CLIENT");
		exchange.getMessage().setBody(objectMapper.writeValueAsString(messageDTO));
		exchange.getMessage().setHeader(Exchange.INTERCEPTED_ENDPOINT,
				"workflow-cmd://pause-and-request-additional-info");
		exchange.setProperty(JsonConstant.ADDITIONAL_INFO_PROCESS, "CORRECTION");
		exchange.setProperty(JsonConstant.PAUSE_FOR, "200");
		assertTrue(workflowCommandPredicate.matches(exchange));
		WorkflowInternalActionDTO workflowInternalActionDTO = objectMapper
				.readValue(exchange.getMessage().getBody().toString(), WorkflowInternalActionDTO.class);
		assertEquals(WorkflowInternalActionCode.PAUSE_AND_REQUEST_ADDITIONAL_INFO.toString(),
				workflowInternalActionDTO.getActionCode());
	}
	@SuppressWarnings("unchecked")
	@Test
	public void testRoutePredicatePositiveForRestartParentFlow() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10002100741000120201231071308");
		messageDTO.setReg_type("CORRECTION");
		messageDTO.setIteration(1);
		messageDTO.setSource("REGISTRATION_CLIENT");
		exchange.getMessage().setBody(objectMapper.writeValueAsString(messageDTO));
		exchange.getMessage().setHeader(Exchange.INTERCEPTED_ENDPOINT, "workflow-cmd://restart-parent-flow");
		assertTrue(workflowCommandPredicate.matches(exchange));
		WorkflowInternalActionDTO workflowInternalActionDTO = objectMapper
				.readValue(exchange.getMessage().getBody().toString(), WorkflowInternalActionDTO.class);
		assertEquals(WorkflowInternalActionCode.RESTART_PARENT_FLOW.toString(),
				workflowInternalActionDTO.getActionCode());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testRoutePredicatePositiveForCompleteAsRejectedWithoutParentFlow() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10002100741000120201231071308");
		messageDTO.setReg_type("CORRECTION");
		messageDTO.setIteration(1);
		messageDTO.setSource("REGISTRATION_CLIENT");
		exchange.getMessage().setBody(objectMapper.writeValueAsString(messageDTO));
		exchange.getMessage().setHeader(Exchange.INTERCEPTED_ENDPOINT,
				("workflow-cmd://complete-as-rejected-without-parent-flow"));
		assertTrue(workflowCommandPredicate.matches(exchange));
		WorkflowInternalActionDTO workflowInternalActionDTO = objectMapper
				.readValue(exchange.getMessage().getBody().toString(), WorkflowInternalActionDTO.class);
		assertEquals(WorkflowInternalActionCode.COMPLETE_AS_REJECTED_WITHOUT_PARENT_FLOW.toString(),
				workflowInternalActionDTO.getActionCode());
	}

}

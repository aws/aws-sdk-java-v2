package software.amazon.awssdk.services.xml;

import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.AsyncAws4Signer;
import software.amazon.awssdk.auth.token.signer.aws.BearerTokenSigner;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.handler.AwsAsyncClientHandler;
import software.amazon.awssdk.awscore.eventstream.EventStreamAsyncResponseTransformer;
import software.amazon.awssdk.awscore.eventstream.EventStreamTaggedUnionPojoSupplier;
import software.amazon.awssdk.awscore.eventstream.RestEventStreamAsyncResponseTransformer;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.CredentialType;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.SdkPojoBuilder;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.AsyncResponseTransformerUtils;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.AsyncClientHandler;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksumRequired;
import software.amazon.awssdk.core.internal.interceptor.trait.RequestCompression;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.runtime.transform.AsyncStreamingRequestMarshaller;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.xml.AwsXmlProtocolFactory;
import software.amazon.awssdk.protocols.xml.XmlOperationMetadata;
import software.amazon.awssdk.services.xml.model.APostOperationRequest;
import software.amazon.awssdk.services.xml.model.APostOperationResponse;
import software.amazon.awssdk.services.xml.model.APostOperationWithOutputRequest;
import software.amazon.awssdk.services.xml.model.APostOperationWithOutputResponse;
import software.amazon.awssdk.services.xml.model.BearerAuthOperationRequest;
import software.amazon.awssdk.services.xml.model.BearerAuthOperationResponse;
import software.amazon.awssdk.services.xml.model.EventStream;
import software.amazon.awssdk.services.xml.model.EventStreamOperationRequest;
import software.amazon.awssdk.services.xml.model.EventStreamOperationResponse;
import software.amazon.awssdk.services.xml.model.EventStreamOperationResponseHandler;
import software.amazon.awssdk.services.xml.model.GetOperationWithChecksumRequest;
import software.amazon.awssdk.services.xml.model.GetOperationWithChecksumResponse;
import software.amazon.awssdk.services.xml.model.InvalidInputException;
import software.amazon.awssdk.services.xml.model.OperationWithChecksumRequiredRequest;
import software.amazon.awssdk.services.xml.model.OperationWithChecksumRequiredResponse;
import software.amazon.awssdk.services.xml.model.OperationWithNoneAuthTypeRequest;
import software.amazon.awssdk.services.xml.model.OperationWithNoneAuthTypeResponse;
import software.amazon.awssdk.services.xml.model.OperationWithRequestCompressionRequest;
import software.amazon.awssdk.services.xml.model.OperationWithRequestCompressionResponse;
import software.amazon.awssdk.services.xml.model.PutOperationWithChecksumRequest;
import software.amazon.awssdk.services.xml.model.PutOperationWithChecksumResponse;
import software.amazon.awssdk.services.xml.model.StreamingInputOperationRequest;
import software.amazon.awssdk.services.xml.model.StreamingInputOperationResponse;
import software.amazon.awssdk.services.xml.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.services.xml.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.services.xml.model.XmlException;
import software.amazon.awssdk.services.xml.model.XmlRequest;
import software.amazon.awssdk.services.xml.transform.APostOperationRequestMarshaller;
import software.amazon.awssdk.services.xml.transform.APostOperationWithOutputRequestMarshaller;
import software.amazon.awssdk.services.xml.transform.BearerAuthOperationRequestMarshaller;
import software.amazon.awssdk.services.xml.transform.EventStreamOperationRequestMarshaller;
import software.amazon.awssdk.services.xml.transform.GetOperationWithChecksumRequestMarshaller;
import software.amazon.awssdk.services.xml.transform.OperationWithChecksumRequiredRequestMarshaller;
import software.amazon.awssdk.services.xml.transform.OperationWithNoneAuthTypeRequestMarshaller;
import software.amazon.awssdk.services.xml.transform.OperationWithRequestCompressionRequestMarshaller;
import software.amazon.awssdk.services.xml.transform.PutOperationWithChecksumRequestMarshaller;
import software.amazon.awssdk.services.xml.transform.StreamingInputOperationRequestMarshaller;
import software.amazon.awssdk.services.xml.transform.StreamingOutputOperationRequestMarshaller;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Pair;

/**
 * Internal implementation of {@link XmlAsyncClient}.
 *
 * @see XmlAsyncClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultXmlAsyncClient implements XmlAsyncClient {
    private static final Logger log = LoggerFactory.getLogger(DefaultXmlAsyncClient.class);

    private final AsyncClientHandler clientHandler;

    private final AwsXmlProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    private final XmlServiceClientConfiguration serviceClientConfiguration;

    private final Executor executor;

    protected DefaultXmlAsyncClient(XmlServiceClientConfiguration serviceClientConfiguration,
                                    SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsAsyncClientHandler(clientConfiguration);
        this.clientConfiguration = clientConfiguration;
        this.serviceClientConfiguration = serviceClientConfiguration;
        this.protocolFactory = init();
        this.executor = clientConfiguration.option(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR);
    }

    /**
     * <p>
     * Performs a post operation to the xml service and has no output
     * </p>
     *
     * @param aPostOperationRequest
     * @return A Java Future containing the result of the APostOperation operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidInputException The request was rejected because an invalid or out-of-range value was supplied
     *         for an input parameter.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>XmlException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample XmlAsyncClient.APostOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/APostOperation" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public CompletableFuture<APostOperationResponse> aPostOperation(APostOperationRequest aPostOperationRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, aPostOperationRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Xml Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "APostOperation");

            HttpResponseHandler<Response<APostOperationResponse>> responseHandler = protocolFactory
                .createCombinedResponseHandler(APostOperationResponse::builder,
                                               new XmlOperationMetadata().withHasStreamingSuccessResponse(false));
            String hostPrefix = "foo-";
            String resolvedHostExpression = "foo-";

            CompletableFuture<APostOperationResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<APostOperationRequest, APostOperationResponse>()
                             .withOperationName("APostOperation")
                             .withMarshaller(new APostOperationRequestMarshaller(protocolFactory))
                             .withCombinedResponseHandler(responseHandler).hostPrefixExpression(resolvedHostExpression)
                             .withMetricCollector(apiCallMetricCollector).withInput(aPostOperationRequest));
            CompletableFuture<APostOperationResponse> whenCompleteFuture = null;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
            return whenCompleteFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Performs a post operation to the xml service and has modelled output
     * </p>
     *
     * @param aPostOperationWithOutputRequest
     * @return A Java Future containing the result of the APostOperationWithOutput operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidInputException The request was rejected because an invalid or out-of-range value was supplied
     *         for an input parameter.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>XmlException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample XmlAsyncClient.APostOperationWithOutput
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/APostOperationWithOutput"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<APostOperationWithOutputResponse> aPostOperationWithOutput(
        APostOperationWithOutputRequest aPostOperationWithOutputRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, aPostOperationWithOutputRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Xml Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "APostOperationWithOutput");

            HttpResponseHandler<Response<APostOperationWithOutputResponse>> responseHandler = protocolFactory
                .createCombinedResponseHandler(APostOperationWithOutputResponse::builder,
                                               new XmlOperationMetadata().withHasStreamingSuccessResponse(false));

            CompletableFuture<APostOperationWithOutputResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<APostOperationWithOutputRequest, APostOperationWithOutputResponse>()
                             .withOperationName("APostOperationWithOutput")
                             .withMarshaller(new APostOperationWithOutputRequestMarshaller(protocolFactory))
                             .withCombinedResponseHandler(responseHandler).withMetricCollector(apiCallMetricCollector)
                             .withInput(aPostOperationWithOutputRequest));
            CompletableFuture<APostOperationWithOutputResponse> whenCompleteFuture = null;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
            return whenCompleteFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Invokes the BearerAuthOperation operation asynchronously.
     *
     * @param bearerAuthOperationRequest
     * @return A Java Future containing the result of the BearerAuthOperation operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>XmlException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample XmlAsyncClient.BearerAuthOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/BearerAuthOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<BearerAuthOperationResponse> bearerAuthOperation(
        BearerAuthOperationRequest bearerAuthOperationRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, bearerAuthOperationRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Xml Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "BearerAuthOperation");
            bearerAuthOperationRequest = applySignerOverride(bearerAuthOperationRequest, BearerTokenSigner.create());

            HttpResponseHandler<Response<BearerAuthOperationResponse>> responseHandler = protocolFactory
                .createCombinedResponseHandler(BearerAuthOperationResponse::builder,
                                               new XmlOperationMetadata().withHasStreamingSuccessResponse(false));

            CompletableFuture<BearerAuthOperationResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<BearerAuthOperationRequest, BearerAuthOperationResponse>()
                             .withOperationName("BearerAuthOperation")
                             .withMarshaller(new BearerAuthOperationRequestMarshaller(protocolFactory))
                             .withCombinedResponseHandler(responseHandler).credentialType(CredentialType.TOKEN)
                             .withMetricCollector(apiCallMetricCollector).withInput(bearerAuthOperationRequest));
            CompletableFuture<BearerAuthOperationResponse> whenCompleteFuture = null;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
            return whenCompleteFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Invokes the EventStreamOperation operation asynchronously.
     *
     * @param eventStreamOperationRequest
     * @return A Java Future containing the result of the EventStreamOperation operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>XmlException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample XmlAsyncClient.EventStreamOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/EventStreamOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<Void> eventStreamOperation(EventStreamOperationRequest eventStreamOperationRequest,
                                                        EventStreamOperationResponseHandler asyncResponseHandler) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, eventStreamOperationRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Xml Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "EventStreamOperation");
            HttpResponseHandler<EventStreamOperationResponse> responseHandler = protocolFactory.createResponseHandler(
                EventStreamOperationResponse::builder, XmlOperationMetadata.builder().hasStreamingSuccessResponse(true)
                                                                           .build());
            HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();
            HttpResponseHandler<? extends EventStream> eventResponseHandler = protocolFactory.createResponseHandler(
                EventStreamTaggedUnionPojoSupplier.builder()
                                                  .putSdkPojoSupplier("EventPayloadEvent", EventStream::eventPayloadEventBuilder)
                                                  .putSdkPojoSupplier("NonEventPayloadEvent", EventStream::nonEventPayloadEventBuilder)
                                                  .putSdkPojoSupplier("SecondEventPayloadEvent", EventStream::secondEventPayloadEventBuilder)
                                                  .defaultSdkPojoSupplier(() -> new SdkPojoBuilder(EventStream.UNKNOWN)).build(), XmlOperationMetadata
                    .builder().hasStreamingSuccessResponse(false).build());
            CompletableFuture<Void> eventStreamTransformFuture = new CompletableFuture<>();
            EventStreamAsyncResponseTransformer<EventStreamOperationResponse, EventStream> asyncResponseTransformer = EventStreamAsyncResponseTransformer
                .<EventStreamOperationResponse, EventStream> builder().eventStreamResponseHandler(asyncResponseHandler)
                .eventResponseHandler(eventResponseHandler).initialResponseHandler(responseHandler)
                .exceptionResponseHandler(errorResponseHandler).future(eventStreamTransformFuture).executor(executor)
                .serviceName(serviceName()).build();
            RestEventStreamAsyncResponseTransformer<EventStreamOperationResponse, EventStream> restAsyncResponseTransformer = RestEventStreamAsyncResponseTransformer
                .<EventStreamOperationResponse, EventStream> builder()
                .eventStreamAsyncResponseTransformer(asyncResponseTransformer)
                .eventStreamResponseHandler(asyncResponseHandler).build();

            CompletableFuture<Void> executeFuture = clientHandler.execute(
                new ClientExecutionParams<EventStreamOperationRequest, EventStreamOperationResponse>()
                    .withOperationName("EventStreamOperation")
                    .withMarshaller(new EventStreamOperationRequestMarshaller(protocolFactory))
                    .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                    .withMetricCollector(apiCallMetricCollector).withInput(eventStreamOperationRequest),
                restAsyncResponseTransformer);
            CompletableFuture<Void> whenCompleteFuture = null;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                if (e != null) {
                    runAndLogError(log, "Exception thrown in exceptionOccurred callback, ignoring",
                                   () -> asyncResponseHandler.exceptionOccurred(e));
                    eventStreamTransformFuture.completeExceptionally(e);
                }
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
            return CompletableFutureUtils.forwardExceptionTo(eventStreamTransformFuture, executeFuture);
        } catch (Throwable t) {
            runAndLogError(log, "Exception thrown in exceptionOccurred callback, ignoring",
                           () -> asyncResponseHandler.exceptionOccurred(t));
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Invokes the GetOperationWithChecksum operation asynchronously.
     *
     * @param getOperationWithChecksumRequest
     * @return A Java Future containing the result of the GetOperationWithChecksum operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>XmlException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample XmlAsyncClient.GetOperationWithChecksum
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/GetOperationWithChecksum"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<GetOperationWithChecksumResponse> getOperationWithChecksum(
        GetOperationWithChecksumRequest getOperationWithChecksumRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, getOperationWithChecksumRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Xml Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetOperationWithChecksum");

            HttpResponseHandler<Response<GetOperationWithChecksumResponse>> responseHandler = protocolFactory
                .createCombinedResponseHandler(GetOperationWithChecksumResponse::builder,
                                               new XmlOperationMetadata().withHasStreamingSuccessResponse(false));

            CompletableFuture<GetOperationWithChecksumResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<GetOperationWithChecksumRequest, GetOperationWithChecksumResponse>()
                             .withOperationName("GetOperationWithChecksum")
                             .withMarshaller(new GetOperationWithChecksumRequestMarshaller(protocolFactory))
                             .withCombinedResponseHandler(responseHandler)
                             .withMetricCollector(apiCallMetricCollector)
                             .putExecutionAttribute(
                                 SdkInternalExecutionAttribute.HTTP_CHECKSUM,
                                 HttpChecksum.builder().requestChecksumRequired(true)
                                             .requestAlgorithm(getOperationWithChecksumRequest.checksumAlgorithmAsString())
                                             .isRequestStreaming(false).build()).withInput(getOperationWithChecksumRequest));
            CompletableFuture<GetOperationWithChecksumResponse> whenCompleteFuture = null;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
            return whenCompleteFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Invokes the OperationWithChecksumRequired operation asynchronously.
     *
     * @param operationWithChecksumRequiredRequest
     * @return A Java Future containing the result of the OperationWithChecksumRequired operation returned by the
     *         service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>XmlException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample XmlAsyncClient.OperationWithChecksumRequired
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/OperationWithChecksumRequired"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<OperationWithChecksumRequiredResponse> operationWithChecksumRequired(
        OperationWithChecksumRequiredRequest operationWithChecksumRequiredRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         operationWithChecksumRequiredRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Xml Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithChecksumRequired");

            HttpResponseHandler<Response<OperationWithChecksumRequiredResponse>> responseHandler = protocolFactory
                .createCombinedResponseHandler(OperationWithChecksumRequiredResponse::builder,
                                               new XmlOperationMetadata().withHasStreamingSuccessResponse(false));

            CompletableFuture<OperationWithChecksumRequiredResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<OperationWithChecksumRequiredRequest, OperationWithChecksumRequiredResponse>()
                             .withOperationName("OperationWithChecksumRequired")
                             .withMarshaller(new OperationWithChecksumRequiredRequestMarshaller(protocolFactory))
                             .withCombinedResponseHandler(responseHandler)
                             .withMetricCollector(apiCallMetricCollector)
                             .putExecutionAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM_REQUIRED,
                                                    HttpChecksumRequired.create()).withInput(operationWithChecksumRequiredRequest));
            CompletableFuture<OperationWithChecksumRequiredResponse> whenCompleteFuture = null;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
            return whenCompleteFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Invokes the OperationWithNoneAuthType operation asynchronously.
     *
     * @param operationWithNoneAuthTypeRequest
     * @return A Java Future containing the result of the OperationWithNoneAuthType operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>XmlException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample XmlAsyncClient.OperationWithNoneAuthType
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/OperationWithNoneAuthType"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<OperationWithNoneAuthTypeResponse> operationWithNoneAuthType(
        OperationWithNoneAuthTypeRequest operationWithNoneAuthTypeRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, operationWithNoneAuthTypeRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Xml Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithNoneAuthType");

            HttpResponseHandler<Response<OperationWithNoneAuthTypeResponse>> responseHandler = protocolFactory
                .createCombinedResponseHandler(OperationWithNoneAuthTypeResponse::builder,
                                               new XmlOperationMetadata().withHasStreamingSuccessResponse(false));

            CompletableFuture<OperationWithNoneAuthTypeResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<OperationWithNoneAuthTypeRequest, OperationWithNoneAuthTypeResponse>()
                             .withOperationName("OperationWithNoneAuthType")
                             .withMarshaller(new OperationWithNoneAuthTypeRequestMarshaller(protocolFactory))
                             .withCombinedResponseHandler(responseHandler).withMetricCollector(apiCallMetricCollector)
                             .putExecutionAttribute(SdkInternalExecutionAttribute.IS_NONE_AUTH_TYPE_REQUEST, false)
                             .withInput(operationWithNoneAuthTypeRequest));
            CompletableFuture<OperationWithNoneAuthTypeResponse> whenCompleteFuture = null;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
            return whenCompleteFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Invokes the OperationWithRequestCompression operation asynchronously.
     *
     * @param operationWithRequestCompressionRequest
     * @return A Java Future containing the result of the OperationWithRequestCompression operation returned by the
     *         service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>XmlException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample XmlAsyncClient.OperationWithRequestCompression
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/OperationWithRequestCompression"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<OperationWithRequestCompressionResponse> operationWithRequestCompression(
        OperationWithRequestCompressionRequest operationWithRequestCompressionRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         operationWithRequestCompressionRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Xml Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithRequestCompression");

            HttpResponseHandler<Response<OperationWithRequestCompressionResponse>> responseHandler = protocolFactory
                .createCombinedResponseHandler(OperationWithRequestCompressionResponse::builder,
                                               new XmlOperationMetadata().withHasStreamingSuccessResponse(false));

            CompletableFuture<OperationWithRequestCompressionResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<OperationWithRequestCompressionRequest, OperationWithRequestCompressionResponse>()
                             .withOperationName("OperationWithRequestCompression")
                             .withMarshaller(new OperationWithRequestCompressionRequestMarshaller(protocolFactory))
                             .withCombinedResponseHandler(responseHandler)
                             .withMetricCollector(apiCallMetricCollector)
                             .putExecutionAttribute(SdkInternalExecutionAttribute.REQUEST_COMPRESSION,
                                                    RequestCompression.builder().encodings("gzip").isStreaming(false).build())
                             .withInput(operationWithRequestCompressionRequest));
            CompletableFuture<OperationWithRequestCompressionResponse> whenCompleteFuture = null;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
            return whenCompleteFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Invokes the PutOperationWithChecksum operation asynchronously.
     *
     * @param putOperationWithChecksumRequest
     * @param requestBody
     *        Functional interface that can be implemented to produce the request content in a non-blocking manner. The
     *        size of the content is expected to be known up front. See {@link AsyncRequestBody} for specific details on
     *        implementing this interface as well as links to precanned implementations for common scenarios like
     *        uploading from a file. The service documentation for the request content is as follows '
     *        <p>
     *        Object data.
     *        </p>
     *        '
     * @param asyncResponseTransformer
     *        The response transformer for processing the streaming response in a non-blocking manner. See
     *        {@link AsyncResponseTransformer} for details on how this callback should be implemented and for links to
     *        precanned implementations for common scenarios like downloading to a file. The service documentation for
     *        the response content is as follows '
     *        <p>
     *        Object data.
     *        </p>
     *        '.
     * @return A future to the transformed result of the AsyncResponseTransformer.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>XmlException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample XmlAsyncClient.PutOperationWithChecksum
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/PutOperationWithChecksum"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public <ReturnT> CompletableFuture<ReturnT> putOperationWithChecksum(
        PutOperationWithChecksumRequest putOperationWithChecksumRequest, AsyncRequestBody requestBody,
        AsyncResponseTransformer<PutOperationWithChecksumResponse, ReturnT> asyncResponseTransformer) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, putOperationWithChecksumRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Xml Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "PutOperationWithChecksum");
            Pair<AsyncResponseTransformer<PutOperationWithChecksumResponse, ReturnT>, CompletableFuture<Void>> pair = AsyncResponseTransformerUtils
                .wrapWithEndOfStreamFuture(asyncResponseTransformer);
            asyncResponseTransformer = pair.left();
            CompletableFuture<Void> endOfStreamFuture = pair.right();
            if (!isSignerOverridden(clientConfiguration)) {
                putOperationWithChecksumRequest = applySignerOverride(putOperationWithChecksumRequest, AsyncAws4Signer.create());
            }

            HttpResponseHandler<PutOperationWithChecksumResponse> responseHandler = protocolFactory.createResponseHandler(
                PutOperationWithChecksumResponse::builder, new XmlOperationMetadata().withHasStreamingSuccessResponse(true));

            HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();

            CompletableFuture<ReturnT> executeFuture = clientHandler.execute(
                new ClientExecutionParams<PutOperationWithChecksumRequest, PutOperationWithChecksumResponse>()
                    .withOperationName("PutOperationWithChecksum")
                    .withMarshaller(
                        AsyncStreamingRequestMarshaller.builder()
                                                       .delegateMarshaller(new PutOperationWithChecksumRequestMarshaller(protocolFactory))
                                                       .asyncRequestBody(requestBody).build())
                    .withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler)
                    .withMetricCollector(apiCallMetricCollector)
                    .putExecutionAttribute(
                        SdkInternalExecutionAttribute.HTTP_CHECKSUM,
                        HttpChecksum.builder().requestChecksumRequired(false)
                                    .requestValidationMode(putOperationWithChecksumRequest.checksumModeAsString())
                                    .responseAlgorithms("CRC32C", "CRC32", "SHA1", "SHA256").isRequestStreaming(true)
                                    .build()).withAsyncRequestBody(requestBody)
                    .withInput(putOperationWithChecksumRequest), asyncResponseTransformer);
            CompletableFuture<ReturnT> whenCompleteFuture = null;
            AsyncResponseTransformer<PutOperationWithChecksumResponse, ReturnT> finalAsyncResponseTransformer = asyncResponseTransformer;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                if (e != null) {
                    runAndLogError(log, "Exception thrown in exceptionOccurred callback, ignoring",
                                   () -> finalAsyncResponseTransformer.exceptionOccurred(e));
                }
                endOfStreamFuture.whenComplete((r2, e2) -> {
                    metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
                });
            });
            return CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
        } catch (Throwable t) {
            AsyncResponseTransformer<PutOperationWithChecksumResponse, ReturnT> finalAsyncResponseTransformer = asyncResponseTransformer;
            runAndLogError(log, "Exception thrown in exceptionOccurred callback, ignoring",
                           () -> finalAsyncResponseTransformer.exceptionOccurred(t));
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Some operation with a streaming input
     *
     * @param streamingInputOperationRequest
     * @param requestBody
     *        Functional interface that can be implemented to produce the request content in a non-blocking manner. The
     *        size of the content is expected to be known up front. See {@link AsyncRequestBody} for specific details on
     *        implementing this interface as well as links to precanned implementations for common scenarios like
     *        uploading from a file. The service documentation for the request content is as follows 'This be a stream'
     * @return A Java Future containing the result of the StreamingInputOperation operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>XmlException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample XmlAsyncClient.StreamingInputOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<StreamingInputOperationResponse> streamingInputOperation(
        StreamingInputOperationRequest streamingInputOperationRequest, AsyncRequestBody requestBody) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, streamingInputOperationRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Xml Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "StreamingInputOperation");
            if (!isSignerOverridden(clientConfiguration)) {
                streamingInputOperationRequest = applySignerOverride(streamingInputOperationRequest, AsyncAws4Signer.create());
            }

            HttpResponseHandler<Response<StreamingInputOperationResponse>> responseHandler = protocolFactory
                .createCombinedResponseHandler(StreamingInputOperationResponse::builder,
                                               new XmlOperationMetadata().withHasStreamingSuccessResponse(false));

            CompletableFuture<StreamingInputOperationResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<StreamingInputOperationRequest, StreamingInputOperationResponse>()
                             .withOperationName("StreamingInputOperation")
                             .withMarshaller(
                                 AsyncStreamingRequestMarshaller.builder()
                                                                .delegateMarshaller(new StreamingInputOperationRequestMarshaller(protocolFactory))
                                                                .asyncRequestBody(requestBody).build()).withCombinedResponseHandler(responseHandler)
                             .withMetricCollector(apiCallMetricCollector).withAsyncRequestBody(requestBody)
                             .withInput(streamingInputOperationRequest));
            CompletableFuture<StreamingInputOperationResponse> whenCompleteFuture = null;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
            return whenCompleteFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Some operation with a streaming output
     *
     * @param streamingOutputOperationRequest
     * @param asyncResponseTransformer
     *        The response transformer for processing the streaming response in a non-blocking manner. See
     *        {@link AsyncResponseTransformer} for details on how this callback should be implemented and for links to
     *        precanned implementations for common scenarios like downloading to a file. The service documentation for
     *        the response content is as follows 'This be a stream'.
     * @return A future to the transformed result of the AsyncResponseTransformer.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>XmlException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample XmlAsyncClient.StreamingOutputOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public <ReturnT> CompletableFuture<ReturnT> streamingOutputOperation(
        StreamingOutputOperationRequest streamingOutputOperationRequest,
        AsyncResponseTransformer<StreamingOutputOperationResponse, ReturnT> asyncResponseTransformer) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, streamingOutputOperationRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Xml Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "StreamingOutputOperation");
            Pair<AsyncResponseTransformer<StreamingOutputOperationResponse, ReturnT>, CompletableFuture<Void>> pair = AsyncResponseTransformerUtils
                .wrapWithEndOfStreamFuture(asyncResponseTransformer);
            asyncResponseTransformer = pair.left();
            CompletableFuture<Void> endOfStreamFuture = pair.right();

            HttpResponseHandler<StreamingOutputOperationResponse> responseHandler = protocolFactory.createResponseHandler(
                StreamingOutputOperationResponse::builder, new XmlOperationMetadata().withHasStreamingSuccessResponse(true));

            HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();

            CompletableFuture<ReturnT> executeFuture = clientHandler.execute(
                new ClientExecutionParams<StreamingOutputOperationRequest, StreamingOutputOperationResponse>()
                    .withOperationName("StreamingOutputOperation")
                    .withMarshaller(new StreamingOutputOperationRequestMarshaller(protocolFactory))
                    .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                    .withMetricCollector(apiCallMetricCollector).withInput(streamingOutputOperationRequest),
                asyncResponseTransformer);
            CompletableFuture<ReturnT> whenCompleteFuture = null;
            AsyncResponseTransformer<StreamingOutputOperationResponse, ReturnT> finalAsyncResponseTransformer = asyncResponseTransformer;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                if (e != null) {
                    runAndLogError(log, "Exception thrown in exceptionOccurred callback, ignoring",
                                   () -> finalAsyncResponseTransformer.exceptionOccurred(e));
                }
                endOfStreamFuture.whenComplete((r2, e2) -> {
                    metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
                });
            });
            return CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
        } catch (Throwable t) {
            AsyncResponseTransformer<StreamingOutputOperationResponse, ReturnT> finalAsyncResponseTransformer = asyncResponseTransformer;
            runAndLogError(log, "Exception thrown in exceptionOccurred callback, ignoring",
                           () -> finalAsyncResponseTransformer.exceptionOccurred(t));
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    @Override
    public final XmlServiceClientConfiguration serviceClientConfiguration() {
        return this.serviceClientConfiguration;
    }

    @Override
    public final String serviceName() {
        return SERVICE_NAME;
    }

    private AwsXmlProtocolFactory init() {
        return AwsXmlProtocolFactory
            .builder()
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("InvalidInput")
                                 .exceptionBuilderSupplier(InvalidInputException::builder).httpStatusCode(400).build())
            .clientConfiguration(clientConfiguration).defaultServiceExceptionSupplier(XmlException::builder).build();
    }

    private static List<MetricPublisher> resolveMetricPublishers(SdkClientConfiguration clientConfiguration,
                                                                 RequestOverrideConfiguration requestOverrideConfiguration) {
        List<MetricPublisher> publishers = null;
        if (requestOverrideConfiguration != null) {
            publishers = requestOverrideConfiguration.metricPublishers();
        }
        if (publishers == null || publishers.isEmpty()) {
            publishers = clientConfiguration.option(SdkClientOption.METRIC_PUBLISHERS);
        }
        if (publishers == null) {
            publishers = Collections.emptyList();
        }
        return publishers;
    }

    private <T extends XmlRequest> T applySignerOverride(T request, Signer signer) {
        if (request.overrideConfiguration().flatMap(c -> c.signer()).isPresent()) {
            return request;
        }
        Consumer<AwsRequestOverrideConfiguration.Builder> signerOverride = b -> b.signer(signer).build();
        AwsRequestOverrideConfiguration overrideConfiguration = request.overrideConfiguration()
                                                                       .map(c -> c.toBuilder().applyMutation(signerOverride).build())
                                                                       .orElse((AwsRequestOverrideConfiguration.builder().applyMutation(signerOverride).build()));
        return (T) request.toBuilder().overrideConfiguration(overrideConfiguration).build();
    }

    private static boolean isSignerOverridden(SdkClientConfiguration clientConfiguration) {
        return Boolean.TRUE.equals(clientConfiguration.option(SdkClientOption.SIGNER_OVERRIDDEN));
    }

    @Override
    public void close() {
        clientHandler.close();
    }
}

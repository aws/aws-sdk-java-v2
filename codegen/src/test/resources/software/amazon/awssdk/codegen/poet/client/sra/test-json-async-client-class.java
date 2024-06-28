package software.amazon.awssdk.services.json;

import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.handler.AwsAsyncClientHandler;
import software.amazon.awssdk.awscore.client.handler.AwsClientHandlerUtils;
import software.amazon.awssdk.awscore.eventstream.EventStreamAsyncResponseTransformer;
import software.amazon.awssdk.awscore.eventstream.EventStreamTaggedUnionJsonMarshaller;
import software.amazon.awssdk.awscore.eventstream.EventStreamTaggedUnionPojoSupplier;
import software.amazon.awssdk.awscore.eventstream.RestEventStreamAsyncResponseTransformer;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.internal.AwsProtocolMetadata;
import software.amazon.awssdk.awscore.internal.AwsServiceProtocol;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.CredentialType;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkPojoBuilder;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.AsyncResponseTransformerUtils;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.AsyncClientHandler;
import software.amazon.awssdk.core.client.handler.AttachHttpMetadataResponseHandler;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksumRequired;
import software.amazon.awssdk.core.internal.interceptor.trait.RequestCompression;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.protocol.VoidSdkResponse;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.runtime.transform.AsyncStreamingRequestMarshaller;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.JsonOperationMetadata;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.json.batchmanager.JsonAsyncBatchManager;
import software.amazon.awssdk.services.json.internal.JsonServiceClientConfigurationBuilder;
import software.amazon.awssdk.services.json.model.APostOperationRequest;
import software.amazon.awssdk.services.json.model.APostOperationResponse;
import software.amazon.awssdk.services.json.model.APostOperationWithOutputRequest;
import software.amazon.awssdk.services.json.model.APostOperationWithOutputResponse;
import software.amazon.awssdk.services.json.model.BearerAuthOperationRequest;
import software.amazon.awssdk.services.json.model.BearerAuthOperationResponse;
import software.amazon.awssdk.services.json.model.EventStream;
import software.amazon.awssdk.services.json.model.EventStreamOperationRequest;
import software.amazon.awssdk.services.json.model.EventStreamOperationResponse;
import software.amazon.awssdk.services.json.model.EventStreamOperationResponseHandler;
import software.amazon.awssdk.services.json.model.EventStreamOperationWithOnlyInputRequest;
import software.amazon.awssdk.services.json.model.EventStreamOperationWithOnlyInputResponse;
import software.amazon.awssdk.services.json.model.EventStreamOperationWithOnlyOutputRequest;
import software.amazon.awssdk.services.json.model.EventStreamOperationWithOnlyOutputResponse;
import software.amazon.awssdk.services.json.model.EventStreamOperationWithOnlyOutputResponseHandler;
import software.amazon.awssdk.services.json.model.GetOperationWithChecksumRequest;
import software.amazon.awssdk.services.json.model.GetOperationWithChecksumResponse;
import software.amazon.awssdk.services.json.model.GetWithoutRequiredMembersRequest;
import software.amazon.awssdk.services.json.model.GetWithoutRequiredMembersResponse;
import software.amazon.awssdk.services.json.model.InputEventStream;
import software.amazon.awssdk.services.json.model.InputEventStreamTwo;
import software.amazon.awssdk.services.json.model.InvalidInputException;
import software.amazon.awssdk.services.json.model.JsonException;
import software.amazon.awssdk.services.json.model.OperationWithChecksumRequiredRequest;
import software.amazon.awssdk.services.json.model.OperationWithChecksumRequiredResponse;
import software.amazon.awssdk.services.json.model.OperationWithRequestCompressionRequest;
import software.amazon.awssdk.services.json.model.OperationWithRequestCompressionResponse;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyRequest;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyResponse;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyRequest;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyResponse;
import software.amazon.awssdk.services.json.model.PutOperationWithChecksumRequest;
import software.amazon.awssdk.services.json.model.PutOperationWithChecksumResponse;
import software.amazon.awssdk.services.json.model.StreamingInputOperationRequest;
import software.amazon.awssdk.services.json.model.StreamingInputOperationResponse;
import software.amazon.awssdk.services.json.model.StreamingInputOutputOperationRequest;
import software.amazon.awssdk.services.json.model.StreamingInputOutputOperationResponse;
import software.amazon.awssdk.services.json.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.services.json.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.services.json.model.inputeventstream.DefaultInputEvent;
import software.amazon.awssdk.services.json.model.inputeventstreamtwo.DefaultInputEventOne;
import software.amazon.awssdk.services.json.model.inputeventstreamtwo.DefaultInputEventTwo;
import software.amazon.awssdk.services.json.transform.APostOperationRequestMarshaller;
import software.amazon.awssdk.services.json.transform.APostOperationWithOutputRequestMarshaller;
import software.amazon.awssdk.services.json.transform.BearerAuthOperationRequestMarshaller;
import software.amazon.awssdk.services.json.transform.EventStreamOperationRequestMarshaller;
import software.amazon.awssdk.services.json.transform.EventStreamOperationWithOnlyInputRequestMarshaller;
import software.amazon.awssdk.services.json.transform.EventStreamOperationWithOnlyOutputRequestMarshaller;
import software.amazon.awssdk.services.json.transform.GetOperationWithChecksumRequestMarshaller;
import software.amazon.awssdk.services.json.transform.GetWithoutRequiredMembersRequestMarshaller;
import software.amazon.awssdk.services.json.transform.InputEventMarshaller;
import software.amazon.awssdk.services.json.transform.InputEventTwoMarshaller;
import software.amazon.awssdk.services.json.transform.OperationWithChecksumRequiredRequestMarshaller;
import software.amazon.awssdk.services.json.transform.OperationWithRequestCompressionRequestMarshaller;
import software.amazon.awssdk.services.json.transform.PaginatedOperationWithResultKeyRequestMarshaller;
import software.amazon.awssdk.services.json.transform.PaginatedOperationWithoutResultKeyRequestMarshaller;
import software.amazon.awssdk.services.json.transform.PutOperationWithChecksumRequestMarshaller;
import software.amazon.awssdk.services.json.transform.StreamingInputOperationRequestMarshaller;
import software.amazon.awssdk.services.json.transform.StreamingInputOutputOperationRequestMarshaller;
import software.amazon.awssdk.services.json.transform.StreamingOutputOperationRequestMarshaller;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.HostnameValidator;
import software.amazon.awssdk.utils.Pair;

/**
 * Internal implementation of {@link JsonAsyncClient}.
 *
 * @see JsonAsyncClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultJsonAsyncClient implements JsonAsyncClient {
    private static final Logger log = LoggerFactory.getLogger(DefaultJsonAsyncClient.class);

    private static final AwsProtocolMetadata protocolMetadata = AwsProtocolMetadata.builder()
            .serviceProtocol(AwsServiceProtocol.REST_JSON).build();

    private final AsyncClientHandler clientHandler;

    private final AwsJsonProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    private final ScheduledExecutorService executorService;

    private final Executor executor;

    protected DefaultJsonAsyncClient(SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsAsyncClientHandler(clientConfiguration);
        this.clientConfiguration = clientConfiguration.toBuilder().option(SdkClientOption.SDK_CLIENT, this).build();
        this.protocolFactory = init(AwsJsonProtocolFactory.builder()).build();
        this.executor = clientConfiguration.option(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR);
        this.executorService = clientConfiguration.option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE);
    }

    @Override
    public JsonUtilities utilities() {
        return JsonUtilities.create(param1, param2, param3);
    }

    /**
     * <p>
     * Performs a post operation to the query service and has no output
     * </p>
     *
     * @param aPostOperationRequest
     * @return A Java Future containing the result of the APostOperation operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>InvalidInputException The request was rejected because an invalid or out-of-range value was supplied
     *         for an input parameter.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.APostOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/APostOperation" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public CompletableFuture<APostOperationResponse> aPostOperation(APostOperationRequest aPostOperationRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(aPostOperationRequest, this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, aPostOperationRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "APostOperation");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<APostOperationResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, APostOperationResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);
            String hostPrefix = "{StringMember}-foo.";
            HostnameValidator.validateHostnameCompliant(aPostOperationRequest.stringMember(), "StringMember",
                    "aPostOperationRequest");
            String resolvedHostExpression = String.format("%s-foo.", aPostOperationRequest.stringMember());

            CompletableFuture<APostOperationResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<APostOperationRequest, APostOperationResponse>()
                            .withOperationName("APostOperation").withProtocolMetadata(protocolMetadata)
                            .withMarshaller(new APostOperationRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                            .hostPrefixExpression(resolvedHostExpression).withInput(aPostOperationRequest));
            CompletableFuture<APostOperationResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Performs a post operation to the query service and has modelled output
     * </p>
     *
     * @param aPostOperationWithOutputRequest
     * @return A Java Future containing the result of the APostOperationWithOutput operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>InvalidInputException The request was rejected because an invalid or out-of-range value was supplied
     *         for an input parameter.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.APostOperationWithOutput
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/APostOperationWithOutput"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<APostOperationWithOutputResponse> aPostOperationWithOutput(
            APostOperationWithOutputRequest aPostOperationWithOutputRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(aPostOperationWithOutputRequest,
                this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, aPostOperationWithOutputRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "APostOperationWithOutput");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<APostOperationWithOutputResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, APostOperationWithOutputResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<APostOperationWithOutputResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<APostOperationWithOutputRequest, APostOperationWithOutputResponse>()
                            .withOperationName("APostOperationWithOutput").withProtocolMetadata(protocolMetadata)
                            .withMarshaller(new APostOperationWithOutputRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                            .withInput(aPostOperationWithOutputRequest));
            CompletableFuture<APostOperationWithOutputResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
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
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.BearerAuthOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/BearerAuthOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<BearerAuthOperationResponse> bearerAuthOperation(
            BearerAuthOperationRequest bearerAuthOperationRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(bearerAuthOperationRequest,
                this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, bearerAuthOperationRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "BearerAuthOperation");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<BearerAuthOperationResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, BearerAuthOperationResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<BearerAuthOperationResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<BearerAuthOperationRequest, BearerAuthOperationResponse>()
                            .withOperationName("BearerAuthOperation").withProtocolMetadata(protocolMetadata)
                            .withMarshaller(new BearerAuthOperationRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                            .credentialType(CredentialType.TOKEN).withInput(bearerAuthOperationRequest));
            CompletableFuture<BearerAuthOperationResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
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
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.EventStreamOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/EventStreamOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<Void> eventStreamOperation(EventStreamOperationRequest eventStreamOperationRequest,
            Publisher<InputEventStream> requestStream, EventStreamOperationResponseHandler asyncResponseHandler) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(eventStreamOperationRequest,
                this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, eventStreamOperationRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "EventStreamOperation");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<EventStreamOperationResponse> responseHandler = new AttachHttpMetadataResponseHandler(
                    protocolFactory.createResponseHandler(operationMetadata, EventStreamOperationResponse::builder));

            HttpResponseHandler<SdkResponse> voidResponseHandler = protocolFactory.createResponseHandler(JsonOperationMetadata
                    .builder().isPayloadJson(false).hasStreamingSuccessResponse(true).build(), VoidSdkResponse::builder);

            HttpResponseHandler<? extends EventStream> eventResponseHandler = protocolFactory.createResponseHandler(
                    JsonOperationMetadata.builder().isPayloadJson(true).hasStreamingSuccessResponse(false).build(),
                    EventStreamTaggedUnionPojoSupplier.builder().putSdkPojoSupplier("EventOne", EventStream::eventOneBuilder)
                            .putSdkPojoSupplier("EventTheSecond", EventStream::eventTheSecondBuilder)
                            .putSdkPojoSupplier("secondEventOne", EventStream::secondEventOneBuilder)
                            .putSdkPojoSupplier("eventThree", EventStream::eventThreeBuilder)
                            .defaultSdkPojoSupplier(() -> new SdkPojoBuilder(EventStream.UNKNOWN)).build());

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);
            EventStreamTaggedUnionJsonMarshaller eventMarshaller = EventStreamTaggedUnionJsonMarshaller.builder()
                    .putMarshaller(DefaultInputEvent.class, new InputEventMarshaller(protocolFactory)).build();
            SdkPublisher<InputEventStream> eventPublisher = SdkPublisher.adapt(requestStream);
            Publisher<ByteBuffer> adapted = eventPublisher.map(event -> eventMarshaller.marshall(event)).map(
                    AwsClientHandlerUtils::encodeEventStreamRequestToByteBuffer);
            CompletableFuture<Void> future = new CompletableFuture<>();
            EventStreamAsyncResponseTransformer<EventStreamOperationResponse, EventStream> asyncResponseTransformer = EventStreamAsyncResponseTransformer
                    .<EventStreamOperationResponse, EventStream> builder().eventStreamResponseHandler(asyncResponseHandler)
                    .eventResponseHandler(eventResponseHandler).initialResponseHandler(responseHandler)
                    .exceptionResponseHandler(errorResponseHandler).future(future).executor(executor).serviceName(serviceName())
                    .build();
            RestEventStreamAsyncResponseTransformer<EventStreamOperationResponse, EventStream> restAsyncResponseTransformer = RestEventStreamAsyncResponseTransformer
                    .<EventStreamOperationResponse, EventStream> builder()
                    .eventStreamAsyncResponseTransformer(asyncResponseTransformer)
                    .eventStreamResponseHandler(asyncResponseHandler).build();

            CompletableFuture<Void> executeFuture = clientHandler.execute(
                    new ClientExecutionParams<EventStreamOperationRequest, EventStreamOperationResponse>()
                            .withOperationName("EventStreamOperation").withProtocolMetadata(protocolMetadata)
                            .withMarshaller(new EventStreamOperationRequestMarshaller(protocolFactory))
                            .withAsyncRequestBody(AsyncRequestBody.fromPublisher(adapted)).withFullDuplex(true)
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                            .withInput(eventStreamOperationRequest), restAsyncResponseTransformer);
            CompletableFuture<Void> whenCompleted = executeFuture.whenComplete((r, e) -> {
                if (e != null) {
                    try {
                        asyncResponseHandler.exceptionOccurred(e);
                    } finally {
                        future.completeExceptionally(e);
                    }
                }
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return CompletableFutureUtils.forwardExceptionTo(future, executeFuture);
        } catch (Throwable t) {
            runAndLogError(log, "Exception thrown in exceptionOccurred callback, ignoring",
                    () -> asyncResponseHandler.exceptionOccurred(t));
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Invokes the EventStreamOperationWithOnlyInput operation asynchronously.
     *
     * @param eventStreamOperationWithOnlyInputRequest
     * @return A Java Future containing the result of the EventStreamOperationWithOnlyInput operation returned by the
     *         service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.EventStreamOperationWithOnlyInput
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/EventStreamOperationWithOnlyInput"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<EventStreamOperationWithOnlyInputResponse> eventStreamOperationWithOnlyInput(
            EventStreamOperationWithOnlyInputRequest eventStreamOperationWithOnlyInputRequest,
            Publisher<InputEventStreamTwo> requestStream) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(eventStreamOperationWithOnlyInputRequest,
                this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                eventStreamOperationWithOnlyInputRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "EventStreamOperationWithOnlyInput");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<EventStreamOperationWithOnlyInputResponse> responseHandler = protocolFactory
                    .createResponseHandler(operationMetadata, EventStreamOperationWithOnlyInputResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);
            EventStreamTaggedUnionJsonMarshaller eventMarshaller = EventStreamTaggedUnionJsonMarshaller.builder()
                    .putMarshaller(DefaultInputEventOne.class, new InputEventMarshaller(protocolFactory))
                    .putMarshaller(DefaultInputEventTwo.class, new InputEventTwoMarshaller(protocolFactory)).build();
            SdkPublisher<InputEventStreamTwo> eventPublisher = SdkPublisher.adapt(requestStream);
            Publisher<ByteBuffer> adapted = eventPublisher.map(event -> eventMarshaller.marshall(event)).map(
                    AwsClientHandlerUtils::encodeEventStreamRequestToByteBuffer);

            CompletableFuture<EventStreamOperationWithOnlyInputResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<EventStreamOperationWithOnlyInputRequest, EventStreamOperationWithOnlyInputResponse>()
                            .withOperationName("EventStreamOperationWithOnlyInput").withProtocolMetadata(protocolMetadata)
                            .withMarshaller(new EventStreamOperationWithOnlyInputRequestMarshaller(protocolFactory))
                            .withAsyncRequestBody(AsyncRequestBody.fromPublisher(adapted)).withResponseHandler(responseHandler)
                            .withErrorResponseHandler(errorResponseHandler).withRequestConfiguration(clientConfiguration)
                            .withMetricCollector(apiCallMetricCollector).withInput(eventStreamOperationWithOnlyInputRequest));
            CompletableFuture<EventStreamOperationWithOnlyInputResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Invokes the EventStreamOperationWithOnlyOutput operation asynchronously.
     *
     * @param eventStreamOperationWithOnlyOutputRequest
     * @return A Java Future containing the result of the EventStreamOperationWithOnlyOutput operation returned by the
     *         service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.EventStreamOperationWithOnlyOutput
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/EventStreamOperationWithOnlyOutput"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<Void> eventStreamOperationWithOnlyOutput(
            EventStreamOperationWithOnlyOutputRequest eventStreamOperationWithOnlyOutputRequest,
            EventStreamOperationWithOnlyOutputResponseHandler asyncResponseHandler) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(eventStreamOperationWithOnlyOutputRequest,
                this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                eventStreamOperationWithOnlyOutputRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "EventStreamOperationWithOnlyOutput");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<EventStreamOperationWithOnlyOutputResponse> responseHandler = new AttachHttpMetadataResponseHandler(
                    protocolFactory.createResponseHandler(operationMetadata, EventStreamOperationWithOnlyOutputResponse::builder));

            HttpResponseHandler<SdkResponse> voidResponseHandler = protocolFactory.createResponseHandler(JsonOperationMetadata
                    .builder().isPayloadJson(false).hasStreamingSuccessResponse(true).build(), VoidSdkResponse::builder);

            HttpResponseHandler<? extends EventStream> eventResponseHandler = protocolFactory.createResponseHandler(
                    JsonOperationMetadata.builder().isPayloadJson(true).hasStreamingSuccessResponse(false).build(),
                    EventStreamTaggedUnionPojoSupplier.builder().putSdkPojoSupplier("EventOne", EventStream::eventOneBuilder)
                            .putSdkPojoSupplier("EventTheSecond", EventStream::eventTheSecondBuilder)
                            .putSdkPojoSupplier("secondEventOne", EventStream::secondEventOneBuilder)
                            .putSdkPojoSupplier("eventThree", EventStream::eventThreeBuilder)
                            .defaultSdkPojoSupplier(() -> new SdkPojoBuilder(EventStream.UNKNOWN)).build());

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);
            CompletableFuture<Void> future = new CompletableFuture<>();
            EventStreamAsyncResponseTransformer<EventStreamOperationWithOnlyOutputResponse, EventStream> asyncResponseTransformer = EventStreamAsyncResponseTransformer
                    .<EventStreamOperationWithOnlyOutputResponse, EventStream> builder()
                    .eventStreamResponseHandler(asyncResponseHandler).eventResponseHandler(eventResponseHandler)
                    .initialResponseHandler(responseHandler).exceptionResponseHandler(errorResponseHandler).future(future)
                    .executor(executor).serviceName(serviceName()).build();
            RestEventStreamAsyncResponseTransformer<EventStreamOperationWithOnlyOutputResponse, EventStream> restAsyncResponseTransformer = RestEventStreamAsyncResponseTransformer
                    .<EventStreamOperationWithOnlyOutputResponse, EventStream> builder()
                    .eventStreamAsyncResponseTransformer(asyncResponseTransformer)
                    .eventStreamResponseHandler(asyncResponseHandler).build();

            CompletableFuture<Void> executeFuture = clientHandler
                    .execute(
                            new ClientExecutionParams<EventStreamOperationWithOnlyOutputRequest, EventStreamOperationWithOnlyOutputResponse>()
                                    .withOperationName("EventStreamOperationWithOnlyOutput")
                                    .withProtocolMetadata(protocolMetadata)
                                    .withMarshaller(new EventStreamOperationWithOnlyOutputRequestMarshaller(protocolFactory))
                                    .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                    .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                                    .withInput(eventStreamOperationWithOnlyOutputRequest), restAsyncResponseTransformer);
            CompletableFuture<Void> whenCompleted = executeFuture.whenComplete((r, e) -> {
                if (e != null) {
                    try {
                        asyncResponseHandler.exceptionOccurred(e);
                    } finally {
                        future.completeExceptionally(e);
                    }
                }
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return CompletableFutureUtils.forwardExceptionTo(future, executeFuture);
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
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.GetOperationWithChecksum
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/GetOperationWithChecksum"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<GetOperationWithChecksumResponse> getOperationWithChecksum(
            GetOperationWithChecksumRequest getOperationWithChecksumRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(getOperationWithChecksumRequest,
                this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, getOperationWithChecksumRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetOperationWithChecksum");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(false).build();

            HttpResponseHandler<GetOperationWithChecksumResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, GetOperationWithChecksumResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<GetOperationWithChecksumResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<GetOperationWithChecksumRequest, GetOperationWithChecksumResponse>()
                            .withOperationName("GetOperationWithChecksum")
                            .withProtocolMetadata(protocolMetadata)
                            .withMarshaller(new GetOperationWithChecksumRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler)
                            .withErrorResponseHandler(errorResponseHandler)
                            .withRequestConfiguration(clientConfiguration)
                            .withMetricCollector(apiCallMetricCollector)
                            .putExecutionAttribute(
                                    SdkInternalExecutionAttribute.HTTP_CHECKSUM,
                                    HttpChecksum.builder().requestChecksumRequired(true)
                                            .requestAlgorithm(getOperationWithChecksumRequest.checksumAlgorithmAsString())
                                            .isRequestStreaming(false).build()).withInput(getOperationWithChecksumRequest));
            CompletableFuture<GetOperationWithChecksumResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Performs a post operation to the query service and has no output
     * </p>
     *
     * @param getWithoutRequiredMembersRequest
     * @return A Java Future containing the result of the GetWithoutRequiredMembers operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>InvalidInputException The request was rejected because an invalid or out-of-range value was supplied
     *         for an input parameter.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.GetWithoutRequiredMembers
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/GetWithoutRequiredMembers"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<GetWithoutRequiredMembersResponse> getWithoutRequiredMembers(
            GetWithoutRequiredMembersRequest getWithoutRequiredMembersRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(getWithoutRequiredMembersRequest,
                this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, getWithoutRequiredMembersRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetWithoutRequiredMembers");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<GetWithoutRequiredMembersResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, GetWithoutRequiredMembersResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<GetWithoutRequiredMembersResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<GetWithoutRequiredMembersRequest, GetWithoutRequiredMembersResponse>()
                            .withOperationName("GetWithoutRequiredMembers").withProtocolMetadata(protocolMetadata)
                            .withMarshaller(new GetWithoutRequiredMembersRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                            .withInput(getWithoutRequiredMembersRequest));
            CompletableFuture<GetWithoutRequiredMembersResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
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
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.OperationWithChecksumRequired
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/OperationWithChecksumRequired"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<OperationWithChecksumRequiredResponse> operationWithChecksumRequired(
            OperationWithChecksumRequiredRequest operationWithChecksumRequiredRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(operationWithChecksumRequiredRequest,
                this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                operationWithChecksumRequiredRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithChecksumRequired");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<OperationWithChecksumRequiredResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, OperationWithChecksumRequiredResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<OperationWithChecksumRequiredResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<OperationWithChecksumRequiredRequest, OperationWithChecksumRequiredResponse>()
                            .withOperationName("OperationWithChecksumRequired")
                            .withProtocolMetadata(protocolMetadata)
                            .withMarshaller(new OperationWithChecksumRequiredRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler)
                            .withErrorResponseHandler(errorResponseHandler)
                            .withRequestConfiguration(clientConfiguration)
                            .withMetricCollector(apiCallMetricCollector)
                            .putExecutionAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM_REQUIRED,
                                    HttpChecksumRequired.create()).withInput(operationWithChecksumRequiredRequest));
            CompletableFuture<OperationWithChecksumRequiredResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
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
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.OperationWithRequestCompression
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/OperationWithRequestCompression"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<OperationWithRequestCompressionResponse> operationWithRequestCompression(
            OperationWithRequestCompressionRequest operationWithRequestCompressionRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(operationWithRequestCompressionRequest,
                this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                operationWithRequestCompressionRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithRequestCompression");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<OperationWithRequestCompressionResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, OperationWithRequestCompressionResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<OperationWithRequestCompressionResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<OperationWithRequestCompressionRequest, OperationWithRequestCompressionResponse>()
                            .withOperationName("OperationWithRequestCompression")
                            .withProtocolMetadata(protocolMetadata)
                            .withMarshaller(new OperationWithRequestCompressionRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler)
                            .withErrorResponseHandler(errorResponseHandler)
                            .withRequestConfiguration(clientConfiguration)
                            .withMetricCollector(apiCallMetricCollector)
                            .putExecutionAttribute(SdkInternalExecutionAttribute.REQUEST_COMPRESSION,
                                    RequestCompression.builder().encodings("gzip").isStreaming(false).build())
                            .withInput(operationWithRequestCompressionRequest));
            CompletableFuture<OperationWithRequestCompressionResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Some paginated operation with result_key in paginators.json file
     *
     * @param paginatedOperationWithResultKeyRequest
     * @return A Java Future containing the result of the PaginatedOperationWithResultKey operation returned by the
     *         service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.PaginatedOperationWithResultKey
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<PaginatedOperationWithResultKeyResponse> paginatedOperationWithResultKey(
            PaginatedOperationWithResultKeyRequest paginatedOperationWithResultKeyRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(paginatedOperationWithResultKeyRequest,
                this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                paginatedOperationWithResultKeyRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "PaginatedOperationWithResultKey");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<PaginatedOperationWithResultKeyResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, PaginatedOperationWithResultKeyResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<PaginatedOperationWithResultKeyResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<PaginatedOperationWithResultKeyRequest, PaginatedOperationWithResultKeyResponse>()
                            .withOperationName("PaginatedOperationWithResultKey").withProtocolMetadata(protocolMetadata)
                            .withMarshaller(new PaginatedOperationWithResultKeyRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                            .withInput(paginatedOperationWithResultKeyRequest));
            CompletableFuture<PaginatedOperationWithResultKeyResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Some paginated operation without result_key in paginators.json file
     *
     * @param paginatedOperationWithoutResultKeyRequest
     * @return A Java Future containing the result of the PaginatedOperationWithoutResultKey operation returned by the
     *         service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.PaginatedOperationWithoutResultKey
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithoutResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<PaginatedOperationWithoutResultKeyResponse> paginatedOperationWithoutResultKey(
            PaginatedOperationWithoutResultKeyRequest paginatedOperationWithoutResultKeyRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(paginatedOperationWithoutResultKeyRequest,
                this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                paginatedOperationWithoutResultKeyRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "PaginatedOperationWithoutResultKey");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<PaginatedOperationWithoutResultKeyResponse> responseHandler = protocolFactory
                    .createResponseHandler(operationMetadata, PaginatedOperationWithoutResultKeyResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<PaginatedOperationWithoutResultKeyResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<PaginatedOperationWithoutResultKeyRequest, PaginatedOperationWithoutResultKeyResponse>()
                            .withOperationName("PaginatedOperationWithoutResultKey").withProtocolMetadata(protocolMetadata)
                            .withMarshaller(new PaginatedOperationWithoutResultKeyRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                            .withInput(paginatedOperationWithoutResultKeyRequest));
            CompletableFuture<PaginatedOperationWithoutResultKeyResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
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
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.PutOperationWithChecksum
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PutOperationWithChecksum"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public <ReturnT> CompletableFuture<ReturnT> putOperationWithChecksum(
            PutOperationWithChecksumRequest putOperationWithChecksumRequest, AsyncRequestBody requestBody,
            AsyncResponseTransformer<PutOperationWithChecksumResponse, ReturnT> asyncResponseTransformer) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(putOperationWithChecksumRequest,
                this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, putOperationWithChecksumRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "PutOperationWithChecksum");
            Pair<AsyncResponseTransformer<PutOperationWithChecksumResponse, ReturnT>, CompletableFuture<Void>> pair = AsyncResponseTransformerUtils
                    .wrapWithEndOfStreamFuture(asyncResponseTransformer);
            asyncResponseTransformer = pair.left();
            CompletableFuture<Void> endOfStreamFuture = pair.right();
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(true)
                    .isPayloadJson(false).build();

            HttpResponseHandler<PutOperationWithChecksumResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, PutOperationWithChecksumResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<ReturnT> executeFuture = clientHandler.execute(
                    new ClientExecutionParams<PutOperationWithChecksumRequest, PutOperationWithChecksumResponse>()
                            .withOperationName("PutOperationWithChecksum")
                            .withProtocolMetadata(protocolMetadata)
                            .withMarshaller(
                                    AsyncStreamingRequestMarshaller.builder()
                                            .delegateMarshaller(new PutOperationWithChecksumRequestMarshaller(protocolFactory))
                                            .asyncRequestBody(requestBody).build())
                            .withResponseHandler(responseHandler)
                            .withErrorResponseHandler(errorResponseHandler)
                            .withRequestConfiguration(clientConfiguration)
                            .withMetricCollector(apiCallMetricCollector)
                            .withAsyncRequestBody(requestBody)
                            .putExecutionAttribute(
                                    SdkInternalExecutionAttribute.HTTP_CHECKSUM,
                                    HttpChecksum.builder().requestChecksumRequired(false)
                                            .requestValidationMode(putOperationWithChecksumRequest.checksumModeAsString())
                                            .responseAlgorithms("CRC32C", "CRC32", "SHA1", "SHA256").isRequestStreaming(true)
                                            .build()).withInput(putOperationWithChecksumRequest), asyncResponseTransformer);
            AsyncResponseTransformer<PutOperationWithChecksumResponse, ReturnT> finalAsyncResponseTransformer = asyncResponseTransformer;
            CompletableFuture<ReturnT> whenCompleted = executeFuture.whenComplete((r, e) -> {
                if (e != null) {
                    runAndLogError(log, "Exception thrown in exceptionOccurred callback, ignoring",
                            () -> finalAsyncResponseTransformer.exceptionOccurred(e));
                }
                endOfStreamFuture.whenComplete((r2, e2) -> {
                    metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
                });
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
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
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.StreamingInputOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<StreamingInputOperationResponse> streamingInputOperation(
            StreamingInputOperationRequest streamingInputOperationRequest, AsyncRequestBody requestBody) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(streamingInputOperationRequest,
                this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, streamingInputOperationRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "StreamingInputOperation");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<StreamingInputOperationResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, StreamingInputOperationResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<StreamingInputOperationResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<StreamingInputOperationRequest, StreamingInputOperationResponse>()
                            .withOperationName("StreamingInputOperation")
                            .withProtocolMetadata(protocolMetadata)
                            .withMarshaller(
                                    AsyncStreamingRequestMarshaller.builder()
                                            .delegateMarshaller(new StreamingInputOperationRequestMarshaller(protocolFactory))
                                            .asyncRequestBody(requestBody).build()).withResponseHandler(responseHandler)
                            .withErrorResponseHandler(errorResponseHandler).withRequestConfiguration(clientConfiguration)
                            .withMetricCollector(apiCallMetricCollector).withAsyncRequestBody(requestBody)
                            .withInput(streamingInputOperationRequest));
            CompletableFuture<StreamingInputOperationResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Some operation with streaming input and streaming output
     *
     * @param streamingInputOutputOperationRequest
     * @param requestBody
     *        Functional interface that can be implemented to produce the request content in a non-blocking manner. The
     *        size of the content is expected to be known up front. See {@link AsyncRequestBody} for specific details on
     *        implementing this interface as well as links to precanned implementations for common scenarios like
     *        uploading from a file. The service documentation for the request content is as follows 'This be a stream'
     * @param asyncResponseTransformer
     *        The response transformer for processing the streaming response in a non-blocking manner. See
     *        {@link AsyncResponseTransformer} for details on how this callback should be implemented and for links to
     *        precanned implementations for common scenarios like downloading to a file. The service documentation for
     *        the response content is as follows 'This be a stream'.
     * @return A future to the transformed result of the AsyncResponseTransformer.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.StreamingInputOutputOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public <ReturnT> CompletableFuture<ReturnT> streamingInputOutputOperation(
            StreamingInputOutputOperationRequest streamingInputOutputOperationRequest, AsyncRequestBody requestBody,
            AsyncResponseTransformer<StreamingInputOutputOperationResponse, ReturnT> asyncResponseTransformer) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(streamingInputOutputOperationRequest,
                this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                streamingInputOutputOperationRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "StreamingInputOutputOperation");
            Pair<AsyncResponseTransformer<StreamingInputOutputOperationResponse, ReturnT>, CompletableFuture<Void>> pair = AsyncResponseTransformerUtils
                    .wrapWithEndOfStreamFuture(asyncResponseTransformer);
            asyncResponseTransformer = pair.left();
            CompletableFuture<Void> endOfStreamFuture = pair.right();
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(true)
                    .isPayloadJson(false).build();

            HttpResponseHandler<StreamingInputOutputOperationResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, StreamingInputOutputOperationResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<ReturnT> executeFuture = clientHandler.execute(
                    new ClientExecutionParams<StreamingInputOutputOperationRequest, StreamingInputOutputOperationResponse>()
                            .withOperationName("StreamingInputOutputOperation")
                            .withProtocolMetadata(protocolMetadata)
                            .withMarshaller(
                                    AsyncStreamingRequestMarshaller
                                            .builder()
                                            .delegateMarshaller(
                                                    new StreamingInputOutputOperationRequestMarshaller(protocolFactory))
                                            .asyncRequestBody(requestBody).transferEncoding(true).build())
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                            .withAsyncRequestBody(requestBody).withInput(streamingInputOutputOperationRequest),
                    asyncResponseTransformer);
            AsyncResponseTransformer<StreamingInputOutputOperationResponse, ReturnT> finalAsyncResponseTransformer = asyncResponseTransformer;
            CompletableFuture<ReturnT> whenCompleted = executeFuture.whenComplete((r, e) -> {
                if (e != null) {
                    runAndLogError(log, "Exception thrown in exceptionOccurred callback, ignoring",
                            () -> finalAsyncResponseTransformer.exceptionOccurred(e));
                }
                endOfStreamFuture.whenComplete((r2, e2) -> {
                    metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
                });
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            AsyncResponseTransformer<StreamingInputOutputOperationResponse, ReturnT> finalAsyncResponseTransformer = asyncResponseTransformer;
            runAndLogError(log, "Exception thrown in exceptionOccurred callback, ignoring",
                    () -> finalAsyncResponseTransformer.exceptionOccurred(t));
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
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.StreamingOutputOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public <ReturnT> CompletableFuture<ReturnT> streamingOutputOperation(
            StreamingOutputOperationRequest streamingOutputOperationRequest,
            AsyncResponseTransformer<StreamingOutputOperationResponse, ReturnT> asyncResponseTransformer) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(streamingOutputOperationRequest,
                this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, streamingOutputOperationRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "StreamingOutputOperation");
            Pair<AsyncResponseTransformer<StreamingOutputOperationResponse, ReturnT>, CompletableFuture<Void>> pair = AsyncResponseTransformerUtils
                    .wrapWithEndOfStreamFuture(asyncResponseTransformer);
            asyncResponseTransformer = pair.left();
            CompletableFuture<Void> endOfStreamFuture = pair.right();
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(true)
                    .isPayloadJson(false).build();

            HttpResponseHandler<StreamingOutputOperationResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, StreamingOutputOperationResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<ReturnT> executeFuture = clientHandler.execute(
                    new ClientExecutionParams<StreamingOutputOperationRequest, StreamingOutputOperationResponse>()
                            .withOperationName("StreamingOutputOperation").withProtocolMetadata(protocolMetadata)
                            .withMarshaller(new StreamingOutputOperationRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                            .withInput(streamingOutputOperationRequest), asyncResponseTransformer);
            AsyncResponseTransformer<StreamingOutputOperationResponse, ReturnT> finalAsyncResponseTransformer = asyncResponseTransformer;
            CompletableFuture<ReturnT> whenCompleted = executeFuture.whenComplete((r, e) -> {
                if (e != null) {
                    runAndLogError(log, "Exception thrown in exceptionOccurred callback, ignoring",
                            () -> finalAsyncResponseTransformer.exceptionOccurred(e));
                }
                endOfStreamFuture.whenComplete((r2, e2) -> {
                    metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
                });
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            AsyncResponseTransformer<StreamingOutputOperationResponse, ReturnT> finalAsyncResponseTransformer = asyncResponseTransformer;
            runAndLogError(log, "Exception thrown in exceptionOccurred callback, ignoring",
                    () -> finalAsyncResponseTransformer.exceptionOccurred(t));
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    @Override
    public JsonAsyncBatchManager batchManager() {
        return JsonAsyncBatchManager.builder().client(this).scheduledExecutor(executorService).build();
    }

    @Override
    public final JsonServiceClientConfiguration serviceClientConfiguration() {
        return new JsonServiceClientConfigurationBuilder(this.clientConfiguration.toBuilder()).build();
    }

    @Override
    public final String serviceName() {
        return SERVICE_NAME;
    }

    private <T extends BaseAwsJsonProtocolFactory.Builder<T>> T init(T builder) {
        return builder
                .clientConfiguration(clientConfiguration)
                .defaultServiceExceptionSupplier(JsonException::builder)
                .protocol(AwsJsonProtocol.REST_JSON)
                .protocolVersion("1.1")
                .registerModeledException(
                        ExceptionMetadata.builder().errorCode("InvalidInput")
                                .exceptionBuilderSupplier(InvalidInputException::builder).httpStatusCode(400).build());
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

    private void updateRetryStrategyClientConfiguration(SdkClientConfiguration.Builder configuration) {
        ClientOverrideConfiguration.Builder builder = configuration.asOverrideConfigurationBuilder();
        RetryMode retryMode = builder.retryMode();
        if (retryMode != null) {
            configuration.option(SdkClientOption.RETRY_STRATEGY, AwsRetryStrategy.forRetryMode(retryMode));
        } else {
            Consumer<RetryStrategy.Builder<?, ?>> configurator = builder.retryStrategyConfigurator();
            if (configurator != null) {
                RetryStrategy.Builder<?, ?> defaultBuilder = AwsRetryStrategy.defaultRetryStrategy().toBuilder();
                configurator.accept(defaultBuilder);
                configuration.option(SdkClientOption.RETRY_STRATEGY, defaultBuilder.build());
            } else {
                RetryStrategy retryStrategy = builder.retryStrategy();
                if (retryStrategy != null) {
                    configuration.option(SdkClientOption.RETRY_STRATEGY, retryStrategy);
                }
            }
        }
        configuration.option(SdkClientOption.CONFIGURED_RETRY_MODE, null);
        configuration.option(SdkClientOption.CONFIGURED_RETRY_STRATEGY, null);
        configuration.option(SdkClientOption.CONFIGURED_RETRY_CONFIGURATOR, null);
    }

    private SdkClientConfiguration updateSdkClientConfiguration(SdkRequest request, SdkClientConfiguration clientConfiguration) {
        List<SdkPlugin> plugins = request.overrideConfiguration().map(c -> c.plugins()).orElse(Collections.emptyList());
        SdkClientConfiguration.Builder configuration = clientConfiguration.toBuilder();
        if (plugins.isEmpty()) {
            return configuration.build();
        }
        JsonServiceClientConfigurationBuilder serviceConfigBuilder = new JsonServiceClientConfigurationBuilder(configuration);
        for (SdkPlugin plugin : plugins) {
            plugin.configureClient(serviceConfigBuilder);
        }
        updateRetryStrategyClientConfiguration(configuration);
        return configuration.build();
    }

    private HttpResponseHandler<AwsServiceException> createErrorResponseHandler(BaseAwsJsonProtocolFactory protocolFactory,
            JsonOperationMetadata operationMetadata) {
        return protocolFactory.createErrorResponseHandler(operationMetadata);
    }

    @Override
    public void close() {
        clientHandler.close();
    }
}

package software.amazon.awssdk.services.smithyrpcv2protocol;

import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.handler.AwsAsyncClientHandler;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.internal.AwsProtocolMetadata;
import software.amazon.awssdk.awscore.internal.AwsServiceProtocol;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.AsyncClientHandler;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.JsonOperationMetadata;
import software.amazon.awssdk.protocols.rpcv2.SmithyRpcV2CborProtocolFactory;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.smithyrpcv2protocol.internal.ServiceVersionInfo;
import software.amazon.awssdk.services.smithyrpcv2protocol.internal.SmithyRpcV2ProtocolServiceClientConfigurationBuilder;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.ComplexErrorException;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.EmptyInputOutputRequest;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.EmptyInputOutputResponse;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.Float16Request;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.Float16Response;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.FractionalSecondsRequest;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.FractionalSecondsResponse;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.GreetingWithErrorsRequest;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.GreetingWithErrorsResponse;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.InvalidGreetingException;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.NoInputOutputRequest;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.NoInputOutputResponse;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.OperationWithDefaultsRequest;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.OperationWithDefaultsResponse;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.OptionalInputOutputRequest;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.OptionalInputOutputResponse;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.RecursiveShapesRequest;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.RecursiveShapesResponse;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.RpcV2CborDenseMapsRequest;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.RpcV2CborDenseMapsResponse;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.RpcV2CborListsRequest;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.RpcV2CborListsResponse;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.RpcV2CborSparseMapsRequest;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.RpcV2CborSparseMapsResponse;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.SimpleScalarPropertiesRequest;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.SimpleScalarPropertiesResponse;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.SmithyRpcV2ProtocolException;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.SparseNullsOperationRequest;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.SparseNullsOperationResponse;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.ValidationException;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.EmptyInputOutputRequestMarshaller;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.Float16RequestMarshaller;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.FractionalSecondsRequestMarshaller;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.GreetingWithErrorsRequestMarshaller;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.NoInputOutputRequestMarshaller;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.OperationWithDefaultsRequestMarshaller;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.OptionalInputOutputRequestMarshaller;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.RecursiveShapesRequestMarshaller;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.RpcV2CborDenseMapsRequestMarshaller;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.RpcV2CborListsRequestMarshaller;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.RpcV2CborSparseMapsRequestMarshaller;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.SimpleScalarPropertiesRequestMarshaller;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.SparseNullsOperationRequestMarshaller;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * Internal implementation of {@link SmithyRpcV2ProtocolAsyncClient}.
 *
 * @see SmithyRpcV2ProtocolAsyncClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultSmithyRpcV2ProtocolAsyncClient implements SmithyRpcV2ProtocolAsyncClient {
    private static final Logger log = LoggerFactory.getLogger(DefaultSmithyRpcV2ProtocolAsyncClient.class);

    private static final AwsProtocolMetadata protocolMetadata = AwsProtocolMetadata.builder()
                                                                                   .serviceProtocol(AwsServiceProtocol.SMITHY_RPC_V2_CBOR).build();

    private final AsyncClientHandler clientHandler;

    private final SmithyRpcV2CborProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    protected DefaultSmithyRpcV2ProtocolAsyncClient(SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsAsyncClientHandler(clientConfiguration);
        this.clientConfiguration = clientConfiguration.toBuilder().option(SdkClientOption.SDK_CLIENT, this)
                                                      .option(SdkClientOption.API_METADATA, "SmithyRpcV2Protocol" + "#" + ServiceVersionInfo.VERSION).build();
        this.protocolFactory = init(SmithyRpcV2CborProtocolFactory.builder()).build();
    }

    /**
     * Invokes the EmptyInputOutput operation asynchronously.
     *
     * @param emptyInputOutputRequest
     * @return A Java Future containing the result of the EmptyInputOutput operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>SmithyRpcV2ProtocolException Base class for all service exceptions. Unknown exceptions will be thrown
     *         as an instance of this type.</li>
     *         </ul>
     * @sample SmithyRpcV2ProtocolAsyncClient.EmptyInputOutput
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/EmptyInputOutput"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<EmptyInputOutputResponse> emptyInputOutput(EmptyInputOutputRequest emptyInputOutputRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(emptyInputOutputRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, emptyInputOutputRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "EmptyInputOutput");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<EmptyInputOutputResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, EmptyInputOutputResponse::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                    case "ValidationException":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                            .exceptionBuilderSupplier(ValidationException::builder).build());
                    case "InvalidGreeting":
                        return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                            .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                    case "ComplexError":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                            .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                    default:
                        return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata, exceptionMetadataMapper);

            CompletableFuture<EmptyInputOutputResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<EmptyInputOutputRequest, EmptyInputOutputResponse>()
                             .withOperationName("EmptyInputOutput").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new EmptyInputOutputRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(emptyInputOutputRequest));
            CompletableFuture<EmptyInputOutputResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
     * Invokes the Float16 operation asynchronously.
     *
     * @param float16Request
     * @return A Java Future containing the result of the Float16 operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>SmithyRpcV2ProtocolException Base class for all service exceptions. Unknown exceptions will be thrown
     *         as an instance of this type.</li>
     *         </ul>
     * @sample SmithyRpcV2ProtocolAsyncClient.Float16
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/Float16" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public CompletableFuture<Float16Response> float16(Float16Request float16Request) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(float16Request, this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, float16Request
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "Float16");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<Float16Response> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                                                                                                         Float16Response::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                    case "ValidationException":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                            .exceptionBuilderSupplier(ValidationException::builder).build());
                    case "InvalidGreeting":
                        return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                            .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                    case "ComplexError":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                            .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                    default:
                        return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata, exceptionMetadataMapper);

            CompletableFuture<Float16Response> executeFuture = clientHandler
                .execute(new ClientExecutionParams<Float16Request, Float16Response>().withOperationName("Float16")
                                                                                     .withProtocolMetadata(protocolMetadata).withMarshaller(new Float16RequestMarshaller(protocolFactory))
                                                                                     .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                                                                     .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                                                                                     .withInput(float16Request));
            CompletableFuture<Float16Response> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
     * Invokes the FractionalSeconds operation asynchronously.
     *
     * @param fractionalSecondsRequest
     * @return A Java Future containing the result of the FractionalSeconds operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>SmithyRpcV2ProtocolException Base class for all service exceptions. Unknown exceptions will be thrown
     *         as an instance of this type.</li>
     *         </ul>
     * @sample SmithyRpcV2ProtocolAsyncClient.FractionalSeconds
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/FractionalSeconds"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<FractionalSecondsResponse> fractionalSeconds(FractionalSecondsRequest fractionalSecondsRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(fractionalSecondsRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, fractionalSecondsRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "FractionalSeconds");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<FractionalSecondsResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, FractionalSecondsResponse::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                    case "ValidationException":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                            .exceptionBuilderSupplier(ValidationException::builder).build());
                    case "InvalidGreeting":
                        return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                            .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                    case "ComplexError":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                            .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                    default:
                        return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata, exceptionMetadataMapper);

            CompletableFuture<FractionalSecondsResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<FractionalSecondsRequest, FractionalSecondsResponse>()
                             .withOperationName("FractionalSeconds").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new FractionalSecondsRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(fractionalSecondsRequest));
            CompletableFuture<FractionalSecondsResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
     * Invokes the GreetingWithErrors operation asynchronously.
     *
     * @param greetingWithErrorsRequest
     * @return A Java Future containing the result of the GreetingWithErrors operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>ComplexErrorException</li>
     *         <li>InvalidGreetingException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>SmithyRpcV2ProtocolException Base class for all service exceptions. Unknown exceptions will be thrown
     *         as an instance of this type.</li>
     *         </ul>
     * @sample SmithyRpcV2ProtocolAsyncClient.GreetingWithErrors
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/GreetingWithErrors"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<GreetingWithErrorsResponse> greetingWithErrors(GreetingWithErrorsRequest greetingWithErrorsRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(greetingWithErrorsRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, greetingWithErrorsRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GreetingWithErrors");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<GreetingWithErrorsResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, GreetingWithErrorsResponse::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                    case "ValidationException":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                            .exceptionBuilderSupplier(ValidationException::builder).build());
                    case "InvalidGreeting":
                        return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                            .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                    case "ComplexError":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                            .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                    default:
                        return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata, exceptionMetadataMapper);

            CompletableFuture<GreetingWithErrorsResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<GreetingWithErrorsRequest, GreetingWithErrorsResponse>()
                             .withOperationName("GreetingWithErrors").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new GreetingWithErrorsRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(greetingWithErrorsRequest));
            CompletableFuture<GreetingWithErrorsResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
     * Invokes the NoInputOutput operation asynchronously.
     *
     * @param noInputOutputRequest
     * @return A Java Future containing the result of the NoInputOutput operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>SmithyRpcV2ProtocolException Base class for all service exceptions. Unknown exceptions will be thrown
     *         as an instance of this type.</li>
     *         </ul>
     * @sample SmithyRpcV2ProtocolAsyncClient.NoInputOutput
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/NoInputOutput"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<NoInputOutputResponse> noInputOutput(NoInputOutputRequest noInputOutputRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(noInputOutputRequest, this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, noInputOutputRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "NoInputOutput");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<NoInputOutputResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                                                                                                               NoInputOutputResponse::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                    case "ValidationException":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                            .exceptionBuilderSupplier(ValidationException::builder).build());
                    case "InvalidGreeting":
                        return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                            .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                    case "ComplexError":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                            .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                    default:
                        return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata, exceptionMetadataMapper);

            CompletableFuture<NoInputOutputResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<NoInputOutputRequest, NoInputOutputResponse>()
                             .withOperationName("NoInputOutput").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new NoInputOutputRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(noInputOutputRequest));
            CompletableFuture<NoInputOutputResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
     * Invokes the OperationWithDefaults operation asynchronously.
     *
     * @param operationWithDefaultsRequest
     * @return A Java Future containing the result of the OperationWithDefaults operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>ValidationException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>SmithyRpcV2ProtocolException Base class for all service exceptions. Unknown exceptions will be thrown
     *         as an instance of this type.</li>
     *         </ul>
     * @sample SmithyRpcV2ProtocolAsyncClient.OperationWithDefaults
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/OperationWithDefaults"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<OperationWithDefaultsResponse> operationWithDefaults(
        OperationWithDefaultsRequest operationWithDefaultsRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(operationWithDefaultsRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, operationWithDefaultsRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithDefaults");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<OperationWithDefaultsResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, OperationWithDefaultsResponse::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                    case "ValidationException":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                            .exceptionBuilderSupplier(ValidationException::builder).build());
                    case "InvalidGreeting":
                        return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                            .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                    case "ComplexError":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                            .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                    default:
                        return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata, exceptionMetadataMapper);

            CompletableFuture<OperationWithDefaultsResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<OperationWithDefaultsRequest, OperationWithDefaultsResponse>()
                             .withOperationName("OperationWithDefaults").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new OperationWithDefaultsRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(operationWithDefaultsRequest));
            CompletableFuture<OperationWithDefaultsResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
     * Invokes the OptionalInputOutput operation asynchronously.
     *
     * @param optionalInputOutputRequest
     * @return A Java Future containing the result of the OptionalInputOutput operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>SmithyRpcV2ProtocolException Base class for all service exceptions. Unknown exceptions will be thrown
     *         as an instance of this type.</li>
     *         </ul>
     * @sample SmithyRpcV2ProtocolAsyncClient.OptionalInputOutput
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/OptionalInputOutput"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<OptionalInputOutputResponse> optionalInputOutput(
        OptionalInputOutputRequest optionalInputOutputRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(optionalInputOutputRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, optionalInputOutputRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OptionalInputOutput");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<OptionalInputOutputResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, OptionalInputOutputResponse::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                    case "ValidationException":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                            .exceptionBuilderSupplier(ValidationException::builder).build());
                    case "InvalidGreeting":
                        return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                            .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                    case "ComplexError":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                            .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                    default:
                        return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata, exceptionMetadataMapper);

            CompletableFuture<OptionalInputOutputResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<OptionalInputOutputRequest, OptionalInputOutputResponse>()
                             .withOperationName("OptionalInputOutput").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new OptionalInputOutputRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(optionalInputOutputRequest));
            CompletableFuture<OptionalInputOutputResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
     * Invokes the RecursiveShapes operation asynchronously.
     *
     * @param recursiveShapesRequest
     * @return A Java Future containing the result of the RecursiveShapes operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>SmithyRpcV2ProtocolException Base class for all service exceptions. Unknown exceptions will be thrown
     *         as an instance of this type.</li>
     *         </ul>
     * @sample SmithyRpcV2ProtocolAsyncClient.RecursiveShapes
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/RecursiveShapes"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<RecursiveShapesResponse> recursiveShapes(RecursiveShapesRequest recursiveShapesRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(recursiveShapesRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, recursiveShapesRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "RecursiveShapes");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<RecursiveShapesResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, RecursiveShapesResponse::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                    case "ValidationException":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                            .exceptionBuilderSupplier(ValidationException::builder).build());
                    case "InvalidGreeting":
                        return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                            .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                    case "ComplexError":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                            .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                    default:
                        return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata, exceptionMetadataMapper);

            CompletableFuture<RecursiveShapesResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<RecursiveShapesRequest, RecursiveShapesResponse>()
                             .withOperationName("RecursiveShapes").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new RecursiveShapesRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(recursiveShapesRequest));
            CompletableFuture<RecursiveShapesResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
     * Invokes the RpcV2CborDenseMaps operation asynchronously.
     *
     * @param rpcV2CborDenseMapsRequest
     * @return A Java Future containing the result of the RpcV2CborDenseMaps operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>ValidationException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>SmithyRpcV2ProtocolException Base class for all service exceptions. Unknown exceptions will be thrown
     *         as an instance of this type.</li>
     *         </ul>
     * @sample SmithyRpcV2ProtocolAsyncClient.RpcV2CborDenseMaps
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/RpcV2CborDenseMaps"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<RpcV2CborDenseMapsResponse> rpcV2CborDenseMaps(RpcV2CborDenseMapsRequest rpcV2CborDenseMapsRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(rpcV2CborDenseMapsRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, rpcV2CborDenseMapsRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "RpcV2CborDenseMaps");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<RpcV2CborDenseMapsResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, RpcV2CborDenseMapsResponse::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                    case "ValidationException":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                            .exceptionBuilderSupplier(ValidationException::builder).build());
                    case "InvalidGreeting":
                        return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                            .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                    case "ComplexError":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                            .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                    default:
                        return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata, exceptionMetadataMapper);

            CompletableFuture<RpcV2CborDenseMapsResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<RpcV2CborDenseMapsRequest, RpcV2CborDenseMapsResponse>()
                             .withOperationName("RpcV2CborDenseMaps").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new RpcV2CborDenseMapsRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(rpcV2CborDenseMapsRequest));
            CompletableFuture<RpcV2CborDenseMapsResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
     * Invokes the RpcV2CborLists operation asynchronously.
     *
     * @param rpcV2CborListsRequest
     * @return A Java Future containing the result of the RpcV2CborLists operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>ValidationException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>SmithyRpcV2ProtocolException Base class for all service exceptions. Unknown exceptions will be thrown
     *         as an instance of this type.</li>
     *         </ul>
     * @sample SmithyRpcV2ProtocolAsyncClient.RpcV2CborLists
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/RpcV2CborLists"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<RpcV2CborListsResponse> rpcV2CborLists(RpcV2CborListsRequest rpcV2CborListsRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(rpcV2CborListsRequest, this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, rpcV2CborListsRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "RpcV2CborLists");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<RpcV2CborListsResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, RpcV2CborListsResponse::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                    case "ValidationException":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                            .exceptionBuilderSupplier(ValidationException::builder).build());
                    case "InvalidGreeting":
                        return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                            .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                    case "ComplexError":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                            .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                    default:
                        return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata, exceptionMetadataMapper);

            CompletableFuture<RpcV2CborListsResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<RpcV2CborListsRequest, RpcV2CborListsResponse>()
                             .withOperationName("RpcV2CborLists").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new RpcV2CborListsRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(rpcV2CborListsRequest));
            CompletableFuture<RpcV2CborListsResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
     * Invokes the RpcV2CborSparseMaps operation asynchronously.
     *
     * @param rpcV2CborSparseMapsRequest
     * @return A Java Future containing the result of the RpcV2CborSparseMaps operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>ValidationException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>SmithyRpcV2ProtocolException Base class for all service exceptions. Unknown exceptions will be thrown
     *         as an instance of this type.</li>
     *         </ul>
     * @sample SmithyRpcV2ProtocolAsyncClient.RpcV2CborSparseMaps
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/RpcV2CborSparseMaps"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<RpcV2CborSparseMapsResponse> rpcV2CborSparseMaps(
        RpcV2CborSparseMapsRequest rpcV2CborSparseMapsRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(rpcV2CborSparseMapsRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, rpcV2CborSparseMapsRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "RpcV2CborSparseMaps");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<RpcV2CborSparseMapsResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, RpcV2CborSparseMapsResponse::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                    case "ValidationException":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                            .exceptionBuilderSupplier(ValidationException::builder).build());
                    case "InvalidGreeting":
                        return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                            .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                    case "ComplexError":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                            .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                    default:
                        return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata, exceptionMetadataMapper);

            CompletableFuture<RpcV2CborSparseMapsResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<RpcV2CborSparseMapsRequest, RpcV2CborSparseMapsResponse>()
                             .withOperationName("RpcV2CborSparseMaps").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new RpcV2CborSparseMapsRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(rpcV2CborSparseMapsRequest));
            CompletableFuture<RpcV2CborSparseMapsResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
     * Invokes the SimpleScalarProperties operation asynchronously.
     *
     * @param simpleScalarPropertiesRequest
     * @return A Java Future containing the result of the SimpleScalarProperties operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>SmithyRpcV2ProtocolException Base class for all service exceptions. Unknown exceptions will be thrown
     *         as an instance of this type.</li>
     *         </ul>
     * @sample SmithyRpcV2ProtocolAsyncClient.SimpleScalarProperties
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/SimpleScalarProperties"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<SimpleScalarPropertiesResponse> simpleScalarProperties(
        SimpleScalarPropertiesRequest simpleScalarPropertiesRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(simpleScalarPropertiesRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, simpleScalarPropertiesRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "SimpleScalarProperties");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<SimpleScalarPropertiesResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, SimpleScalarPropertiesResponse::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                    case "ValidationException":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                            .exceptionBuilderSupplier(ValidationException::builder).build());
                    case "InvalidGreeting":
                        return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                            .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                    case "ComplexError":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                            .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                    default:
                        return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata, exceptionMetadataMapper);

            CompletableFuture<SimpleScalarPropertiesResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<SimpleScalarPropertiesRequest, SimpleScalarPropertiesResponse>()
                             .withOperationName("SimpleScalarProperties").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new SimpleScalarPropertiesRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(simpleScalarPropertiesRequest));
            CompletableFuture<SimpleScalarPropertiesResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
     * Invokes the SparseNullsOperation operation asynchronously.
     *
     * @param sparseNullsOperationRequest
     * @return A Java Future containing the result of the SparseNullsOperation operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>SmithyRpcV2ProtocolException Base class for all service exceptions. Unknown exceptions will be thrown
     *         as an instance of this type.</li>
     *         </ul>
     * @sample SmithyRpcV2ProtocolAsyncClient.SparseNullsOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/SparseNullsOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<SparseNullsOperationResponse> sparseNullsOperation(
        SparseNullsOperationRequest sparseNullsOperationRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(sparseNullsOperationRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, sparseNullsOperationRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "SparseNullsOperation");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<SparseNullsOperationResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, SparseNullsOperationResponse::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                    case "ValidationException":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                            .exceptionBuilderSupplier(ValidationException::builder).build());
                    case "InvalidGreeting":
                        return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                            .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                    case "ComplexError":
                        return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                            .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                    default:
                        return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata, exceptionMetadataMapper);

            CompletableFuture<SparseNullsOperationResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<SparseNullsOperationRequest, SparseNullsOperationResponse>()
                             .withOperationName("SparseNullsOperation").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new SparseNullsOperationRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(sparseNullsOperationRequest));
            CompletableFuture<SparseNullsOperationResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    @Override
    public final SmithyRpcV2ProtocolServiceClientConfiguration serviceClientConfiguration() {
        return new SmithyRpcV2ProtocolServiceClientConfigurationBuilder(this.clientConfiguration.toBuilder()).build();
    }

    @Override
    public final String serviceName() {
        return SERVICE_NAME;
    }

    private <T extends BaseAwsJsonProtocolFactory.Builder<T>> T init(T builder) {
        return builder.clientConfiguration(clientConfiguration)
                      .defaultServiceExceptionSupplier(SmithyRpcV2ProtocolException::builder)
                      .protocol(AwsJsonProtocol.SMITHY_RPC_V2_CBOR).protocolVersion("1.1");
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
        if (plugins.isEmpty()) {
            return clientConfiguration;
        }
        SdkClientConfiguration.Builder configuration = clientConfiguration.toBuilder();
        SmithyRpcV2ProtocolServiceClientConfigurationBuilder serviceConfigBuilder = new SmithyRpcV2ProtocolServiceClientConfigurationBuilder(
            configuration);
        for (SdkPlugin plugin : plugins) {
            plugin.configureClient(serviceConfigBuilder);
        }
        updateRetryStrategyClientConfiguration(configuration);
        return configuration.build();
    }

    private HttpResponseHandler<AwsServiceException> createErrorResponseHandler(BaseAwsJsonProtocolFactory protocolFactory,
                                                                                JsonOperationMetadata operationMetadata, Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper) {
        return protocolFactory.createErrorResponseHandler(operationMetadata, exceptionMetadataMapper);
    }

    @Override
    public void close() {
        clientHandler.close();
    }
}

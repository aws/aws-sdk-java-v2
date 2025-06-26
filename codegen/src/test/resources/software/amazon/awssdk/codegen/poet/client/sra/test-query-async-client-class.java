package software.amazon.awssdk.services.query;

import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.handler.AwsAsyncClientHandler;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.internal.AwsProtocolMetadata;
import software.amazon.awssdk.awscore.internal.AwsServiceProtocol;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.codegen.internal.UtilsTest;
import software.amazon.awssdk.core.CredentialType;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.AsyncResponseTransformerUtils;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
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
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.runtime.transform.AsyncStreamingRequestMarshaller;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.query.AwsQueryProtocolFactory;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.query.internal.QueryServiceClientConfigurationBuilder;
import software.amazon.awssdk.services.query.model.APostOperationRequest;
import software.amazon.awssdk.services.query.model.APostOperationResponse;
import software.amazon.awssdk.services.query.model.APostOperationWithOutputRequest;
import software.amazon.awssdk.services.query.model.APostOperationWithOutputResponse;
import software.amazon.awssdk.services.query.model.BearerAuthOperationRequest;
import software.amazon.awssdk.services.query.model.BearerAuthOperationResponse;
import software.amazon.awssdk.services.query.model.GetOperationWithChecksumRequest;
import software.amazon.awssdk.services.query.model.GetOperationWithChecksumResponse;
import software.amazon.awssdk.services.query.model.InvalidInputException;
import software.amazon.awssdk.services.query.model.OperationWithChecksumRequiredRequest;
import software.amazon.awssdk.services.query.model.OperationWithChecksumRequiredResponse;
import software.amazon.awssdk.services.query.model.OperationWithContextParamRequest;
import software.amazon.awssdk.services.query.model.OperationWithContextParamResponse;
import software.amazon.awssdk.services.query.model.OperationWithCustomMemberRequest;
import software.amazon.awssdk.services.query.model.OperationWithCustomMemberResponse;
import software.amazon.awssdk.services.query.model.OperationWithCustomizedOperationContextParamRequest;
import software.amazon.awssdk.services.query.model.OperationWithCustomizedOperationContextParamResponse;
import software.amazon.awssdk.services.query.model.OperationWithMapOperationContextParamRequest;
import software.amazon.awssdk.services.query.model.OperationWithMapOperationContextParamResponse;
import software.amazon.awssdk.services.query.model.OperationWithNoneAuthTypeRequest;
import software.amazon.awssdk.services.query.model.OperationWithNoneAuthTypeResponse;
import software.amazon.awssdk.services.query.model.OperationWithOperationContextParamRequest;
import software.amazon.awssdk.services.query.model.OperationWithOperationContextParamResponse;
import software.amazon.awssdk.services.query.model.OperationWithRequestCompressionRequest;
import software.amazon.awssdk.services.query.model.OperationWithRequestCompressionResponse;
import software.amazon.awssdk.services.query.model.OperationWithStaticContextParamsRequest;
import software.amazon.awssdk.services.query.model.OperationWithStaticContextParamsResponse;
import software.amazon.awssdk.services.query.model.PutOperationWithChecksumRequest;
import software.amazon.awssdk.services.query.model.PutOperationWithChecksumResponse;
import software.amazon.awssdk.services.query.model.QueryException;
import software.amazon.awssdk.services.query.model.StreamingInputOperationRequest;
import software.amazon.awssdk.services.query.model.StreamingInputOperationResponse;
import software.amazon.awssdk.services.query.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.services.query.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.services.query.transform.APostOperationRequestMarshaller;
import software.amazon.awssdk.services.query.transform.APostOperationWithOutputRequestMarshaller;
import software.amazon.awssdk.services.query.transform.BearerAuthOperationRequestMarshaller;
import software.amazon.awssdk.services.query.transform.GetOperationWithChecksumRequestMarshaller;
import software.amazon.awssdk.services.query.transform.OperationWithChecksumRequiredRequestMarshaller;
import software.amazon.awssdk.services.query.transform.OperationWithContextParamRequestMarshaller;
import software.amazon.awssdk.services.query.transform.OperationWithCustomMemberRequestMarshaller;
import software.amazon.awssdk.services.query.transform.OperationWithCustomizedOperationContextParamRequestMarshaller;
import software.amazon.awssdk.services.query.transform.OperationWithMapOperationContextParamRequestMarshaller;
import software.amazon.awssdk.services.query.transform.OperationWithNoneAuthTypeRequestMarshaller;
import software.amazon.awssdk.services.query.transform.OperationWithOperationContextParamRequestMarshaller;
import software.amazon.awssdk.services.query.transform.OperationWithRequestCompressionRequestMarshaller;
import software.amazon.awssdk.services.query.transform.OperationWithStaticContextParamsRequestMarshaller;
import software.amazon.awssdk.services.query.transform.PutOperationWithChecksumRequestMarshaller;
import software.amazon.awssdk.services.query.transform.StreamingInputOperationRequestMarshaller;
import software.amazon.awssdk.services.query.transform.StreamingOutputOperationRequestMarshaller;
import software.amazon.awssdk.services.query.waiters.QueryAsyncWaiter;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Pair;

/**
 * Internal implementation of {@link QueryAsyncClient}.
 *
 * @see QueryAsyncClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultQueryAsyncClient implements QueryAsyncClient {
    private static final Logger log = LoggerFactory.getLogger(DefaultQueryAsyncClient.class);

    private static final AwsProtocolMetadata protocolMetadata = AwsProtocolMetadata.builder()
                                                                                   .serviceProtocol(AwsServiceProtocol.QUERY).build();

    private final AsyncClientHandler clientHandler;

    private final AwsQueryProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    private final ScheduledExecutorService executorService;

    protected DefaultQueryAsyncClient(SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsAsyncClientHandler(clientConfiguration);
        this.clientConfiguration = clientConfiguration.toBuilder().option(SdkClientOption.SDK_CLIENT, this).build();
        this.protocolFactory = init();
        this.executorService = clientConfiguration.option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE);
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
     *         <li>QueryException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample QueryAsyncClient.APostOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/APostOperation" target="_top">AWS
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
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "APostOperation");

            HttpResponseHandler<APostOperationResponse> responseHandler = protocolFactory
                .createResponseHandler(APostOperationResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();
            String hostPrefix = "foo-";
            String resolvedHostExpression = "foo-";

            CompletableFuture<APostOperationResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<APostOperationRequest, APostOperationResponse>()
                             .withOperationName("APostOperation").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new APostOperationRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .hostPrefixExpression(resolvedHostExpression).withInput(aPostOperationRequest));
            CompletableFuture<APostOperationResponse> whenCompleteFuture = null;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            return CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
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
     *         <li>QueryException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample QueryAsyncClient.APostOperationWithOutput
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/APostOperationWithOutput"
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
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "APostOperationWithOutput");

            HttpResponseHandler<APostOperationWithOutputResponse> responseHandler = protocolFactory
                .createResponseHandler(APostOperationWithOutputResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();

            CompletableFuture<APostOperationWithOutputResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<APostOperationWithOutputRequest, APostOperationWithOutputResponse>()
                             .withOperationName("APostOperationWithOutput").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new APostOperationWithOutputRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(aPostOperationWithOutputRequest));
            CompletableFuture<APostOperationWithOutputResponse> whenCompleteFuture = null;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            return CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
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
     *         <li>QueryException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample QueryAsyncClient.BearerAuthOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/BearerAuthOperation"
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
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "BearerAuthOperation");

            HttpResponseHandler<BearerAuthOperationResponse> responseHandler = protocolFactory
                .createResponseHandler(BearerAuthOperationResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();

            CompletableFuture<BearerAuthOperationResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<BearerAuthOperationRequest, BearerAuthOperationResponse>()
                             .withOperationName("BearerAuthOperation").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new BearerAuthOperationRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .credentialType(CredentialType.TOKEN).withRequestConfiguration(clientConfiguration)
                             .withMetricCollector(apiCallMetricCollector).withInput(bearerAuthOperationRequest));
            CompletableFuture<BearerAuthOperationResponse> whenCompleteFuture = null;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            return CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
        } catch (Throwable t) {
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
     *         <li>QueryException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample QueryAsyncClient.GetOperationWithChecksum
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/GetOperationWithChecksum"
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
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetOperationWithChecksum");

            HttpResponseHandler<GetOperationWithChecksumResponse> responseHandler = protocolFactory
                .createResponseHandler(GetOperationWithChecksumResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();

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
                                 HttpChecksum.builder().requestChecksumRequired(true).isRequestStreaming(false)
                                             .requestAlgorithm(getOperationWithChecksumRequest.checksumAlgorithmAsString())
                                             .requestAlgorithmHeader("x-amz-sdk-checksum-algorithm").build())
                             .withInput(getOperationWithChecksumRequest));
            CompletableFuture<GetOperationWithChecksumResponse> whenCompleteFuture = null;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            return CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
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
     *         <li>QueryException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample QueryAsyncClient.OperationWithChecksumRequired
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithChecksumRequired"
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
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithChecksumRequired");

            HttpResponseHandler<OperationWithChecksumRequiredResponse> responseHandler = protocolFactory
                .createResponseHandler(OperationWithChecksumRequiredResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();

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
            CompletableFuture<OperationWithChecksumRequiredResponse> whenCompleteFuture = null;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            return CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Invokes the OperationWithContextParam operation asynchronously.
     *
     * @param operationWithContextParamRequest
     * @return A Java Future containing the result of the OperationWithContextParam operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>QueryException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample QueryAsyncClient.OperationWithContextParam
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithContextParam"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<OperationWithContextParamResponse> operationWithContextParam(
        OperationWithContextParamRequest operationWithContextParamRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(operationWithContextParamRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, operationWithContextParamRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithContextParam");

            HttpResponseHandler<OperationWithContextParamResponse> responseHandler = protocolFactory
                .createResponseHandler(OperationWithContextParamResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();

            CompletableFuture<OperationWithContextParamResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<OperationWithContextParamRequest, OperationWithContextParamResponse>()
                             .withOperationName("OperationWithContextParam").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new OperationWithContextParamRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(operationWithContextParamRequest));
            CompletableFuture<OperationWithContextParamResponse> whenCompleteFuture = null;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            return CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Invokes the OperationWithCustomMember operation asynchronously.
     *
     * @param operationWithCustomMemberRequest
     * @return A Java Future containing the result of the OperationWithCustomMember operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>QueryException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample QueryAsyncClient.OperationWithCustomMember
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithCustomMember"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<OperationWithCustomMemberResponse> operationWithCustomMember(
        OperationWithCustomMemberRequest operationWithCustomMemberRequest) {
        operationWithCustomMemberRequest = UtilsTest.dummyRequestModifier(operationWithCustomMemberRequest);
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(operationWithCustomMemberRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, operationWithCustomMemberRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithCustomMember");

            HttpResponseHandler<OperationWithCustomMemberResponse> responseHandler = protocolFactory
                .createResponseHandler(OperationWithCustomMemberResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();

            CompletableFuture<OperationWithCustomMemberResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<OperationWithCustomMemberRequest, OperationWithCustomMemberResponse>()
                             .withOperationName("OperationWithCustomMember").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new OperationWithCustomMemberRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(operationWithCustomMemberRequest));
            CompletableFuture<OperationWithCustomMemberResponse> whenCompleteFuture = null;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            return CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Invokes the OperationWithCustomizedOperationContextParam operation asynchronously.
     *
     * @param operationWithCustomizedOperationContextParamRequest
     * @return A Java Future containing the result of the OperationWithCustomizedOperationContextParam operation
     *         returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>QueryException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample QueryAsyncClient.OperationWithCustomizedOperationContextParam
     * @see <a
     *      href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithCustomizedOperationContextParam"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<OperationWithCustomizedOperationContextParamResponse> operationWithCustomizedOperationContextParam(
        OperationWithCustomizedOperationContextParamRequest operationWithCustomizedOperationContextParamRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(
            operationWithCustomizedOperationContextParamRequest, this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         operationWithCustomizedOperationContextParamRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithCustomizedOperationContextParam");

            HttpResponseHandler<OperationWithCustomizedOperationContextParamResponse> responseHandler = protocolFactory
                .createResponseHandler(OperationWithCustomizedOperationContextParamResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();

            CompletableFuture<OperationWithCustomizedOperationContextParamResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<OperationWithCustomizedOperationContextParamRequest, OperationWithCustomizedOperationContextParamResponse>()
                             .withOperationName("OperationWithCustomizedOperationContextParam")
                             .withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new OperationWithCustomizedOperationContextParamRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(operationWithCustomizedOperationContextParamRequest));
            CompletableFuture<OperationWithCustomizedOperationContextParamResponse> whenCompleteFuture = null;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            return CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Invokes the OperationWithMapOperationContextParam operation asynchronously.
     *
     * @param operationWithMapOperationContextParamRequest
     * @return A Java Future containing the result of the OperationWithMapOperationContextParam operation returned by
     *         the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>QueryException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample QueryAsyncClient.OperationWithMapOperationContextParam
     * @see <a
     *      href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithMapOperationContextParam"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<OperationWithMapOperationContextParamResponse> operationWithMapOperationContextParam(
        OperationWithMapOperationContextParamRequest operationWithMapOperationContextParamRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(operationWithMapOperationContextParamRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         operationWithMapOperationContextParamRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithMapOperationContextParam");

            HttpResponseHandler<OperationWithMapOperationContextParamResponse> responseHandler = protocolFactory
                .createResponseHandler(OperationWithMapOperationContextParamResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();

            CompletableFuture<OperationWithMapOperationContextParamResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<OperationWithMapOperationContextParamRequest, OperationWithMapOperationContextParamResponse>()
                             .withOperationName("OperationWithMapOperationContextParam").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new OperationWithMapOperationContextParamRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(operationWithMapOperationContextParamRequest));
            CompletableFuture<OperationWithMapOperationContextParamResponse> whenCompleteFuture = null;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            return CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
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
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>QueryException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample QueryAsyncClient.OperationWithNoneAuthType
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithNoneAuthType"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<OperationWithNoneAuthTypeResponse> operationWithNoneAuthType(
        OperationWithNoneAuthTypeRequest operationWithNoneAuthTypeRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(operationWithNoneAuthTypeRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, operationWithNoneAuthTypeRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithNoneAuthType");

            HttpResponseHandler<OperationWithNoneAuthTypeResponse> responseHandler = protocolFactory
                .createResponseHandler(OperationWithNoneAuthTypeResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();

            CompletableFuture<OperationWithNoneAuthTypeResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<OperationWithNoneAuthTypeRequest, OperationWithNoneAuthTypeResponse>()
                             .withOperationName("OperationWithNoneAuthType").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new OperationWithNoneAuthTypeRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(operationWithNoneAuthTypeRequest));
            CompletableFuture<OperationWithNoneAuthTypeResponse> whenCompleteFuture = null;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            return CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Invokes the OperationWithOperationContextParam operation asynchronously.
     *
     * @param operationWithOperationContextParamRequest
     * @return A Java Future containing the result of the OperationWithOperationContextParam operation returned by the
     *         service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>QueryException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample QueryAsyncClient.OperationWithOperationContextParam
     * @see <a
     *      href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithOperationContextParam"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<OperationWithOperationContextParamResponse> operationWithOperationContextParam(
        OperationWithOperationContextParamRequest operationWithOperationContextParamRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(operationWithOperationContextParamRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         operationWithOperationContextParamRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithOperationContextParam");

            HttpResponseHandler<OperationWithOperationContextParamResponse> responseHandler = protocolFactory
                .createResponseHandler(OperationWithOperationContextParamResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();

            CompletableFuture<OperationWithOperationContextParamResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<OperationWithOperationContextParamRequest, OperationWithOperationContextParamResponse>()
                             .withOperationName("OperationWithOperationContextParam").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new OperationWithOperationContextParamRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(operationWithOperationContextParamRequest));
            CompletableFuture<OperationWithOperationContextParamResponse> whenCompleteFuture = null;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            return CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
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
     *         <li>QueryException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample QueryAsyncClient.OperationWithRequestCompression
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithRequestCompression"
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
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithRequestCompression");

            HttpResponseHandler<OperationWithRequestCompressionResponse> responseHandler = protocolFactory
                .createResponseHandler(OperationWithRequestCompressionResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();

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
            CompletableFuture<OperationWithRequestCompressionResponse> whenCompleteFuture = null;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            return CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Invokes the OperationWithStaticContextParams operation asynchronously.
     *
     * @param operationWithStaticContextParamsRequest
     * @return A Java Future containing the result of the OperationWithStaticContextParams operation returned by the
     *         service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>QueryException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample QueryAsyncClient.OperationWithStaticContextParams
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithStaticContextParams"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<OperationWithStaticContextParamsResponse> operationWithStaticContextParams(
        OperationWithStaticContextParamsRequest operationWithStaticContextParamsRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(operationWithStaticContextParamsRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         operationWithStaticContextParamsRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithStaticContextParams");

            HttpResponseHandler<OperationWithStaticContextParamsResponse> responseHandler = protocolFactory
                .createResponseHandler(OperationWithStaticContextParamsResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();

            CompletableFuture<OperationWithStaticContextParamsResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<OperationWithStaticContextParamsRequest, OperationWithStaticContextParamsResponse>()
                             .withOperationName("OperationWithStaticContextParams").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new OperationWithStaticContextParamsRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(operationWithStaticContextParamsRequest));
            CompletableFuture<OperationWithStaticContextParamsResponse> whenCompleteFuture = null;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            return CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
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
     *         <li>QueryException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample QueryAsyncClient.PutOperationWithChecksum
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/PutOperationWithChecksum"
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
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "PutOperationWithChecksum");
            Pair<AsyncResponseTransformer<PutOperationWithChecksumResponse, ReturnT>, CompletableFuture<Void>> pair = AsyncResponseTransformerUtils
                .wrapWithEndOfStreamFuture(asyncResponseTransformer);
            asyncResponseTransformer = pair.left();
            CompletableFuture<Void> endOfStreamFuture = pair.right();

            HttpResponseHandler<PutOperationWithChecksumResponse> responseHandler = protocolFactory
                .createResponseHandler(PutOperationWithChecksumResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();

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
                    .putExecutionAttribute(
                        SdkInternalExecutionAttribute.HTTP_CHECKSUM,
                        HttpChecksum
                            .builder()
                            .requestChecksumRequired(false)
                            .isRequestStreaming(true)
                            .requestValidationMode(putOperationWithChecksumRequest.checksumModeAsString())
                            .responseAlgorithmsV2(DefaultChecksumAlgorithm.CRC32C,
                                                  DefaultChecksumAlgorithm.CRC32, DefaultChecksumAlgorithm.SHA1,
                                                  DefaultChecksumAlgorithm.SHA256).build())
                    .withAsyncResponseTransformer(asyncResponseTransformer).withAsyncRequestBody(requestBody)
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
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>QueryException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample QueryAsyncClient.StreamingInputOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/StreamingInputOperation"
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
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "StreamingInputOperation");

            HttpResponseHandler<StreamingInputOperationResponse> responseHandler = protocolFactory
                .createResponseHandler(StreamingInputOperationResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();

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
            CompletableFuture<StreamingInputOperationResponse> whenCompleteFuture = null;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            return CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
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
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>QueryException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample QueryAsyncClient.StreamingOutputOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/StreamingOutputOperation"
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
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "StreamingOutputOperation");
            Pair<AsyncResponseTransformer<StreamingOutputOperationResponse, ReturnT>, CompletableFuture<Void>> pair = AsyncResponseTransformerUtils
                .wrapWithEndOfStreamFuture(asyncResponseTransformer);
            asyncResponseTransformer = pair.left();
            CompletableFuture<Void> endOfStreamFuture = pair.right();

            HttpResponseHandler<StreamingOutputOperationResponse> responseHandler = protocolFactory
                .createResponseHandler(StreamingOutputOperationResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();

            CompletableFuture<ReturnT> executeFuture = clientHandler.execute(
                new ClientExecutionParams<StreamingOutputOperationRequest, StreamingOutputOperationResponse>()
                    .withOperationName("StreamingOutputOperation").withProtocolMetadata(protocolMetadata)
                    .withMarshaller(new StreamingOutputOperationRequestMarshaller(protocolFactory))
                    .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                    .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                    .withAsyncResponseTransformer(asyncResponseTransformer).withInput(streamingOutputOperationRequest),
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
    public QueryAsyncWaiter waiter() {
        return QueryAsyncWaiter.builder().client(this).scheduledExecutorService(executorService).build();
    }

    @Override
    public final QueryServiceClientConfiguration serviceClientConfiguration() {
        return new QueryServiceClientConfigurationBuilder(this.clientConfiguration.toBuilder()).build();
    }

    @Override
    public final String serviceName() {
        return SERVICE_NAME;
    }

    private AwsQueryProtocolFactory init() {
        return AwsQueryProtocolFactory
            .builder()
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("InvalidInput")
                                 .exceptionBuilderSupplier(InvalidInputException::builder).httpStatusCode(400).build())
            .clientConfiguration(clientConfiguration).defaultServiceExceptionSupplier(QueryException::builder).build();
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
        QueryServiceClientConfigurationBuilder serviceConfigBuilder = new QueryServiceClientConfigurationBuilder(configuration);
        for (SdkPlugin plugin : plugins) {
            plugin.configureClient(serviceConfigBuilder);
        }
        updateRetryStrategyClientConfiguration(configuration);
        return configuration.build();
    }

    @Override
    public void close() {
        clientHandler.close();
    }
}

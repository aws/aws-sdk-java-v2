package software.amazon.awssdk.services.xml;

import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.AsyncAws4Signer;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.handler.AwsAsyncClientHandler;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.AsyncClientHandler;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksumRequired;
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
import software.amazon.awssdk.services.xml.model.InvalidInputException;
import software.amazon.awssdk.services.xml.model.OperationWithChecksumRequiredRequest;
import software.amazon.awssdk.services.xml.model.OperationWithChecksumRequiredResponse;
import software.amazon.awssdk.services.xml.model.StreamingInputOperationRequest;
import software.amazon.awssdk.services.xml.model.StreamingInputOperationResponse;
import software.amazon.awssdk.services.xml.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.services.xml.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.services.xml.model.XmlException;
import software.amazon.awssdk.services.xml.model.XmlRequest;
import software.amazon.awssdk.services.xml.transform.APostOperationRequestMarshaller;
import software.amazon.awssdk.services.xml.transform.APostOperationWithOutputRequestMarshaller;
import software.amazon.awssdk.services.xml.transform.OperationWithChecksumRequiredRequestMarshaller;
import software.amazon.awssdk.services.xml.transform.StreamingInputOperationRequestMarshaller;
import software.amazon.awssdk.services.xml.transform.StreamingOutputOperationRequestMarshaller;
import software.amazon.awssdk.utils.CompletableFutureUtils;

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

    protected DefaultXmlAsyncClient(SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsAsyncClientHandler(clientConfiguration);
        this.clientConfiguration = clientConfiguration;
        this.protocolFactory = init();
    }

    @Override
    public final String serviceName() {
        return SERVICE_NAME;
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
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/APostOperation" target="_top">AWS API
     *      Documentation</a>
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
            AwsRequestOverrideConfiguration requestOverrideConfig = aPostOperationRequest.overrideConfiguration().orElse(null);
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
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/APostOperationWithOutput"
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
            AwsRequestOverrideConfiguration requestOverrideConfig = aPostOperationWithOutputRequest.overrideConfiguration()
                                                                                                   .orElse(null);
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
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/OperationWithChecksumRequired"
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
            AwsRequestOverrideConfiguration requestOverrideConfig = operationWithChecksumRequiredRequest.overrideConfiguration()
                                                                                                        .orElse(null);
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
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/StreamingInputOperation"
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
            AwsRequestOverrideConfiguration requestOverrideConfig = streamingInputOperationRequest.overrideConfiguration()
                                                                                                  .orElse(null);
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
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/StreamingOutputOperation"
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
            AwsRequestOverrideConfiguration requestOverrideConfig = streamingOutputOperationRequest.overrideConfiguration()
                                                                                                   .orElse(null);
            CompletableFuture<ReturnT> whenCompleteFuture = null;
            whenCompleteFuture = executeFuture.whenComplete((r, e) -> {
                if (e != null) {
                    runAndLogError(log, "Exception thrown in exceptionOccurred callback, ignoring",
                                   () -> asyncResponseTransformer.exceptionOccurred(e));
                }
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            return CompletableFutureUtils.forwardExceptionTo(whenCompleteFuture, executeFuture);
        } catch (Throwable t) {
            runAndLogError(log, "Exception thrown in exceptionOccurred callback, ignoring",
                           () -> asyncResponseTransformer.exceptionOccurred(t));
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    @Override
    public void close() {
        clientHandler.close();
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
}

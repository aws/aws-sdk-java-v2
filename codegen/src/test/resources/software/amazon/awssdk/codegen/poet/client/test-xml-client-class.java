package software.amazon.awssdk.services.xml;

import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.handler.AwsSyncClientHandler;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksumRequired;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.runtime.transform.StreamingRequestMarshaller;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
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
import software.amazon.awssdk.services.xml.transform.APostOperationRequestMarshaller;
import software.amazon.awssdk.services.xml.transform.APostOperationWithOutputRequestMarshaller;
import software.amazon.awssdk.services.xml.transform.OperationWithChecksumRequiredRequestMarshaller;
import software.amazon.awssdk.services.xml.transform.StreamingInputOperationRequestMarshaller;
import software.amazon.awssdk.services.xml.transform.StreamingOutputOperationRequestMarshaller;
import software.amazon.awssdk.utils.Logger;

/**
 * Internal implementation of {@link XmlClient}.
 *
 * @see XmlClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultXmlClient implements XmlClient {
    private static final Logger log = Logger.loggerFor(DefaultXmlClient.class);

    private final SyncClientHandler clientHandler;

    private final AwsXmlProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    protected DefaultXmlClient(SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsSyncClientHandler(clientConfiguration);
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
     * @return Result of the APostOperation operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws XmlException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample XmlClient.APostOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/APostOperation" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public APostOperationResponse aPostOperation(APostOperationRequest aPostOperationRequest) throws InvalidInputException,
                                                                                                     AwsServiceException, SdkClientException, XmlException {

        HttpResponseHandler<Response<APostOperationResponse>> responseHandler = protocolFactory.createCombinedResponseHandler(
            APostOperationResponse::builder, new XmlOperationMetadata().withHasStreamingSuccessResponse(false));
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, aPostOperationRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Xml Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "APostOperation");
            String hostPrefix = "foo-";
            String resolvedHostExpression = "foo-";

            return clientHandler.execute(new ClientExecutionParams<APostOperationRequest, APostOperationResponse>()
                                             .withOperationName("APostOperation").withCombinedResponseHandler(responseHandler)
                                             .withMetricCollector(apiCallMetricCollector).hostPrefixExpression(resolvedHostExpression)
                                             .withInput(aPostOperationRequest).withMarshaller(new APostOperationRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Performs a post operation to the xml service and has modelled output
     * </p>
     *
     * @param aPostOperationWithOutputRequest
     * @return Result of the APostOperationWithOutput operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws XmlException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample XmlClient.APostOperationWithOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/APostOperationWithOutput"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public APostOperationWithOutputResponse aPostOperationWithOutput(
        APostOperationWithOutputRequest aPostOperationWithOutputRequest) throws InvalidInputException, AwsServiceException,
                                                                                SdkClientException, XmlException {

        HttpResponseHandler<Response<APostOperationWithOutputResponse>> responseHandler = protocolFactory
            .createCombinedResponseHandler(APostOperationWithOutputResponse::builder,
                                           new XmlOperationMetadata().withHasStreamingSuccessResponse(false));
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, aPostOperationWithOutputRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Xml Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "APostOperationWithOutput");

            return clientHandler
                .execute(new ClientExecutionParams<APostOperationWithOutputRequest, APostOperationWithOutputResponse>()
                             .withOperationName("APostOperationWithOutput").withCombinedResponseHandler(responseHandler)
                             .withMetricCollector(apiCallMetricCollector).withInput(aPostOperationWithOutputRequest)
                             .withMarshaller(new APostOperationWithOutputRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the OperationWithChecksumRequired operation.
     *
     * @param operationWithChecksumRequiredRequest
     * @return Result of the OperationWithChecksumRequired operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws XmlException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample XmlClient.OperationWithChecksumRequired
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/OperationWithChecksumRequired"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OperationWithChecksumRequiredResponse operationWithChecksumRequired(
        OperationWithChecksumRequiredRequest operationWithChecksumRequiredRequest) throws AwsServiceException,
                                                                                          SdkClientException, XmlException {

        HttpResponseHandler<Response<OperationWithChecksumRequiredResponse>> responseHandler = protocolFactory
            .createCombinedResponseHandler(OperationWithChecksumRequiredResponse::builder,
                                           new XmlOperationMetadata().withHasStreamingSuccessResponse(false));
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         operationWithChecksumRequiredRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Xml Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithChecksumRequired");

            return clientHandler
                .execute(new ClientExecutionParams<OperationWithChecksumRequiredRequest, OperationWithChecksumRequiredResponse>()
                             .withOperationName("OperationWithChecksumRequired")
                             .withCombinedResponseHandler(responseHandler)
                             .withMetricCollector(apiCallMetricCollector)
                             .withInput(operationWithChecksumRequiredRequest)
                             .putExecutionAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM_REQUIRED,
                                                    HttpChecksumRequired.create())
                             .withMarshaller(new OperationWithChecksumRequiredRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Some operation with a streaming input
     *
     * @param streamingInputOperationRequest
     * @param requestBody
     *        The content to send to the service. A {@link RequestBody} can be created using one of several factory
     *        methods for various sources of data. For example, to create a request body from a file you can do the
     *        following.
     *
     *        <pre>
     * {@code RequestBody.fromFile(new File("myfile.txt"))}
     * </pre>
     *
     *        See documentation in {@link RequestBody} for additional details and which sources of data are supported.
     *        The service documentation for the request content is as follows 'This be a stream'
     * @return Result of the StreamingInputOperation operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws XmlException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample XmlClient.StreamingInputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public StreamingInputOperationResponse streamingInputOperation(StreamingInputOperationRequest streamingInputOperationRequest,
                                                                   RequestBody requestBody) throws AwsServiceException, SdkClientException, XmlException {

        HttpResponseHandler<Response<StreamingInputOperationResponse>> responseHandler = protocolFactory
            .createCombinedResponseHandler(StreamingInputOperationResponse::builder,
                                           new XmlOperationMetadata().withHasStreamingSuccessResponse(false));
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, streamingInputOperationRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Xml Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "StreamingInputOperation");

            return clientHandler
                .execute(new ClientExecutionParams<StreamingInputOperationRequest, StreamingInputOperationResponse>()
                             .withOperationName("StreamingInputOperation")
                             .withCombinedResponseHandler(responseHandler)
                             .withMetricCollector(apiCallMetricCollector)
                             .withInput(streamingInputOperationRequest)
                             .withRequestBody(requestBody)
                             .withMarshaller(
                                 StreamingRequestMarshaller.builder()
                                                           .delegateMarshaller(new StreamingInputOperationRequestMarshaller(protocolFactory))
                                                           .requestBody(requestBody).build()));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Some operation with a streaming output
     *
     * @param streamingOutputOperationRequest
     * @param responseTransformer
     *        Functional interface for processing the streamed response content. The unmarshalled
     *        StreamingOutputOperationResponse and an InputStream to the response content are provided as parameters to
     *        the callback. The callback may return a transformed type which will be the return value of this method.
     *        See {@link software.amazon.awssdk.core.sync.ResponseTransformer} for details on implementing this
     *        interface and for links to pre-canned implementations for common scenarios like downloading to a file. The
     *        service documentation for the response content is as follows 'This be a stream'.
     * @return The transformed result of the ResponseTransformer.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws XmlException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample XmlClient.StreamingOutputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public <ReturnT> ReturnT streamingOutputOperation(StreamingOutputOperationRequest streamingOutputOperationRequest,
                                                      ResponseTransformer<StreamingOutputOperationResponse, ReturnT> responseTransformer) throws AwsServiceException,
                                                                                                                                                 SdkClientException, XmlException {

        HttpResponseHandler<StreamingOutputOperationResponse> responseHandler = protocolFactory.createResponseHandler(
            StreamingOutputOperationResponse::builder, new XmlOperationMetadata().withHasStreamingSuccessResponse(true));

        HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, streamingOutputOperationRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Xml Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "StreamingOutputOperation");

            return clientHandler.execute(
                new ClientExecutionParams<StreamingOutputOperationRequest, StreamingOutputOperationResponse>()
                    .withOperationName("StreamingOutputOperation").withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler).withInput(streamingOutputOperationRequest)
                    .withMetricCollector(apiCallMetricCollector)
                    .withMarshaller(new StreamingOutputOperationRequestMarshaller(protocolFactory)), responseTransformer);
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
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

    private AwsXmlProtocolFactory init() {
        return AwsXmlProtocolFactory
            .builder()
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("InvalidInput")
                                 .exceptionBuilderSupplier(InvalidInputException::builder).httpStatusCode(400).build())
            .clientConfiguration(clientConfiguration).defaultServiceExceptionSupplier(XmlException::builder).build();
    }

    @Override
    public void close() {
        clientHandler.close();
    }
}

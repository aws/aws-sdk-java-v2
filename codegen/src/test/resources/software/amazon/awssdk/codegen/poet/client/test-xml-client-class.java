package software.amazon.awssdk.services.xml;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.token.signer.aws.BearerTokenSigner;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.handler.AwsSyncClientHandler;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.internal.AwsProtocolMetadata;
import software.amazon.awssdk.awscore.internal.AwsServiceProtocol;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.core.CredentialType;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksumRequired;
import software.amazon.awssdk.core.internal.interceptor.trait.RequestCompression;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.runtime.transform.StreamingRequestMarshaller;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.xml.AwsXmlProtocolFactory;
import software.amazon.awssdk.protocols.xml.XmlOperationMetadata;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.xml.internal.ServiceVersionInfo;
import software.amazon.awssdk.services.xml.internal.XmlServiceClientConfigurationBuilder;
import software.amazon.awssdk.services.xml.model.APostOperationRequest;
import software.amazon.awssdk.services.xml.model.APostOperationResponse;
import software.amazon.awssdk.services.xml.model.APostOperationWithOutputRequest;
import software.amazon.awssdk.services.xml.model.APostOperationWithOutputResponse;
import software.amazon.awssdk.services.xml.model.BearerAuthOperationRequest;
import software.amazon.awssdk.services.xml.model.BearerAuthOperationResponse;
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
import software.amazon.awssdk.services.xml.transform.GetOperationWithChecksumRequestMarshaller;
import software.amazon.awssdk.services.xml.transform.OperationWithChecksumRequiredRequestMarshaller;
import software.amazon.awssdk.services.xml.transform.OperationWithNoneAuthTypeRequestMarshaller;
import software.amazon.awssdk.services.xml.transform.OperationWithRequestCompressionRequestMarshaller;
import software.amazon.awssdk.services.xml.transform.PutOperationWithChecksumRequestMarshaller;
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

    private static final AwsProtocolMetadata protocolMetadata = AwsProtocolMetadata.builder()
                                                                                   .serviceProtocol(AwsServiceProtocol.REST_XML).build();

    private final SyncClientHandler clientHandler;

    private final AwsXmlProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    protected DefaultXmlClient(SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsSyncClientHandler(clientConfiguration);
        this.clientConfiguration = clientConfiguration.toBuilder().option(SdkClientOption.SDK_CLIENT, this)
                                                      .option(SdkClientOption.API_METADATA, "Xml_Service" + "#" + ServiceVersionInfo.VERSION).build();
        this.protocolFactory = init();
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/APostOperation" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public APostOperationResponse aPostOperation(APostOperationRequest aPostOperationRequest) throws InvalidInputException,
                                                                                                     AwsServiceException, SdkClientException, XmlException {

        HttpResponseHandler<Response<APostOperationResponse>> responseHandler = protocolFactory.createCombinedResponseHandler(
            APostOperationResponse::builder, new XmlOperationMetadata().withHasStreamingSuccessResponse(false));
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(aPostOperationRequest, this.clientConfiguration);
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
                                             .withOperationName("APostOperation").withProtocolMetadata(protocolMetadata)
                                             .withCombinedResponseHandler(responseHandler).withMetricCollector(apiCallMetricCollector)
                                             .hostPrefixExpression(resolvedHostExpression).withRequestConfiguration(clientConfiguration)
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/APostOperationWithOutput"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public APostOperationWithOutputResponse aPostOperationWithOutput(
        APostOperationWithOutputRequest aPostOperationWithOutputRequest) throws InvalidInputException, AwsServiceException,
                                                                                SdkClientException, XmlException {

        HttpResponseHandler<Response<APostOperationWithOutputResponse>> responseHandler = protocolFactory
            .createCombinedResponseHandler(APostOperationWithOutputResponse::builder,
                                           new XmlOperationMetadata().withHasStreamingSuccessResponse(false));
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(aPostOperationWithOutputRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, aPostOperationWithOutputRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Xml Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "APostOperationWithOutput");

            return clientHandler
                .execute(new ClientExecutionParams<APostOperationWithOutputRequest, APostOperationWithOutputResponse>()
                             .withOperationName("APostOperationWithOutput").withProtocolMetadata(protocolMetadata)
                             .withCombinedResponseHandler(responseHandler).withMetricCollector(apiCallMetricCollector)
                             .withRequestConfiguration(clientConfiguration).withInput(aPostOperationWithOutputRequest)
                             .withMarshaller(new APostOperationWithOutputRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the BearerAuthOperation operation.
     *
     * @param bearerAuthOperationRequest
     * @return Result of the BearerAuthOperation operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws XmlException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample XmlClient.BearerAuthOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/BearerAuthOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public BearerAuthOperationResponse bearerAuthOperation(BearerAuthOperationRequest bearerAuthOperationRequest)
        throws AwsServiceException, SdkClientException, XmlException {
        bearerAuthOperationRequest = applySignerOverride(bearerAuthOperationRequest, BearerTokenSigner.create());

        HttpResponseHandler<Response<BearerAuthOperationResponse>> responseHandler = protocolFactory
            .createCombinedResponseHandler(BearerAuthOperationResponse::builder,
                                           new XmlOperationMetadata().withHasStreamingSuccessResponse(false));
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(bearerAuthOperationRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, bearerAuthOperationRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Xml Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "BearerAuthOperation");

            return clientHandler.execute(new ClientExecutionParams<BearerAuthOperationRequest, BearerAuthOperationResponse>()
                                             .withOperationName("BearerAuthOperation").withProtocolMetadata(protocolMetadata)
                                             .withCombinedResponseHandler(responseHandler).withMetricCollector(apiCallMetricCollector)
                                             .credentialType(CredentialType.TOKEN).withRequestConfiguration(clientConfiguration)
                                             .withInput(bearerAuthOperationRequest)
                                             .withMarshaller(new BearerAuthOperationRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the GetOperationWithChecksum operation.
     *
     * @param getOperationWithChecksumRequest
     * @return Result of the GetOperationWithChecksum operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws XmlException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample XmlClient.GetOperationWithChecksum
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/GetOperationWithChecksum"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public GetOperationWithChecksumResponse getOperationWithChecksum(
        GetOperationWithChecksumRequest getOperationWithChecksumRequest) throws AwsServiceException, SdkClientException,
                                                                                XmlException {

        HttpResponseHandler<Response<GetOperationWithChecksumResponse>> responseHandler = protocolFactory
            .createCombinedResponseHandler(GetOperationWithChecksumResponse::builder,
                                           new XmlOperationMetadata().withHasStreamingSuccessResponse(false));
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(getOperationWithChecksumRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, getOperationWithChecksumRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Xml Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetOperationWithChecksum");

            return clientHandler
                .execute(new ClientExecutionParams<GetOperationWithChecksumRequest, GetOperationWithChecksumResponse>()
                             .withOperationName("GetOperationWithChecksum")
                             .withProtocolMetadata(protocolMetadata)
                             .withCombinedResponseHandler(responseHandler)
                             .withMetricCollector(apiCallMetricCollector)
                             .withRequestConfiguration(clientConfiguration)
                             .withInput(getOperationWithChecksumRequest)
                             .putExecutionAttribute(
                                 SdkInternalExecutionAttribute.HTTP_CHECKSUM,
                                 HttpChecksum.builder().requestChecksumRequired(true).isRequestStreaming(false)
                                             .requestAlgorithm(getOperationWithChecksumRequest.checksumAlgorithmAsString())
                                             .requestAlgorithmHeader("x-amz-sdk-checksum-algorithm").build())
                             .withMarshaller(new GetOperationWithChecksumRequestMarshaller(protocolFactory)));
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/OperationWithChecksumRequired"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OperationWithChecksumRequiredResponse operationWithChecksumRequired(
        OperationWithChecksumRequiredRequest operationWithChecksumRequiredRequest) throws AwsServiceException,
                                                                                          SdkClientException, XmlException {

        HttpResponseHandler<Response<OperationWithChecksumRequiredResponse>> responseHandler = protocolFactory
            .createCombinedResponseHandler(OperationWithChecksumRequiredResponse::builder,
                                           new XmlOperationMetadata().withHasStreamingSuccessResponse(false));
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(operationWithChecksumRequiredRequest,
                                                                                  this.clientConfiguration);
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
                             .withProtocolMetadata(protocolMetadata)
                             .withCombinedResponseHandler(responseHandler)
                             .withMetricCollector(apiCallMetricCollector)
                             .withRequestConfiguration(clientConfiguration)
                             .withInput(operationWithChecksumRequiredRequest)
                             .putExecutionAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM_REQUIRED,
                                                    HttpChecksumRequired.create())
                             .withMarshaller(new OperationWithChecksumRequiredRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the OperationWithNoneAuthType operation.
     *
     * @param operationWithNoneAuthTypeRequest
     * @return Result of the OperationWithNoneAuthType operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws XmlException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample XmlClient.OperationWithNoneAuthType
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/OperationWithNoneAuthType"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OperationWithNoneAuthTypeResponse operationWithNoneAuthType(
        OperationWithNoneAuthTypeRequest operationWithNoneAuthTypeRequest) throws AwsServiceException, SdkClientException,
                                                                                  XmlException {

        HttpResponseHandler<Response<OperationWithNoneAuthTypeResponse>> responseHandler = protocolFactory
            .createCombinedResponseHandler(OperationWithNoneAuthTypeResponse::builder,
                                           new XmlOperationMetadata().withHasStreamingSuccessResponse(false));
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(operationWithNoneAuthTypeRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, operationWithNoneAuthTypeRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Xml Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithNoneAuthType");

            return clientHandler
                .execute(new ClientExecutionParams<OperationWithNoneAuthTypeRequest, OperationWithNoneAuthTypeResponse>()
                             .withOperationName("OperationWithNoneAuthType").withProtocolMetadata(protocolMetadata)
                             .withCombinedResponseHandler(responseHandler).withMetricCollector(apiCallMetricCollector)
                             .withRequestConfiguration(clientConfiguration).withInput(operationWithNoneAuthTypeRequest)
                             .putExecutionAttribute(SdkInternalExecutionAttribute.IS_NONE_AUTH_TYPE_REQUEST, false)
                             .withMarshaller(new OperationWithNoneAuthTypeRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the OperationWithRequestCompression operation.
     *
     * @param operationWithRequestCompressionRequest
     * @return Result of the OperationWithRequestCompression operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws XmlException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample XmlClient.OperationWithRequestCompression
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/OperationWithRequestCompression"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OperationWithRequestCompressionResponse operationWithRequestCompression(
        OperationWithRequestCompressionRequest operationWithRequestCompressionRequest) throws AwsServiceException,
                                                                                              SdkClientException, XmlException {

        HttpResponseHandler<Response<OperationWithRequestCompressionResponse>> responseHandler = protocolFactory
            .createCombinedResponseHandler(OperationWithRequestCompressionResponse::builder,
                                           new XmlOperationMetadata().withHasStreamingSuccessResponse(false));
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(operationWithRequestCompressionRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         operationWithRequestCompressionRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Xml Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithRequestCompression");

            return clientHandler
                .execute(new ClientExecutionParams<OperationWithRequestCompressionRequest, OperationWithRequestCompressionResponse>()
                             .withOperationName("OperationWithRequestCompression")
                             .withProtocolMetadata(protocolMetadata)
                             .withCombinedResponseHandler(responseHandler)
                             .withMetricCollector(apiCallMetricCollector)
                             .withRequestConfiguration(clientConfiguration)
                             .withInput(operationWithRequestCompressionRequest)
                             .putExecutionAttribute(SdkInternalExecutionAttribute.REQUEST_COMPRESSION,
                                                    RequestCompression.builder().encodings("gzip").isStreaming(false).build())
                             .withMarshaller(new OperationWithRequestCompressionRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the PutOperationWithChecksum operation.
     *
     * @param putOperationWithChecksumRequest
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
     *        The service documentation for the request content is as follows '
     *        <p>
     *        Object data.
     *        </p>
     *        '
     * @param responseTransformer
     *        Functional interface for processing the streamed response content. The unmarshalled
     *        PutOperationWithChecksumResponse and an InputStream to the response content are provided as parameters to
     *        the callback. The callback may return a transformed type which will be the return value of this method.
     *        See {@link software.amazon.awssdk.core.sync.ResponseTransformer} for details on implementing this
     *        interface and for links to pre-canned implementations for common scenarios like downloading to a file. The
     *        service documentation for the response content is as follows '
     *        <p>
     *        Object data.
     *        </p>
     *        '.
     * @return The transformed result of the ResponseTransformer.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws XmlException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample XmlClient.PutOperationWithChecksum
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/PutOperationWithChecksum"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public <ReturnT> ReturnT putOperationWithChecksum(PutOperationWithChecksumRequest putOperationWithChecksumRequest,
                                                      RequestBody requestBody, ResponseTransformer<PutOperationWithChecksumResponse, ReturnT> responseTransformer)
        throws AwsServiceException, SdkClientException, XmlException {

        HttpResponseHandler<PutOperationWithChecksumResponse> responseHandler = protocolFactory.createResponseHandler(
            PutOperationWithChecksumResponse::builder, new XmlOperationMetadata().withHasStreamingSuccessResponse(true));

        HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(putOperationWithChecksumRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, putOperationWithChecksumRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Xml Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "PutOperationWithChecksum");

            return clientHandler
                .execute(new ClientExecutionParams<PutOperationWithChecksumRequest, PutOperationWithChecksumResponse>()
                             .withOperationName("PutOperationWithChecksum")
                             .withProtocolMetadata(protocolMetadata)
                             .withResponseHandler(responseHandler)
                             .withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration)
                             .withInput(putOperationWithChecksumRequest)
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
                             .withResponseTransformer(responseTransformer)
                             .withRequestBody(requestBody)
                             .withMarshaller(
                                 StreamingRequestMarshaller.builder()
                                                           .delegateMarshaller(new PutOperationWithChecksumRequestMarshaller(protocolFactory))
                                                           .requestBody(requestBody).build()));
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public StreamingInputOperationResponse streamingInputOperation(StreamingInputOperationRequest streamingInputOperationRequest,
                                                                   RequestBody requestBody) throws AwsServiceException, SdkClientException, XmlException {

        HttpResponseHandler<Response<StreamingInputOperationResponse>> responseHandler = protocolFactory
            .createCombinedResponseHandler(StreamingInputOperationResponse::builder,
                                           new XmlOperationMetadata().withHasStreamingSuccessResponse(false));
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(streamingInputOperationRequest,
                                                                                  this.clientConfiguration);
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
                             .withProtocolMetadata(protocolMetadata)
                             .withCombinedResponseHandler(responseHandler)
                             .withMetricCollector(apiCallMetricCollector)
                             .withRequestConfiguration(clientConfiguration)
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/xml-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public <ReturnT> ReturnT streamingOutputOperation(StreamingOutputOperationRequest streamingOutputOperationRequest,
                                                      ResponseTransformer<StreamingOutputOperationResponse, ReturnT> responseTransformer) throws AwsServiceException,
                                                                                                                                                 SdkClientException, XmlException {

        HttpResponseHandler<StreamingOutputOperationResponse> responseHandler = protocolFactory.createResponseHandler(
            StreamingOutputOperationResponse::builder, new XmlOperationMetadata().withHasStreamingSuccessResponse(true));

        HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(streamingOutputOperationRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, streamingOutputOperationRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Xml Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "StreamingOutputOperation");

            return clientHandler.execute(
                new ClientExecutionParams<StreamingOutputOperationRequest, StreamingOutputOperationResponse>()
                    .withOperationName("StreamingOutputOperation").withProtocolMetadata(protocolMetadata)
                    .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                    .withRequestConfiguration(clientConfiguration).withInput(streamingOutputOperationRequest)
                    .withMetricCollector(apiCallMetricCollector).withResponseTransformer(responseTransformer)
                    .withMarshaller(new StreamingOutputOperationRequestMarshaller(protocolFactory)), responseTransformer);
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
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

    @Override
    public final String serviceName() {
        return SERVICE_NAME;
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
        XmlServiceClientConfigurationBuilder serviceConfigBuilder = new XmlServiceClientConfigurationBuilder(configuration);
        for (SdkPlugin plugin : plugins) {
            plugin.configureClient(serviceConfigBuilder);
        }
        updateRetryStrategyClientConfiguration(configuration);
        return configuration.build();
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
    public final XmlServiceClientConfiguration serviceClientConfiguration() {
        return new XmlServiceClientConfigurationBuilder(this.clientConfiguration.toBuilder()).build();
    }

    @Override
    public void close() {
        clientHandler.close();
    }
}

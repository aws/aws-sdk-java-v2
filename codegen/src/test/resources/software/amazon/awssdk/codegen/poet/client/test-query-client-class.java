package software.amazon.awssdk.services.query;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.handler.AwsSyncClientHandler;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.query.AwsQueryProtocolFactory;
import software.amazon.awssdk.services.query.model.APostOperationRequest;
import software.amazon.awssdk.services.query.model.APostOperationResponse;
import software.amazon.awssdk.services.query.model.APostOperationWithOutputRequest;
import software.amazon.awssdk.services.query.model.APostOperationWithOutputResponse;
import software.amazon.awssdk.services.query.model.InvalidInputException;
import software.amazon.awssdk.services.query.model.QueryException;
import software.amazon.awssdk.services.query.transform.APostOperationRequestMarshaller;
import software.amazon.awssdk.services.query.transform.APostOperationWithOutputRequestMarshaller;

/**
 * Internal implementation of {@link QueryClient}.
 *
 * @see QueryClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultQueryClient implements QueryClient {
    private final SyncClientHandler clientHandler;

    private final AwsQueryProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    protected DefaultQueryClient(SdkClientConfiguration clientConfiguration) {
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
     * Performs a post operation to the query service and has no output
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
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.APostOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/APostOperation" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public APostOperationResponse aPostOperation(APostOperationRequest aPostOperationRequest) throws InvalidInputException,
                                                                                                     AwsServiceException, SdkClientException, QueryException {
        String hostPrefix = "foo-";
        String resolvedHostExpression = "foo-";

        HttpResponseHandler<APostOperationResponse> responseHandler = protocolFactory
            .createResponseHandler(APostOperationResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<APostOperationRequest, APostOperationResponse>()
                                         .withOperationName("APostOperation")
                                         .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                         .hostPrefixExpression(resolvedHostExpression).withInput(aPostOperationRequest)
                                         .withMarshaller(new APostOperationRequestMarshaller(protocolFactory)));
    }

    /**
     * <p>
     * Performs a post operation to the query service and has modelled output
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
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.APostOperationWithOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/APostOperationWithOutput"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public APostOperationWithOutputResponse aPostOperationWithOutput(
        APostOperationWithOutputRequest aPostOperationWithOutputRequest) throws InvalidInputException, AwsServiceException,
                                                                                SdkClientException, QueryException {

        HttpResponseHandler<APostOperationWithOutputResponse> responseHandler = protocolFactory
            .createResponseHandler(APostOperationWithOutputResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();

        return clientHandler
            .execute(new ClientExecutionParams<APostOperationWithOutputRequest, APostOperationWithOutputResponse>()
                         .withOperationName("APostOperationWithOutput")
                         .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                         .withInput(aPostOperationWithOutputRequest)
                         .withMarshaller(new APostOperationWithOutputRequestMarshaller(protocolFactory)));
    }

    private AwsQueryProtocolFactory init() {
        return AwsQueryProtocolFactory
            .builder()
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("InvalidInput")
                                 .exceptionBuilderSupplier(InvalidInputException::builder).httpStatusCode(400).build())
            .clientConfiguration(clientConfiguration).defaultServiceExceptionSupplier(QueryException::builder).build();
    }

    @Override
    public void close() {
        clientHandler.close();
    }
}

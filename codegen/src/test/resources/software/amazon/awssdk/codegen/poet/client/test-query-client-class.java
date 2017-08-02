package software.amazon.awssdk.services.query;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import org.w3c.dom.Node;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.SdkBaseException;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.client.ClientExecutionParams;
import software.amazon.awssdk.client.ClientHandler;
import software.amazon.awssdk.client.SdkClientHandler;
import software.amazon.awssdk.config.ClientConfiguration;
import software.amazon.awssdk.config.SyncClientConfiguration;
import software.amazon.awssdk.http.DefaultErrorResponseHandler;
import software.amazon.awssdk.http.StaxResponseHandler;
import software.amazon.awssdk.runtime.transform.StandardErrorUnmarshaller;
import software.amazon.awssdk.runtime.transform.Unmarshaller;
import software.amazon.awssdk.services.query.model.APostOperationRequest;
import software.amazon.awssdk.services.query.model.APostOperationResponse;
import software.amazon.awssdk.services.query.model.APostOperationWithOutputRequest;
import software.amazon.awssdk.services.query.model.APostOperationWithOutputResponse;
import software.amazon.awssdk.services.query.model.InvalidInputException;
import software.amazon.awssdk.services.query.model.QueryException;
import software.amazon.awssdk.services.query.transform.APostOperationRequestMarshaller;
import software.amazon.awssdk.services.query.transform.APostOperationResponseUnmarshaller;
import software.amazon.awssdk.services.query.transform.APostOperationWithOutputRequestMarshaller;
import software.amazon.awssdk.services.query.transform.APostOperationWithOutputResponseUnmarshaller;
import software.amazon.awssdk.services.query.transform.InvalidInputExceptionUnmarshaller;
import software.amazon.awssdk.services.query.waiters.QueryClientWaiters;

/**
 * Internal implementation of {@link QueryClient}.
 *
 * @see QueryClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultQueryClient implements QueryClient {
    private final ClientHandler clientHandler;

    private final List<Unmarshaller<AmazonServiceException, Node>> exceptionUnmarshallers;

    private final ClientConfiguration clientConfiguration;

    private volatile QueryClientWaiters waiters;

    protected DefaultQueryClient(SyncClientConfiguration clientConfiguration) {
        this.clientHandler = new SdkClientHandler(clientConfiguration, null);
        this.exceptionUnmarshallers = init();
        this.clientConfiguration = clientConfiguration;
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
     * @throws SdkBaseException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc)
     * @throws QueryException
     *         Base exception for all service exceptions. Unknown exceptions will be thrown as an instance of this type
     * @sample QueryClient.APostOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/APostOperation" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public APostOperationResponse aPostOperation(APostOperationRequest aPostOperationRequest) throws InvalidInputException,
                                                                                                     SdkBaseException, SdkClientException, QueryException {

        StaxResponseHandler<APostOperationResponse> responseHandler = new StaxResponseHandler<APostOperationResponse>(
                new APostOperationResponseUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(new ClientExecutionParams<APostOperationRequest, APostOperationResponse>()
                                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                             .withInput(aPostOperationRequest).withMarshaller(new APostOperationRequestMarshaller()));
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
     * @throws SdkBaseException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc)
     * @throws QueryException
     *         Base exception for all service exceptions. Unknown exceptions will be thrown as an instance of this type
     * @sample QueryClient.APostOperationWithOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/APostOperationWithOutput"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public APostOperationWithOutputResponse aPostOperationWithOutput(
            APostOperationWithOutputRequest aPostOperationWithOutputRequest) throws InvalidInputException, SdkBaseException,
                                                                                    SdkClientException, QueryException {

        StaxResponseHandler<APostOperationWithOutputResponse> responseHandler = new StaxResponseHandler<APostOperationWithOutputResponse>(
                new APostOperationWithOutputResponseUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(new ClientExecutionParams<APostOperationWithOutputRequest, APostOperationWithOutputResponse>()
                                 .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                 .withInput(aPostOperationWithOutputRequest)
                                 .withMarshaller(new APostOperationWithOutputRequestMarshaller()));
    }

    private List<Unmarshaller<AmazonServiceException, Node>> init() {
        List<Unmarshaller<AmazonServiceException, Node>> unmarshallers = new ArrayList<>();
        unmarshallers.add(new InvalidInputExceptionUnmarshaller());
        unmarshallers.add(new StandardErrorUnmarshaller(QueryException.class));
        return unmarshallers;
    }

    public QueryClientWaiters waiters() {
        if (waiters == null) {
            synchronized (this) {
                if (waiters == null) {
                    waiters = new QueryClientWaiters(this);
                }
            }
        }
        return waiters;
    }

    @Override
    public void close() throws Exception {
        clientHandler.close();
    }
}

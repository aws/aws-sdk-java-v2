package software.amazon.awssdk.services.json;

import java.nio.file.Path;
import java.util.function.Consumer;
import javax.annotation.Generated;
import software.amazon.awssdk.core.SdkBaseException;
import software.amazon.awssdk.core.SdkClientException;
import software.amazon.awssdk.core.regions.ServiceMetadata;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseInputStream;
import software.amazon.awssdk.core.sync.StreamingResponseHandler;
import software.amazon.awssdk.services.json.model.APostOperationRequest;
import software.amazon.awssdk.services.json.model.APostOperationResponse;
import software.amazon.awssdk.services.json.model.APostOperationWithOutputRequest;
import software.amazon.awssdk.services.json.model.APostOperationWithOutputResponse;
import software.amazon.awssdk.services.json.model.GetWithoutRequiredMembersRequest;
import software.amazon.awssdk.services.json.model.GetWithoutRequiredMembersResponse;
import software.amazon.awssdk.services.json.model.InvalidInputException;
import software.amazon.awssdk.services.json.model.JsonException;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyRequest;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyResponse;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyRequest;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyResponse;
import software.amazon.awssdk.services.json.model.StreamingInputOperationRequest;
import software.amazon.awssdk.services.json.model.StreamingInputOperationResponse;
import software.amazon.awssdk.services.json.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.services.json.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyPaginator;
import software.amazon.awssdk.services.json.paginators.PaginatedOperationWithoutResultKeyPaginator;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Service client for accessing Json Service. This can be created using the static {@link #builder()} method.
 *
 * A service that is implemented using the query protocol
 */
@Generated("software.amazon.awssdk:codegen")
public interface JsonClient extends SdkAutoCloseable {
    String SERVICE_NAME = "json-service";

    /**
     * Create a {@link JsonClient} with the region loaded from the
     * {@link software.amazon.awssdk.core.regions.providers.DefaultAwsRegionProviderChain} and credentials loaded from
     * the {@link software.amazon.awssdk.core.auth.DefaultCredentialsProvider}.
     */
    static JsonClient create() {
        return builder().build();
    }

    /**
     * Create a builder that can be used to configure and create a {@link JsonClient}.
     */
    static JsonClientBuilder builder() {
        return new DefaultJsonClientBuilder();
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
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.APostOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/APostOperation" target="_top">AWS
     *      API Documentation</a>
     */
    default APostOperationResponse aPostOperation(APostOperationRequest aPostOperationRequest) throws InvalidInputException,
                                                                                                      SdkBaseException, SdkClientException, JsonException {
        throw new UnsupportedOperationException();
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
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.APostOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/APostOperation" target="_top">AWS
     *      API Documentation</a>
     */
    default APostOperationResponse aPostOperation(Consumer<APostOperationRequest.Builder> aPostOperationRequest)
        throws InvalidInputException, SdkBaseException, SdkClientException, JsonException {
        return aPostOperation(APostOperationRequest.builder().apply(aPostOperationRequest).build());
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
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.APostOperationWithOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/APostOperationWithOutput"
     *      target="_top">AWS API Documentation</a>
     */
    default APostOperationWithOutputResponse aPostOperationWithOutput(
        APostOperationWithOutputRequest aPostOperationWithOutputRequest) throws InvalidInputException, SdkBaseException,
                                                                                SdkClientException, JsonException {
        throw new UnsupportedOperationException();
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
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.APostOperationWithOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/APostOperationWithOutput"
     *      target="_top">AWS API Documentation</a>
     */
    default APostOperationWithOutputResponse aPostOperationWithOutput(
        Consumer<APostOperationWithOutputRequest.Builder> aPostOperationWithOutputRequest) throws InvalidInputException,
                                                                                                  SdkBaseException, SdkClientException, JsonException {
        return aPostOperationWithOutput(APostOperationWithOutputRequest.builder().apply(aPostOperationWithOutputRequest).build());
    }

    /**
     * <p>
     * Performs a post operation to the query service and has no output
     * </p>
     *
     * @return Result of the GetWithoutRequiredMembers operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkBaseException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.GetWithoutRequiredMembers
     * @see #getWithoutRequiredMembers(GetWithoutRequiredMembersRequest)
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/GetWithoutRequiredMembers"
     *      target="_top">AWS API Documentation</a>
     */
    default GetWithoutRequiredMembersResponse getWithoutRequiredMembers() throws InvalidInputException, SdkBaseException,
                                                                                 SdkClientException, JsonException {
        return getWithoutRequiredMembers(GetWithoutRequiredMembersRequest.builder().build());
    }

    /**
     * <p>
     * Performs a post operation to the query service and has no output
     * </p>
     *
     * @param getWithoutRequiredMembersRequest
     * @return Result of the GetWithoutRequiredMembers operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkBaseException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.GetWithoutRequiredMembers
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/GetWithoutRequiredMembers"
     *      target="_top">AWS API Documentation</a>
     */
    default GetWithoutRequiredMembersResponse getWithoutRequiredMembers(
        GetWithoutRequiredMembersRequest getWithoutRequiredMembersRequest) throws InvalidInputException, SdkBaseException,
                                                                                  SdkClientException, JsonException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Performs a post operation to the query service and has no output
     * </p>
     *
     * @param getWithoutRequiredMembersRequest
     * @return Result of the GetWithoutRequiredMembers operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkBaseException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.GetWithoutRequiredMembers
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/GetWithoutRequiredMembers"
     *      target="_top">AWS API Documentation</a>
     */
    default GetWithoutRequiredMembersResponse getWithoutRequiredMembers(
        Consumer<GetWithoutRequiredMembersRequest.Builder> getWithoutRequiredMembersRequest) throws InvalidInputException,
                                                                                                    SdkBaseException, SdkClientException, JsonException {
        return getWithoutRequiredMembers(GetWithoutRequiredMembersRequest.builder().apply(getWithoutRequiredMembersRequest)
                                                                         .build());
    }

    /**
     * Some paginated operation with result_key in paginators.json file
     *
     * @param paginatedOperationWithResultKeyRequest
     * @return Result of the PaginatedOperationWithResultKey operation returned by the service.
     * @throws SdkBaseException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.PaginatedOperationWithResultKey
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    default PaginatedOperationWithResultKeyResponse paginatedOperationWithResultKey(
        PaginatedOperationWithResultKeyRequest paginatedOperationWithResultKeyRequest) throws SdkBaseException,
                                                                                              SdkClientException, JsonException {
        throw new UnsupportedOperationException();
    }

    /**
     * Some paginated operation with result_key in paginators.json file
     *
     * @param paginatedOperationWithResultKeyRequest
     * @return Result of the PaginatedOperationWithResultKey operation returned by the service.
     * @throws SdkBaseException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.PaginatedOperationWithResultKey
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    default PaginatedOperationWithResultKeyResponse paginatedOperationWithResultKey(
        Consumer<PaginatedOperationWithResultKeyRequest.Builder> paginatedOperationWithResultKeyRequest)
        throws SdkBaseException, SdkClientException, JsonException {
        return paginatedOperationWithResultKey(PaginatedOperationWithResultKeyRequest.builder()
                                                                                     .apply(paginatedOperationWithResultKeyRequest).build());
    }

    /**
     * Some paginated operation with result_key in paginators.json file<br/>
     * <p>
     * This is a variant of
     * {@link #paginatedOperationWithResultKey(software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyRequest)}
     * operation. The return type is a custom iterable that can be used to iterate through all the pages. SDK will
     * internally handle making service calls for you.
     * </p>
     * <p>
     * When this operation is called, a custom iterable is returned but no service calls are made yet. So there is no
     * guarantee that the request is valid. As you iterate through the iterable, SDK will start lazily loading response
     * pages by making service calls until there are no pages left or your iteration stops. If there are errors in your
     * request, you will see the failures only after you start iterating through the iterable.
     * </p>
     *
     * <p>
     * The following are few ways to iterate through the response pages:
     * </p>
     * 1) Using a Stream
     *
     * <pre>
     * {@code
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyPaginator responses = client.paginatedOperationWithResultKeyIterable(request);
     * responses.stream().forEach(....);
     * }
     * </pre>
     *
     * 2) Using For loop
     *
     * <pre>
     * {
     *     &#064;code
     *     software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyPaginator responses = client
     *             .paginatedOperationWithResultKeyIterable(request);
     *     for (software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyResponse response : responses) {
     *         // do something;
     *     }
     * }
     * </pre>
     *
     * 3) Use iterator directly
     *
     * <pre>
     * {@code
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyPaginator responses = client.paginatedOperationWithResultKeyIterable(request);
     * responses.iterator().forEachRemaining(....);
     * }
     * </pre>
     * <p>
     * <b>Note: If you prefer to have control on service calls, use the
     * {@link #paginatedOperationWithResultKey(software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyRequest)}
     * operation.</b>
     * </p>
     *
     * @param paginatedOperationWithResultKeyRequest
     * @return Result of the PaginatedOperationWithResultKey operation returned by the service.
     * @throws SdkBaseException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.PaginatedOperationWithResultKey
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    default PaginatedOperationWithResultKeyPaginator paginatedOperationWithResultKeyIterable(
        PaginatedOperationWithResultKeyRequest paginatedOperationWithResultKeyRequest) throws SdkBaseException,
                                                                                              SdkClientException, JsonException {
        throw new UnsupportedOperationException();
    }

    /**
     * Some paginated operation without result_key in paginators.json file
     *
     * @param paginatedOperationWithoutResultKeyRequest
     * @return Result of the PaginatedOperationWithoutResultKey operation returned by the service.
     * @throws SdkBaseException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.PaginatedOperationWithoutResultKey
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithoutResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    default PaginatedOperationWithoutResultKeyResponse paginatedOperationWithoutResultKey(
        PaginatedOperationWithoutResultKeyRequest paginatedOperationWithoutResultKeyRequest) throws SdkBaseException,
                                                                                                    SdkClientException, JsonException {
        throw new UnsupportedOperationException();
    }

    /**
     * Some paginated operation without result_key in paginators.json file
     *
     * @param paginatedOperationWithoutResultKeyRequest
     * @return Result of the PaginatedOperationWithoutResultKey operation returned by the service.
     * @throws SdkBaseException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.PaginatedOperationWithoutResultKey
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithoutResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    default PaginatedOperationWithoutResultKeyResponse paginatedOperationWithoutResultKey(
        Consumer<PaginatedOperationWithoutResultKeyRequest.Builder> paginatedOperationWithoutResultKeyRequest)
        throws SdkBaseException, SdkClientException, JsonException {
        return paginatedOperationWithoutResultKey(PaginatedOperationWithoutResultKeyRequest.builder()
                                                                                           .apply(paginatedOperationWithoutResultKeyRequest).build());
    }

    /**
     * Some paginated operation without result_key in paginators.json file<br/>
     * <p>
     * This is a variant of
     * {@link #paginatedOperationWithoutResultKey(software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyRequest)}
     * operation. The return type is a custom iterable that can be used to iterate through all the pages. SDK will
     * internally handle making service calls for you.
     * </p>
     * <p>
     * When this operation is called, a custom iterable is returned but no service calls are made yet. So there is no
     * guarantee that the request is valid. As you iterate through the iterable, SDK will start lazily loading response
     * pages by making service calls until there are no pages left or your iteration stops. If there are errors in your
     * request, you will see the failures only after you start iterating through the iterable.
     * </p>
     *
     * <p>
     * The following are few ways to iterate through the response pages:
     * </p>
     * 1) Using a Stream
     *
     * <pre>
     * {@code
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithoutResultKeyPaginator responses = client.paginatedOperationWithoutResultKeyIterable(request);
     * responses.stream().forEach(....);
     * }
     * </pre>
     *
     * 2) Using For loop
     *
     * <pre>
     * {
     *     &#064;code
     *     software.amazon.awssdk.services.json.paginators.PaginatedOperationWithoutResultKeyPaginator responses = client
     *             .paginatedOperationWithoutResultKeyIterable(request);
     *     for (software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyResponse response : responses) {
     *         // do something;
     *     }
     * }
     * </pre>
     *
     * 3) Use iterator directly
     *
     * <pre>
     * {@code
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithoutResultKeyPaginator responses = client.paginatedOperationWithoutResultKeyIterable(request);
     * responses.iterator().forEachRemaining(....);
     * }
     * </pre>
     * <p>
     * <b>Note: If you prefer to have control on service calls, use the
     * {@link #paginatedOperationWithoutResultKey(software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyRequest)}
     * operation.</b>
     * </p>
     *
     * @param paginatedOperationWithoutResultKeyRequest
     * @return Result of the PaginatedOperationWithoutResultKey operation returned by the service.
     * @throws SdkBaseException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.PaginatedOperationWithoutResultKey
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithoutResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    default PaginatedOperationWithoutResultKeyPaginator paginatedOperationWithoutResultKeyIterable(
        PaginatedOperationWithoutResultKeyRequest paginatedOperationWithoutResultKeyRequest) throws SdkBaseException,
                                                                                                    SdkClientException, JsonException {
        throw new UnsupportedOperationException();
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
     * {@code RequestBody.of(new File("myfile.txt"))}
     * </pre>
     *
     *        See documentation in {@link RequestBody} for additional details and which sources of data are supported.
     *        The service documentation for the request content is as follows 'This be a stream'
     * @return Result of the StreamingInputOperation operation returned by the service.
     * @throws SdkBaseException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.StreamingInputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default StreamingInputOperationResponse streamingInputOperation(
        StreamingInputOperationRequest streamingInputOperationRequest, RequestBody requestBody) throws SdkBaseException,
                                                                                                       SdkClientException, JsonException {
        throw new UnsupportedOperationException();
    }

    /**
     * Some operation with a streaming input
     *
     * @param streamingInputOperationRequest
     * @param path
     *        {@link Path} to file containing data to send to the service. File will be read entirely and may be read
     *        multiple times in the event of a retry. If the file does not exist or the current user does not have
     *        access to read it then an exception will be thrown. The service documentation for the request content is
     *        as follows 'This be a stream'
     * @return Result of the StreamingInputOperation operation returned by the service.
     * @throws SdkBaseException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.StreamingInputOperation
     * @see #streamingInputOperation(StreamingInputOperationRequest, RequestBody)
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default StreamingInputOperationResponse streamingInputOperation(
        StreamingInputOperationRequest streamingInputOperationRequest, Path filePath) throws SdkBaseException,
                                                                                             SdkClientException, JsonException {
        return streamingInputOperation(streamingInputOperationRequest, RequestBody.of(filePath));
    }

    /**
     * Some operation with a streaming output
     *
     * @param streamingOutputOperationRequest
     * @param streamingHandler
     *        Functional interface for processing the streamed response content. The unmarshalled
     *        StreamingInputOperationRequest and an InputStream to the response content are provided as parameters to
     *        the callback. The callback may return a transformed type which will be the return value of this method.
     *        See {@link software.amazon.awssdk.core.sync.StreamingResponseHandler} for details on implementing this
     *        interface and for links to pre-canned implementations for common scenarios like downloading to a file. The
     *        service documentation for the response content is as follows 'This be a stream'.
     * @return The transformed result of the StreamingResponseHandler.
     * @throws SdkBaseException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.StreamingOutputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default <ReturnT> ReturnT streamingOutputOperation(StreamingOutputOperationRequest streamingOutputOperationRequest,
                                                       StreamingResponseHandler<StreamingOutputOperationResponse, ReturnT> streamingResponseHandler)
        throws SdkBaseException, SdkClientException, JsonException {
        throw new UnsupportedOperationException();
    }

    /**
     * Some operation with a streaming output
     *
     * @param streamingOutputOperationRequest
     * @param path
     *        {@link Path} to file that response contents will be written to. The file must not exist or this method
     *        will throw an exception. If the file is not writable by the current user then an exception will be thrown.
     *        The service documentation for the response content is as follows 'This be a stream'.
     * @return The transformed result of the StreamingResponseHandler.
     * @throws SdkBaseException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.StreamingOutputOperation
     * @see #streamingOutputOperation(StreamingOutputOperationRequest, StreamingResponseHandler)
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default StreamingOutputOperationResponse streamingOutputOperation(
        StreamingOutputOperationRequest streamingOutputOperationRequest, Path filePath) throws SdkBaseException,
                                                                                               SdkClientException, JsonException {
        return streamingOutputOperation(streamingOutputOperationRequest, StreamingResponseHandler.toFile(filePath));
    }

    /**
     * Some operation with a streaming output
     *
     * @param streamingOutputOperationRequest
     * @return A {@link ResponseInputStream} containing data streamed from service. Note that this is an unmanaged
     *         reference to the underlying HTTP connection so great care must be taken to ensure all data if fully read
     *         from the input stream and that it is properly closed. Failure to do so may result in sub-optimal behavior
     *         and exhausting connections in the connection pool. The unmarshalled response object can be obtained via
     *         {@link ResponseInputStream#response()}. The service documentation for the response content is as follows
     *         'This be a stream'.
     * @throws SdkBaseException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.StreamingOutputOperation
     * @see #getObject(streamingOutputOperation, StreamingResponseHandler)
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default ResponseInputStream<StreamingOutputOperationResponse> streamingOutputOperation(
        StreamingOutputOperationRequest streamingOutputOperationRequest) throws SdkBaseException, SdkClientException,
                                                                                JsonException {
        return streamingOutputOperation(streamingOutputOperationRequest, StreamingResponseHandler.toInputStream());
    }

    static ServiceMetadata serviceMetadata() {
        return ServiceMetadata.of("json-service");
    }
}

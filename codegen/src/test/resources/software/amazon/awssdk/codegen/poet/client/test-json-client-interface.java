package software.amazon.awssdk.services.json;

import java.nio.file.Path;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.ServiceMetadata;
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
import software.amazon.awssdk.services.json.model.StreamingInputOutputOperationRequest;
import software.amazon.awssdk.services.json.model.StreamingInputOutputOperationResponse;
import software.amazon.awssdk.services.json.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.services.json.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyIterable;
import software.amazon.awssdk.services.json.paginators.PaginatedOperationWithoutResultKeyIterable;

/**
 * Service client for accessing Json Service. This can be created using the static {@link #builder()} method.
 *
 * A service that is implemented using the query protocol
 */
@Generated("software.amazon.awssdk:codegen")
public interface JsonClient extends SdkClient {
    String SERVICE_NAME = "json-service";

    /**
     * Create a {@link JsonClient} with the region loaded from the
     * {@link software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain} and credentials loaded from the
     * {@link software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider}.
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
     * @throws SdkException
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
                                                                                                      AwsServiceException, SdkClientException, JsonException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Performs a post operation to the query service and has no output
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link APostOperationRequest.Builder} avoiding the need to
     * create one manually via {@link APostOperationRequest#builder()}
     * </p>
     *
     * @param aPostOperationRequest
     *        A {@link Consumer} that will call methods on {@link APostOperationRequest.Builder} to create a request.
     * @return Result of the APostOperation operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkException
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
            throws InvalidInputException, AwsServiceException, SdkClientException, JsonException {
        return aPostOperation(APostOperationRequest.builder().applyMutation(aPostOperationRequest).build());
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
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.APostOperationWithOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/APostOperationWithOutput"
     *      target="_top">AWS API Documentation</a>
     */
    default APostOperationWithOutputResponse aPostOperationWithOutput(
            APostOperationWithOutputRequest aPostOperationWithOutputRequest) throws InvalidInputException, AwsServiceException,
                                                                                    SdkClientException, JsonException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Performs a post operation to the query service and has modelled output
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link APostOperationWithOutputRequest.Builder} avoiding
     * the need to create one manually via {@link APostOperationWithOutputRequest#builder()}
     * </p>
     *
     * @param aPostOperationWithOutputRequest
     *        A {@link Consumer} that will call methods on {@link APostOperationWithOutputRequest.Builder} to create a
     *        request.
     * @return Result of the APostOperationWithOutput operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkException
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
                                                                                                      AwsServiceException, SdkClientException, JsonException {
        return aPostOperationWithOutput(APostOperationWithOutputRequest.builder().applyMutation(aPostOperationWithOutputRequest)
                                                                       .build());
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
     * @throws SdkException
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
            GetWithoutRequiredMembersRequest getWithoutRequiredMembersRequest) throws InvalidInputException, AwsServiceException,
                                                                                      SdkClientException, JsonException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Performs a post operation to the query service and has no output
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link GetWithoutRequiredMembersRequest.Builder} avoiding
     * the need to create one manually via {@link GetWithoutRequiredMembersRequest#builder()}
     * </p>
     *
     * @param getWithoutRequiredMembersRequest
     *        A {@link Consumer} that will call methods on {@link GetWithoutRequiredMembersRequest.Builder} to create a
     *        request.
     * @return Result of the GetWithoutRequiredMembers operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkException
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
                                                                                                        AwsServiceException, SdkClientException, JsonException {
        return getWithoutRequiredMembers(GetWithoutRequiredMembersRequest.builder()
                                                                         .applyMutation(getWithoutRequiredMembersRequest).build());
    }

    /**
     * Some paginated operation with result_key in paginators.json file
     *
     * @return Result of the PaginatedOperationWithResultKey operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.PaginatedOperationWithResultKey
     * @see #paginatedOperationWithResultKey(PaginatedOperationWithResultKeyRequest)
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    default PaginatedOperationWithResultKeyResponse paginatedOperationWithResultKey() throws AwsServiceException,
                                                                                             SdkClientException, JsonException {
        return paginatedOperationWithResultKey(PaginatedOperationWithResultKeyRequest.builder().build());
    }

    /**
     * Some paginated operation with result_key in paginators.json file
     *
     * @param paginatedOperationWithResultKeyRequest
     * @return Result of the PaginatedOperationWithResultKey operation returned by the service.
     * @throws SdkException
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
            PaginatedOperationWithResultKeyRequest paginatedOperationWithResultKeyRequest) throws AwsServiceException,
                                                                                                  SdkClientException, JsonException {
        throw new UnsupportedOperationException();
    }

    /**
     * Some paginated operation with result_key in paginators.json file<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link PaginatedOperationWithResultKeyRequest.Builder}
     * avoiding the need to create one manually via {@link PaginatedOperationWithResultKeyRequest#builder()}
     * </p>
     *
     * @param paginatedOperationWithResultKeyRequest
     *        A {@link Consumer} that will call methods on {@link PaginatedOperationWithResultKeyRequest.Builder} to
     *        create a request.
     * @return Result of the PaginatedOperationWithResultKey operation returned by the service.
     * @throws SdkException
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
            throws AwsServiceException, SdkClientException, JsonException {
        return paginatedOperationWithResultKey(PaginatedOperationWithResultKeyRequest.builder()
                                                                                     .applyMutation(paginatedOperationWithResultKeyRequest).build());
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
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyIterable responses = client.paginatedOperationWithResultKeyPaginator(request);
     * responses.stream().forEach(....);
     * }
     * </pre>
     *
     * 2) Using For loop
     *
     * <pre>
     * {
     *     &#064;code
     *     software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyIterable responses = client
     *             .paginatedOperationWithResultKeyPaginator(request);
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
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyIterable responses = client.paginatedOperationWithResultKeyPaginator(request);
     * responses.iterator().forEachRemaining(....);
     * }
     * </pre>
     * <p>
     * <b>Note: If you prefer to have control on service calls, use the
     * {@link #paginatedOperationWithResultKey(software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyRequest)}
     * operation.</b>
     * </p>
     *
     * @return A custom iterable that can be used to iterate through all the response pages.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.PaginatedOperationWithResultKey
     * @see #paginatedOperationWithResultKeyPaginator(PaginatedOperationWithResultKeyRequest)
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    default PaginatedOperationWithResultKeyIterable paginatedOperationWithResultKeyPaginator() throws AwsServiceException,
                                                                                                      SdkClientException, JsonException {
        return paginatedOperationWithResultKeyPaginator(PaginatedOperationWithResultKeyRequest.builder().build());
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
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyIterable responses = client.paginatedOperationWithResultKeyPaginator(request);
     * responses.stream().forEach(....);
     * }
     * </pre>
     *
     * 2) Using For loop
     *
     * <pre>
     * {
     *     &#064;code
     *     software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyIterable responses = client
     *             .paginatedOperationWithResultKeyPaginator(request);
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
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyIterable responses = client.paginatedOperationWithResultKeyPaginator(request);
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
     * @return A custom iterable that can be used to iterate through all the response pages.
     * @throws SdkException
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
    default PaginatedOperationWithResultKeyIterable paginatedOperationWithResultKeyPaginator(
            PaginatedOperationWithResultKeyRequest paginatedOperationWithResultKeyRequest) throws AwsServiceException,
                                                                                                  SdkClientException, JsonException {
        throw new UnsupportedOperationException();
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
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyIterable responses = client.paginatedOperationWithResultKeyPaginator(request);
     * responses.stream().forEach(....);
     * }
     * </pre>
     *
     * 2) Using For loop
     *
     * <pre>
     * {
     *     &#064;code
     *     software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyIterable responses = client
     *             .paginatedOperationWithResultKeyPaginator(request);
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
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyIterable responses = client.paginatedOperationWithResultKeyPaginator(request);
     * responses.iterator().forEachRemaining(....);
     * }
     * </pre>
     * <p>
     * <b>Note: If you prefer to have control on service calls, use the
     * {@link #paginatedOperationWithResultKey(software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyRequest)}
     * operation.</b>
     * </p>
     * <p>
     * This is a convenience which creates an instance of the {@link PaginatedOperationWithResultKeyRequest.Builder}
     * avoiding the need to create one manually via {@link PaginatedOperationWithResultKeyRequest#builder()}
     * </p>
     *
     * @param paginatedOperationWithResultKeyRequest
     *        A {@link Consumer} that will call methods on {@link PaginatedOperationWithResultKeyRequest.Builder} to
     *        create a request.
     * @return A custom iterable that can be used to iterate through all the response pages.
     * @throws SdkException
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
    default PaginatedOperationWithResultKeyIterable paginatedOperationWithResultKeyPaginator(
            Consumer<PaginatedOperationWithResultKeyRequest.Builder> paginatedOperationWithResultKeyRequest)
            throws AwsServiceException, SdkClientException, JsonException {
        return paginatedOperationWithResultKeyPaginator(PaginatedOperationWithResultKeyRequest.builder()
                                                                                              .applyMutation(paginatedOperationWithResultKeyRequest).build());
    }

    /**
     * Some paginated operation without result_key in paginators.json file
     *
     * @param paginatedOperationWithoutResultKeyRequest
     * @return Result of the PaginatedOperationWithoutResultKey operation returned by the service.
     * @throws SdkException
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
            PaginatedOperationWithoutResultKeyRequest paginatedOperationWithoutResultKeyRequest) throws AwsServiceException,
                                                                                                        SdkClientException, JsonException {
        throw new UnsupportedOperationException();
    }

    /**
     * Some paginated operation without result_key in paginators.json file<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link PaginatedOperationWithoutResultKeyRequest.Builder}
     * avoiding the need to create one manually via {@link PaginatedOperationWithoutResultKeyRequest#builder()}
     * </p>
     *
     * @param paginatedOperationWithoutResultKeyRequest
     *        A {@link Consumer} that will call methods on {@link PaginatedOperationWithoutResultKeyRequest.Builder} to
     *        create a request.
     * @return Result of the PaginatedOperationWithoutResultKey operation returned by the service.
     * @throws SdkException
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
            throws AwsServiceException, SdkClientException, JsonException {
        return paginatedOperationWithoutResultKey(PaginatedOperationWithoutResultKeyRequest.builder()
                                                                                           .applyMutation(paginatedOperationWithoutResultKeyRequest).build());
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
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithoutResultKeyIterable responses = client.paginatedOperationWithoutResultKeyPaginator(request);
     * responses.stream().forEach(....);
     * }
     * </pre>
     *
     * 2) Using For loop
     *
     * <pre>
     * {
     *     &#064;code
     *     software.amazon.awssdk.services.json.paginators.PaginatedOperationWithoutResultKeyIterable responses = client
     *             .paginatedOperationWithoutResultKeyPaginator(request);
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
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithoutResultKeyIterable responses = client.paginatedOperationWithoutResultKeyPaginator(request);
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
     * @return A custom iterable that can be used to iterate through all the response pages.
     * @throws SdkException
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
    default PaginatedOperationWithoutResultKeyIterable paginatedOperationWithoutResultKeyPaginator(
            PaginatedOperationWithoutResultKeyRequest paginatedOperationWithoutResultKeyRequest) throws AwsServiceException,
                                                                                                        SdkClientException, JsonException {
        throw new UnsupportedOperationException();
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
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithoutResultKeyIterable responses = client.paginatedOperationWithoutResultKeyPaginator(request);
     * responses.stream().forEach(....);
     * }
     * </pre>
     *
     * 2) Using For loop
     *
     * <pre>
     * {
     *     &#064;code
     *     software.amazon.awssdk.services.json.paginators.PaginatedOperationWithoutResultKeyIterable responses = client
     *             .paginatedOperationWithoutResultKeyPaginator(request);
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
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithoutResultKeyIterable responses = client.paginatedOperationWithoutResultKeyPaginator(request);
     * responses.iterator().forEachRemaining(....);
     * }
     * </pre>
     * <p>
     * <b>Note: If you prefer to have control on service calls, use the
     * {@link #paginatedOperationWithoutResultKey(software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyRequest)}
     * operation.</b>
     * </p>
     * <p>
     * This is a convenience which creates an instance of the {@link PaginatedOperationWithoutResultKeyRequest.Builder}
     * avoiding the need to create one manually via {@link PaginatedOperationWithoutResultKeyRequest#builder()}
     * </p>
     *
     * @param paginatedOperationWithoutResultKeyRequest
     *        A {@link Consumer} that will call methods on {@link PaginatedOperationWithoutResultKeyRequest.Builder} to
     *        create a request.
     * @return A custom iterable that can be used to iterate through all the response pages.
     * @throws SdkException
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
    default PaginatedOperationWithoutResultKeyIterable paginatedOperationWithoutResultKeyPaginator(
            Consumer<PaginatedOperationWithoutResultKeyRequest.Builder> paginatedOperationWithoutResultKeyRequest)
            throws AwsServiceException, SdkClientException, JsonException {
        return paginatedOperationWithoutResultKeyPaginator(PaginatedOperationWithoutResultKeyRequest.builder()
                                                                                                    .applyMutation(paginatedOperationWithoutResultKeyRequest).build());
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
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.StreamingInputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default StreamingInputOperationResponse streamingInputOperation(
            StreamingInputOperationRequest streamingInputOperationRequest, RequestBody requestBody) throws AwsServiceException,
                                                                                                           SdkClientException, JsonException {
        throw new UnsupportedOperationException();
    }

    /**
     * Some operation with a streaming input<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StreamingInputOperationRequest.Builder} avoiding
     * the need to create one manually via {@link StreamingInputOperationRequest#builder()}
     * </p>
     *
     * @param streamingInputOperationRequest
     *        A {@link Consumer} that will call methods on {@link StructureWithStreamingMember.Builder} to create a
     *        request.
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
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.StreamingInputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default StreamingInputOperationResponse streamingInputOperation(
            Consumer<StreamingInputOperationRequest.Builder> streamingInputOperationRequest, RequestBody requestBody)
            throws AwsServiceException, SdkClientException, JsonException {
        return streamingInputOperation(StreamingInputOperationRequest.builder().applyMutation(streamingInputOperationRequest)
                                                                     .build(), requestBody);
    }

    /**
     * Some operation with a streaming input
     *
     * @param streamingInputOperationRequest
     * @param sourcePath
     *        {@link Path} to file containing data to send to the service. File will be read entirely and may be read
     *        multiple times in the event of a retry. If the file does not exist or the current user does not have
     *        access to read it then an exception will be thrown. The service documentation for the request content is
     *        as follows 'This be a stream'
     * @return Result of the StreamingInputOperation operation returned by the service.
     * @throws SdkException
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
            StreamingInputOperationRequest streamingInputOperationRequest, Path filePath) throws AwsServiceException,
                                                                                                 SdkClientException, JsonException {
        return streamingInputOperation(streamingInputOperationRequest, RequestBody.fromFile(filePath));
    }

    /**
     * Some operation with a streaming input<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StreamingInputOperationRequest.Builder} avoiding
     * the need to create one manually via {@link StreamingInputOperationRequest#builder()}
     * </p>
     *
     * @param streamingInputOperationRequest
     *        A {@link Consumer} that will call methods on {@link StructureWithStreamingMember.Builder} to create a
     *        request.
     * @param sourcePath
     *        {@link Path} to file containing data to send to the service. File will be read entirely and may be read
     *        multiple times in the event of a retry. If the file does not exist or the current user does not have
     *        access to read it then an exception will be thrown. The service documentation for the request content is
     *        as follows 'This be a stream'
     * @return Result of the StreamingInputOperation operation returned by the service.
     * @throws SdkException
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
            Consumer<StreamingInputOperationRequest.Builder> streamingInputOperationRequest, Path filePath)
            throws AwsServiceException, SdkClientException, JsonException {
        return streamingInputOperation(StreamingInputOperationRequest.builder().applyMutation(streamingInputOperationRequest)
                                                                     .build(), filePath);
    }

    /**
     * Some operation with streaming input and streaming output
     *
     * @param streamingInputOutputOperationRequest
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
     * @param responseTransformer
     *        Functional interface for processing the streamed response content. The unmarshalled
     *        StreamingInputOutputOperationRequest and an InputStream to the response content are provided as parameters
     *        to the callback. The callback may return a transformed type which will be the return value of this method.
     *        See {@link software.amazon.awssdk.core.sync.ResponseTransformer} for details on implementing this
     *        interface and for links to pre-canned implementations for common scenarios like downloading to a file. The
     *        service documentation for the response content is as follows 'This be a stream'.
     * @return The transformed result of the ResponseTransformer.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.StreamingInputOutputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default <ReturnT> ReturnT streamingInputOutputOperation(
            StreamingInputOutputOperationRequest streamingInputOutputOperationRequest, RequestBody requestBody,
            ResponseTransformer<StreamingInputOutputOperationResponse, ReturnT> responseTransformer) throws AwsServiceException,
                                                                                                            SdkClientException, JsonException {
        throw new UnsupportedOperationException();
    }

    /**
     * Some operation with streaming input and streaming output<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StreamingInputOutputOperationRequest.Builder}
     * avoiding the need to create one manually via {@link StreamingInputOutputOperationRequest#builder()}
     * </p>
     *
     * @param streamingInputOutputOperationRequest
     *        A {@link Consumer} that will call methods on {@link StructureWithStreamingMember.Builder} to create a
     *        request.
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
     * @param responseTransformer
     *        Functional interface for processing the streamed response content. The unmarshalled
     *        StreamingInputOutputOperationRequest and an InputStream to the response content are provided as parameters
     *        to the callback. The callback may return a transformed type which will be the return value of this method.
     *        See {@link software.amazon.awssdk.core.sync.ResponseTransformer} for details on implementing this
     *        interface and for links to pre-canned implementations for common scenarios like downloading to a file. The
     *        service documentation for the response content is as follows 'This be a stream'.
     * @return The transformed result of the ResponseTransformer.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.StreamingInputOutputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default <ReturnT> ReturnT streamingInputOutputOperation(
            Consumer<StreamingInputOutputOperationRequest.Builder> streamingInputOutputOperationRequest, RequestBody requestBody,
            ResponseTransformer<StreamingInputOutputOperationResponse, ReturnT> responseTransformer) throws AwsServiceException,
                                                                                                            SdkClientException, JsonException {
        return streamingInputOutputOperation(
                StreamingInputOutputOperationRequest.builder().applyMutation(streamingInputOutputOperationRequest).build(),
                requestBody, responseTransformer);
    }

    /**
     * Some operation with streaming input and streaming output
     *
     * @param streamingInputOutputOperationRequest
     * @param sourcePath
     *        {@link Path} to file containing data to send to the service. File will be read entirely and may be read
     *        multiple times in the event of a retry. If the file does not exist or the current user does not have
     *        access to read it then an exception will be thrown. The service documentation for the request content is
     *        as follows 'This be a stream'
     * @param destinationPath
     *        {@link Path} to file that response contents will be written to. The file must not exist or this method
     *        will throw an exception. If the file is not writable by the current user then an exception will be thrown.
     *        The service documentation for the response content is as follows 'This be a stream'.
     * @return The transformed result of the ResponseTransformer.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.StreamingInputOutputOperation
     * @see #streamingInputOutputOperation(StreamingInputOutputOperationRequest, RequestBody)
     * @see #streamingInputOutputOperation(StreamingInputOutputOperationRequest, ResponseTransformer)
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default StreamingInputOutputOperationResponse streamingInputOutputOperation(
            StreamingInputOutputOperationRequest streamingInputOutputOperationRequest, Path sourcePath, Path destinationPath)
            throws AwsServiceException, SdkClientException, JsonException {
        return streamingInputOutputOperation(streamingInputOutputOperationRequest, RequestBody.fromFile(sourcePath),
                                             ResponseTransformer.toFile(destinationPath));
    }

    /**
     * Some operation with streaming input and streaming output<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StreamingInputOutputOperationRequest.Builder}
     * avoiding the need to create one manually via {@link StreamingInputOutputOperationRequest#builder()}
     * </p>
     *
     * @param streamingInputOutputOperationRequest
     *        A {@link Consumer} that will call methods on {@link StructureWithStreamingMember.Builder} to create a
     *        request.
     * @param sourcePath
     *        {@link Path} to file containing data to send to the service. File will be read entirely and may be read
     *        multiple times in the event of a retry. If the file does not exist or the current user does not have
     *        access to read it then an exception will be thrown. The service documentation for the request content is
     *        as follows 'This be a stream'
     * @param destinationPath
     *        {@link Path} to file that response contents will be written to. The file must not exist or this method
     *        will throw an exception. If the file is not writable by the current user then an exception will be thrown.
     *        The service documentation for the response content is as follows 'This be a stream'.
     * @return The transformed result of the ResponseTransformer.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.StreamingInputOutputOperation
     * @see #streamingInputOutputOperation(StreamingInputOutputOperationRequest, RequestBody)
     * @see #streamingInputOutputOperation(StreamingInputOutputOperationRequest, ResponseTransformer)
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default StreamingInputOutputOperationResponse streamingInputOutputOperation(
            Consumer<StreamingInputOutputOperationRequest.Builder> streamingInputOutputOperationRequest, Path sourcePath,
            Path destinationPath) throws AwsServiceException, SdkClientException, JsonException {
        return streamingInputOutputOperation(
                StreamingInputOutputOperationRequest.builder().applyMutation(streamingInputOutputOperationRequest).build(),
                sourcePath, destinationPath);
    }

    /**
     * Some operation with a streaming output
     *
     * @param streamingOutputOperationRequest
     * @param responseTransformer
     *        Functional interface for processing the streamed response content. The unmarshalled
     *        StreamingInputOutputOperationRequest and an InputStream to the response content are provided as parameters
     *        to the callback. The callback may return a transformed type which will be the return value of this method.
     *        See {@link software.amazon.awssdk.core.sync.ResponseTransformer} for details on implementing this
     *        interface and for links to pre-canned implementations for common scenarios like downloading to a file. The
     *        service documentation for the response content is as follows 'This be a stream'.
     * @return The transformed result of the ResponseTransformer.
     * @throws SdkException
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
                                                       ResponseTransformer<StreamingOutputOperationResponse, ReturnT> responseTransformer) throws AwsServiceException,
                                                                                                                                                  SdkClientException, JsonException {
        throw new UnsupportedOperationException();
    }

    /**
     * Some operation with a streaming output<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StreamingOutputOperationRequest.Builder} avoiding
     * the need to create one manually via {@link StreamingOutputOperationRequest#builder()}
     * </p>
     *
     * @param streamingOutputOperationRequest
     *        A {@link Consumer} that will call methods on {@link StreamingOutputOperationRequest.Builder} to create a
     *        request.
     * @param responseTransformer
     *        Functional interface for processing the streamed response content. The unmarshalled
     *        StreamingInputOutputOperationRequest and an InputStream to the response content are provided as parameters
     *        to the callback. The callback may return a transformed type which will be the return value of this method.
     *        See {@link software.amazon.awssdk.core.sync.ResponseTransformer} for details on implementing this
     *        interface and for links to pre-canned implementations for common scenarios like downloading to a file. The
     *        service documentation for the response content is as follows 'This be a stream'.
     * @return The transformed result of the ResponseTransformer.
     * @throws SdkException
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
    default <ReturnT> ReturnT streamingOutputOperation(
            Consumer<StreamingOutputOperationRequest.Builder> streamingOutputOperationRequest,
            ResponseTransformer<StreamingOutputOperationResponse, ReturnT> responseTransformer) throws AwsServiceException,
                                                                                                       SdkClientException, JsonException {
        return streamingOutputOperation(StreamingOutputOperationRequest.builder().applyMutation(streamingOutputOperationRequest)
                                                                       .build(), responseTransformer);
    }

    /**
     * Some operation with a streaming output
     *
     * @param streamingOutputOperationRequest
     * @param destinationPath
     *        {@link Path} to file that response contents will be written to. The file must not exist or this method
     *        will throw an exception. If the file is not writable by the current user then an exception will be thrown.
     *        The service documentation for the response content is as follows 'This be a stream'.
     * @return The transformed result of the ResponseTransformer.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.StreamingOutputOperation
     * @see #streamingOutputOperation(StreamingOutputOperationRequest, ResponseTransformer)
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default StreamingOutputOperationResponse streamingOutputOperation(
            StreamingOutputOperationRequest streamingOutputOperationRequest, Path filePath) throws AwsServiceException,
                                                                                                   SdkClientException, JsonException {
        return streamingOutputOperation(streamingOutputOperationRequest, ResponseTransformer.toFile(filePath));
    }

    /**
     * Some operation with a streaming output<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StreamingOutputOperationRequest.Builder} avoiding
     * the need to create one manually via {@link StreamingOutputOperationRequest#builder()}
     * </p>
     *
     * @param streamingOutputOperationRequest
     *        A {@link Consumer} that will call methods on {@link StreamingOutputOperationRequest.Builder} to create a
     *        request.
     * @param destinationPath
     *        {@link Path} to file that response contents will be written to. The file must not exist or this method
     *        will throw an exception. If the file is not writable by the current user then an exception will be thrown.
     *        The service documentation for the response content is as follows 'This be a stream'.
     * @return The transformed result of the ResponseTransformer.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.StreamingOutputOperation
     * @see #streamingOutputOperation(StreamingOutputOperationRequest, ResponseTransformer)
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default StreamingOutputOperationResponse streamingOutputOperation(
            Consumer<StreamingOutputOperationRequest.Builder> streamingOutputOperationRequest, Path filePath)
            throws AwsServiceException, SdkClientException, JsonException {
        return streamingOutputOperation(StreamingOutputOperationRequest.builder().applyMutation(streamingOutputOperationRequest)
                                                                       .build(), filePath);
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
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.StreamingOutputOperation
     * @see #getObject(streamingOutputOperation, ResponseTransformer)
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default ResponseInputStream<StreamingOutputOperationResponse> streamingOutputOperation(
            StreamingOutputOperationRequest streamingOutputOperationRequest) throws AwsServiceException, SdkClientException,
                                                                                    JsonException {
        return streamingOutputOperation(streamingOutputOperationRequest, ResponseTransformer.toInputStream());
    }

    /**
     * Some operation with a streaming output<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StreamingOutputOperationRequest.Builder} avoiding
     * the need to create one manually via {@link StreamingOutputOperationRequest#builder()}
     * </p>
     *
     * @param streamingOutputOperationRequest
     *        A {@link Consumer} that will call methods on {@link StreamingOutputOperationRequest.Builder} to create a
     *        request.
     * @return A {@link ResponseInputStream} containing data streamed from service. Note that this is an unmanaged
     *         reference to the underlying HTTP connection so great care must be taken to ensure all data if fully read
     *         from the input stream and that it is properly closed. Failure to do so may result in sub-optimal behavior
     *         and exhausting connections in the connection pool. The unmarshalled response object can be obtained via
     *         {@link ResponseInputStream#response()}. The service documentation for the response content is as follows
     *         'This be a stream'.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.StreamingOutputOperation
     * @see #getObject(streamingOutputOperation, ResponseTransformer)
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default ResponseInputStream<StreamingOutputOperationResponse> streamingOutputOperation(
            Consumer<StreamingOutputOperationRequest.Builder> streamingOutputOperationRequest) throws AwsServiceException,
                                                                                                      SdkClientException, JsonException {
        return streamingOutputOperation(StreamingOutputOperationRequest.builder().applyMutation(streamingOutputOperationRequest)
                                                                       .build());
    }

    /**
     * Some operation with a streaming output
     *
     * @param streamingOutputOperationRequest
     * @return A {@link ResponseBytes} that loads the data streamed from the service into memory and exposes it in
     *         convenient in-memory representations like a byte buffer or string. The unmarshalled response object can
     *         be obtained via {@link ResponseBytes#response()}. The service documentation for the response content is
     *         as follows 'This be a stream'.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.StreamingOutputOperation
     * @see #getObject(streamingOutputOperation, ResponseTransformer)
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default ResponseBytes<StreamingOutputOperationResponse> streamingOutputOperationAsBytes(
            StreamingOutputOperationRequest streamingOutputOperationRequest) throws AwsServiceException, SdkClientException,
                                                                                    JsonException {
        return streamingOutputOperation(streamingOutputOperationRequest, ResponseTransformer.toBytes());
    }

    /**
     * Some operation with a streaming output<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StreamingOutputOperationRequest.Builder} avoiding
     * the need to create one manually via {@link StreamingOutputOperationRequest#builder()}
     * </p>
     *
     * @param streamingOutputOperationRequest
     *        A {@link Consumer} that will call methods on {@link StreamingOutputOperationRequest.Builder} to create a
     *        request.
     * @return A {@link ResponseBytes} that loads the data streamed from the service into memory and exposes it in
     *         convenient in-memory representations like a byte buffer or string. The unmarshalled response object can
     *         be obtained via {@link ResponseBytes#response()}. The service documentation for the response content is
     *         as follows 'This be a stream'.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.StreamingOutputOperation
     * @see #getObject(streamingOutputOperation, ResponseTransformer)
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default ResponseBytes<StreamingOutputOperationResponse> streamingOutputOperationAsBytes(
            Consumer<StreamingOutputOperationRequest.Builder> streamingOutputOperationRequest) throws AwsServiceException,
                                                                                                      SdkClientException, JsonException {
        return streamingOutputOperationAsBytes(StreamingOutputOperationRequest.builder()
                                                                              .applyMutation(streamingOutputOperationRequest).build());
    }

    static ServiceMetadata serviceMetadata() {
        return ServiceMetadata.of("json-service");
    }

    /**
     * Creates an instance of {@link JsonUtilities} object with the configuration set on this client.
     */
    default JsonUtilities utilities() {
        throw new UnsupportedOperationException();
    }
}

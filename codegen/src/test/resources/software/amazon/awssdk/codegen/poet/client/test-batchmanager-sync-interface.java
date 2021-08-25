package software.amazon.awssdk.services.batchmanagertest;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.services.batchmanagertest.batchmanager.BatchManagerTestBatchManager;
import software.amazon.awssdk.services.batchmanagertest.model.BatchManagerTestException;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestBatchRequest;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestBatchResponse;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestRequest;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestResponse;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestBatchRequest;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestBatchResponse;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestRequest;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestResponse;

/**
 * Service client for accessing BatchManagerTest. This can be created using the static {@link #builder()} method.
 *
 * A service that implements the batchManager() method
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
@ThreadSafe
public interface BatchManagerTestClient extends SdkClient {
    String SERVICE_NAME = "batchmanagertest";

    /**
     * Value for looking up the service's metadata from the
     * {@link software.amazon.awssdk.regions.ServiceMetadataProvider}.
     */
    String SERVICE_METADATA_ID = "batchmanagertest";

    /**
     * Create a {@link BatchManagerTestClient} with the region loaded from the
     * {@link software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain} and credentials loaded from the
     * {@link software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider}.
     */
    static BatchManagerTestClient create() {
        return builder().build();
    }

    /**
     * Create a builder that can be used to configure and create a {@link BatchManagerTestClient}.
     */
    static BatchManagerTestClientBuilder builder() {
        return new DefaultBatchManagerTestClientBuilder();
    }

    /**
     * Invokes the DeleteRequest operation.
     *
     * @param deleteRequestRequest
     * @return Result of the DeleteRequest operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws BatchManagerTestException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample BatchManagerTestClient.DeleteRequest
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/batchmanagertest-2016-03-11/DeleteRequest"
     *      target="_top">AWS API Documentation</a>
     */
    default DeleteRequestResponse deleteRequest(DeleteRequestRequest deleteRequestRequest) throws AwsServiceException,
                                                                                                  SdkClientException, BatchManagerTestException {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the DeleteRequest operation.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link DeleteRequestRequest.Builder} avoiding the need to
     * create one manually via {@link DeleteRequestRequest#builder()}
     * </p>
     *
     * @param deleteRequestRequest
     *        A {@link Consumer} that will call methods on {@link DeleteRequestRequest.Builder} to create a request.
     * @return Result of the DeleteRequest operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws BatchManagerTestException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample BatchManagerTestClient.DeleteRequest
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/batchmanagertest-2016-03-11/DeleteRequest"
     *      target="_top">AWS API Documentation</a>
     */
    default DeleteRequestResponse deleteRequest(Consumer<DeleteRequestRequest.Builder> deleteRequestRequest)
        throws AwsServiceException, SdkClientException, BatchManagerTestException {
        return deleteRequest(DeleteRequestRequest.builder().applyMutation(deleteRequestRequest).build());
    }

    /**
     * Invokes the DeleteRequestBatch operation.
     *
     * @param deleteRequestBatchRequest
     * @return Result of the DeleteRequestBatch operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws BatchManagerTestException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample BatchManagerTestClient.DeleteRequestBatch
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/batchmanagertest-2016-03-11/DeleteRequestBatch"
     *      target="_top">AWS API Documentation</a>
     */
    default DeleteRequestBatchResponse deleteRequestBatch(DeleteRequestBatchRequest deleteRequestBatchRequest)
        throws AwsServiceException, SdkClientException, BatchManagerTestException {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the DeleteRequestBatch operation.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link DeleteRequestBatchRequest.Builder} avoiding the
     * need to create one manually via {@link DeleteRequestBatchRequest#builder()}
     * </p>
     *
     * @param deleteRequestBatchRequest
     *        A {@link Consumer} that will call methods on {@link DeleteRequestBatchRequest.Builder} to create a
     *        request.
     * @return Result of the DeleteRequestBatch operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws BatchManagerTestException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample BatchManagerTestClient.DeleteRequestBatch
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/batchmanagertest-2016-03-11/DeleteRequestBatch"
     *      target="_top">AWS API Documentation</a>
     */
    default DeleteRequestBatchResponse deleteRequestBatch(Consumer<DeleteRequestBatchRequest.Builder> deleteRequestBatchRequest)
        throws AwsServiceException, SdkClientException, BatchManagerTestException {
        return deleteRequestBatch(DeleteRequestBatchRequest.builder().applyMutation(deleteRequestBatchRequest).build());
    }

    /**
     * Invokes the SendRequest operation.
     *
     * @param sendRequestRequest
     * @return Result of the SendRequest operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws BatchManagerTestException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample BatchManagerTestClient.SendRequest
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/batchmanagertest-2016-03-11/SendRequest" target="_top">AWS
     *      API Documentation</a>
     */
    default SendRequestResponse sendRequest(SendRequestRequest sendRequestRequest) throws AwsServiceException,
                                                                                          SdkClientException, BatchManagerTestException {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the SendRequest operation.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link SendRequestRequest.Builder} avoiding the need to
     * create one manually via {@link SendRequestRequest#builder()}
     * </p>
     *
     * @param sendRequestRequest
     *        A {@link Consumer} that will call methods on {@link SendRequestRequest.Builder} to create a request.
     * @return Result of the SendRequest operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws BatchManagerTestException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample BatchManagerTestClient.SendRequest
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/batchmanagertest-2016-03-11/SendRequest" target="_top">AWS
     *      API Documentation</a>
     */
    default SendRequestResponse sendRequest(Consumer<SendRequestRequest.Builder> sendRequestRequest) throws AwsServiceException,
                                                                                                            SdkClientException, BatchManagerTestException {
        return sendRequest(SendRequestRequest.builder().applyMutation(sendRequestRequest).build());
    }

    /**
     * Invokes the SendRequestBatch operation.
     *
     * @param sendRequestBatchRequest
     * @return Result of the SendRequestBatch operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws BatchManagerTestException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample BatchManagerTestClient.SendRequestBatch
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/batchmanagertest-2016-03-11/SendRequestBatch"
     *      target="_top">AWS API Documentation</a>
     */
    default SendRequestBatchResponse sendRequestBatch(SendRequestBatchRequest sendRequestBatchRequest)
        throws AwsServiceException, SdkClientException, BatchManagerTestException {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the SendRequestBatch operation.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link SendRequestBatchRequest.Builder} avoiding the need
     * to create one manually via {@link SendRequestBatchRequest#builder()}
     * </p>
     *
     * @param sendRequestBatchRequest
     *        A {@link Consumer} that will call methods on {@link SendRequestBatchRequest.Builder} to create a request.
     * @return Result of the SendRequestBatch operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws BatchManagerTestException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample BatchManagerTestClient.SendRequestBatch
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/batchmanagertest-2016-03-11/SendRequestBatch"
     *      target="_top">AWS API Documentation</a>
     */
    default SendRequestBatchResponse sendRequestBatch(Consumer<SendRequestBatchRequest.Builder> sendRequestBatchRequest)
        throws AwsServiceException, SdkClientException, BatchManagerTestException {
        return sendRequestBatch(SendRequestBatchRequest.builder().applyMutation(sendRequestBatchRequest).build());
    }

    static ServiceMetadata serviceMetadata() {
        return ServiceMetadata.of(SERVICE_METADATA_ID);
    }

    /**
     * Creates an instance of {@link BatchManagerTestBatchManager} object with the configuration set on this client.
     */
    default BatchManagerTestBatchManager batchManager() {
        throw new UnsupportedOperationException();
    }
}

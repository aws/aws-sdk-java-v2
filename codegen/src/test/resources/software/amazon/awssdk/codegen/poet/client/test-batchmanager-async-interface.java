package software.amazon.awssdk.services.batchmanager;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager;

/**
 * Service client for accessing BatchManager asynchronously. This can be created using the static {@link #builder()}
 * method.
 *
 * A service that implements the batchManager() method
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
@ThreadSafe
public interface BatchManagerAsyncClient extends SdkClient {
    String SERVICE_NAME = "batchmanager";

    /**
     * Value for looking up the service's metadata from the
     * {@link software.amazon.awssdk.regions.ServiceMetadataProvider}.
     */
    String SERVICE_METADATA_ID = "batchmanager";

    /**
     * Create a {@link BatchManagerAsyncClient} with the region loaded from the
     * {@link software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain} and credentials loaded from the
     * {@link software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider}.
     */
    static BatchManagerAsyncClient create() {
        return builder().build();
    }

    /**
     * Create a builder that can be used to configure and create a {@link BatchManagerAsyncClient}.
     */
    static BatchManagerAsyncClientBuilder builder() {
        return new DefaultBatchManagerAsyncClientBuilder();
    }

    /**
     * Creates an instance of {@link SqsAsyncBatchManager} object with the configuration set on this client.
     */
    default SqsAsyncBatchManager batchManager() {
        throw new UnsupportedOperationException();
    }
}

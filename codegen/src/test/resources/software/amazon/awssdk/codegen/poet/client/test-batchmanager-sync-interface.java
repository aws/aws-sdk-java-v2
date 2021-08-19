package software.amazon.awssdk.services.batchmanager;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.services.batchmanagertest.batchmanager.SyncBatchManagerTest;

/**
 * Service client for accessing BatchManager. This can be created using the static {@link #builder()} method.
 *
 * A service that implements the batchManager() method
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
@ThreadSafe
public interface BatchManagerClient extends SdkClient {
    String SERVICE_NAME = "batchmanager";

    /**
     * Value for looking up the service's metadata from the
     * {@link software.amazon.awssdk.regions.ServiceMetadataProvider}.
     */
    String SERVICE_METADATA_ID = "batchmanager";

    /**
     * Create a {@link BatchManagerClient} with the region loaded from the
     * {@link software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain} and credentials loaded from the
     * {@link software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider}.
     */
    static BatchManagerClient create() {
        return builder().build();
    }

    /**
     * Create a builder that can be used to configure and create a {@link BatchManagerClient}.
     */
    static BatchManagerClientBuilder builder() {
        return new DefaultBatchManagerClientBuilder();
    }

    static ServiceMetadata serviceMetadata() {
        return ServiceMetadata.of(SERVICE_METADATA_ID);
    }

    /**
     * Creates an instance of {@link SyncBatchManagerTest} object with the configuration set on this client.
     */
    default SyncBatchManagerTest batchManager() {
        throw new UnsupportedOperationException();
    }
}

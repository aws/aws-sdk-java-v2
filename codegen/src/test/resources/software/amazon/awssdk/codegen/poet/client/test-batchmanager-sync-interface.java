package software.amazon.awssdk.services.batchmanagertest;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.services.batchmanagertest.batchmanager.BatchManagerTestBatchManager;

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

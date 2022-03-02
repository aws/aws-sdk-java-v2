package software.amazon.awssdk.services.sdkextensions;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.services.sdkextensions.extensions.SdkExtensionsClientSdkExtension;
import software.amazon.awssdk.services.sdkextensions.model.OneOperationRequest;
import software.amazon.awssdk.services.sdkextensions.model.OneOperationResponse;
import software.amazon.awssdk.services.sdkextensions.model.SdkExtensionsException;

/**
 * Service client for accessing AmazonSdkExtensions. This can be created using the static {@link #builder()} method.
 *
 * null
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
@ThreadSafe
public interface SdkExtensionsClient extends SdkClient, SdkExtensionsClientSdkExtension {
    String SERVICE_NAME = "clientextensions";

    /**
     * Value for looking up the service's metadata from the
     * {@link software.amazon.awssdk.regions.ServiceMetadataProvider}.
     */
    String SERVICE_METADATA_ID = "clientextensions";

    /**
     * Create a {@link SdkExtensionsClient} with the region loaded from the
     * {@link software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain} and credentials loaded from the
     * {@link software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider}.
     */
    static SdkExtensionsClient create() {
        return builder().build();
    }

    /**
     * Create a builder that can be used to configure and create a {@link SdkExtensionsClient}.
     */
    static SdkExtensionsClientBuilder builder() {
        return new DefaultSdkExtensionsClientBuilder();
    }

    /**
     * Invokes the OneOperation operation.
     *
     * @param oneOperationRequest
     * @return Result of the OneOperation operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SdkExtensionsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SdkExtensionsClient.OneOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OneOperation" target="_top">AWS API
     *      Documentation</a>
     */
    default OneOperationResponse oneOperation(OneOperationRequest oneOperationRequest) throws AwsServiceException,
                                                                                              SdkClientException, SdkExtensionsException {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the OneOperation operation.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link OneOperationRequest.Builder} avoiding the need to
     * create one manually via {@link OneOperationRequest#builder()}
     * </p>
     *
     * @param oneOperationRequest
     *        A {@link Consumer} that will call methods on {@link OneShape.Builder} to create a request.
     * @return Result of the OneOperation operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SdkExtensionsException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SdkExtensionsClient.OneOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OneOperation" target="_top">AWS API
     *      Documentation</a>
     */
    default OneOperationResponse oneOperation(Consumer<OneOperationRequest.Builder> oneOperationRequest)
        throws AwsServiceException, SdkClientException, SdkExtensionsException {
        return oneOperation(OneOperationRequest.builder().applyMutation(oneOperationRequest).build());
    }

    static ServiceMetadata serviceMetadata() {
        return ServiceMetadata.of(SERVICE_METADATA_ID);
    }
}

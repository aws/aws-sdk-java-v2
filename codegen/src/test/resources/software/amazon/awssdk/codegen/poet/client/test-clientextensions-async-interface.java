package software.amazon.awssdk.services.sdkextensions;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.sdkextensions.extensions.SdkExtensionsAsyncClientSdkExtension;
import software.amazon.awssdk.services.sdkextensions.model.OneOperationRequest;
import software.amazon.awssdk.services.sdkextensions.model.OneOperationResponse;

/**
 * Service client for accessing AmazonSdkExtensions asynchronously. This can be created using the static
 * {@link #builder()} method.
 *
 * null
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
@ThreadSafe
public interface SdkExtensionsAsyncClient extends SdkClient, SdkExtensionsAsyncClientSdkExtension {
    String SERVICE_NAME = "clientextensions";

    /**
     * Value for looking up the service's metadata from the
     * {@link software.amazon.awssdk.regions.ServiceMetadataProvider}.
     */
    String SERVICE_METADATA_ID = "clientextensions";

    /**
     * Create a {@link SdkExtensionsAsyncClient} with the region loaded from the
     * {@link software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain} and credentials loaded from the
     * {@link software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider}.
     */
    static SdkExtensionsAsyncClient create() {
        return builder().build();
    }

    /**
     * Create a builder that can be used to configure and create a {@link SdkExtensionsAsyncClient}.
     */
    static SdkExtensionsAsyncClientBuilder builder() {
        return new DefaultSdkExtensionsAsyncClientBuilder();
    }

    /**
     * Invokes the OneOperation operation asynchronously.
     *
     * @param oneOperationRequest
     * @return A Java Future containing the result of the OneOperation operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>SdkExtensionsException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample SdkExtensionsAsyncClient.OneOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OneOperation" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<OneOperationResponse> oneOperation(OneOperationRequest oneOperationRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the OneOperation operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link OneOperationRequest.Builder} avoiding the need to
     * create one manually via {@link OneOperationRequest#builder()}
     * </p>
     *
     * @param oneOperationRequest
     *        A {@link Consumer} that will call methods on {@link OneShape.Builder} to create a request.
     * @return A Java Future containing the result of the OneOperation operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>SdkExtensionsException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample SdkExtensionsAsyncClient.OneOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OneOperation" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<OneOperationResponse> oneOperation(Consumer<OneOperationRequest.Builder> oneOperationRequest) {
        return oneOperation(OneOperationRequest.builder().applyMutation(oneOperationRequest).build());
    }
}

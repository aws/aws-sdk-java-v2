package software.amazon.awssdk.services.database;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.services.database.auth.scheme.DatabaseAuthSchemeProvider;
import software.amazon.awssdk.services.database.endpoints.DatabaseEndpointProvider;

/**
 * This includes configuration specific to Database Service that is supported by both {@link DatabaseClientBuilder} and
 * {@link DatabaseAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public interface DatabaseBaseClientBuilder<B extends DatabaseBaseClientBuilder<B, C>, C> extends AwsClientBuilder<B, C> {
    /**
     * Set the {@link DatabaseEndpointProvider} implementation that will be used by the client to determine the endpoint
     * for each request. This is optional; if none is provided a default implementation will be used the SDK.
     */
    default B endpointProvider(DatabaseEndpointProvider endpointProvider) {
        throw new UnsupportedOperationException();
    }

    /**
     * Set the {@link DatabaseAuthSchemeProvider} implementation that will be used by the client to resolve the auth
     * scheme for each request. This is optional; if none is provided a default implementation will be used the SDK.
     */
    default B authSchemeProvider(DatabaseAuthSchemeProvider authSchemeProvider) {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the {@link RegionSet} to be used for operations using Sigv4a signing requests. This is optional; if not
     * provided, the following precedence is used:
     * <ol>
     * <li>{@link software.amazon.awssdk.core.SdkSystemSetting#AWS_SIGV4A_SIGNING_REGION_SET}.</li>
     * <li>as <code>sigv4a_signing_region_set</code> in the configuration file.</li>
     * <li>The region configured for the client.</li>
     * </ol>
     */
    default B sigv4aRegionSet(RegionSet sigv4aRegionSet) {
        throw new UnsupportedOperationException();
    }
}

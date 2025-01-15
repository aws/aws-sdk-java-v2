package software.amazon.awssdk.services.database.auth.scheme;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.database.auth.scheme.internal.DefaultDatabaseAuthSchemeParams;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The parameters object used to resolve the auth schemes for the Database service.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public interface DatabaseAuthSchemeParams extends ToCopyableBuilder<DatabaseAuthSchemeParams.Builder, DatabaseAuthSchemeParams> {
    /**
     * Get a new builder for creating a {@link DatabaseAuthSchemeParams}.
     */
    static Builder builder() {
        return DefaultDatabaseAuthSchemeParams.builder();
    }

    /**
     * Returns the operation for which to resolve the auth scheme.
     */
    String operation();

    /**
     * Returns the region. The region parameter may be used with the "aws.auth#sigv4" auth scheme.
     */
    Region region();

    /**
     * Returns the RegionSet. The regionSet parameter may be used with the "aws.auth#sigv4a" auth scheme.
     */
    RegionSet regionSet();

    Boolean useDualStackEndpoint();

    Boolean useFipsEndpoint();

    String accountId();

    String operationContextParam();

    /**
     * Returns a {@link Builder} to customize the parameters.
     */
    Builder toBuilder();

    /**
     * A builder for a {@link DatabaseAuthSchemeParams}.
     */
    interface Builder extends CopyableBuilder<Builder, DatabaseAuthSchemeParams> {
        /**
         * Set the operation for which to resolve the auth scheme.
         */
        Builder operation(String operation);

        /**
         * Set the region. The region parameter may be used with the "aws.auth#sigv4" auth scheme.
         */
        Builder region(Region region);

        /**
         * Set the RegionSet. The regionSet parameter may be used with the "aws.auth#sigv4a" auth scheme.
         */
        Builder regionSet(RegionSet regionSet);

        Builder useDualStackEndpoint(Boolean useDualStackEndpoint);

        Builder useFipsEndpoint(Boolean useFIPSEndpoint);

        Builder accountId(String accountId);

        Builder operationContextParam(String operationContextParam);

        /**
         * Returns a {@link DatabaseAuthSchemeParams} object that is created from the properties that have been set on
         * the builder.
         */
        DatabaseAuthSchemeParams build();
    }
}

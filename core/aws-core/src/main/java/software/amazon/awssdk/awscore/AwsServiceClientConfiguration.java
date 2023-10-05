/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.awscore;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;

/**
 * Class to expose AWS service client settings to the user, e.g., region
 */
@SdkPublicApi
public abstract class AwsServiceClientConfiguration extends SdkServiceClientConfiguration {

    private final Region region;
    private final IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider;
    private final Map<String, AuthScheme<?>> authSchemes;

    protected AwsServiceClientConfiguration(Builder builder) {
        super(builder);
        this.region = builder.region();
        this.credentialsProvider = builder.credentialsProvider();
        this.authSchemes = builder.authSchemes();
    }

    /**
     *
     * @return The configured region of the AwsClient
     */
    public Region region() {
        return this.region;
    }

    /**
     * @return The configured identity provider
     */
    public IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider() {
        return credentialsProvider;
    }

    /**
     * @return The configured map of auth schemes.
     */
    public Map<String, AuthScheme<?>> authSchemes() {
        return authSchemes;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + super.hashCode();
        result = 31 * result + (region != null ? region.hashCode() : 0);
        result = 31 * result + (credentialsProvider != null ? credentialsProvider.hashCode() : 0);
        result = 31 * result + (authSchemes != null ? authSchemes.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }

        AwsServiceClientConfiguration that = (AwsServiceClientConfiguration) o;
        return Objects.equals(region, that.region)
               && Objects.equals(credentialsProvider, that.credentialsProvider)
               && Objects.equals(authSchemes, that.authSchemes);
    }

    /**
     * The base interface for all AWS service client configuration builders
     */
    public interface Builder extends SdkServiceClientConfiguration.Builder {
        /**
         * Return the region
         */
        default Region region() {
            throw new UnsupportedOperationException();
        }

        /**
         * Configure the region
         */
        default Builder region(Region region) {
            throw new UnsupportedOperationException();
        }

        @Override
        default Builder overrideConfiguration(ClientOverrideConfiguration clientOverrideConfiguration)  {
            throw new UnsupportedOperationException();
        }

        @Override
        default Builder endpointOverride(URI endpointOverride)  {
            throw new UnsupportedOperationException();
        }

        @Override
        default Builder endpointProvider(EndpointProvider endpointProvider)  {
            throw new UnsupportedOperationException();
        }

        /**
         * Configure the credentials provider
         */
        default Builder credentialsProvider(IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
            throw new UnsupportedOperationException();
        }

        default IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider() {
            throw new UnsupportedOperationException();
        }

        /**
         * Adds the given auth scheme. Replaces the an existing auth scheme with the same id.
         */
        default Builder putAuthScheme(AuthScheme<?> authScheme) {
            throw new UnsupportedOperationException();
        }

        /**
         * Returns the configured map of auth schemes.
         */
        default Map<String, AuthScheme<?>> authSchemes() {
            throw new UnsupportedOperationException();
        }

        @Override
        AwsServiceClientConfiguration build();
    }

    protected abstract static class BuilderImpl implements Builder {
        protected ClientOverrideConfiguration overrideConfiguration;
        protected Region region;
        protected URI endpointOverride;
        protected EndpointProvider endpointProvider;
        protected IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider;
        protected Map<String, AuthScheme<?>> authSchemes;

        protected BuilderImpl() {

        }

        protected BuilderImpl(AwsServiceClientConfiguration awsServiceClientConfiguration) {
            this.overrideConfiguration = awsServiceClientConfiguration.overrideConfiguration();
            this.region = awsServiceClientConfiguration.region();
            this.endpointOverride = awsServiceClientConfiguration.endpointOverride().orElse(null);
            this.endpointProvider =  awsServiceClientConfiguration.endpointProvider().orElse(null);
        }

        @Override
        public final ClientOverrideConfiguration overrideConfiguration() {
            return overrideConfiguration;
        }

        @Override
        public final Region region() {
            return region;
        }

        @Override
        public final URI endpointOverride() {
            return endpointOverride;
        }

        @Override
        public final EndpointProvider endpointProvider() {
            return endpointProvider;
        }

        @Override
        public final Builder credentialsProvider(IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return this;
        }

        @Override
        public final IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider() {
            return credentialsProvider;
        }

        @Override
        public final Builder putAuthScheme(AuthScheme<?> authScheme) {
            if (authSchemes == null) {
                authSchemes = new HashMap<>();
            }
            authSchemes.put(authScheme.schemeId(), authScheme);
            return this;
        }

        @Override
        public final Map<String, AuthScheme<?>> authSchemes() {
            if (authSchemes == null) {
                return Collections.emptyMap();
            }
            return Collections.unmodifiableMap(new HashMap<>(authSchemes));
        }
    }
}

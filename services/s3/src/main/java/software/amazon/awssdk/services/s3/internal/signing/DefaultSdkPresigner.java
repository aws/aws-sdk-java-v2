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

package software.amazon.awssdk.services.s3.internal.signing;

import java.net.URI;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.endpoint.DualstackEnabledProvider;
import software.amazon.awssdk.awscore.endpoint.FipsEnabledProvider;
import software.amazon.awssdk.awscore.presigner.SdkPresigner;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.utils.IoUtils;

/**
 * The base class implementing the {@link SdkPresigner} interface.
 * <p/>
 * TODO: This should get moved to aws-core (or split and moved to sdk-core and aws-core) when we support presigning from
 * multiple services.
 * TODO: After moving, this should get marked as an @SdkProtectedApi.
 */
@SdkInternalApi
public abstract class DefaultSdkPresigner implements SdkPresigner {
    private final ProfileFile profileFile;
    private final String profileName;
    private final Region region;
    private final URI endpointOverride;
    private final AwsCredentialsProvider credentialsProvider;
    private final Boolean dualstackEnabled;
    private final boolean fipsEnabled;

    protected DefaultSdkPresigner(Builder<?> b) {
        this.profileFile = ProfileFile.defaultProfileFile();
        this.profileName = ProfileFileSystemSetting.AWS_PROFILE.getStringValueOrThrow();
        this.region = b.region != null ? b.region : DefaultAwsRegionProviderChain.builder()
                                                                                 .profileFile(() -> profileFile)
                                                                                 .profileName(profileName)
                                                                                 .build()
                                                                                 .getRegion();
        this.credentialsProvider = b.credentialsProvider != null ? b.credentialsProvider
                                                                 : DefaultCredentialsProvider.builder()
                                                                                             .profileFile(profileFile)
                                                                                             .profileName(profileName)
                                                                                             .build();
        this.endpointOverride = b.endpointOverride;
        this.dualstackEnabled = b.dualstackEnabled != null ? b.dualstackEnabled
                                                           : DualstackEnabledProvider.builder()
                                                                                     .profileFile(() -> profileFile)
                                                                                     .profileName(profileName)
                                                                                     .build()
                                                                                     .isDualstackEnabled()
                                                                                     .orElse(null);
        this.fipsEnabled = b.fipsEnabled != null ? b.fipsEnabled
                                                 : FipsEnabledProvider.builder()
                                                                      .profileFile(() -> profileFile)
                                                                      .profileName(profileName)
                                                                      .build()
                                                                      .isFipsEnabled()
                                                                      .orElse(false);
    }

    protected ProfileFile profileFile() {
        return profileFile;
    }

    protected String profileName() {
        return profileName;
    }

    protected Region region() {
        return region;
    }

    protected AwsCredentialsProvider credentialsProvider() {
        return credentialsProvider;
    }

    protected Boolean dualstackEnabled() {
        return dualstackEnabled;
    }

    protected boolean fipsEnabled() {
        return fipsEnabled;
    }

    protected URI endpointOverride() {
        return endpointOverride;
    }

    @Override
    public void close() {
        IoUtils.closeIfCloseable(credentialsProvider, null);
    }

    /**
     * The base class implementing the {@link SdkPresigner.Builder} interface.
     */
    @SdkInternalApi
    public abstract static class Builder<B extends Builder<B>>
        implements SdkPresigner.Builder {
        private Region region;
        private AwsCredentialsProvider credentialsProvider;
        private Boolean dualstackEnabled;
        private Boolean fipsEnabled;
        private URI endpointOverride;

        protected Builder() {
        }

        @Override
        public B region(Region region) {
            this.region = region;
            return thisBuilder();
        }

        @Override
        public B credentialsProvider(AwsCredentialsProvider credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return thisBuilder();
        }

        @Override
        public B dualstackEnabled(Boolean dualstackEnabled) {
            this.dualstackEnabled = dualstackEnabled;
            return thisBuilder();
        }

        @Override
        public B fipsEnabled(Boolean fipsEnabled) {
            this.fipsEnabled = fipsEnabled;
            return thisBuilder();
        }

        @Override
        public B endpointOverride(URI endpointOverride) {
            this.endpointOverride = endpointOverride;
            return thisBuilder();
        }

        @SuppressWarnings("unchecked")
        private B thisBuilder() {
            return (B) this;
        }
    }
}

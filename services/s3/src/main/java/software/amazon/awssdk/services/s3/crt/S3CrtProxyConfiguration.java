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

package software.amazon.awssdk.services.s3.crt;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.crtcore.CrtProxyConfiguration;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;


/**
 * Proxy configuration for {@link S3CrtAsyncClientBuilder}. This class is used to configure proxy to be used
 * by the AWS CRT-based S3 client.
 *
 * @see S3CrtHttpConfiguration.Builder#proxyConfiguration(S3CrtProxyConfiguration)
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class S3CrtProxyConfiguration extends CrtProxyConfiguration
    implements ToCopyableBuilder<S3CrtProxyConfiguration.Builder, S3CrtProxyConfiguration>  {

    private S3CrtProxyConfiguration(DefaultBuilder builder) {
        super(builder);
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    /**
     * Builder for {@link S3CrtProxyConfiguration}.
     */
    public interface Builder extends CrtProxyConfiguration.Builder, CopyableBuilder<Builder, S3CrtProxyConfiguration> {

        @Override
        Builder host(String host);

        @Override
        Builder port(int port);

        @Override
        Builder scheme(String scheme);

        @Override
        Builder username(String username);

        @Override
        Builder password(String password);

        @Override
        Builder useSystemPropertyValues(Boolean useSystemPropertyValues);

        @Override
        S3CrtProxyConfiguration build();
    }

    private static final class DefaultBuilder extends CrtProxyConfiguration.DefaultBuilder<DefaultBuilder> implements Builder {

        private DefaultBuilder(S3CrtProxyConfiguration proxyConfiguration) {
            super(proxyConfiguration);
        }

        private DefaultBuilder() {

        }

        @Override
        public S3CrtProxyConfiguration build() {
            return new S3CrtProxyConfiguration(this);
        }
    }
}
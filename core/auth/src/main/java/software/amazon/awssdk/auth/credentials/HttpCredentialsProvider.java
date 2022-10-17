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

package software.amazon.awssdk.auth.credentials;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * A base for many credential providers within the SDK that rely on calling a remote HTTP endpoint to refresh credentials.
 *
 * @see InstanceProfileCredentialsProvider
 * @see ContainerCredentialsProvider
 */
@SdkPublicApi
public interface HttpCredentialsProvider extends AwsCredentialsProvider, SdkAutoCloseable {
    interface Builder<TypeToBuildT extends HttpCredentialsProvider, BuilderT extends Builder<?, ?>> {
        /**
         * Configure whether the provider should fetch credentials asynchronously in the background. If this is true,
         * threads are less likely to block when credentials are loaded, but additional resources are used to maintain
         * the provider.
         *
         * <p>By default, this is disabled.</p>
         */
        BuilderT asyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled);

        /**
         * When {@link #asyncCredentialUpdateEnabled(Boolean)} is true, this configures the name of the threads used for
         * credential refreshing.
         */
        BuilderT asyncThreadName(String asyncThreadName);

        /**
         * Override the default hostname (not path) that is used for credential refreshing. Most users do not need to modify
         * this behavior, except for testing purposes where mocking the HTTP credential source would be useful.
         */
        BuilderT endpoint(String endpoint);

        /**
         * Build the credentials provider based on the configuration on this builder.
         */
        TypeToBuildT build();
    }
}

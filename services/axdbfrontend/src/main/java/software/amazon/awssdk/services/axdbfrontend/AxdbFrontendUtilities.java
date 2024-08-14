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

package software.amazon.awssdk.services.axdbfrontend;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.axdbfrontend.model.GenerateAuthenticationTokenRequest;

/**
 * Utilities for working with AxdbFrontend. An instance of this class can be created by:
 * <p>
 * 1) Using the low-level client {@link AxdbFrontendClient#utilities()} (or {@link AxdbFrontendAsyncClient#utilities()}} method.
 * This is recommended as SDK will use the same configuration from the {@link AxdbFrontendClient} object to create the
 * {@link AxdbFrontendUtilities} object.
 *
 * @snippet :
 * {@code
 * AxdbFrontendClient AxdbFrontendClient = AxdbFrontendClient.create();
 * AxdbFrontendUtilities utilities = AxdbFrontendClient.utilities();
 * }
 *
 * <p>
 * 2) Directly using the {@link #builder()} method.
 *
 * @snippet :
 * {@code
 * AxdbFrontendUtilities utilities = AxdbFrontendUtilities.builder()
 *  .credentialsProvider(DefaultCredentialsProvider.create())
 *  .region(Region.US_WEST_2)
 *  .build()
 * }
 *
 * Note: This class does not make network calls.
 */
@SdkPublicApi
public interface AxdbFrontendUtilities {
    /**
     * Create a builder that can be used to configure and create a {@link AxdbFrontendUtilities}.
     */
    static Builder builder() {
        return new DefaultAxdbFrontendUtilities.DefaultBuilder();
    }

    /**
     * Generates an authentication token for IAM authentication to an AxdbFrontend database.
     *
     * @param request The request used to generate the authentication token
     * @return String to use as the AxdbFrontend authentication token
     * @throws IllegalArgumentException if the required parameters are not valid
     */
    default String generateAuthenticationToken(Consumer<GenerateAuthenticationTokenRequest.Builder> request) {
        return generateAuthenticationToken(GenerateAuthenticationTokenRequest.builder().applyMutation(request).build());
    }

    /**
     * Generates an authentication token for IAM authentication to an AxdbFrontend database.
     *
     * @param request The request used to generate the authentication token
     * @return String to use as the AxdbFrontend authentication token
     * @throws IllegalArgumentException if the required parameters are not valid
     */
    default String generateAuthenticationToken(GenerateAuthenticationTokenRequest request) {
        throw new UnsupportedOperationException();
    }

    /**
     * Builder for creating an instance of {@link AxdbFrontendUtilities}. It can be configured using
     * {@link AxdbFrontendUtilities#builder()}.
     * Once configured, the {@link AxdbFrontendUtilities} can created using {@link #build()}.
     */
    @SdkPublicApi
    interface Builder {
        /**
         * The default region to use when working with the methods in {@link AxdbFrontendUtilities} class.
         *
         * @return This object for method chaining
         */
        Builder region(Region region);

        /**
         * The default credentials provider to use when working with the methods in {@link AxdbFrontendUtilities} class.
         *
         * @return This object for method chaining
         */
        default Builder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
            return credentialsProvider((IdentityProvider<? extends AwsCredentialsIdentity>) credentialsProvider);
        }

        /**
         * The default credentials provider to use when working with the methods in {@link AxdbFrontendUtilities} class.
         *
         * @return This object for method chaining
         */
        default Builder credentialsProvider(IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
            throw new UnsupportedOperationException();
        }

        /**
         * Create a {@link AxdbFrontendUtilities}
         */
        AxdbFrontendUtilities build();
    }
}
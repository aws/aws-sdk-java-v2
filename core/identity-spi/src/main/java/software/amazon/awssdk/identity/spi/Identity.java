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

package software.amazon.awssdk.identity.spi;

import java.time.Instant;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;

/**
 * Interface to represent <b>who</b> is using the SDK. This is the identity of the caller, used for authentication. See
 * {@link IdentityProvider} for more information.
 *
 * <p>
 * Examples include {@link AwsCredentialsIdentity} and {@link TokenIdentity}.
 */
@SdkPublicApi
@ThreadSafe
public interface Identity {
    /**
     * (Optional) The time after which this identity is no longer valid. When not specified, the identity may
     * still expire at some unknown time in the future.
     */
    default Optional<Instant> expirationTime() {
        return Optional.empty();
    }

    /**
     * (Optional) The name of the identity provider that created this credential identity. This value should only be
     * specified by standard providers. If you're creating your own identity or provider, you should not configure this
     * value.
     */
    default Optional<String> providerName() {
        return Optional.empty();
    }
}

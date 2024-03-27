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
 * Interface to represent <b>who</b> is using the SDK, i.e., the identity of the caller, used for authentication.
 *
 * <p>Examples include {@link AwsCredentialsIdentity} and {@link TokenIdentity}.</p>
 *
 * @see IdentityProvider
 */
@SdkPublicApi
@ThreadSafe
public interface Identity {
    /**
     * The time after which this identity will no longer be valid. If this is empty,
     * an expiration time is not known (but the identity may still expire at some
     * time in the future).
     */
    default Optional<Instant> expirationTime() {
        return Optional.empty();
    }

    /**
     * The source that resolved this identity, normally an identity provider. Note that
     * this string value would be set by an identity provider implementation and is
     * intended to be used for for tracking purposes. Avoid building logic on its value.
     */
    default Optional<String> provider() {
        return Optional.empty();
    }
}

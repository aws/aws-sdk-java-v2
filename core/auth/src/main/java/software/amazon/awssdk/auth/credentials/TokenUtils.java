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

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.auth.token.credentials.SdkToken;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@SdkProtectedApi
public final class TokenUtils {

    private TokenUtils() {
    }

    /**
     * Converts an {@link TokenIdentity} to {@link SdkToken}.
     *
     * <p>Usage of the new TokenIdentity type is preferred over SdkToken. But some places may need to still
     * convert to the older SdkToken type to work with existing code.</p>
     *
     * <p>The conversion is only aware of {@link TokenIdentity} interface. If the  input is another sub-type that has other
     * properties, they are not carried over.
     * </p>
     *
     * @param tokenIdentity The {@link TokenIdentity} to convert
     * @return The corresponding {@link SdkToken}
     */

    public static SdkToken toSdkToken(TokenIdentity tokenIdentity) {
        if (tokenIdentity == null) {
            return null;
        }
        if (tokenIdentity instanceof SdkToken) {
            return (SdkToken) tokenIdentity;
        }
        return () -> tokenIdentity.token();
    }

    /**
     * Converts an {@link IdentityProvider<? extends TokenIdentity>} to {@link SdkTokenProvider} based on
     * {@link #toSdkToken(TokenIdentity)}.
     *
     * <p>Usage of the new IdentityProvider type is preferred over SdkTokenProvider. But some places may need to still
     * convert to the older SdkTokenProvider type to work with existing code.
     * </p>
     *
     * @param identityProvider The {@link IdentityProvider<? extends TokenIdentity>} to convert
     * @return The corresponding {@link SdkTokenProvider}
     */
    public static SdkTokenProvider toSdkTokenProvider(
            IdentityProvider<? extends TokenIdentity> identityProvider) {
        if (identityProvider == null) {
            return null;
        }
        if (identityProvider instanceof SdkTokenProvider) {
            return (SdkTokenProvider) identityProvider;
        }

        return () -> toSdkToken(CompletableFutureUtils.joinLikeSync(identityProvider.resolveIdentity()));
    }
}

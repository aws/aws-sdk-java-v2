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

package software.amazon.awssdk.auth.token.credentials.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.token.credentials.SdkToken;
import software.amazon.awssdk.identity.spi.TokenIdentity;

// TODO(sra-identity-and-auth): Delete this once more SRA work is done where signers are using the new Identity types and this
//  conversion is not necessary.
@SdkInternalApi
public class TokenUtils {

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
}

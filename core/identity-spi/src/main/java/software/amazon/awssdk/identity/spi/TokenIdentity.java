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

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Provides token which is used to securely authorize requests to services that use token based auth, e.g., OAuth.
 *
 * <p>For more details on OAuth tokens, see:
 * <a href="https://oauth.net/2/access-tokens">
 * https://oauth.net/2/access-tokens</a></p>
 */
@SdkPublicApi
@ThreadSafe
public interface TokenIdentity extends Identity {

    /**
     * Retrieves string field representing the literal token string.
     */
    String token();

    /**
     * Constructs a new token object, which can be used to authorize requests to services that use token based auth
     *
     * @param token The token used to authorize requests.
     */
    static TokenIdentity create(String token) {
        Validate.paramNotNull(token, "token");

        return new TokenIdentity() {
            @Override
            public String token() {
                return token;
            }

            @Override
            public String toString() {
                return ToString.builder("TokenIdentity")
                               .add("token", token)
                               .build();
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                TokenIdentity that = (TokenIdentity) o;
                return Objects.equals(token, that.token());
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(token());
            }
        };
    }
}

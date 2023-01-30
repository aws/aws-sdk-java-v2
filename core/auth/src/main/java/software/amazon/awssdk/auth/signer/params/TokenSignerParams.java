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

package software.amazon.awssdk.auth.signer.params;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.token.credentials.SdkToken;
import software.amazon.awssdk.utils.Validate;

/**
 * Parameters that are used during signing.
 *
 * Required parameters vary based on signer implementations. Signer implementations might only use a
 * subset of params in this class.
 */
@SdkPublicApi
public class TokenSignerParams {
    private final SdkToken token;

    TokenSignerParams(BuilderImpl<?> builder) {
        this.token = Validate.paramNotNull(builder.token, "Signing token");
    }

    public static Builder builder() {
        return new BuilderImpl<>();
    }

    public SdkToken token() {
        return token;
    }

    public interface Builder<B extends Builder> {

        /**
         * Set this value to provide a token for signing
         *
         * This is required for token signing.
         *
         * @param token A token implementing {@link SdkToken}
         */
        B token(SdkToken token);

        TokenSignerParams build();
    }

    protected static class BuilderImpl<B extends Builder> implements Builder<B> {
        private SdkToken token;

        protected BuilderImpl() {
        }

        @Override
        public B token(SdkToken token) {
            this.token = token;
            return (B) this;
        }

        public void setToken(SdkToken token) {
            token(token);
        }

        @Override
        public TokenSignerParams build() {
            return new TokenSignerParams(this);
        }
    }
}

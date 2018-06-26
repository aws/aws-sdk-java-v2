/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.time.Instant;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.signer.SignerConstant;

@SdkPublicApi
public final class Aws4PresignerParams extends Aws4SignerParams {

    private final Instant expirationTime;

    private Aws4PresignerParams(Builder builder) {
        super(builder);
        this.expirationTime = builder.expirationTime;
    }

    public Optional<Instant> expirationTime() {
        return Optional.ofNullable(expirationTime);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends Aws4SignerParams.Builder<Builder> {
        private Instant expirationTime;

        /**
         * Sets an expiration time for the presigned url. If this value is not specified,
         * {@link SignerConstant#PRESIGN_URL_MAX_EXPIRATION_SECONDS} is used.
         *
         * @param expirationTime Expiration time for the presigned url expressed in {@link Instant}.
         */
        public Builder expirationTime(Instant expirationTime) {
            this.expirationTime = expirationTime;
            return this;
        }

        public Aws4PresignerParams build() {
            return new Aws4PresignerParams(this);
        }
    }
}
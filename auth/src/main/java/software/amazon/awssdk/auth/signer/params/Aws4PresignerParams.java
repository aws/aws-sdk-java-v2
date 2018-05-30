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

import java.util.Date;
import software.amazon.awssdk.auth.signer.SignerConstants;

public final class Aws4PresignerParams extends Aws4SignerParams {

    private final Date expirationDate;

    private Aws4PresignerParams(Builder builder) {
        super(builder);
        this.expirationDate = builder.expirationDate;
    }

    public Date expirationDate() {
        return expirationDate;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends Aws4SignerParams.Builder<Builder> {
        private Date expirationDate;

        /**
         * Sets an expiration date for the presigned url. If this value is not specified,
         * {@link SignerConstants#PRESIGN_URL_MAX_EXPIRATION_SECONDS} is used.
         *
         * @param expirationDate Expiration date for the presigned url.
         */
        public Builder expirationDate(Date expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        public Aws4PresignerParams build() {
            return new Aws4PresignerParams(this);
        }
    }
}
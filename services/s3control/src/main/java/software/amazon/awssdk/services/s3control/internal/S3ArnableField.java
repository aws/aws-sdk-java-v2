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

package software.amazon.awssdk.services.s3control.internal;


import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.arns.Arn;

/**
 * Indicating a field that can be an ARN
 */
@SdkInternalApi
public final class S3ArnableField {
    private final Arn arn;

    private S3ArnableField(Builder builder) {
        this.arn = builder.arn;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return the ARN
     */
    public Arn arn() {
        return arn;
    }

    public static final class Builder {
        private Arn arn;

        private Builder() {
        }

        /**
         * Sets the arn
         *
         * @param arn The new arn value.
         * @return This object for method chaining.
         */
        public Builder arn(Arn arn) {
            this.arn = arn;
            return this;
        }


        public S3ArnableField build() {
            return new S3ArnableField(this);
        }
    }

}

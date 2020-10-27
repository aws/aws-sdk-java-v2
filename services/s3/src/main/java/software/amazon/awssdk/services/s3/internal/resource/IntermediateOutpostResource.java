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

package software.amazon.awssdk.services.s3.internal.resource;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.arns.ArnResource;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * The intermediate outpost resource
 */
@SdkInternalApi
public final class IntermediateOutpostResource {
    private final String outpostId;
    private final ArnResource outpostSubresource;

    private IntermediateOutpostResource(Builder builder) {
        this.outpostId = Validate.paramNotBlank(builder.outpostId, "outpostId");
        this.outpostSubresource = Validate.notNull(builder.outpostSubresource, "outpostSubresource");
        Validate.isTrue(StringUtils.isNotBlank(builder.outpostSubresource.resource()), "Invalid format for S3 Outpost ARN");
        Validate.isTrue(builder.outpostSubresource.resourceType().isPresent(), "Invalid format for S3 Outpost ARN");
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return the ID of the outpost
     */
    public String outpostId() {
        return outpostId;
    }

    /**
     * @return the outpost subresource
     */
    public ArnResource outpostSubresource() {
        return outpostSubresource;
    }

    public static final class Builder {
        private String outpostId;
        private ArnResource outpostSubresource;

        private Builder() {
        }

        /**
         * Sets the outpostSubResource
         *
         * @param outpostSubResource The new outpostSubResource value.
         * @return This object for method chaining.
         */
        public Builder outpostSubresource(ArnResource outpostSubResource) {
            this.outpostSubresource = outpostSubResource;
            return this;
        }

        /**
         * Sets the outpostId
         *
         * @param outpostId The new outpostId value.
         * @return This object for method chaining.
         */
        public Builder outpostId(String outpostId) {
            this.outpostId = outpostId;
            return this;
        }

        public IntermediateOutpostResource build() {
            return new IntermediateOutpostResource(this);
        }
    }
}

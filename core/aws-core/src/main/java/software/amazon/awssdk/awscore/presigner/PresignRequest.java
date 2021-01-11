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

package software.amazon.awssdk.awscore.presigner;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;

/**
 * The base class for all presign requests.
 */
@SdkPublicApi
public abstract class PresignRequest {
    private final Duration signatureDuration;

    protected PresignRequest(DefaultBuilder<?> builder) {
        this.signatureDuration = Validate.paramNotNull(builder.signatureDuration, "signatureDuration");
    }

    /**
     * Retrieves the duration for which this presigned request should be valid. After this time has
     * expired, attempting to use the presigned request will fail. 
     */
    public Duration signatureDuration() {
        return this.signatureDuration;
    }

    /**
     * The base interface for all presign request builders.
     */
    @SdkPublicApi
    public interface Builder {
        /**
         * Specifies the duration for which this presigned request should be valid. After this time has
         * expired, attempting to use the presigned request will fail. 
         */
        Builder signatureDuration(Duration signatureDuration);

        /**
         * Build the presigned request, based on the configuration on this builder.
         */
        PresignRequest build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PresignRequest that = (PresignRequest) o;

        return signatureDuration.equals(that.signatureDuration);
    }

    @Override
    public int hashCode() {
        return signatureDuration.hashCode();
    }

    @SdkProtectedApi
    protected abstract static class DefaultBuilder<B extends DefaultBuilder<B>> implements Builder {
        private Duration signatureDuration;

        protected DefaultBuilder() {
        }

        protected DefaultBuilder(PresignRequest request) {
            this.signatureDuration = request.signatureDuration;
        }

        @Override
        public B signatureDuration(Duration signatureDuration) {
            this.signatureDuration = signatureDuration;
            return thisBuilder();
        }

        @SuppressWarnings("unchecked")
        private B thisBuilder() {
            return (B) this;
        }
    }
}

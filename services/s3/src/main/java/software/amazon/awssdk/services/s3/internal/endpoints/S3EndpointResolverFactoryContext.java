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

package software.amazon.awssdk.services.s3.internal.endpoints;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.model.S3Request;

@SdkInternalApi
public final class S3EndpointResolverFactoryContext {
    private final String bucketName;
    private final S3Request originalRequest;

    private S3EndpointResolverFactoryContext(DefaultBuilder builder) {
        this.bucketName = builder.bucketName;
        this.originalRequest = builder.originalRequest;
    }

    public Optional<String> bucketName() {
        return Optional.ofNullable(bucketName);
    }

    public S3Request originalRequest() {
        return originalRequest;
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    public interface Builder {
        Builder bucketName(String bucketName);

        Builder originalRequest(S3Request originalRequest);

        S3EndpointResolverFactoryContext build();
    }

    private static final class DefaultBuilder implements Builder {
        private String bucketName;
        private S3Request originalRequest;

        @Override
        public Builder bucketName(String bucketName) {
            this.bucketName = bucketName;
            return this;
        }

        @Override
        public Builder originalRequest(S3Request originalRequest) {
            this.originalRequest = originalRequest;
            return this;
        }

        @Override
        public S3EndpointResolverFactoryContext build() {
            return new S3EndpointResolverFactoryContext(this);
        }
    }
}

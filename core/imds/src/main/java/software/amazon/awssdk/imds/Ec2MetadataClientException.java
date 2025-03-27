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

package software.amazon.awssdk.imds;


import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * Extends {@link SdkClientException} for EC2 Instance Metadata Service (IMDS) non-successful
 * responses (4XX codes). Provides detailed error information through:
 * <p>
 * - HTTP status code via {@link #statusCode()} for specific error handling
 * - Raw response content via {@link #rawResponse()} containing the error response body
 * - HTTP headers via {@link #sdkHttpResponse()} providing additional error context from the response
 */

@SdkPublicApi
public final class Ec2MetadataClientException extends SdkClientException {

    private final int statusCode;
    private final SdkBytes rawResponse;
    private final SdkHttpResponse sdkHttpResponse;

    private Ec2MetadataClientException(BuilderImpl builder) {
        super(builder);
        this.statusCode = builder.statusCode;
        this.rawResponse = builder.rawResponse;
        this.sdkHttpResponse = builder.sdkHttpResponse;
    }

    /**
     * @return The HTTP status code returned by the IMDS service.
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Returns the response payload as bytes.
     */
    public SdkBytes rawResponse() {
        return rawResponse;
    }

    /**
     * Returns a map of HTTP headers associated with the error response.
     */
    public SdkHttpResponse sdkHttpResponse() {
        return sdkHttpResponse;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends SdkClientException.Builder {
        Builder statusCode(int statusCode);

        Builder rawResponse(SdkBytes rawResponse);

        Builder sdkHttpResponse(SdkHttpResponse sdkHttpResponse);

        @Override
        Ec2MetadataClientException build();
    }

    private static final class BuilderImpl extends SdkClientException.BuilderImpl implements Builder {
        private int statusCode;
        private SdkBytes rawResponse;
        private SdkHttpResponse sdkHttpResponse;

        @Override
        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        @Override
        public Builder rawResponse(SdkBytes rawResponse) {
            this.rawResponse = rawResponse;
            return this;
        }

        @Override
        public Builder sdkHttpResponse(SdkHttpResponse sdkHttpResponse) {
            this.sdkHttpResponse = sdkHttpResponse;
            return this;
        }

        @Override
        public Ec2MetadataClientException build() {
            return new Ec2MetadataClientException(this);
        }
    }
}

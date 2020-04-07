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

package software.amazon.awssdk.awscore.exception;

import java.io.Serializable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.ToString;

@SdkPublicApi
public class AwsErrorDetails implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String errorMessage;

    private final String errorCode;

    private final String serviceName;

    private final SdkHttpResponse sdkHttpResponse;

    private final SdkBytes rawResponse;

    protected AwsErrorDetails(Builder b) {
        this.errorMessage = b.errorMessage();
        this.errorCode = b.errorCode();
        this.serviceName = b.serviceName();
        this.sdkHttpResponse = b.sdkHttpResponse();
        this.rawResponse = b.rawResponse();
    }

    /**
     * Returns the name of the service as defined in the static constant
     * SERVICE_NAME variable of each service's interface.
     *
     * @return The name of the service that sent this error response.
     */
    public String serviceName() {
        return serviceName;
    }

    /**
     * @return the human-readable error message provided by the service.
     */
    public String errorMessage() {
        return errorMessage;
    }

    /**
     * Returns the error code associated with the response.
     */
    public String errorCode() {
        return errorCode;
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

    /**
     * @return {@link AwsErrorDetails.Builder} instance to construct a new {@link AwsErrorDetails}.
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * Create a {@link AwsErrorDetails.Builder} initialized with the properties of this {@code AwsErrorDetails}.
     *
     * @return A new builder initialized with this config's properties.
     */
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return BuilderImpl.class;
    }

    @Override
    public String toString() {
        return ToString.builder("AwsErrorDetails")
                       .add("errorMessage", errorMessage)
                       .add("errorCode", errorCode)
                       .add("serviceName", serviceName)
                       .build();
    }

    public interface Builder {
        /**
         * Specifies the error message returned by the service.
         *
         * @param errorMessage The error message returned by the service.
         * @return This object for method chaining.
         */
        Builder errorMessage(String errorMessage);

        /**
         * The error message specified by the service.
         *
         * @return The error message specified by the service.
         */
        String errorMessage();

        /**
         * Specifies the error code returned by the service.
         *
         * @param errorCode The error code returned by the service.
         * @return This object for method chaining.
         */
        Builder errorCode(String errorCode);

        /**
         * The error code specified by the service.
         *
         * @return The error code specified by the service.
         */
        String errorCode();

        /**
         * Specifies the name of the service that returned this error.
         *
         * @param serviceName The name of the service.
         * @return This object for method chaining.
         */
        Builder serviceName(String serviceName);

        /**
         * Returns the name of the service as defined in the static constant
         * SERVICE_NAME variable of each service's interface.
         *
         * @return The name of the service that returned this error.
         */
        String serviceName();

        /**
         * Specifies the {@link SdkHttpResponse} returned on the error response from the service.
         *
         * @param sdkHttpResponse The HTTP response from the service.
         * @return This object for method chaining.
         */
        Builder sdkHttpResponse(SdkHttpResponse sdkHttpResponse);

        /**
         * The HTTP response returned from the service.
         *
         * @return {@link SdkHttpResponse}.
         */
        SdkHttpResponse sdkHttpResponse();

        /**
         * Specifies raw http response from the service.
         *
         * @param rawResponse raw byte response from the service.
         * @return The object for method chaining.
         */
        Builder rawResponse(SdkBytes rawResponse);

        /**
         * The raw response from the service.
         *
         * @return The raw response from the service in a byte array.
         */
        SdkBytes rawResponse();

        /**
         * Creates a new {@link AwsErrorDetails} with the properties set on this builder.
         *
         * @return The new {@link AwsErrorDetails}.
         */
        AwsErrorDetails build();
    }

    protected static final class BuilderImpl implements Builder {

        private String errorMessage;
        private String errorCode;
        private String serviceName;
        private SdkHttpResponse sdkHttpResponse;
        private SdkBytes rawResponse;

        private BuilderImpl() {
        }

        private BuilderImpl(AwsErrorDetails awsErrorDetails) {
            this.errorMessage = awsErrorDetails.errorMessage();
            this.errorCode = awsErrorDetails.errorCode();
            this.serviceName = awsErrorDetails.serviceName();
            this.sdkHttpResponse = awsErrorDetails.sdkHttpResponse();
            this.rawResponse = awsErrorDetails.rawResponse();
        }

        @Override
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        @Override
        public String errorMessage() {
            return errorMessage;
        }

        @Override
        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        @Override
        public String errorCode() {
            return errorCode;
        }

        @Override
        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        @Override
        public String serviceName() {
            return serviceName;
        }

        @Override
        public Builder sdkHttpResponse(SdkHttpResponse sdkHttpResponse) {
            this.sdkHttpResponse = sdkHttpResponse;
            return this;
        }

        @Override
        public SdkHttpResponse sdkHttpResponse() {
            return sdkHttpResponse;
        }

        @Override
        public Builder rawResponse(SdkBytes rawResponse) {
            this.rawResponse = rawResponse;
            return this;
        }

        @Override
        public SdkBytes rawResponse() {
            return rawResponse;
        }

        @Override
        public AwsErrorDetails build() {
            return new AwsErrorDetails(this);
        }
    }

}

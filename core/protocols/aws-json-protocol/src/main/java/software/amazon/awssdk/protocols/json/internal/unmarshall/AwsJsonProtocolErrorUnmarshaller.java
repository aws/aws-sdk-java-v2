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

package software.amazon.awssdk.protocols.json.internal.unmarshall;

import com.fasterxml.jackson.core.JsonFactory;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.json.ErrorCodeParser;
import software.amazon.awssdk.protocols.json.JsonContent;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Unmarshaller for AWS specific error responses. All errors are unmarshalled into a subtype of
 * {@link AwsServiceException} (more specifically a subtype generated for each AWS service).
 */
@SdkInternalApi
public final class AwsJsonProtocolErrorUnmarshaller implements HttpResponseHandler<AwsServiceException> {

    private final JsonProtocolUnmarshaller jsonProtocolUnmarshaller;
    private final List<ExceptionMetadata> exceptions;
    private final ErrorMessageParser errorMessageParser;
    private final JsonFactory jsonFactory;
    private final Supplier<SdkPojo> defaultExceptionSupplier;
    private final ErrorCodeParser errorCodeParser;

    private AwsJsonProtocolErrorUnmarshaller(Builder builder) {
        this.jsonProtocolUnmarshaller = builder.jsonProtocolUnmarshaller;
        this.errorCodeParser = builder.errorCodeParser;
        this.errorMessageParser = builder.errorMessageParser;
        this.jsonFactory = builder.jsonFactory;
        this.defaultExceptionSupplier = builder.defaultExceptionSupplier;
        this.exceptions = builder.exceptions;
    }

    @Override
    public AwsServiceException handle(SdkHttpFullResponse response, ExecutionAttributes executionAttributes) {
        return unmarshall(response, executionAttributes);
    }

    private AwsServiceException unmarshall(SdkHttpFullResponse response, ExecutionAttributes executionAttributes) {
        JsonContent jsonContent = JsonContent.createJsonContent(response, jsonFactory);
        String errorCode = errorCodeParser.parseErrorCode(response, jsonContent);

        Optional<ExceptionMetadata> modeledExceptionMetadata = exceptions.stream()
                                                                         .filter(e -> e.errorCode().equals(errorCode))
                                                                         .findAny();

        SdkPojo sdkPojo = modeledExceptionMetadata.map(ExceptionMetadata::exceptionBuilderSupplier)
                                                  .orElse(defaultExceptionSupplier)
                                                  .get();

        AwsServiceException.Builder exception = ((AwsServiceException) jsonProtocolUnmarshaller
            .unmarshall(sdkPojo, response, jsonContent.getJsonNode())).toBuilder();
        String errorMessage = errorMessageParser.parseErrorMessage(response, jsonContent.getJsonNode());
        exception.awsErrorDetails(extractAwsErrorDetails(response, executionAttributes, jsonContent,
                                                         errorCode, errorMessage));
        exception.clockSkew(getClockSkew(executionAttributes));
        // Status code and request id are sdk level fields
        exception.message(errorMessage);
        exception.statusCode(statusCode(response, modeledExceptionMetadata));
        exception.requestId(getRequestIdFromHeaders(response.headers()));
        exception.extendedRequestId(getExtendedRequestIdFromHeaders(response.headers()));
        return exception.build();
    }

    private Duration getClockSkew(ExecutionAttributes executionAttributes) {
        Integer timeOffset = executionAttributes.getAttribute(SdkExecutionAttribute.TIME_OFFSET);
        return timeOffset == null ? null : Duration.ofSeconds(timeOffset);
    }

    private int statusCode(SdkHttpFullResponse response, Optional<ExceptionMetadata> modeledExceptionMetadata) {
        if (response.statusCode() != 0) {
            return response.statusCode();
        }

        return modeledExceptionMetadata.filter(m -> m.httpStatusCode() != null)
                                       .map(ExceptionMetadata::httpStatusCode)
                                       .orElse(500);
    }

    /**
     * Build the {@link AwsErrorDetails} from the metadata in the response.
     *
     * @param response HTTP response.
     * @param executionAttributes Execution attributes.
     * @param jsonContent Parsed JSON content.
     * @param errorCode Parsed error code/type.
     * @param errorMessage Parsed error message.
     * @return AwsErrorDetails
     */
    private AwsErrorDetails extractAwsErrorDetails(SdkHttpFullResponse response,
                                                   ExecutionAttributes executionAttributes,
                                                   JsonContent jsonContent,
                                                   String errorCode,
                                                   String errorMessage) {
        AwsErrorDetails.Builder errorDetails =
            AwsErrorDetails.builder()
                           .errorCode(errorCode)
                           .serviceName(executionAttributes.getAttribute(SdkExecutionAttribute.SERVICE_NAME))
                           .sdkHttpResponse(response);

        if (jsonContent.getRawContent() != null) {
            errorDetails.rawResponse(SdkBytes.fromByteArray(jsonContent.getRawContent()));
        }

        errorDetails.errorMessage(errorMessage);
        return errorDetails.build();
    }

    private String getRequestIdFromHeaders(Map<String, List<String>> headers) {
        return SdkHttpUtils.firstMatchingHeaderFromCollection(headers, X_AMZN_REQUEST_ID_HEADERS).orElse(null);
    }

    private String getExtendedRequestIdFromHeaders(Map<String, List<String>> headers) {
        return SdkHttpUtils.firstMatchingHeader(headers, X_AMZ_ID_2_HEADER).orElse(null);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AwsJsonProtocolErrorUnmarshaller}.
     */
    public static final class Builder {

        private JsonProtocolUnmarshaller jsonProtocolUnmarshaller;
        private List<ExceptionMetadata> exceptions;
        private ErrorMessageParser errorMessageParser;
        private JsonFactory jsonFactory;
        private Supplier<SdkPojo> defaultExceptionSupplier;
        private ErrorCodeParser errorCodeParser;

        private Builder() {
        }

        /**
         * Underlying response unmarshaller. Exceptions for the JSON protocol are follow the same unmarshalling logic
         * as success responses but with an additional "error type" that allows for polymorphic deserialization.
         *
         * @return This builder for method chaining.
         */
        public Builder jsonProtocolUnmarshaller(JsonProtocolUnmarshaller jsonProtocolUnmarshaller) {
            this.jsonProtocolUnmarshaller = jsonProtocolUnmarshaller;
            return this;
        }

        /**
         * List of {@link ExceptionMetadata} to represent the modeled exceptions for the service.
         * For AWS services the error type is a string representing the type of the modeled exception.
         *
         * @return This builder for method chaining.
         */
        public Builder exceptions(List<ExceptionMetadata> exceptions) {
            this.exceptions = exceptions;
            return this;
        }

        /**
         * Implementation that can extract an error message from the JSON response. Implementations may look for a
         * specific field in the JSON document or a specific header for example.
         *
         * @return This builder for method chaining.
         */
        public Builder errorMessageParser(ErrorMessageParser errorMessageParser) {
            this.errorMessageParser = errorMessageParser;
            return this;
        }

        /**
         * JSON Factory to create a JSON parser.
         *
         * @return This builder for method chaining.
         */
        public Builder jsonFactory(JsonFactory jsonFactory) {
            this.jsonFactory = jsonFactory;
            return this;
        }

        /**
         * Default exception type if "error code" does not match any known modeled exception. This is the generated
         * base exception for the service (i.e. DynamoDbException).
         *
         * @return This builder for method chaining.
         */
        public Builder defaultExceptionSupplier(Supplier<SdkPojo> defaultExceptionSupplier) {
            this.defaultExceptionSupplier = defaultExceptionSupplier;
            return this;
        }

        /**
         * Implementation of {@link ErrorCodeParser} that can extract an error code or type from the JSON response.
         * Implementations may look for a specific field in the JSON document or a specific header for example.
         *
         * @return This builder for method chaining.
         */
        public Builder errorCodeParser(ErrorCodeParser errorCodeParser) {
            this.errorCodeParser = errorCodeParser;
            return this;
        }

        public AwsJsonProtocolErrorUnmarshaller build() {
            return new AwsJsonProtocolErrorUnmarshaller(this);
        }
    }
}

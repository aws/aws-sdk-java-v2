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

package software.amazon.awssdk.protocols.query.internal.unmarshall;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.query.unmarshall.XmlElement;
import software.amazon.awssdk.protocols.query.unmarshall.XmlErrorUnmarshaller;

/**
 * Unmarshalls an AWS XML exception from parsed XML.
 */
@SdkInternalApi
public final class AwsXmlErrorUnmarshaller {
    private static final String X_AMZN_REQUEST_ID_HEADER = "x-amzn-RequestId";

    private final List<ExceptionMetadata> exceptions;
    private final Supplier<SdkPojo> defaultExceptionSupplier;

    private final XmlErrorUnmarshaller errorUnmarshaller;

    private AwsXmlErrorUnmarshaller(Builder builder) {
        this.exceptions = builder.exceptions;
        this.errorUnmarshaller = builder.errorUnmarshaller;
        this.defaultExceptionSupplier = builder.defaultExceptionSupplier;
    }

    /**
     * @return New Builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Unmarshal an AWS XML exception
     * @param documentRoot The parsed payload document
     * @param errorRoot The specific element of the parsed payload document that contains the error to be marshalled
     *                  or empty if it could not be located.
     * @param documentBytes The raw bytes of the original payload document if they are available
     * @param response The HTTP response object
     * @param executionAttributes {@link ExecutionAttributes} for the current execution
     * @return An {@link AwsServiceException} unmarshalled from the XML.
     */
    public AwsServiceException unmarshall(XmlElement documentRoot,
                                          Optional<XmlElement> errorRoot,
                                          Optional<SdkBytes> documentBytes,
                                          SdkHttpFullResponse response,
                                          ExecutionAttributes executionAttributes) {
        String errorCode = getErrorCode(errorRoot);

        AwsServiceException.Builder builder = errorRoot
            .map(e -> invokeSafely(() -> unmarshallFromErrorCode(response, e, errorCode)))
            .orElseGet(this::defaultException);

        AwsErrorDetails awsErrorDetails =
            AwsErrorDetails.builder()
                           .errorCode(errorCode)
                           .errorMessage(builder.message())
                           .rawResponse(documentBytes.orElse(null))
                           .sdkHttpResponse(response)
                           .serviceName(executionAttributes.getAttribute(AwsExecutionAttribute.SERVICE_NAME))
                           .build();

        builder.requestId(getRequestId(response, documentRoot))
               .statusCode(response.statusCode())
               .clockSkew(getClockSkew(executionAttributes))
               .awsErrorDetails(awsErrorDetails);

        return builder.build();
    }

    private Duration getClockSkew(ExecutionAttributes executionAttributes) {
        Integer timeOffset = executionAttributes.getAttribute(SdkExecutionAttribute.TIME_OFFSET);
        return timeOffset == null ? null : Duration.ofSeconds(timeOffset);
    }

    /**
     * @return Builder for the default service exception. Used when the error code doesn't match
     * any known modeled exception or when we can't determine the error code.
     */
    private AwsServiceException.Builder defaultException() {
        return (AwsServiceException.Builder) defaultExceptionSupplier.get();
    }

    /**
     * Unmarshalls the XML into the appropriate modeled exception based on the error code. If the error code
     * is not present or does not match any known exception we unmarshall into the base service exception.
     *
     * @param errorRoot Root of <Error/> element. Contains any modeled fields of the exception.
     * @param errorCode Error code identifying the modeled exception.
     * @return Unmarshalled exception builder.
     */
    private AwsServiceException.Builder unmarshallFromErrorCode(SdkHttpFullResponse response,
                                                                XmlElement errorRoot,
                                                                String errorCode) {
        SdkPojo sdkPojo = exceptions.stream()
                                    .filter(e -> e.errorCode().equals(errorCode))
                                    .map(ExceptionMetadata::exceptionBuilderSupplier)
                                    .findAny()
                                    .orElse(defaultExceptionSupplier)
                                    .get();

        AwsServiceException.Builder builder =
            ((AwsServiceException) errorUnmarshaller.unmarshall(sdkPojo, errorRoot, response)).toBuilder();
        builder.message(getMessage(errorRoot));
        return builder;
    }

    /**
     * Extracts the error code (used to identify the modeled exception) from the <Error/>
     * element.
     *
     * @param errorRoot Error element root.
     * @return Error code or null if not present.
     */
    private String getErrorCode(Optional<XmlElement> errorRoot) {
        return errorRoot.map(e -> e.getOptionalElementByName("Code")
                                   .map(XmlElement::textContent)
                                   .orElse(null))
                        .orElse(null);
    }

    /**
     * Extracts the error message from the XML document. The message is in the <Error/>
     * element for all services.
     *
     * @param errorRoot Error element root.
     * @return Error message or null if not present.
     */
    private String getMessage(XmlElement errorRoot) {
        return errorRoot.getOptionalElementByName("Message")
                        .map(XmlElement::textContent)
                        .orElse(null);
    }

    /**
     * Extracts the request ID from the XML document. Request ID is a top level element
     * for all protocols, it may be RequestId or RequestID depending on the service.
     *
     * @param document Root XML document.
     * @return Request ID string or null if not present.
     */
    private String getRequestId(SdkHttpFullResponse response, XmlElement document) {
        XmlElement requestId = document.getOptionalElementByName("RequestId")
                                       .orElse(document.getElementByName("RequestID"));
        return requestId != null ?
               requestId.textContent() :
               response.firstMatchingHeader(X_AMZN_REQUEST_ID_HEADER).orElse(null);
    }

    /**
     * Builder for {@link AwsXmlErrorUnmarshaller}.
     */
    public static final class Builder {

        private List<ExceptionMetadata> exceptions;
        private Supplier<SdkPojo> defaultExceptionSupplier;
        private XmlErrorUnmarshaller errorUnmarshaller;

        private Builder() {
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
         * The unmarshaller to use. The unmarshaller only unmarshalls any modeled fields of the exception,
         * additional metadata is extracted by {@link AwsXmlErrorUnmarshaller}.
         *
         * @param errorUnmarshaller Error unmarshaller to use.
         * @return This builder for method chaining.
         */
        public Builder errorUnmarshaller(XmlErrorUnmarshaller errorUnmarshaller) {
            this.errorUnmarshaller = errorUnmarshaller;
            return this;
        }

        /**
         * @return New instance of {@link AwsXmlErrorUnmarshaller}.
         */
        public AwsXmlErrorUnmarshaller build() {
            return new AwsXmlErrorUnmarshaller(this);
        }
    }
}

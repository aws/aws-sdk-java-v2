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

package software.amazon.awssdk.services.sns.messagemanager;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Base exception for all SNS message validation failures.
 * <p>
 * This exception is thrown when SNS message validation fails for any reason, including:
 * <ul>
 *   <li>JSON parsing or format errors</li>
 *   <li>Signature verification failures</li>
 *   <li>Certificate retrieval or validation problems</li>
 *   <li>Missing required fields</li>
 *   <li>Invalid message structure</li>
 * </ul>
 * <p>
 * Specific subclasses provide more detailed error information for different types of validation failures.
 *
 * @see SnsMessageParsingException
 * @see SnsSignatureValidationException
 * @see SnsCertificateException
 */
@SdkPublicApi
public class SnsMessageValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new SnsMessageValidationException with the specified detail message.
     *
     * @param message The detail message explaining the validation failure.
     */
    public SnsMessageValidationException(String message) {
        super(message);
    }

    /**
     * Constructs a new SnsMessageValidationException with the specified detail message and cause.
     *
     * @param message The detail message explaining the validation failure.
     * @param cause   The underlying cause of the validation failure.
     */
    public SnsMessageValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new builder for constructing SnsMessageValidationException instances.
     *
     * @return A new builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating SnsMessageValidationException instances.
     */
    public static class Builder {
        protected String message;
        protected Throwable cause;

        protected Builder() {
        }

        /**
         * Sets the detail message for the exception.
         *
         * @param message The detail message.
         * @return This builder for method chaining.
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Sets the underlying cause of the exception.
         *
         * @param cause The underlying cause.
         * @return This builder for method chaining.
         */
        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        /**
         * Builds a new SnsMessageValidationException instance.
         *
         * @return A new exception with the configured properties.
         */
        public SnsMessageValidationException build() {
            if (cause != null) {
                return new SnsMessageValidationException(message, cause);
            }
            return new SnsMessageValidationException(message);
        }
    }
}
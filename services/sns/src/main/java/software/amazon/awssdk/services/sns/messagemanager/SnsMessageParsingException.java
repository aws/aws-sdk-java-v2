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
 * Exception thrown when SNS message parsing fails due to JSON format errors or invalid message structure.
 * <p>
 * This exception is thrown in the following scenarios:
 * <ul>
 *   <li>Invalid JSON format in the message payload</li>
 *   <li>Missing required fields (Type, MessageId, TopicArn, etc.)</li>
 *   <li>Unexpected fields or message structure</li>
 *   <li>Invalid field values or formats</li>
 *   <li>Unsupported message types</li>
 * </ul>
 * <p>
 * The exception message provides specific details about what parsing error occurred,
 * helping developers identify and fix message format issues.
 */
@SdkPublicApi
public class SnsMessageParsingException extends SnsMessageValidationException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new SnsMessageParsingException with the specified detail message.
     *
     * @param message The detail message explaining the parsing failure.
     */
    public SnsMessageParsingException(String message) {
        super(message);
    }

    /**
     * Constructs a new SnsMessageParsingException with the specified detail message and cause.
     *
     * @param message The detail message explaining the parsing failure.
     * @param cause   The underlying cause of the parsing failure (e.g., JSON parsing exception).
     */
    public SnsMessageParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new builder for constructing SnsMessageParsingException instances.
     *
     * @return A new builder instance.
     */
    public static SnsMessageValidationException.Builder builder() {
        return new SnsMessageValidationException.Builder() {
            @Override
            public SnsMessageValidationException build() {
                if (cause != null) {
                    return new SnsMessageParsingException(message, cause);
                }
                return new SnsMessageParsingException(message);
            }
        };
    }
}
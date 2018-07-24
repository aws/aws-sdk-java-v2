/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.awscore.eventstream;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.runtime.transform.JsonUnmarshallerContext;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;

@SdkProtectedApi
public class EventStreamExceptionJsonUnmarshaller<T extends AwsServiceException>
    implements Unmarshaller<T, JsonUnmarshallerContext> {

    private static final Logger log = LoggerFactory.getLogger(EventStreamExceptionJsonUnmarshaller.class);

    private final Map<String, Unmarshaller<? extends T, JsonUnmarshallerContext>> unmarshallers;
    private final Unmarshaller<? extends T, JsonUnmarshallerContext> defaultUnmarshaller;

    private EventStreamExceptionJsonUnmarshaller(Builder<T> builder) {
        this.unmarshallers = new HashMap<>(builder.unmarshallers);
        this.defaultUnmarshaller = builder.defaultUnmarshaller;
    }

    public static <T extends AwsServiceException> Builder<T> builder() {
        return new Builder<>();
    }

    public static AwsServiceException populateDefaultException(
        Supplier<? extends AwsServiceException.Builder> exceptionBuilderSupplier, JsonUnmarshallerContext context) {
        String errorMessage;
        String errorCode;

        String messageType = context.getHeader(":message-type");
        if ("error".equals(messageType)) {
            errorMessage = context.getHeader(":error-message");
            errorCode = context.getHeader(":error-code");
        } else if ("exception".equals(messageType)) {
            errorMessage = extractErrorMessageFromPayload(context);
            errorCode = context.getHeader(":exception-type");
        } else {
            throw new IllegalStateException("Unexpected exception message type: " + messageType);
        }

        SdkBytes rawResponse = SdkBytes.fromInputStream(context.getHttpResponse().getContent());
        return exceptionBuilderSupplier.get()
                                       .awsErrorDetails(AwsErrorDetails.builder()
                                                                       .errorMessage(errorMessage)
                                                                       .errorCode(errorCode)
                                                                       .rawResponse(rawResponse)
                                                                       .build())
                                       .build();
    }

    @Override
    public T unmarshall(JsonUnmarshallerContext in) throws Exception {
        String exceptionType = Optional.ofNullable(in.getHeader(":exception-type")).orElse("");
        return unmarshallers.getOrDefault(exceptionType, defaultUnmarshaller).unmarshall(in);
    }

    private static String extractErrorMessageFromPayload(JsonUnmarshallerContext context) {
        try {
            do {
                if (context.testExpression("message", 1)) {
                    context.nextToken();
                    return context.getUnmarshaller(String.class).unmarshall(context);
                }
            } while (context.nextToken() != null);
        } catch (IOException e) {
            log.info("Could not parse error message from content");
        } catch (Exception e) {
            // Find bugs doesn't like one catch clause for some reason
            log.info("Could not parse error message from content");
        }
        return null;
    }

    public static final class Builder<T extends AwsServiceException> {
        private final Map<String, Unmarshaller<? extends T, JsonUnmarshallerContext>> unmarshallers =
            new HashMap<>();

        private Unmarshaller<? extends T, JsonUnmarshallerContext> defaultUnmarshaller;

        private Builder() {
        }

        /**
         * Registers a new {@link Unmarshaller} with the given type.
         *
         * @param type Value of ':exception-type' header this unmarshaller handles.
         * @param unmarshaller Unmarshaller of a event subtype.
         * @return This object for method chaining.
         */
        public Builder<T> addUnmarshaller(String type,
                                          Unmarshaller<? extends T, JsonUnmarshallerContext> unmarshaller) {
            unmarshallers.put(type, unmarshaller);
            return this;
        }

        /**
         * Registers the default unmarshaller. Used when the value in the ':exception-type' header does not match
         * a registered unmarshaller (i.e. this is a new event that this version of the SDK doesn't know about).
         *
         * @param defaultUnmarshaller Default unmarshaller to use when exception-type doesn't match a registered unmarshaller.
         * @return This object for method chaining.
         */
        public Builder<T> defaultUnmarshaller(Unmarshaller<? extends T, JsonUnmarshallerContext>
                                                  defaultUnmarshaller) {
            this.defaultUnmarshaller = defaultUnmarshaller;
            return this;
        }

        public EventStreamExceptionJsonUnmarshaller<T> build() {
            return new EventStreamExceptionJsonUnmarshaller<>(this);
        }
    }
}

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

package software.amazon.awssdk.core.eventstream;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.runtime.transform.JsonUnmarshallerContext;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;
import software.amazon.awssdk.utils.IoUtils;

@SdkProtectedApi
public class EventStreamExceptionJsonUnmarshaller<T extends SdkServiceException>
        implements Unmarshaller<T, JsonUnmarshallerContext> {

    private final Map<String, Unmarshaller<? extends T, JsonUnmarshallerContext>> unmarshallers;
    private final Unmarshaller<? extends T, JsonUnmarshallerContext> defaultUnmarshaller;

    private EventStreamExceptionJsonUnmarshaller(Builder<T> builder) {
        this.unmarshallers = new HashMap<>(builder.unmarshallers);
        this.defaultUnmarshaller = builder.defaultUnmarshaller;
    }

    public static <T extends SdkServiceException> Builder<T> builder() {
        return new Builder<>();
    }

    public static SdkServiceException populateDefaultException(
            Function<String, ? extends SdkServiceException> exceptionConstructor, JsonUnmarshallerContext context) {
        String errorMessage;
        String errorCode;

        String messageType = context.getHeader(":message-type");
        if ("error".equals(messageType)) {
            errorMessage = context.getHeader(":error-message");
            errorCode = context.getHeader(":error-code");
        } else if ("exception".equals(messageType)) {
            errorMessage = null;
            errorCode = context.getHeader(":exception-type");
        } else {
            throw new IllegalStateException("Unexpected exception message type: " + messageType);
        }

        SdkServiceException exception = exceptionConstructor.apply("Service returned error code " + errorCode +
                                                                   (errorMessage == null ? "" : " (" + errorMessage + ")"));
        exception.errorMessage(errorMessage);
        exception.errorCode(errorCode);

        // Do not populate HTTP headers, status code, etc. because the HTTP response here isn't the real HTTP response.
        // Fix this when we fix EventStreamAsyncResponseTransformer. The content is real, so it's safe.
        exception.rawResponse(invokeSafely(() -> IoUtils.toByteArray(context.getHttpResponse().getContent())));

        return exception;
    }

    @Override
    public T unmarshall(JsonUnmarshallerContext in) throws Exception {
        String exceptionType = Optional.ofNullable(in.getHeader(":exception-type")).orElse("");
        return unmarshallers.getOrDefault(exceptionType, defaultUnmarshaller).unmarshall(in);
    }

    public static final class Builder<T extends SdkServiceException> {
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

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

package software.amazon.awssdk.awscore.eventstream;

import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Composite {@link Marshaller} that dispatches the given event to the
 * correct marshaller based on the event class type.
 *
 * @param <BaseEventT> Base type for all events.
 */
@SdkProtectedApi
public final class EventStreamTaggedUnionJsonMarshaller<BaseEventT> implements Marshaller<BaseEventT> {

    private final Map<Class<? extends BaseEventT>,
        Marshaller<BaseEventT>> marshallers;
    private final Marshaller<BaseEventT> defaultMarshaller;

    private EventStreamTaggedUnionJsonMarshaller(Builder<BaseEventT> builder) {
        this.marshallers = new HashMap<>(builder.marshallers);
        this.defaultMarshaller = builder.defaultMarshaller;
    }

    @Override
    public SdkHttpFullRequest marshall(BaseEventT eventT) {
        return marshallers.getOrDefault(eventT.getClass(), defaultMarshaller).marshall(eventT);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder<BaseEventT> {
        private final Map<Class<? extends BaseEventT>, Marshaller<BaseEventT>> marshallers = new HashMap<>();
        private Marshaller<BaseEventT> defaultMarshaller;

        private Builder() {
        }

        /**
         * Registers a new {@link Marshaller} with given event class type.
         *
         * @param eventClass Class type of the event
         * @param marshaller Marshaller for the event
         * @return This object for method chaining
         */
        public Builder putMarshaller(Class<? extends BaseEventT> eventClass,
                                     Marshaller<BaseEventT> marshaller) {
            marshallers.put(eventClass, marshaller);
            return this;
        }

        public EventStreamTaggedUnionJsonMarshaller<BaseEventT> build() {
            defaultMarshaller =  e -> {
                String errorMsg = "Event type should be one of the following types: " +
                                  marshallers.keySet().stream().map(Class::getSimpleName).collect(toList());
                throw new IllegalArgumentException(errorMsg);
            };

            return new EventStreamTaggedUnionJsonMarshaller<>(this);
        }
    }
}

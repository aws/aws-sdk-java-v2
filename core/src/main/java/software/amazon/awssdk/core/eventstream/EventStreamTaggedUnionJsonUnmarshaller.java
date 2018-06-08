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

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.core.runtime.transform.JsonUnmarshallerContext;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;

/**
 * Composite {@link Unmarshaller} that dispatches the {@link JsonUnmarshallerContext} to the
 * correct unmarshaller based on the ':event-type' header.
 *
 * @param <BaseEventT> Base type for all events.
 */
public final class EventStreamTaggedUnionJsonUnmarshaller<BaseEventT>
    implements Unmarshaller<BaseEventT, JsonUnmarshallerContext> {

    private final Map<String, Unmarshaller<? extends BaseEventT, JsonUnmarshallerContext>> unmarshallers;
    private final Unmarshaller<? extends BaseEventT, JsonUnmarshallerContext> defaultUnmarshaller;

    private EventStreamTaggedUnionJsonUnmarshaller(Builder<BaseEventT> builder) {
        this.unmarshallers = new HashMap<>(builder.unmarshallers);
        this.defaultUnmarshaller = builder.defaultUnmarshaller;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public BaseEventT unmarshall(JsonUnmarshallerContext in) throws Exception {
        return unmarshallers.getOrDefault(in.getHeader(":event-type"), defaultUnmarshaller).unmarshall(in);
    }

    public static final class Builder<BaseEventT> {

        private final Map<String, Unmarshaller<? extends BaseEventT, JsonUnmarshallerContext>> unmarshallers = new HashMap<>();
        private Unmarshaller<? extends BaseEventT, JsonUnmarshallerContext> defaultUnmarshaller;

        private Builder() {
        }

        /**
         * Registers a new {@link Unmarshaller} with the given type.
         *
         * @param type Value of ':event-type' header this unmarshaller handles.
         * @param unmarshaller Unmarshaller of a event subtype.
         * @return This object for method chaining.
         */
        public Builder addUnmarshaller(String type,
                                       Unmarshaller<? extends BaseEventT, JsonUnmarshallerContext> unmarshaller) {
            unmarshallers.put(type, unmarshaller);
            return this;
        }

        /**
         * Registers the default unmarshaller. Used when the value in the ':event-type' header does not match
         * a registered unmarshaller (i.e. this is a new event that this version of the SDK doesn't know about).
         *
         * @param defaultUnmarshaller Default unmarshaller to use when event-type doesn't match a registered unmarshaller.
         * @return This object for method chaining.
         */
        public Builder defaultUnmarshaller(Unmarshaller<? extends BaseEventT, JsonUnmarshallerContext> defaultUnmarshaller) {
            this.defaultUnmarshaller = defaultUnmarshaller;
            return this;
        }

        public EventStreamTaggedUnionJsonUnmarshaller<BaseEventT> build() {
            return new EventStreamTaggedUnionJsonUnmarshaller<>(this);
        }
    }
}

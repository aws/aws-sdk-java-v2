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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.http.SdkHttpFullResponse;

@SdkProtectedApi
public final class EventStreamTaggedUnionPojoSupplier implements Function<SdkHttpFullResponse, SdkPojo> {

    private final Map<String, Supplier<SdkPojo>> pojoSuppliers;
    private final Supplier<SdkPojo> defaultPojoSupplier;

    private EventStreamTaggedUnionPojoSupplier(Builder builder) {
        this.pojoSuppliers = new HashMap<>(builder.pojoSuppliers);
        this.defaultPojoSupplier = builder.defaultPojoSupplier;
    }

    @Override
    public SdkPojo apply(SdkHttpFullResponse sdkHttpFullResponse) {
        String eventType = sdkHttpFullResponse.firstMatchingHeader(":event-type").orElse(null);
        return pojoSuppliers.getOrDefault(eventType, defaultPojoSupplier).get();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final Map<String, Supplier<SdkPojo>> pojoSuppliers = new HashMap<>();
        private Supplier<SdkPojo> defaultPojoSupplier;

        private Builder() {
        }

        /**
         * Registers a new {@link Supplier} of an {@link SdkPojo} associated with a given event type.
         *
         * @param type Value of ':event-type' header this unmarshaller handles.
         * @param pojoSupplier Supplier of {@link SdkPojo}.
         * @return This object for method chaining.
         */
        public Builder putSdkPojoSupplier(String type,
                                          Supplier<SdkPojo> pojoSupplier) {
            pojoSuppliers.put(type, pojoSupplier);
            return this;
        }

        /**
         * Registers the default {@link SdkPojo} supplier. Used when the value in the ':event-type' header does not match
         * a registered event type (i.e. this is a new event that this version of the SDK doesn't know about).
         *
         * @param defaultPojoSupplier Default POJO supplier to use when event-type doesn't match a registered event type.
         * @return This object for method chaining.
         */
        public Builder defaultSdkPojoSupplier(Supplier<SdkPojo> defaultPojoSupplier) {
            this.defaultPojoSupplier = defaultPojoSupplier;
            return this;
        }

        public EventStreamTaggedUnionPojoSupplier build() {
            return new EventStreamTaggedUnionPojoSupplier(this);
        }
    }
}

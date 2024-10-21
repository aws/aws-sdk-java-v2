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
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.http.SdkHttpFullResponse;

@SdkProtectedApi
public final class EventStreamTaggedUnionExceptionSupplier implements Function<SdkHttpFullResponse, Optional<String>> {

    private final Map<String, String> exceptionSupplier;
    private final Supplier<SdkPojo> defaultPojoSupplier;

    private EventStreamTaggedUnionExceptionSupplier(Builder builder) {
        this.exceptionSupplier = new HashMap<>(builder.exceptionSuppliers);
        this.defaultPojoSupplier = builder.defaultExceptionSupplier;
    }

    @Override
    public Optional<String> apply(SdkHttpFullResponse sdkHttpFullResponse) {
        String exceptionType = sdkHttpFullResponse.firstMatchingHeader(":exception-type").orElse(null);
        return Optional.of(exceptionSupplier.get(exceptionType));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final Map<String, String> exceptionSuppliers = new HashMap<>();
        private Supplier<SdkPojo> defaultExceptionSupplier;

        private Builder() {
        }

        /**
         * Registers a new {@link Supplier} of an {@link SdkPojo} associated with a given event type.
         *
         * @param type Value of ':exception-type' header this unmarshaller handles.
         * @param exceptionSupplier Supplier of {@link SdkPojo}.
         * @return This object for method chaining.
         */
        public Builder putExceptionErrorCode(String type,
                                             String exceptionSupplier) {
            exceptionSuppliers.put(type, exceptionSupplier);
            return this;
        }

        /**
         * Registers the default {@link SdkPojo} supplier. Used when the value in the ':event-type' header does not match
         * a registered event type (i.e. this is a new event that this version of the SDK doesn't know about).
         *
         * @param defaultPojoSupplier Default POJO supplier to use when event-type doesn't match a registered event type.
         * @return This object for method chaining.
         */
        public Builder defaultExceptionSupplier(Supplier<SdkPojo> defaultPojoSupplier) {
            this.defaultExceptionSupplier = defaultPojoSupplier;
            return this;
        }

        public EventStreamTaggedUnionExceptionSupplier build() {
            return new EventStreamTaggedUnionExceptionSupplier(this);
        }
    }
}

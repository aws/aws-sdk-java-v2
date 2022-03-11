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

package software.amazon.awssdk.enhanced.dynamodb.internal.mapper;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.LongAttributeConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkInternalApi
public final class AtomicCounter {
    public static final String KEY_PREFIX = "_";

    private static final String DELTA_ATTRIBUTE_NAME = KEY_PREFIX + "Delta";
    private static final String STARTVALUE_ATTRIBUTE_NAME = KEY_PREFIX + "Start";

    private final CounterAttribute delta;
    private final CounterAttribute startValue;

    private AtomicCounter(Builder builder) {
        this.delta = new CounterAttribute(builder.delta, DELTA_ATTRIBUTE_NAME);
        this.startValue = new CounterAttribute(builder.startValue, STARTVALUE_ATTRIBUTE_NAME);
    }

    public static Builder builder() {
        return new Builder();
    }

    public CounterAttribute delta() {
        return delta;
    }

    public CounterAttribute startValue() {
        return startValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AtomicCounter that = (AtomicCounter) o;

        if (!Objects.equals(delta, that.delta)) {
            return false;
        }
        return Objects.equals(startValue, that.startValue);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(delta);
        result = 31 * result + Objects.hashCode(startValue);
        return result;
    }

    public static final class Builder  {
        private Long delta;
        private Long startValue;

        private Builder() {
        }

        public Builder delta(Long delta) {
            this.delta = delta;
            return this;
        }

        public Builder startValue(Long startValue) {
            this.startValue = startValue;
            return this;
        }

        public AtomicCounter build() {
            return new AtomicCounter(this);
        }
    }

    public static class CounterAttribute {
        private static final AttributeConverter<Long> CONVERTER = LongAttributeConverter.create();
        private final Long value;
        private final String name;

        CounterAttribute(Long value, String name) {
            this.value = value;
            this.name = name;
        }

        public Long value() {
            return value;
        }

        public String name() {
            return name;
        }

        public static AttributeValue resolvedValue(Long value) {
            return CONVERTER.transformFrom(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            CounterAttribute that = (CounterAttribute) o;

            if (!Objects.equals(value, that.value)) {
                return false;
            }
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(value);
            result = 31 * result + Objects.hashCode(name);
            return result;
        }
    }
}

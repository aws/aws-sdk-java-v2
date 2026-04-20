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

package software.amazon.awssdk.services.s3.presigner.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.Validate;

/**
 * A typed builder for POST policy conditions. Validation of reserved form field names is performed when assembling the full
 * policy in {@code PostPolicyDocument}, not here.
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class PostPolicyConditions {
    private final List<PolicyCondition> conditions;

    private PostPolicyConditions(Builder builder) {
        this.conditions = Collections.unmodifiableList(new ArrayList<>(builder.conditions));
    }

    /**
     * Create an empty conditions object.
     */
    public static PostPolicyConditions empty() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the configured conditions in insertion order.
     */
    public List<PolicyCondition> conditions() {
        return conditions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PostPolicyConditions that = (PostPolicyConditions) o;
        return conditions.equals(that.conditions);
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (PolicyCondition condition : conditions) {
            result = 31 * result + condition.hashCode();
        }
        return result;
    }

    /**
     * A single POST policy condition.
     */
    public abstract static class PolicyCondition {
        @Override
        public abstract boolean equals(Object o);

        @Override
        public abstract int hashCode();
    }

    /**
     * A {@code content-length-range} POST policy condition.
     */
    public static final class ContentLengthRange extends PolicyCondition {
        private final long min;
        private final long max;

        private ContentLengthRange(long min, long max) {
            this.min = min;
            this.max = max;
        }

        public long min() {
            return min;
        }

        public long max() {
            return max;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ContentLengthRange that = (ContentLengthRange) o;
            return min == that.min && max == that.max;
        }

        @Override
        public int hashCode() {
            int result = (int) (min ^ (min >>> 32));
            result = 31 * result + (int) (max ^ (max >>> 32));
            return result;
        }
    }

    /**
     * A {@code starts-with} POST policy condition.
     */
    public static final class StartsWith extends PolicyCondition {
        private final String field;
        private final String prefix;

        private StartsWith(String field, String prefix) {
            this.field = field;
            this.prefix = prefix;
        }

        public String field() {
            return field;
        }

        public String prefix() {
            return prefix;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            StartsWith that = (StartsWith) o;
            return field.equals(that.field) && prefix.equals(that.prefix);
        }

        @Override
        public int hashCode() {
            int result = field.hashCode();
            result = 31 * result + prefix.hashCode();
            return result;
        }
    }

    /**
     * An {@code eq} POST policy condition expressed as a JSON object with one entry.
     */
    public static final class Eq extends PolicyCondition {
        private final String field;
        private final String value;

        private Eq(String field, String value) {
            this.field = field;
            this.value = value;
        }

        public String field() {
            return field;
        }

        public String value() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Eq eq = (Eq) o;
            return field.equals(eq.field) && value.equals(eq.value);
        }

        @Override
        public int hashCode() {
            int result = field.hashCode();
            result = 31 * result + value.hashCode();
            return result;
        }
    }

    /**
     * Builder for {@link PostPolicyConditions}.
     */
    @SdkPublicApi
    @NotThreadSafe
    public static final class Builder {
        private final List<PolicyCondition> conditions = new ArrayList<>();

        private Builder() {
        }

        /**
         * Appends a {@code content-length-range} condition.
         */
        public Builder contentLengthRange(long min, long max) {
            Validate.isTrue(min >= 0, "min must be non-negative");
            Validate.isTrue(max >= min, "max must be greater than or equal to min");
            conditions.add(new ContentLengthRange(min, max));
            return this;
        }

        /**
         * Appends a {@code starts-with} condition. The field name is normalised: {@code key} and {@code $key} are treated as
         * the same, non-{@code key} fields are prefixed with {@code $} when missing (for example, {@code Content-Type}
         * becomes {@code $Content-Type}).
         */
        public Builder startsWith(String field, String prefix) {
            Validate.paramNotNull(field, "field");
            Validate.paramNotNull(prefix, "prefix");
            conditions.add(new StartsWith(normalizeStartsWithField(field), prefix));
            return this;
        }

        /**
         * Appends an equality condition for a named field.
         */
        public Builder eq(String field, String value) {
            Validate.paramNotNull(field, "field");
            Validate.paramNotNull(value, "value");
            conditions.add(new Eq(field, value));
            return this;
        }

        public PostPolicyConditions build() {
            return new PostPolicyConditions(this);
        }

        private static String normalizeStartsWithField(String field) {
            if ("key".equalsIgnoreCase(field) || "$key".equalsIgnoreCase(field)) {
                return "$key";
            }
            if (field.startsWith("$")) {
                return field;
            }
            return "$" + field;
        }
    }

    static PolicyCondition contentLengthRangeInternal(long min, long max) {
        return new ContentLengthRange(min, max);
    }

    static PolicyCondition startsWithInternal(String field, String prefix) {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(prefix, "prefix");
        return new StartsWith(field, prefix);
    }

    static PolicyCondition eqInternal(String field, String value) {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(value, "value");
        return new Eq(field, value);
    }
}

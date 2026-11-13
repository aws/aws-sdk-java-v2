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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.util.Map;
import java.util.Objects;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Schema-level coverage for issue #5589: getters whose remainder after {@code get} starts with {@code _} or
 * {@code $} pair with fluent builders under the normalized name when available, otherwise fall back to a builder
 * method matching the original getter name (legacy).
 *
 * <p>Models are handwritten. The normalized underscore case reproduces Lombok {@code @Value}/{@code @Builder}
 * method names for field {@code __encryption_keyId}. Dollar cases are handwritten method-name support only
 * (not Lombok-generated).
 */
public class ImmutableLeadingUnderscoreAttributeTest {

    @After
    public void tearDown() {
        ImmutableTableSchema.clearSchemaCache();
        BeanTableSchema.clearSchemaCache();
    }

    @Test
    public void underscoreImmutable_fromClass_roundTripsExactAttributeName() {
        TableSchema<UnderscoreImmutable> schema = TableSchema.fromClass(UnderscoreImmutable.class);

        assertThat(schema.attributeNames()).containsExactlyInAnyOrder("id", "__encryption_keyId");

        UnderscoreImmutable item = UnderscoreImmutable.builder()
                                                      .id("id-1")
                                                      .__encryption_keyId("key-value")
                                                      .build();

        Map<String, AttributeValue> itemMap = schema.itemToMap(item, true);
        assertThat(itemMap).containsOnlyKeys("id", "__encryption_keyId");
        assertThat(itemMap.get("__encryption_keyId")).isEqualTo(stringValue("key-value"));
        assertThat(itemMap.get("id")).isEqualTo(stringValue("id-1"));

        UnderscoreImmutable roundTripped = schema.mapToItem(itemMap);
        assertThat(roundTripped).isEqualTo(item);
        assertThat(roundTripped.get__encryption_keyId()).isEqualTo("key-value");
    }

    @Test
    public void dollarImmutable_handwrittenMethodNames_roundTripsExactAttributeName() {
        TableSchema<DollarImmutable> schema = TableSchema.fromClass(DollarImmutable.class);

        assertThat(schema.attributeNames()).containsExactlyInAnyOrder("id", "$encryptionKey");

        DollarImmutable item = DollarImmutable.builder()
                                              .id("id-1")
                                              .$encryptionKey("key-value")
                                              .build();

        Map<String, AttributeValue> itemMap = schema.itemToMap(item, true);
        assertThat(itemMap).containsOnlyKeys("id", "$encryptionKey");
        assertThat(itemMap.get("$encryptionKey")).isEqualTo(stringValue("key-value"));
        assertThat(itemMap.get("id")).isEqualTo(stringValue("id-1"));

        DollarImmutable roundTripped = schema.mapToItem(itemMap);
        assertThat(roundTripped).isEqualTo(item);
        assertThat(roundTripped.get$encryptionKey()).isEqualTo("key-value");
    }

    @Test
    public void underscoreBean_control_roundTripsExactAttributeName() {
        TableSchema<UnderscoreBean> schema = TableSchema.fromBean(UnderscoreBean.class);

        assertThat(schema.attributeNames()).containsExactlyInAnyOrder("id", "__encryption_keyId");

        UnderscoreBean item = new UnderscoreBean();
        item.setId("id-1");
        item.set__encryption_keyId("key-value");

        Map<String, AttributeValue> itemMap = schema.itemToMap(item, true);
        assertThat(itemMap).containsOnlyKeys("id", "__encryption_keyId");
        assertThat(itemMap.get("__encryption_keyId")).isEqualTo(stringValue("key-value"));
        assertThat(itemMap.get("id")).isEqualTo(stringValue("id-1"));

        UnderscoreBean roundTripped = schema.mapToItem(itemMap);
        assertThat(roundTripped.getId()).isEqualTo("id-1");
        assertThat(roundTripped.get__encryption_keyId()).isEqualTo("key-value");
    }

    @Test
    public void underscoreImmutable_legacyBuilderNamedLikeGetter_roundTripsExactAttributeName() {
        TableSchema<LegacyUnderscoreImmutable> schema = TableSchema.fromClass(LegacyUnderscoreImmutable.class);

        assertThat(schema.attributeNames()).containsExactlyInAnyOrder("id", "get__encryption_keyId");

        LegacyUnderscoreImmutable item = LegacyUnderscoreImmutable.builder()
                                                                  .id("id-1")
                                                                  .get__encryption_keyId("key-value")
                                                                  .build();

        Map<String, AttributeValue> itemMap = schema.itemToMap(item, true);
        assertThat(itemMap).containsOnlyKeys("id", "get__encryption_keyId");
        assertThat(itemMap.get("get__encryption_keyId")).isEqualTo(stringValue("key-value"));
        assertThat(itemMap.get("id")).isEqualTo(stringValue("id-1"));

        LegacyUnderscoreImmutable roundTripped = schema.mapToItem(itemMap);
        assertThat(roundTripped).isEqualTo(item);
        assertThat(roundTripped.get__encryption_keyId()).isEqualTo("key-value");
    }

    @Test
    public void dollarImmutable_legacyBuilderNamedLikeGetter_roundTripsExactAttributeName() {
        TableSchema<LegacyDollarImmutable> schema = TableSchema.fromClass(LegacyDollarImmutable.class);

        assertThat(schema.attributeNames()).containsExactlyInAnyOrder("id", "get$encryptionKey");

        LegacyDollarImmutable item = LegacyDollarImmutable.builder()
                                                          .id("id-1")
                                                          .get$encryptionKey("key-value")
                                                          .build();

        Map<String, AttributeValue> itemMap = schema.itemToMap(item, true);
        assertThat(itemMap).containsOnlyKeys("id", "get$encryptionKey");
        assertThat(itemMap.get("get$encryptionKey")).isEqualTo(stringValue("key-value"));
        assertThat(itemMap.get("id")).isEqualTo(stringValue("id-1"));

        LegacyDollarImmutable roundTripped = schema.mapToItem(itemMap);
        assertThat(roundTripped).isEqualTo(item);
        assertThat(roundTripped.get$encryptionKey()).isEqualTo("key-value");
    }

    /**
     * Handwritten Lombok-equivalent for field {@code __encryption_keyId}.
     */
    @DynamoDbImmutable(builder = UnderscoreImmutable.Builder.class)
    public static final class UnderscoreImmutable {
        private final String id;
        private final String __encryption_keyId;

        private UnderscoreImmutable(Builder b) {
            this.id = b.id;
            this.__encryption_keyId = b.__encryption_keyId;
        }

        public static Builder builder() {
            return new Builder();
        }

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public String get__encryption_keyId() {
            return __encryption_keyId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            UnderscoreImmutable that = (UnderscoreImmutable) o;
            return Objects.equals(id, that.id) && Objects.equals(__encryption_keyId, that.__encryption_keyId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, __encryption_keyId);
        }

        public static final class Builder {
            private String id;
            private String __encryption_keyId;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder __encryption_keyId(String encryptionKeyId) {
                this.__encryption_keyId = encryptionKeyId;
                return this;
            }

            public UnderscoreImmutable build() {
                return new UnderscoreImmutable(this);
            }
        }
    }

    /**
     * Handwritten {@code $} method names only — not a Lombok-generated model.
     */
    @DynamoDbImmutable(builder = DollarImmutable.Builder.class)
    public static final class DollarImmutable {
        private final String id;
        private final String encryptionKey;

        private DollarImmutable(Builder b) {
            this.id = b.id;
            this.encryptionKey = b.encryptionKey;
        }

        public static Builder builder() {
            return new Builder();
        }

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public String get$encryptionKey() {
            return encryptionKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DollarImmutable that = (DollarImmutable) o;
            return Objects.equals(id, that.id) && Objects.equals(encryptionKey, that.encryptionKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, encryptionKey);
        }

        public static final class Builder {
            private String id;
            private String encryptionKey;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder $encryptionKey(String encryptionKey) {
                this.encryptionKey = encryptionKey;
                return this;
            }

            public DollarImmutable build() {
                return new DollarImmutable(this);
            }
        }
    }

    @DynamoDbBean
    public static class UnderscoreBean {
        private String id;
        private String __encryption_keyId;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String get__encryption_keyId() {
            return __encryption_keyId;
        }

        public void set__encryption_keyId(String encryptionKeyId) {
            this.__encryption_keyId = encryptionKeyId;
        }
    }

    /**
     * Legacy pairing: getter {@code get__encryption_keyId()} with builder method also named
     * {@code get__encryption_keyId(...)}. Property and attribute remain {@code get__encryption_keyId}.
     */
    @DynamoDbImmutable(builder = LegacyUnderscoreImmutable.Builder.class)
    public static final class LegacyUnderscoreImmutable {
        private final String id;
        private final String encryptionKeyId;

        private LegacyUnderscoreImmutable(Builder b) {
            this.id = b.id;
            this.encryptionKeyId = b.encryptionKeyId;
        }

        public static Builder builder() {
            return new Builder();
        }

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public String get__encryption_keyId() {
            return encryptionKeyId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            LegacyUnderscoreImmutable that = (LegacyUnderscoreImmutable) o;
            return Objects.equals(id, that.id) && Objects.equals(encryptionKeyId, that.encryptionKeyId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, encryptionKeyId);
        }

        public static final class Builder {
            private String id;
            private String encryptionKeyId;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder get__encryption_keyId(String encryptionKeyId) {
                this.encryptionKeyId = encryptionKeyId;
                return this;
            }

            public LegacyUnderscoreImmutable build() {
                return new LegacyUnderscoreImmutable(this);
            }
        }
    }

    /**
     * Legacy pairing for handwritten {@code $} methods: getter {@code get$encryptionKey()} with builder
     * {@code get$encryptionKey(...)}. Property and attribute remain {@code get$encryptionKey}.
     */
    @DynamoDbImmutable(builder = LegacyDollarImmutable.Builder.class)
    public static final class LegacyDollarImmutable {
        private final String id;
        private final String encryptionKey;

        private LegacyDollarImmutable(Builder b) {
            this.id = b.id;
            this.encryptionKey = b.encryptionKey;
        }

        public static Builder builder() {
            return new Builder();
        }

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public String get$encryptionKey() {
            return encryptionKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            LegacyDollarImmutable that = (LegacyDollarImmutable) o;
            return Objects.equals(id, that.id) && Objects.equals(encryptionKey, that.encryptionKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, encryptionKey);
        }

        public static final class Builder {
            private String id;
            private String encryptionKey;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder get$encryptionKey(String encryptionKey) {
                this.encryptionKey = encryptionKey;
                return this;
            }

            public LegacyDollarImmutable build() {
                return new LegacyDollarImmutable(this);
            }
        }
    }
}

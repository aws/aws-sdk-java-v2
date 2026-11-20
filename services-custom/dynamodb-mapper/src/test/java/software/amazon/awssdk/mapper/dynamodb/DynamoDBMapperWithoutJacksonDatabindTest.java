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

package software.amazon.awssdk.mapper.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Checks that the mapper works without jackson-databind, which it doesn't depend on, and that
 * {@link DynamoDBTypeConvertedJson} fails with a clear message when the application doesn't provide it.
 *
 * <p>The {@code without-jackson-databind} surefire execution runs this class with jackson-databind removed from the
 * classpath. Elsewhere, for example in an IDE, it is skipped.
 */
class DynamoDBMapperWithoutJacksonDatabindTest {

    private final DynamoDBMapper mapper = DynamoDBMapper.builder()
                                                        .dynamoDbClient(DynamoDbClient.builder()
                                                            .region(Region.US_EAST_1)
                                                            .credentialsProvider(AnonymousCredentialsProvider.create())
                                                            .build())
                                                        .build();

    @BeforeAll
    static void requireJacksonDatabindExcluded() {
        assumeTrue(Boolean.getBoolean("mapper.test.jacksonDatabindExcluded"),
                   "runs only in the without-jackson-databind surefire execution");
        assertThatThrownBy(() -> Class.forName("com.fasterxml.jackson.databind.ObjectMapper"))
            .as("jackson-databind must not be on the classpath of this test")
            .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void isAvailable_withoutJacksonDatabind_returnsFalse() {
        assertThat(JacksonDatabindSupport.isAvailable()).isFalse();
    }

    @Test
    void getTableModel_withoutJsonAttribute_convertsWithoutJacksonDatabind() {
        PlainItem item = new PlainItem();
        item.setId("1");
        item.setName("plain string");

        DynamoDBMapperTableModel<PlainItem> model = mapper.getTableModel(PlainItem.class);
        Map<String, AttributeValue> stored = model.convert(item);

        assertThat(stored.get("name").s()).isEqualTo("plain string");
        assertThat(model.unconvert(stored).getName()).isEqualTo("plain string");
    }

    @Test
    void getTableModel_withJsonAttribute_throwsMissingJacksonDatabindMessage() {
        Throwable thrown = catchThrowable(() -> mapper.getTableModel(JsonItem.class));

        assertThat(thrown).isInstanceOf(DynamoDBMappingException.class);
        Throwable root = thrown;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        assertThat(root)
            .isInstanceOf(DynamoDBMappingException.class)
            .hasMessageContaining("@DynamoDBTypeConvertedJson requires jackson-databind")
            .hasMessageContaining("com.fasterxml.jackson.core:jackson-databind")
            .hasMessageContaining("Jackson-databind");
    }

    public static class PlainItem {
        private String id;
        private String name;

        @DynamoDBHashKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class JsonItem {
        private String id;
        private Payload payload;

        @DynamoDBHashKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDBTypeConvertedJson
        public Payload getPayload() {
            return payload;
        }

        public void setPayload(Payload payload) {
            this.payload = payload;
        }
    }

    public static class Payload {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}

package software.amazon.awssdk.dynamodb.datamodeling;

import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Migration tests: proves that v1 mapper patterns work after changing imports.
 * Each test represents a real v1 customer pattern.
 */
public class V1MigrationTest {

    private static DynamoDBProxyServer server;
    private static DynamoDbClient client;
    private static DynamoDBMapper mapper;

    // =========================================================================
    // Case 1: Simple POJO — String/Number/Boolean fields, hash key only
    // Migration: change imports only. Zero POJO changes.
    // =========================================================================

    @DynamoDBTable(tableName = "Users")
    public static class User {
        private String userId;
        private String name;
        private Integer age;

        @DynamoDBHashKey
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        @DynamoDBAttribute
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        @DynamoDBAttribute
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            User user = (User) o;
            return Objects.equals(userId, user.userId)
                    && Objects.equals(name, user.name)
                    && Objects.equals(age, user.age);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, name, age);
        }
    }

    // =========================================================================
    // Case 2: Composite key — hash + range, multiple String attributes
    // Migration: change imports only. Zero POJO changes.
    // =========================================================================

    @DynamoDBTable(tableName = "Orders")
    public static class Order {
        private String customerId;
        private String orderId;
        private String status;
        private String itemName;

        @DynamoDBHashKey(attributeName = "customerId")
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }

        @DynamoDBRangeKey(attributeName = "orderId")
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }

        @DynamoDBAttribute(attributeName = "status")
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        @DynamoDBAttribute(attributeName = "itemName")
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Order order = (Order) o;
            return Objects.equals(customerId, order.customerId)
                    && Objects.equals(orderId, order.orderId)
                    && Objects.equals(status, order.status)
                    && Objects.equals(itemName, order.itemName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(customerId, orderId, status, itemName);
        }
    }

    // =========================================================================
    // Setup
    // =========================================================================

    @BeforeClass
    public static void setup() throws Exception {
        int port = getFreePort();
        server = ServerRunner.createServerFromCommandLineArgs(
                new String[]{"-inMemory", "-port", String.valueOf(port)});
        server.start();

        client = DynamoDbClient.builder()
                .endpointOverride(URI.create("http://localhost:" + port))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("fakeKey", "fakeSecret")))
                .build();

        mapper = new DynamoDBMapper(client);

        client.createTable(CreateTableRequest.builder()
                .tableName("Users")
                .keySchema(KeySchemaElement.builder()
                        .attributeName("userId").keyType(KeyType.HASH).build())
                .attributeDefinitions(AttributeDefinition.builder()
                        .attributeName("userId").attributeType(ScalarAttributeType.S).build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build());

        client.createTable(CreateTableRequest.builder()
                .tableName("Orders")
                .keySchema(
                        KeySchemaElement.builder()
                                .attributeName("customerId").keyType(KeyType.HASH).build(),
                        KeySchemaElement.builder()
                                .attributeName("orderId").keyType(KeyType.RANGE).build())
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName("customerId").attributeType(ScalarAttributeType.S).build(),
                        AttributeDefinition.builder()
                                .attributeName("orderId").attributeType(ScalarAttributeType.S).build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build());
    }

    @AfterClass
    public static void teardown() throws Exception {
        if (client != null) client.close();
        if (server != null) server.stop();
    }

    // =========================================================================
    // Case 1 tests: Simple POJO
    // =========================================================================

    @Test
    public void case1_loadByHashKey() {
        putRaw("Users", map("userId", s("u1"), "name", s("Alice"), "age", n("30")));

        User result = mapper.load(User.class, "u1");

        User expected = new User();
        expected.setUserId("u1");
        expected.setName("Alice");
        expected.setAge(30);
        assertThat(result, is(expected));
    }

    @Test
    public void case1_loadByKeyObject() {
        putRaw("Users", map("userId", s("u2"), "name", s("Bob"), "age", n("25")));

        User key = new User();
        key.setUserId("u2");

        User result = mapper.load(key);

        assertThat(result.getName(), is("Bob"));
        assertThat(result.getAge(), is(25));
    }

    @Test
    public void case1_loadNonExistent() {
        User result = mapper.load(User.class, "does-not-exist");
        assertThat(result, is(nullValue()));
    }

    @Test
    public void case1_missingOptionalAttributes() {
        // Item exists but only has the key — other fields should be null
        putRaw("Users", map("userId", s("u3")));

        User result = mapper.load(User.class, "u3");

        assertThat(result.getUserId(), is("u3"));
        assertThat(result.getName(), is(nullValue()));
        assertThat(result.getAge(), is(nullValue()));
    }

    // =========================================================================
    // Case 2 tests: Composite key
    // =========================================================================

    @Test
    public void case2_loadByCompositeKey() {
        putRaw("Orders", map(
                "customerId", s("c1"), "orderId", s("o1"),
                "status", s("SHIPPED"), "itemName", s("Widget")));

        Order result = mapper.load(Order.class, "c1", "o1");

        Order expected = new Order();
        expected.setCustomerId("c1");
        expected.setOrderId("o1");
        expected.setStatus("SHIPPED");
        expected.setItemName("Widget");
        assertThat(result, is(expected));
    }

    @Test
    public void case2_loadByKeyObject() {
        putRaw("Orders", map(
                "customerId", s("c2"), "orderId", s("o2"),
                "status", s("PENDING"), "itemName", s("Gadget")));

        Order key = new Order();
        key.setCustomerId("c2");
        key.setOrderId("o2");

        Order result = mapper.load(key);

        assertThat(result.getStatus(), is("PENDING"));
        assertThat(result.getItemName(), is("Gadget"));
    }

    @Test
    public void case2_consistentReadConfig() {
        putRaw("Orders", map(
                "customerId", s("c3"), "orderId", s("o3"),
                "status", s("DELIVERED"), "itemName", s("Doohickey")));

        DynamoDBMapperConfig config = DynamoDBMapperConfig.builder()
                .withConsistentReads(DynamoDBMapperConfig.ConsistentReads.CONSISTENT)
                .build();

        Order result = mapper.load(Order.class, "c3", "o3", config);

        assertThat(result.getStatus(), is("DELIVERED"));
    }

    @Test
    public void case2_tableNameOverride() {
        // Create a prefixed table
        try {
            client.createTable(CreateTableRequest.builder()
                    .tableName("prod-Orders")
                    .keySchema(
                            KeySchemaElement.builder()
                                    .attributeName("customerId").keyType(KeyType.HASH).build(),
                            KeySchemaElement.builder()
                                    .attributeName("orderId").keyType(KeyType.RANGE).build())
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("customerId").attributeType(ScalarAttributeType.S).build(),
                            AttributeDefinition.builder()
                                    .attributeName("orderId").attributeType(ScalarAttributeType.S).build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
        } catch (ResourceInUseException e) { /* already exists */ }

        putRaw("prod-Orders", map(
                "customerId", s("c4"), "orderId", s("o4"),
                "status", s("PROCESSING"), "itemName", s("Thingamajig")));

        DynamoDBMapperConfig config = DynamoDBMapperConfig.builder()
                .withTableNameOverride(
                        DynamoDBMapperConfig.TableNameOverride.withTableNamePrefix("prod-"))
                .build();

        Order result = mapper.load(Order.class, "c4", "o4", config);

        assertThat(result.getStatus(), is("PROCESSING"));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static AttributeValue s(String val) {
        return AttributeValue.builder().s(val).build();
    }

    private static AttributeValue n(String val) {
        return AttributeValue.builder().n(val).build();
    }

    private static Map<String, AttributeValue> map(String k1, AttributeValue v1) {
        Map<String, AttributeValue> m = new HashMap<>();
        m.put(k1, v1);
        return m;
    }

    private static Map<String, AttributeValue> map(String k1, AttributeValue v1,
                                                    String k2, AttributeValue v2) {
        Map<String, AttributeValue> m = new HashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }

    private static Map<String, AttributeValue> map(String k1, AttributeValue v1,
                                                    String k2, AttributeValue v2,
                                                    String k3, AttributeValue v3) {
        Map<String, AttributeValue> m = map(k1, v1, k2, v2);
        m.put(k3, v3);
        return m;
    }

    private static Map<String, AttributeValue> map(String k1, AttributeValue v1,
                                                    String k2, AttributeValue v2,
                                                    String k3, AttributeValue v3,
                                                    String k4, AttributeValue v4) {
        Map<String, AttributeValue> m = map(k1, v1, k2, v2, k3, v3);
        m.put(k4, v4);
        return m;
    }

    private void putRaw(String table, Map<String, AttributeValue> item) {
        client.putItem(PutItemRequest.builder().tableName(table).item(item).build());
    }

    private static int getFreePort() throws IOException {
        ServerSocket socket = new ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();
        return port;
    }
}

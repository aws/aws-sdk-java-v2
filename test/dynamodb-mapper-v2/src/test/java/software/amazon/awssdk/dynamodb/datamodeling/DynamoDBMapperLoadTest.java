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
 * Ported from Enhanced Client's BasicCrudTest — same scenarios, same assertions.
 * Tests use raw DynamoDbClient for writes and DynamoDBMapper for reads,
 * so we're testing the mapper's deserialization against known DDB state.
 */
public class DynamoDBMapperLoadTest {

    private static DynamoDBProxyServer server;
    private static DynamoDbClient client;
    private static DynamoDBMapper mapper;
    private static final String TABLE = "mapper-v2-test";

    // ---- Models ----

    @DynamoDBTable(tableName = TABLE)
    public static class Record {
        private String id;
        private String sort;
        private String attribute;
        private String attribute2;
        private String attribute3;

        @DynamoDBHashKey(attributeName = "id")
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        @DynamoDBRangeKey(attributeName = "sort")
        public String getSort() { return sort; }
        public void setSort(String sort) { this.sort = sort; }

        @DynamoDBAttribute(attributeName = "attribute")
        public String getAttribute() { return attribute; }
        public void setAttribute(String attribute) { this.attribute = attribute; }

        @DynamoDBAttribute(attributeName = "attribute2")
        public String getAttribute2() { return attribute2; }
        public void setAttribute2(String attribute2) { this.attribute2 = attribute2; }

        @DynamoDBAttribute(attributeName = "attribute3")
        public String getAttribute3() { return attribute3; }
        public void setAttribute3(String attribute3) { this.attribute3 = attribute3; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Record record = (Record) o;
            return Objects.equals(id, record.id) &&
                   Objects.equals(sort, record.sort) &&
                   Objects.equals(attribute, record.attribute) &&
                   Objects.equals(attribute2, record.attribute2) &&
                   Objects.equals(attribute3, record.attribute3);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, sort, attribute, attribute2, attribute3);
        }
    }

    @DynamoDBTable(tableName = TABLE)
    public static class ShortRecord {
        private String id;
        private String sort;
        private String attribute;

        @DynamoDBHashKey(attributeName = "id")
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        @DynamoDBRangeKey(attributeName = "sort")
        public String getSort() { return sort; }
        public void setSort(String sort) { this.sort = sort; }

        @DynamoDBAttribute(attributeName = "attribute")
        public String getAttribute() { return attribute; }
        public void setAttribute(String attribute) { this.attribute = attribute; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ShortRecord that = (ShortRecord) o;
            return Objects.equals(id, that.id) &&
                   Objects.equals(sort, that.sort) &&
                   Objects.equals(attribute, that.attribute);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, sort, attribute);
        }
    }

    // ---- Setup / Teardown ----

    @BeforeClass
    public static void setup() throws Exception {
        int port = getFreePort();
        server = ServerRunner.createServerFromCommandLineArgs(new String[]{"-inMemory", "-port", String.valueOf(port)});
        server.start();

        client = DynamoDbClient.builder()
                .endpointOverride(URI.create("http://localhost:" + port))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("fakeKey", "fakeSecret")))
                .build();

        mapper = new DynamoDBMapper(client);

        client.createTable(CreateTableRequest.builder()
                .tableName(TABLE)
                .keySchema(
                        KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build(),
                        KeySchemaElement.builder().attributeName("sort").keyType(KeyType.RANGE).build())
                .attributeDefinitions(
                        AttributeDefinition.builder().attributeName("id").attributeType(ScalarAttributeType.S).build(),
                        AttributeDefinition.builder().attributeName("sort").attributeType(ScalarAttributeType.S).build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build());
    }

    @AfterClass
    public static void teardown() throws Exception {
        if (client != null) client.close();
        if (server != null) server.stop();
    }

    // ---- Helpers ----

    private void putItem(String id, String sort, String attr, String attr2, String attr3) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(id).build());
        item.put("sort", AttributeValue.builder().s(sort).build());
        if (attr != null) item.put("attribute", AttributeValue.builder().s(attr).build());
        if (attr2 != null) item.put("attribute2", AttributeValue.builder().s(attr2).build());
        if (attr3 != null) item.put("attribute3", AttributeValue.builder().s(attr3).build());
        client.putItem(PutItemRequest.builder().tableName(TABLE).item(item).build());
    }

    private void deleteItem(String id, String sort) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s(id).build());
        key.put("sort", AttributeValue.builder().s(sort).build());
        client.deleteItem(DeleteItemRequest.builder().tableName(TABLE).key(key).build());
    }

    private Record makeRecord(String id, String sort, String attr, String attr2, String attr3) {
        Record r = new Record();
        r.setId(id); r.setSort(sort); r.setAttribute(attr); r.setAttribute2(attr2); r.setAttribute3(attr3);
        return r;
    }

    // ---- Tests ported from Enhanced Client BasicCrudTest ----

    @Test
    public void putThenGetItemUsingKey() {
        putItem("id-value", "sort-value", "one", "two", "three");

        Record result = mapper.load(Record.class, "id-value", "sort-value");

        Record expected = makeRecord("id-value", "sort-value", "one", "two", "three");
        assertThat(result, is(expected));

        deleteItem("id-value", "sort-value");
    }

    @Test
    public void putThenGetItemUsingKeyItem() {
        putItem("id-value-2", "sort-value-2", "one", "two", "three");

        Record keyItem = new Record();
        keyItem.setId("id-value-2");
        keyItem.setSort("sort-value-2");

        Record result = mapper.load(keyItem);

        Record expected = makeRecord("id-value-2", "sort-value-2", "one", "two", "three");
        assertThat(result, is(expected));

        deleteItem("id-value-2", "sort-value-2");
    }

    @Test
    public void getNonExistentItem() {
        Record result = mapper.load(Record.class, "id-value", "sort-value-nonexistent");
        assertThat(result, is(nullValue()));
    }

    @Test
    public void putTwiceThenGetItem() {
        putItem("id-overwrite", "sort-overwrite", "one", "two", "three");
        putItem("id-overwrite", "sort-overwrite", "four", "five", "six");

        Record result = mapper.load(Record.class, "id-overwrite", "sort-overwrite");

        Record expected = makeRecord("id-overwrite", "sort-overwrite", "four", "five", "six");
        assertThat(result, is(expected));

        deleteItem("id-overwrite", "sort-overwrite");
    }

    @Test
    public void getAShortRecordWithNewModelledFields() {
        // Put item with only 3 attributes (id, sort, attribute)
        putItem("id-short", "sort-short", "one", null, null);

        // Load as full Record — extra fields should be null
        Record result = mapper.load(Record.class, "id-short", "sort-short");

        Record expected = makeRecord("id-short", "sort-short", "one", null, null);
        assertThat(result, is(expected));

        deleteItem("id-short", "sort-short");
    }

    @Test
    public void getItemWithConsistentRead() {
        putItem("id-consistent", "sort-consistent", "one", "two", "three");

        DynamoDBMapperConfig config = DynamoDBMapperConfig.builder()
                .withConsistentReads(DynamoDBMapperConfig.ConsistentReads.CONSISTENT)
                .build();

        Record result = mapper.load(Record.class, "id-consistent", "sort-consistent", config);

        Record expected = makeRecord("id-consistent", "sort-consistent", "one", "two", "three");
        assertThat(result, is(expected));

        deleteItem("id-consistent", "sort-consistent");
    }

    @Test
    public void getItemWithEventualConsistency() {
        putItem("id-eventual", "sort-eventual", "one", "two", "three");

        DynamoDBMapperConfig config = DynamoDBMapperConfig.builder()
                .withConsistentReads(DynamoDBMapperConfig.ConsistentReads.EVENTUAL)
                .build();

        Record result = mapper.load(Record.class, "id-eventual", "sort-eventual", config);

        Record expected = makeRecord("id-eventual", "sort-eventual", "one", "two", "three");
        assertThat(result, is(expected));

        deleteItem("id-eventual", "sort-eventual");
    }

    @Test
    public void getItemPartialKeyOnly_hashKey() {
        putItem("id-partial", "sort-partial", "one", "two", "three");

        // Load using key object with only hash+range set (no other fields)
        Record keyItem = new Record();
        keyItem.setId("id-partial");
        keyItem.setSort("sort-partial");

        Record result = mapper.load(keyItem);

        Record expected = makeRecord("id-partial", "sort-partial", "one", "two", "three");
        assertThat(result, is(expected));

        deleteItem("id-partial", "sort-partial");
    }

    @Test
    public void getItemWithTableNameOverride() {
        putItem("id-override", "sort-override", "one", "two", "three");

        DynamoDBMapperConfig config = DynamoDBMapperConfig.builder()
                .withTableNameOverride(DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement(TABLE))
                .build();

        Record result = mapper.load(Record.class, "id-override", "sort-override", config);

        Record expected = makeRecord("id-override", "sort-override", "one", "two", "three");
        assertThat(result, is(expected));

        deleteItem("id-override", "sort-override");
    }

    @Test
    public void getItemReturnsNullForEmptyStringAttributes() {
        // DDB doesn't store empty strings in older schemas — verify mapper handles this
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s("id-empty").build());
        item.put("sort", AttributeValue.builder().s("sort-empty").build());
        // No attribute, attribute2, attribute3 — they simply don't exist in the item
        client.putItem(PutItemRequest.builder().tableName(TABLE).item(item).build());

        Record result = mapper.load(Record.class, "id-empty", "sort-empty");

        Record expected = makeRecord("id-empty", "sort-empty", null, null, null);
        assertThat(result, is(expected));

        deleteItem("id-empty", "sort-empty");
    }

    private static int getFreePort() throws IOException {
        ServerSocket socket = new ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();
        return port;
    }
}

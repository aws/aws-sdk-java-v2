package software.amazon.awssdk.dynamodb.datamodeling;

import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Golden tests: v1 mapper is the oracle.
 * Each test puts a raw item, loads via v1 mapper AND v2 mapper, asserts identical results.
 */
public class GoldenV1ConformanceTest {

    private static DynamoDBProxyServer server;
    private static DynamoDbClient v2Client;
    private static com.amazonaws.services.dynamodbv2.AmazonDynamoDB v1Client;

    private static software.amazon.awssdk.dynamodb.datamodeling.DynamoDBMapper v2Mapper;
    private static com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper v1Mapper;

    private static final String TABLE = "GoldenTest";

    // =========================================================================
    // V1 POJO (the oracle)
    // =========================================================================

    @com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable(tableName = TABLE)
    public static class V1Item {
        private String pk;
        private String sk;
        private String stringVal;
        private Integer intVal;
        private Long longVal;
        private Double doubleVal;
        private BigDecimal bigDecimalVal;
        private Boolean boolVal;
        private ByteBuffer binaryVal;
        private List<String> stringList;
        private Map<String, String> stringMap;
        private Set<String> stringSet;

        @com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey(attributeName = "pk")
        public String getPk() { return pk; }
        public void setPk(String pk) { this.pk = pk; }

        @com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey(attributeName = "sk")
        public String getSk() { return sk; }
        public void setSk(String sk) { this.sk = sk; }

        @com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute(attributeName = "stringVal")
        public String getStringVal() { return stringVal; }
        public void setStringVal(String stringVal) { this.stringVal = stringVal; }

        @com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute(attributeName = "intVal")
        public Integer getIntVal() { return intVal; }
        public void setIntVal(Integer intVal) { this.intVal = intVal; }

        @com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute(attributeName = "longVal")
        public Long getLongVal() { return longVal; }
        public void setLongVal(Long longVal) { this.longVal = longVal; }

        @com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute(attributeName = "doubleVal")
        public Double getDoubleVal() { return doubleVal; }
        public void setDoubleVal(Double doubleVal) { this.doubleVal = doubleVal; }

        @com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute(attributeName = "bigDecimalVal")
        public BigDecimal getBigDecimalVal() { return bigDecimalVal; }
        public void setBigDecimalVal(BigDecimal bigDecimalVal) { this.bigDecimalVal = bigDecimalVal; }

        @com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute(attributeName = "boolVal")
        public Boolean getBoolVal() { return boolVal; }
        public void setBoolVal(Boolean boolVal) { this.boolVal = boolVal; }

        @com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute(attributeName = "binaryVal")
        public ByteBuffer getBinaryVal() { return binaryVal; }
        public void setBinaryVal(ByteBuffer binaryVal) { this.binaryVal = binaryVal; }

        @com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute(attributeName = "stringList")
        public List<String> getStringList() { return stringList; }
        public void setStringList(List<String> stringList) { this.stringList = stringList; }

        @com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute(attributeName = "stringMap")
        public Map<String, String> getStringMap() { return stringMap; }
        public void setStringMap(Map<String, String> stringMap) { this.stringMap = stringMap; }

        @com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute(attributeName = "stringSet")
        public Set<String> getStringSet() { return stringSet; }
        public void setStringSet(Set<String> stringSet) { this.stringSet = stringSet; }
    }

    // =========================================================================
    // V2 POJO (must match v1 exactly)
    // =========================================================================

    @DynamoDBTable(tableName = TABLE)
    public static class V2Item {
        private String pk;
        private String sk;
        private String stringVal;
        private Integer intVal;
        private Long longVal;
        private Double doubleVal;
        private BigDecimal bigDecimalVal;
        private Boolean boolVal;
        private ByteBuffer binaryVal;
        private List<String> stringList;
        private Map<String, String> stringMap;
        private Set<String> stringSet;

        @DynamoDBHashKey(attributeName = "pk")
        public String getPk() { return pk; }
        public void setPk(String pk) { this.pk = pk; }

        @DynamoDBRangeKey(attributeName = "sk")
        public String getSk() { return sk; }
        public void setSk(String sk) { this.sk = sk; }

        @DynamoDBAttribute(attributeName = "stringVal")
        public String getStringVal() { return stringVal; }
        public void setStringVal(String stringVal) { this.stringVal = stringVal; }

        @DynamoDBAttribute(attributeName = "intVal")
        public Integer getIntVal() { return intVal; }
        public void setIntVal(Integer intVal) { this.intVal = intVal; }

        @DynamoDBAttribute(attributeName = "longVal")
        public Long getLongVal() { return longVal; }
        public void setLongVal(Long longVal) { this.longVal = longVal; }

        @DynamoDBAttribute(attributeName = "doubleVal")
        public Double getDoubleVal() { return doubleVal; }
        public void setDoubleVal(Double doubleVal) { this.doubleVal = doubleVal; }

        @DynamoDBAttribute(attributeName = "bigDecimalVal")
        public BigDecimal getBigDecimalVal() { return bigDecimalVal; }
        public void setBigDecimalVal(BigDecimal bigDecimalVal) { this.bigDecimalVal = bigDecimalVal; }

        @DynamoDBAttribute(attributeName = "boolVal")
        public Boolean getBoolVal() { return boolVal; }
        public void setBoolVal(Boolean boolVal) { this.boolVal = boolVal; }

        @DynamoDBAttribute(attributeName = "binaryVal")
        public ByteBuffer getBinaryVal() { return binaryVal; }
        public void setBinaryVal(ByteBuffer binaryVal) { this.binaryVal = binaryVal; }

        @DynamoDBAttribute(attributeName = "stringList")
        public List<String> getStringList() { return stringList; }
        public void setStringList(List<String> stringList) { this.stringList = stringList; }

        @DynamoDBAttribute(attributeName = "stringMap")
        public Map<String, String> getStringMap() { return stringMap; }
        public void setStringMap(Map<String, String> stringMap) { this.stringMap = stringMap; }

        @DynamoDBAttribute(attributeName = "stringSet")
        public Set<String> getStringSet() { return stringSet; }
        public void setStringSet(Set<String> stringSet) { this.stringSet = stringSet; }
    }

    // =========================================================================
    // Setup — both v1 and v2 mappers against same DDB Local
    // =========================================================================

    @BeforeClass
    public static void setup() throws Exception {
        int port = getFreePort();
        server = ServerRunner.createServerFromCommandLineArgs(
                new String[]{"-inMemory", "-port", String.valueOf(port)});
        server.start();

        String endpoint = "http://localhost:" + port;

        // v2 client
        v2Client = DynamoDbClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("fakeKey", "fakeSecret")))
                .build();

        // v1 client
        v1Client = com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(
                        new com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration(
                                endpoint, "us-east-1"))
                .withCredentials(new com.amazonaws.auth.AWSStaticCredentialsProvider(
                        new com.amazonaws.auth.BasicAWSCredentials("fakeKey", "fakeSecret")))
                .build();

        v2Mapper = new software.amazon.awssdk.dynamodb.datamodeling.DynamoDBMapper(v2Client);
        v1Mapper = new com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper(v1Client);

        // Create table
        v2Client.createTable(CreateTableRequest.builder()
                .tableName(TABLE)
                .keySchema(
                        KeySchemaElement.builder().attributeName("pk").keyType(KeyType.HASH).build(),
                        KeySchemaElement.builder().attributeName("sk").keyType(KeyType.RANGE).build())
                .attributeDefinitions(
                        AttributeDefinition.builder().attributeName("pk").attributeType(ScalarAttributeType.S).build(),
                        AttributeDefinition.builder().attributeName("sk").attributeType(ScalarAttributeType.S).build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build());
    }

    @AfterClass
    public static void teardown() throws Exception {
        if (v2Client != null) v2Client.close();
        if (v1Client != null) v1Client.shutdown();
        if (server != null) server.stop();
    }

    // =========================================================================
    // Golden tests
    // =========================================================================

    @Test
    public void golden_stringFields() {
        putRaw("str-1", "sk-1", map("stringVal", s("hello")));

        V1Item v1 = v1Mapper.load(V1Item.class, "str-1", "sk-1");
        V2Item v2 = v2Mapper.load(V2Item.class, "str-1", "sk-1");

        assertEquals(v1.getStringVal(), v2.getStringVal());
    }

    @Test
    public void golden_integerField() {
        putRaw("int-1", "sk-1", map("intVal", n("42")));

        V1Item v1 = v1Mapper.load(V1Item.class, "int-1", "sk-1");
        V2Item v2 = v2Mapper.load(V2Item.class, "int-1", "sk-1");

        assertEquals(v1.getIntVal(), v2.getIntVal());
    }

    @Test
    public void golden_longField() {
        putRaw("long-1", "sk-1", map("longVal", n("9999999999")));

        V1Item v1 = v1Mapper.load(V1Item.class, "long-1", "sk-1");
        V2Item v2 = v2Mapper.load(V2Item.class, "long-1", "sk-1");

        assertEquals(v1.getLongVal(), v2.getLongVal());
    }

    @Test
    public void golden_doubleField() {
        putRaw("dbl-1", "sk-1", map("doubleVal", n("3.14159")));

        V1Item v1 = v1Mapper.load(V1Item.class, "dbl-1", "sk-1");
        V2Item v2 = v2Mapper.load(V2Item.class, "dbl-1", "sk-1");

        assertEquals(v1.getDoubleVal(), v2.getDoubleVal());
    }

    @Test
    public void golden_bigDecimalField() {
        putRaw("bd-1", "sk-1", map("bigDecimalVal", n("12345.6789")));

        V1Item v1 = v1Mapper.load(V1Item.class, "bd-1", "sk-1");
        V2Item v2 = v2Mapper.load(V2Item.class, "bd-1", "sk-1");

        assertEquals(v1.getBigDecimalVal(), v2.getBigDecimalVal());
    }

    @Test
    public void golden_booleanStoredAsNumber() {
        // V2_COMPATIBLE stores booleans as N "1"/"0"
        putRaw("bool-n-1", "sk-1", map("boolVal", n("1")));

        V1Item v1 = v1Mapper.load(V1Item.class, "bool-n-1", "sk-1");
        V2Item v2 = v2Mapper.load(V2Item.class, "bool-n-1", "sk-1");

        assertEquals(v1.getBoolVal(), v2.getBoolVal());
    }

    @Test
    public void golden_booleanStoredAsNativeBool() {
        // Some items may have been written with native BOOL type
        putRaw("bool-b-1", "sk-1", mapWithBool("boolVal", true));

        V1Item v1 = v1Mapper.load(V1Item.class, "bool-b-1", "sk-1");
        V2Item v2 = v2Mapper.load(V2Item.class, "bool-b-1", "sk-1");

        assertEquals(v1.getBoolVal(), v2.getBoolVal());
    }

    @Test
    public void golden_binaryField() {
        byte[] data = new byte[]{1, 2, 3, 4, 5};
        putRaw("bin-1", "sk-1", mapWithBinary("binaryVal", data));

        V1Item v1 = v1Mapper.load(V1Item.class, "bin-1", "sk-1");
        V2Item v2 = v2Mapper.load(V2Item.class, "bin-1", "sk-1");

        assertEquals(v1.getBinaryVal(), v2.getBinaryVal());
    }

    @Test
    public void golden_stringList() {
        putRaw("list-1", "sk-1", mapWithList("stringList",
                Arrays.asList(s("a"), s("b"), s("c"))));

        V1Item v1 = v1Mapper.load(V1Item.class, "list-1", "sk-1");
        V2Item v2 = v2Mapper.load(V2Item.class, "list-1", "sk-1");

        assertEquals(v1.getStringList(), v2.getStringList());
    }

    @Test
    public void golden_stringMap() {
        Map<String, AttributeValue> inner = new HashMap<>();
        inner.put("key1", s("val1"));
        inner.put("key2", s("val2"));
        putRaw("map-1", "sk-1", mapWithMap("stringMap", inner));

        V1Item v1 = v1Mapper.load(V1Item.class, "map-1", "sk-1");
        V2Item v2 = v2Mapper.load(V2Item.class, "map-1", "sk-1");

        assertEquals(v1.getStringMap(), v2.getStringMap());
    }

    @Test
    public void golden_stringSet() {
        putRaw("set-1", "sk-1", mapWithSS("stringSet", Arrays.asList("x", "y", "z")));

        V1Item v1 = v1Mapper.load(V1Item.class, "set-1", "sk-1");
        V2Item v2 = v2Mapper.load(V2Item.class, "set-1", "sk-1");

        assertEquals(v1.getStringSet(), v2.getStringSet());
    }

    @Test
    public void golden_nullFields() {
        // Item with only keys — all other fields absent
        putRaw("null-1", "sk-1", Collections.emptyMap());

        V1Item v1 = v1Mapper.load(V1Item.class, "null-1", "sk-1");
        V2Item v2 = v2Mapper.load(V2Item.class, "null-1", "sk-1");

        assertEquals(v1.getStringVal(), v2.getStringVal());
        assertEquals(v1.getIntVal(), v2.getIntVal());
        assertEquals(v1.getBoolVal(), v2.getBoolVal());
        assertEquals(v1.getStringList(), v2.getStringList());
        assertEquals(v1.getStringMap(), v2.getStringMap());
        assertEquals(v1.getStringSet(), v2.getStringSet());
        assertEquals(v1.getBinaryVal(), v2.getBinaryVal());
    }

    @Test
    public void golden_nonExistentItem() {
        V1Item v1 = v1Mapper.load(V1Item.class, "nope", "nope");
        V2Item v2 = v2Mapper.load(V2Item.class, "nope", "nope");

        assertNull(v1);
        assertNull(v2);
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

    private void putRaw(String pk, String sk, Map<String, AttributeValue> extra) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("pk", s(pk));
        item.put("sk", s(sk));
        item.putAll(extra);
        v2Client.putItem(PutItemRequest.builder().tableName(TABLE).item(item).build());
    }

    private static Map<String, AttributeValue> map(String k, AttributeValue v) {
        Map<String, AttributeValue> m = new HashMap<>();
        m.put(k, v);
        return m;
    }

    private static Map<String, AttributeValue> mapWithBool(String k, boolean v) {
        Map<String, AttributeValue> m = new HashMap<>();
        m.put(k, AttributeValue.builder().bool(v).build());
        return m;
    }

    private static Map<String, AttributeValue> mapWithBinary(String k, byte[] v) {
        Map<String, AttributeValue> m = new HashMap<>();
        m.put(k, AttributeValue.builder().b(SdkBytes.fromByteArray(v)).build());
        return m;
    }

    private static Map<String, AttributeValue> mapWithList(String k, List<AttributeValue> v) {
        Map<String, AttributeValue> m = new HashMap<>();
        m.put(k, AttributeValue.builder().l(v).build());
        return m;
    }

    private static Map<String, AttributeValue> mapWithMap(String k, Map<String, AttributeValue> v) {
        Map<String, AttributeValue> m = new HashMap<>();
        m.put(k, AttributeValue.builder().m(v).build());
        return m;
    }

    private static Map<String, AttributeValue> mapWithSS(String k, List<String> v) {
        Map<String, AttributeValue> m = new HashMap<>();
        m.put(k, AttributeValue.builder().ss(v).build());
        return m;
    }

    private static int getFreePort() throws IOException {
        ServerSocket socket = new ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();
        return port;
    }
}

package software.amazon.awssdk.dynamodb.datamodeling;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

/**
 * Ported from Enhanced Client's GetItemOperationTest.
 * Verifies the exact request sent over the wire and the exact deserialization of the response.
 */
@RunWith(MockitoJUnitRunner.class)
public class DynamoDBMapperGetItemWireTest {

    private static final String TABLE_NAME = "TestTable";

    @Mock
    private DynamoDbClient mockClient;

    private DynamoDBMapper mapper;

    // ---- Models ----

    @DynamoDBTable(tableName = TABLE_NAME)
    public static class FakeItem {
        private String id;
        private String stringAttr;

        @DynamoDBHashKey(attributeName = "id")
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        @DynamoDBAttribute(attributeName = "stringAttr")
        public String getStringAttr() { return stringAttr; }
        public void setStringAttr(String stringAttr) { this.stringAttr = stringAttr; }
    }

    @DynamoDBTable(tableName = TABLE_NAME)
    public static class FakeItemWithSort {
        private String id;
        private String sort;
        private String data;

        @DynamoDBHashKey(attributeName = "id")
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        @DynamoDBRangeKey(attributeName = "sort")
        public String getSort() { return sort; }
        public void setSort(String sort) { this.sort = sort; }

        @DynamoDBAttribute(attributeName = "data")
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
    }

    @Before
    public void setup() {
        mapper = new DynamoDBMapper(mockClient);
    }

    // ---- Request verification tests (what goes over the wire) ----

    @Test
    public void generateRequest_partitionKeyOnly() {
        when(mockClient.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder().build());

        mapper.load(FakeItem.class, "test-id");

        ArgumentCaptor<GetItemRequest> captor = ArgumentCaptor.forClass(GetItemRequest.class);
        verify(mockClient).getItem(captor.capture());

        GetItemRequest actual = captor.getValue();
        Map<String, AttributeValue> expectedKey = new HashMap<>();
        expectedKey.put("id", AttributeValue.builder().s("test-id").build());

        assertThat(actual.tableName(), is(TABLE_NAME));
        assertThat(actual.key(), is(expectedKey));
        assertThat(actual.consistentRead(), is(false));
    }

    @Test
    public void generateRequest_partitionAndSortKey() {
        when(mockClient.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder().build());

        mapper.load(FakeItemWithSort.class, "pk-val", "sk-val");

        ArgumentCaptor<GetItemRequest> captor = ArgumentCaptor.forClass(GetItemRequest.class);
        verify(mockClient).getItem(captor.capture());

        GetItemRequest actual = captor.getValue();
        Map<String, AttributeValue> expectedKey = new HashMap<>();
        expectedKey.put("id", AttributeValue.builder().s("pk-val").build());
        expectedKey.put("sort", AttributeValue.builder().s("sk-val").build());

        assertThat(actual.tableName(), is(TABLE_NAME));
        assertThat(actual.key(), is(expectedKey));
    }

    @Test
    public void generateRequest_consistentRead() {
        when(mockClient.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder().build());

        DynamoDBMapperConfig config = DynamoDBMapperConfig.builder()
                .withConsistentReads(DynamoDBMapperConfig.ConsistentReads.CONSISTENT)
                .build();

        mapper.load(FakeItem.class, "test-id", null, config);

        ArgumentCaptor<GetItemRequest> captor = ArgumentCaptor.forClass(GetItemRequest.class);
        verify(mockClient).getItem(captor.capture());

        assertThat(captor.getValue().consistentRead(), is(true));
    }

    @Test
    public void generateRequest_eventualConsistency() {
        when(mockClient.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder().build());

        DynamoDBMapperConfig config = DynamoDBMapperConfig.builder()
                .withConsistentReads(DynamoDBMapperConfig.ConsistentReads.EVENTUAL)
                .build();

        mapper.load(FakeItem.class, "test-id", null, config);

        ArgumentCaptor<GetItemRequest> captor = ArgumentCaptor.forClass(GetItemRequest.class);
        verify(mockClient).getItem(captor.capture());

        assertThat(captor.getValue().consistentRead(), is(false));
    }

    @Test
    public void generateRequest_usingKeyObject() {
        when(mockClient.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder().build());

        FakeItemWithSort keyObj = new FakeItemWithSort();
        keyObj.setId("pk-val");
        keyObj.setSort("sk-val");
        mapper.load(keyObj);

        ArgumentCaptor<GetItemRequest> captor = ArgumentCaptor.forClass(GetItemRequest.class);
        verify(mockClient).getItem(captor.capture());

        Map<String, AttributeValue> expectedKey = new HashMap<>();
        expectedKey.put("id", AttributeValue.builder().s("pk-val").build());
        expectedKey.put("sort", AttributeValue.builder().s("sk-val").build());

        assertThat(captor.getValue().key(), is(expectedKey));
    }

    @Test
    public void generateRequest_tableNameOverride() {
        when(mockClient.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder().build());

        DynamoDBMapperConfig config = DynamoDBMapperConfig.builder()
                .withTableNameOverride(DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement("OverriddenTable"))
                .build();

        mapper.load(FakeItem.class, "test-id", null, config);

        ArgumentCaptor<GetItemRequest> captor = ArgumentCaptor.forClass(GetItemRequest.class);
        verify(mockClient).getItem(captor.capture());

        assertThat(captor.getValue().tableName(), is("OverriddenTable"));
    }

    // ---- Response deserialization tests (what comes back from the wire) ----

    @Test
    public void transformResponse_noItem() {
        when(mockClient.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder().build());

        FakeItem result = mapper.load(FakeItem.class, "nonexistent");

        assertThat(result, is(nullValue()));
    }

    @Test
    public void transformResponse_emptyItemMap() {
        when(mockClient.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder().item(new HashMap<>()).build());

        FakeItem result = mapper.load(FakeItem.class, "nonexistent");

        assertThat(result, is(nullValue()));
    }

    @Test
    public void transformResponse_correctlyDeserializesItem() {
        Map<String, AttributeValue> responseMap = new HashMap<>();
        responseMap.put("id", AttributeValue.builder().s("test-id").build());
        responseMap.put("stringAttr", AttributeValue.builder().s("test-value").build());

        when(mockClient.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder().item(responseMap).build());

        FakeItem result = mapper.load(FakeItem.class, "test-id");

        assertThat(result.getId(), is("test-id"));
        assertThat(result.getStringAttr(), is("test-value"));
    }

    @Test
    public void transformResponse_compositeKey_correctlyDeserializesItem() {
        Map<String, AttributeValue> responseMap = new HashMap<>();
        responseMap.put("id", AttributeValue.builder().s("pk-val").build());
        responseMap.put("sort", AttributeValue.builder().s("sk-val").build());
        responseMap.put("data", AttributeValue.builder().s("payload").build());

        when(mockClient.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder().item(responseMap).build());

        FakeItemWithSort result = mapper.load(FakeItemWithSort.class, "pk-val", "sk-val");

        assertThat(result.getId(), is("pk-val"));
        assertThat(result.getSort(), is("sk-val"));
        assertThat(result.getData(), is("payload"));
    }

    @Test
    public void transformResponse_missingAttributes_deserializesAsNull() {
        Map<String, AttributeValue> responseMap = new HashMap<>();
        responseMap.put("id", AttributeValue.builder().s("test-id").build());
        // stringAttr not in response

        when(mockClient.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder().item(responseMap).build());

        FakeItem result = mapper.load(FakeItem.class, "test-id");

        assertThat(result.getId(), is("test-id"));
        assertThat(result.getStringAttr(), is(nullValue()));
    }

    @Test
    public void transformResponse_extraAttributesInResponse_ignored() {
        Map<String, AttributeValue> responseMap = new HashMap<>();
        responseMap.put("id", AttributeValue.builder().s("test-id").build());
        responseMap.put("stringAttr", AttributeValue.builder().s("val").build());
        responseMap.put("unknownField", AttributeValue.builder().s("ignored").build());

        when(mockClient.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder().item(responseMap).build());

        FakeItem result = mapper.load(FakeItem.class, "test-id");

        assertThat(result.getId(), is("test-id"));
        assertThat(result.getStringAttr(), is("val"));
    }
}

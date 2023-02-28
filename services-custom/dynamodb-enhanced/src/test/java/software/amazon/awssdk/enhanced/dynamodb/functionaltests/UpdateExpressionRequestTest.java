package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.keyRef;
import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.valueRef;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.update.AddAction;
import software.amazon.awssdk.enhanced.dynamodb.update.DeleteAction;
import software.amazon.awssdk.enhanced.dynamodb.update.RemoveAction;
import software.amazon.awssdk.enhanced.dynamodb.update.SetAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class UpdateExpressionRequestTest extends LocalDynamoDbSyncTestBase {

    private static final Set<String> SET_ATTRIBUTE_INIT_VALUE = Stream.of("YELLOW", "BLUE", "RED", "GREEN")
                                                                      .collect(Collectors.toSet());
    private static final Set<String> SET_ATTRIBUTE_DELETE = Stream.of("YELLOW", "RED").collect(Collectors.toSet());
    private static final List<String> REQUEST_ATTRIBUTE_LIST_INIT_VAL = Arrays.asList("a", "c");

    private static final TableSchema<Record> TABLE_SCHEMA =
        StaticTableSchema.builder(Record.class)
                         .newItemSupplier(Record::new)
                         .addAttribute(String.class, a -> a.name("id")
                                                           .getter(Record::getId)
                                                           .setter(Record::setId)
                                                           .tags(primaryPartitionKey()))
                         .addAttribute(String.class, a -> a.name("sort")
                                                           .getter(Record::getSort)
                                                           .setter(Record::setSort)
                                                           .tags(primarySortKey()))
                         .addAttribute(EnhancedType.listOf(String.class), a -> a.name("listAttribute")
                                                           .getter(Record::getListAttribute)
                                                           .setter(Record::setListAttribute))
                         .addAttribute(EnhancedType.mapOf(String.class, String.class), a -> a.name("mapAttribute")
                                                           .getter(Record::getMapAttribute)
                                                           .setter(Record::setMapAttribute))
                         .addAttribute(EnhancedType.setOf(String.class), a -> a.name("setAttribute")
                                                           .getter(Record::getSetAttribute)
                                                           .setter(Record::setSetAttribute))
                         .build();

    private final DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                                .dynamoDbClient(getDynamoDbClient())
                                                                                .build();
    private final DynamoDbTable<Record> mappedTable = enhancedClient.table(getConcreteTableName("table-name"), TABLE_SCHEMA);

    @Before
    public void initRecord() {
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
        Map<String, String> map = new HashMap<>();
        map.put("a", "1");
        map.put("b", "2");
        Set<String> sett = new HashSet<>();
        sett.add("aba");
        Record initialRecord = new Record().setId("1")
                                           .setSort("a")
                                           .setListAttribute(Arrays.asList("gi", "li"))
                                           .setMapAttribute(map)
                                           .setSetAttribute(sett);
        mappedTable.putItem(initialRecord);
    }

    @After
    public void deleteTable() {
        getDynamoDbClient().deleteTable(r -> r.tableName(getConcreteTableName("table-name")));
    }

    @Test
    public void listOperations() {
        String partitionKey = "1";
        String sortKey = "a";
        Key key = Key.builder().partitionValue(partitionKey).sortValue(sortKey).build();

        Record persistedRecord = mappedTable.getItem(key);
        List<String> listAttribute = persistedRecord.getListAttribute();
        assertThat(listAttribute).hasSize(2);
        assertThat(listAttribute).containsExactly("gi", "li");

        Record record = new Record().setId(partitionKey).setSort(sortKey);
        UpdateExpression updateExpression = UpdateExpression.builder()
                                                            .addAction(setListElement("listAttribute", 0, "gah"))
                                                            .build();
        mappedTable.updateItem(r -> r.item(record).updateExpression(updateExpression));

        persistedRecord = mappedTable.getItem(key);
        listAttribute = persistedRecord.getListAttribute();
        assertThat(listAttribute).hasSize(2);
        assertThat(listAttribute).containsExactly("gah", "li");

        mappedTable.updateItem(r -> r.item(record)
                                     .updateExpression(UpdateExpression.builder()
                                                                       .addAction(removeAttributeFromList("listAttribute", 0))
                                                                       .build()));
        persistedRecord = mappedTable.getItem(key);
        listAttribute = persistedRecord.getListAttribute();
        assertThat(listAttribute).hasSize(1);
        assertThat(listAttribute).containsExactly("li");

        List<String> subList = Arrays.asList("si", "fu");
        mappedTable.updateItem(r -> r.item(record)
                                     .updateExpression(UpdateExpression.builder()
                                                                       .addAction(appendToList("listAttribute", subList))
                                                                       .build()));
        persistedRecord = mappedTable.getItem(key);
        listAttribute = persistedRecord.getListAttribute();
        assertThat(listAttribute).hasSize(3);
        assertThat(listAttribute).containsExactly("li", "si", "fu");
    }

    @Test
    public void setOperations() {
        String partitionKey = "1";
        String sortKey = "a";
        Key key = Key.builder().partitionValue(partitionKey).sortValue(sortKey).build();

        Record persistedRecord = mappedTable.getItem(key);
        Set<String> setAttribute = persistedRecord.getSetAttribute();
        assertThat(setAttribute).hasSize(1);
        assertThat(setAttribute).containsExactly("aba");

        Record record = new Record().setId(partitionKey).setSort(sortKey);
        UpdateExpression updateExpression = UpdateExpression.builder()
                                                            .addAction(addValuesToSet("setAttribute", Arrays.asList("mip", "foo")))
                                                            .build();
        mappedTable.updateItem(r -> r.item(record).updateExpression(updateExpression));

        persistedRecord = mappedTable.getItem(key);
        setAttribute = persistedRecord.getSetAttribute();
        assertThat(setAttribute).hasSize(3);
        assertThat(setAttribute).containsExactlyInAnyOrder("aba", "mip", "foo");

        mappedTable.updateItem(r -> r.item(record)
                                     .updateExpression(UpdateExpression.builder()
                                                                       .addAction(deleteValuesFromSet("setAttribute", Arrays.asList(
                                                                                                      "aba")))
                                                                       .build()));
        persistedRecord = mappedTable.getItem(key);
        setAttribute = persistedRecord.getSetAttribute();
        assertThat(setAttribute).hasSize(2);
        assertThat(setAttribute).containsExactlyInAnyOrder("mip", "foo");
    }

    @Test
    public void mapOperations() {
        String partitionKey = "1";
        String sortKey = "a";
        Key key = Key.builder().partitionValue(partitionKey).sortValue(sortKey).build();

        Record persistedRecord = mappedTable.getItem(key);
        Map<String, String> mapAttribute = persistedRecord.getMapAttribute();
        assertThat(mapAttribute).hasSize(2);

        Record record = new Record().setId(partitionKey).setSort(sortKey);
        UpdateExpression updateExpression = UpdateExpression.builder()
                                                            .addAction(setMapAttributeToStringValue("mapAttribute", "c",
                                                                                                    "3"))
                                                            .build();
        mappedTable.updateItem(r -> r.item(record).updateExpression(updateExpression));

        persistedRecord = mappedTable.getItem(key);
        mapAttribute = persistedRecord.getMapAttribute();
        assertThat(mapAttribute).hasSize(3);
        assertThat(mapAttribute).containsEntry("c", "3");

        mappedTable.updateItem(r -> r.item(record)
                                     .updateExpression(UpdateExpression.builder()
                                                                       .addAction(removeAttributeFromMap("mapAttribute", "a"))
                                                                       .build()));
        persistedRecord = mappedTable.getItem(key);
        mapAttribute = persistedRecord.getMapAttribute();
        assertThat(mapAttribute).hasSize(2);
        assertThat(mapAttribute).doesNotContainEntry("1", "a");
    }

    private SetAction setListElement(String listAttributeName, int index, String value) {
        return SetAction.builder()
                        .path(keyRef(listAttributeName) + "[" + index + "]")
                        .value(valueRef(listAttributeName))
                        .putExpressionValue(valueRef(listAttributeName), AttributeValue.fromS(value))
                        .putExpressionName(keyRef(listAttributeName), listAttributeName)
                        .build();
    }

    private SetAction setMapAttributeToStringValue(String mapAttributeName,
                                                   String nestedAttributeName,
                                                   String value) {
        return SetAction.builder()
                        .path(keyRef(mapAttributeName) + "." + keyRef(nestedAttributeName))
                        .value(valueRef(mapAttributeName))
                        .putExpressionValue(valueRef(mapAttributeName), AttributeValue.fromS(value))
                        .putExpressionName(keyRef(mapAttributeName), mapAttributeName)
                        .putExpressionName(keyRef(nestedAttributeName), nestedAttributeName)
                        .build();
    }

    private SetAction appendToList(String listAttributeName, List<String> listToAppend) {
        List<AttributeValue> listValues = listToAppend.stream().map(AttributeValue::fromS).collect(Collectors.toList());
        return SetAction.builder()
                        .path(keyRef(listAttributeName))
                        .value("list_append(" + keyRef(listAttributeName) + "," + valueRef(listAttributeName) + ")")
                        .putExpressionValue(valueRef(listAttributeName), AttributeValue.fromL(listValues))
                        .putExpressionName(keyRef(listAttributeName), listAttributeName)
                        .build();
    }

    private RemoveAction removeAttributeFromList(String listAttributeName, int index) {
        return RemoveAction.builder()
                        .path(keyRef(listAttributeName) + "[" + index + "]")
                        .putExpressionName(keyRef(listAttributeName), listAttributeName)
                        .build();
    }

    private RemoveAction removeAttributeFromMap(String mapAttributeName, String nestedAttributeName) {
        return RemoveAction.builder()
                           .path(keyRef(mapAttributeName) + "." + keyRef(nestedAttributeName))
                           .putExpressionName(keyRef(mapAttributeName), mapAttributeName)
                           .putExpressionName(keyRef(nestedAttributeName), nestedAttributeName)
                           .build();
    }

    private AddAction addValuesToSet(String setAttributeName, List<String> values) {
        return AddAction.builder()
                        .path(keyRef(setAttributeName))
                        .value(valueRef(setAttributeName))
                        .putExpressionValue(valueRef(setAttributeName), AttributeValue.fromSs(values))
                        .putExpressionName(keyRef(setAttributeName), setAttributeName)
                        .build();
    }

    private DeleteAction deleteValuesFromSet(String setAttributeName, List<String> values) {
        return DeleteAction.builder()
                           .path(keyRef(setAttributeName))
                           .value(valueRef(setAttributeName))
                           .putExpressionValue(valueRef(setAttributeName), AttributeValue.fromSs(values))
                           .putExpressionName(keyRef(setAttributeName), setAttributeName)
                           .build();
    }

    private static class Record {
        private String id;
        private String sort;
        private List<String> listAttribute;
        private Map<String, String> mapAttribute;
        private Set<String> setAttribute;

        private String getId() {
            return id;
        }

        private Record setId(String id) {
            this.id = id;
            return this;
        }

        private String getSort() {
            return sort;
        }

        private Record setSort(String sort) {
            this.sort = sort;
            return this;
        }

        private List<String> getListAttribute() {
            return listAttribute;
        }

        private Record setListAttribute(List<String> listAttribute) {
            this.listAttribute = listAttribute;
            return this;
        }

        private Map<String, String> getMapAttribute() {
            return mapAttribute;
        }

        private Record setMapAttribute(Map<String, String> mapAttribute) {
            this.mapAttribute = mapAttribute;
            return this;
        }

        private Set<String> getSetAttribute() {
            return setAttribute;
        }

        private Record setSetAttribute(Set<String> setAttribute) {
            this.setAttribute = setAttribute;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Record record = (Record) o;
            return Objects.equals(id, record.id) &&
                   Objects.equals(sort, record.sort) &&
                   Objects.equals(listAttribute, record.listAttribute) &&
                   Objects.equals(mapAttribute, record.mapAttribute) &&
                   Objects.equals(setAttribute, record.setAttribute);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, sort, listAttribute, mapAttribute, setAttribute);
        }
    }
}

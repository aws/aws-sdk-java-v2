package software.amazon.awssdk.enhanced.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ListAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.MapAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.SetAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.StringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Consolidated coverage for the supported collection declarations.
 */
public class DefaultAttributeConverterProviderCollectionTest {

    // List declarations and nested list values.
    @Test
    @DisplayName("List conversion preserves order and duplicates as an ArrayList")
    void converterFor_whenStringList_preservesOrderAndDuplicatesAsArrayList() {
        DefaultAttributeConverterProvider listProvider = new DefaultAttributeConverterProvider();
        EnhancedType<List<String>> type = EnhancedType.listOf(String.class);
        List<String> input = new ArrayList<>();
        input.add("a");
        input.add("b");
        input.add("a");

        AttributeConverter<List<String>> converter = listProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        List<String> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(ListAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.L);
        assertThat(read).isInstanceOf(ArrayList.class)
                        .isEqualTo(input)
                        .containsExactly("a", "b", "a");
    }

    @Test
    @DisplayName("Empty list conversion stores an empty L and reads an empty ArrayList")
    void converterFor_whenEmptyStringList_convertsToEmptyLAndReadsEmptyArrayList() {
        DefaultAttributeConverterProvider listProvider = new DefaultAttributeConverterProvider();
        EnhancedType<List<String>> type = EnhancedType.listOf(String.class);
        List<String> input = new ArrayList<>();

        AttributeConverter<List<String>> converter = listProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        List<String> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(ListAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.L);
        assertThat(stored.l()).isEmpty();
        assertThat(read).isInstanceOf(ArrayList.class)
                        .isEmpty();
    }

    @Test
    @DisplayName("A null string list member is stored as DynamoDB NULL and read back as null")
    void converterFor_whenStringListWithNullMember_storesNulAndReadsNullMember() {
        DefaultAttributeConverterProvider listProvider = new DefaultAttributeConverterProvider();
        EnhancedType<List<String>> type = EnhancedType.listOf(String.class);
        List<String> input = new ArrayList<>();
        input.add("a");
        input.add(null);
        input.add("b");

        AttributeConverter<List<String>> converter = listProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        List<String> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(ListAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.L);
        assertThat(stored.l()).hasSize(3);
        assertThat(stored.l().get(1).nul()).isTrue();
        assertThat(read).isInstanceOf(ArrayList.class)
                        .containsExactly("a", null, "b");
        assertThat(read.get(1)).isNull();
    }

    @Test
    @DisplayName("An Instant list member is stored as S and read as an equal ArrayList")
    void converterFor_whenInstantList_convertsMemberAsSAndReadsEqualArrayList() {
        DefaultAttributeConverterProvider listProvider = new DefaultAttributeConverterProvider();
        EnhancedType<List<Instant>> type = EnhancedType.listOf(Instant.class);
        Instant instant = Instant.parse("2020-01-02T03:04:05Z");
        List<Instant> input = new ArrayList<>();
        input.add(instant);

        AttributeConverter<List<Instant>> converter = listProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        List<Instant> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(ListAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.L);
        assertThat(stored.l()).hasSize(1);
        assertThat(stored.l().get(0).s()).isNotNull();
        assertThat(read).isInstanceOf(ArrayList.class)
                        .isEqualTo(input)
                        .containsExactly(instant);
    }

    @Test
    @DisplayName("An enum list member is stored as S and read as the same constant")
    void converterFor_whenEnumList_convertsMemberAsSAndReadsSameConstant() {
        DefaultAttributeConverterProvider listProvider = new DefaultAttributeConverterProvider();
        EnhancedType<List<ListTestEnum>> type = EnhancedType.listOf(ListTestEnum.class);
        List<ListTestEnum> input = new ArrayList<>();
        input.add(ListTestEnum.OPEN);

        AttributeConverter<List<ListTestEnum>> converter = listProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        List<ListTestEnum> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(ListAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.L);
        assertThat(stored.l()).hasSize(1);
        assertThat(stored.l().get(0).s()).isEqualTo(ListTestEnum.OPEN.toString());
        assertThat(read).isInstanceOf(ArrayList.class)
                        .containsExactly(ListTestEnum.OPEN);
    }

    @Test
    @DisplayName("A document list member is stored as M and reconstructed through the table schema")
    void converterFor_whenDocumentList_recordsBothConversionsAndReadsReconstructedDocument() {
        DefaultAttributeConverterProvider listProvider = new DefaultAttributeConverterProvider();
        TableSchema<ListDocumentType> schema = listDocumentSchema();
        EnhancedType<List<ListDocumentType>> type =
            EnhancedType.listOf(EnhancedType.documentOf(ListDocumentType.class, schema));
        ListDocumentType document = new ListDocumentType();
        document.setName("doc");
        List<ListDocumentType> input = new ArrayList<>();
        input.add(document);

        AttributeConverter<List<ListDocumentType>> converter = listProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        List<ListDocumentType> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(ListAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.L);
        assertThat(stored.l()).hasSize(1);
        assertThat(stored.l().get(0).m()).containsEntry("name", AttributeValue.fromS("doc"));
        assertThat(read).isInstanceOf(ArrayList.class);
        assertThat(read.get(0).getName()).isEqualTo("doc");
    }

    @Test
    @DisplayName("A nested list uses L at both levels and reads ArrayList at both levels")
    void converterFor_whenNestedStringList_usesLAtBothLevelsAndReadsArrayLists() {
        DefaultAttributeConverterProvider listProvider = new DefaultAttributeConverterProvider();
        EnhancedType<List<List<String>>> type = EnhancedType.listOf(EnhancedType.listOf(String.class));
        List<String> inner = new ArrayList<>();
        inner.add("a");
        inner.add("b");
        List<List<String>> input = new ArrayList<>();
        input.add(inner);

        AttributeConverter<List<List<String>>> converter = listProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        List<List<String>> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(ListAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.L);
        assertThat(stored.l()).hasSize(1);
        assertThat(stored.l().get(0).l()).containsExactly(AttributeValue.fromS("a"),
                                                          AttributeValue.fromS("b"));
        assertThat(read).isInstanceOf(ArrayList.class);
        assertThat(read.get(0)).isInstanceOf(ArrayList.class)
                               .containsExactly("a", "b");
    }

    @Test
    @DisplayName("A map list member uses L then M and reads ArrayList then LinkedHashMap")
    void converterFor_whenMapListMember_usesLThenMAndReadsArrayListThenLinkedHashMap() {
        DefaultAttributeConverterProvider listProvider = new DefaultAttributeConverterProvider();
        EnhancedType<List<Map<String, Integer>>> type =
            EnhancedType.listOf(EnhancedType.mapOf(String.class, Integer.class));
        Map<String, Integer> inner = new LinkedHashMap<>();
        inner.put("one", 1);
        List<Map<String, Integer>> input = new ArrayList<>();
        input.add(inner);

        AttributeConverter<List<Map<String, Integer>>> converter = listProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        List<Map<String, Integer>> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(ListAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.L);
        assertThat(stored.l()).hasSize(1);
        assertThat(stored.l().get(0).m()).isNotNull();
        assertThat(read).isInstanceOf(ArrayList.class);
        assertThat(read.get(0)).isInstanceOf(LinkedHashMap.class)
                               .containsEntry("one", 1);
    }

    @Test
    @DisplayName("A set list member uses L then SS and reads ArrayList then LinkedHashSet")
    void converterFor_whenSetListMember_usesLThenSsAndReadsArrayListThenLinkedHashSet() {
        DefaultAttributeConverterProvider listProvider = new DefaultAttributeConverterProvider();
        EnhancedType<List<Set<String>>> type = EnhancedType.listOf(EnhancedType.setOf(String.class));
        Set<String> inner = new LinkedHashSet<>();
        inner.add("a");
        inner.add("b");
        List<Set<String>> input = new ArrayList<>();
        input.add(inner);

        AttributeConverter<List<Set<String>>> converter = listProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        List<Set<String>> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(ListAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.L);
        assertThat(stored.l()).hasSize(1);
        assertThat(stored.l().get(0).ss()).containsExactly("a", "b");
        assertThat(read).isInstanceOf(ArrayList.class);
        assertThat(read.get(0)).isInstanceOf(LinkedHashSet.class)
                               .containsExactly("a", "b");
    }

    @Test
    @DisplayName("A list of maps of documents uses L, M, then M and reconstructs the document")
    void converterFor_whenListOfMapsOfDocuments_usesLThenMThenMAndReadsReconstructedDocument() {
        DefaultAttributeConverterProvider listProvider = new DefaultAttributeConverterProvider();
        TableSchema<ListDocumentType> schema = listDocumentSchema();
        EnhancedType<List<Map<String, ListDocumentType>>> type =
            EnhancedType.listOf(EnhancedType.mapOf(EnhancedType.of(String.class),
                                                   EnhancedType.documentOf(ListDocumentType.class, schema)));
        ListDocumentType nested = new ListDocumentType();
        nested.setName("doc");
        Map<String, ListDocumentType> inner = new LinkedHashMap<>();
        inner.put("doc", nested);
        List<Map<String, ListDocumentType>> input = new ArrayList<>();
        input.add(inner);

        AttributeConverter<List<Map<String, ListDocumentType>>> converter = listProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        List<Map<String, ListDocumentType>> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(ListAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.L);
        assertThat(stored.l()).hasSize(1);
        assertThat(stored.l().get(0).m()).isNotNull();
        assertThat(stored.l().get(0).m().get("doc").m()).containsEntry("name", AttributeValue.fromS("doc"));
        assertThat(read).isInstanceOf(ArrayList.class);
        assertThat(read.get(0)).isInstanceOf(LinkedHashMap.class);
        assertThat(read.get(0).get("doc").getName()).isEqualTo("doc");
    }

    @Test
    @DisplayName("An Object list member fails converter lookup")
    void converterFor_whenObjectListMember_throwsConverterNotFound() {
        DefaultAttributeConverterProvider listProvider = new DefaultAttributeConverterProvider();
        EnhancedType<List<Object>> type = EnhancedType.listOf(Object.class);

        assertThatThrownBy(() -> listProvider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(Object.class));
    }

    @Test
    @DisplayName("A document class without a schema is an unsupported list member")
    void converterFor_whenDocumentClassWithoutSchemaList_throwsIllegalStateException() {
        DefaultAttributeConverterProvider listProvider = new DefaultAttributeConverterProvider();
        EnhancedType<List<ListDocumentType>> type = EnhancedType.listOf(ListDocumentType.class);

        assertThatThrownBy(() -> listProvider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(ListDocumentType.class));
    }

    @Test
    @DisplayName("A wildcard list member fails while reading the raw class")
    void converterFor_whenWildcardListMember_throwsIllegalArgumentException() {
        DefaultAttributeConverterProvider listProvider = new DefaultAttributeConverterProvider();
        EnhancedType<List<?>> type = new EnhancedType<List<?>>() {
        };

        assertThatThrownBy(() -> listProvider.converterFor(type))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A wildcard type is not expected here.");
    }

    @Test
    @DisplayName("A nested Object list fails converter lookup for the inner Object")
    void converterFor_whenNestedObjectList_throwsConverterNotFoundForInnerObject() {
        DefaultAttributeConverterProvider listProvider = new DefaultAttributeConverterProvider();
        EnhancedType<List<Object>> innerType = EnhancedType.listOf(Object.class);
        EnhancedType<List<List<Object>>> type = EnhancedType.listOf(innerType);

        assertThatThrownBy(() -> listProvider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(Object.class));
    }

    @Test
    @DisplayName("A nested Boolean set rejects a BOOL member converter")
    void converterFor_whenNestedBooleanSetList_throwsIllegalArgumentException() {
        DefaultAttributeConverterProvider listProvider = new DefaultAttributeConverterProvider();
        EnhancedType<List<Set<Boolean>>> type = EnhancedType.listOf(EnhancedType.setOf(Boolean.class));

        assertThatThrownBy(() -> listProvider.converterFor(type))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("SetAttributeConverter cannot be created with a parameterized type of 'class java.lang.Boolean'. "
                        + "Supported parameterized types must convert to B, S or N DynamoDB AttributeValues.");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unsupportedListImplementationTypes")
    @DisplayName("Unsupported List implementations fail converter lookup")
    void converterFor_whenUnsupportedListImplementation_throwsIllegalStateException(EnhancedType<?> type) {
        DefaultAttributeConverterProvider listProvider = new DefaultAttributeConverterProvider();

        assertThatThrownBy(() -> listProvider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    private static Stream<EnhancedType<?>> unsupportedListImplementationTypes() {
        return Stream.of(new EnhancedType<LinkedList<String>>() {
        }, new EnhancedType<ListCustomList<String>>() {
        }, new EnhancedType<Vector<String>>() {
        }, new EnhancedType<CopyOnWriteArrayList<String>>() {
        }, new EnhancedType<Stack<String>>() {
        });
    }

    private static TableSchema<ListDocumentType> listDocumentSchema() {
        return StaticTableSchema.builder(ListDocumentType.class)
                                .newItemSupplier(ListDocumentType::new)
                                .addAttribute(String.class, a -> a.name("name")
                                                                  .getter(ListDocumentType::getName)
                                                                  .setter(ListDocumentType::setName))
                                .build();
    }

    static class ListDocumentType {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    enum ListTestEnum {
        OPEN,
        CLOSED
    }

    static final class ListCustomList<T> extends ArrayList<T> {
    }

    private static final String UNSUPPORTED_SET_MEMBER_SUFFIX =
        "'. Supported parameterized types must convert to B, S or N DynamoDB AttributeValues.";

    // Set declarations, including Collection and Iterable routing.
    @Test
    @DisplayName("String set conversion preserves insertion order as SS and reads a LinkedHashSet")
    void converterFor_whenStringSet_convertsToInsertionOrderedSsAndReadsLinkedHashSet() {
        DefaultAttributeConverterProvider setProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Set<String>> type = EnhancedType.setOf(String.class);
        Set<String> input = linkedSet("a", "b");

        AttributeConverter<Set<String>> converter = setProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        Set<String> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(SetAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.SS);
        assertThat(stored.ss()).containsExactly("a", "b");
        assertThat(read).isInstanceOf(LinkedHashSet.class)
                        .isEqualTo(linkedSet("a", "b"))
                        .containsExactly("a", "b");
    }

    @Test
    @DisplayName("Number set conversion preserves insertion order as NS and reads a LinkedHashSet")
    void converterFor_whenIntegerSet_convertsToInsertionOrderedNsAndReadsLinkedHashSet() {
        DefaultAttributeConverterProvider setProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Set<Integer>> type = EnhancedType.setOf(Integer.class);
        Set<Integer> input = linkedSet(1, 2);

        AttributeConverter<Set<Integer>> converter = setProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        Set<Integer> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(SetAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.NS);
        assertThat(stored.ns()).containsExactly("1", "2");
        assertThat(read).isInstanceOf(LinkedHashSet.class)
                        .isEqualTo(linkedSet(1, 2))
                        .containsExactly(1, 2);
    }

    @Test
    @DisplayName("Binary set conversion stores BS and reads a LinkedHashSet")
    void converterFor_whenSdkBytesSet_convertsToBsAndReadsLinkedHashSet() {
        DefaultAttributeConverterProvider setProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Set<SdkBytes>> type = EnhancedType.setOf(SdkBytes.class);
        SdkBytes bytes = SdkBytes.fromUtf8String("a");
        Set<SdkBytes> input = linkedSet(bytes);

        AttributeConverter<Set<SdkBytes>> converter = setProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        Set<SdkBytes> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(SetAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.BS);
        assertThat(stored.bs()).containsExactly(SdkBytes.fromUtf8String("a"));
        assertThat(read).isInstanceOf(LinkedHashSet.class)
                        .isEqualTo(linkedSet(bytes))
                        .containsExactly(bytes);
    }

    @Test
    @DisplayName("Enum set conversion stores toString() member text as SS")
    void converterFor_whenEnumSet_convertsToSsUsingToStringAndReadsLinkedHashSet() {
        DefaultAttributeConverterProvider setProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Set<SetTestEnum>> type = EnhancedType.setOf(SetTestEnum.class);
        Set<SetTestEnum> input = linkedSet(SetTestEnum.OPEN);

        AttributeConverter<Set<SetTestEnum>> converter = setProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        Set<SetTestEnum> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(SetAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.SS);
        assertThat(stored.ss()).containsExactly(SetTestEnum.OPEN.toString());
        assertThat(read).isInstanceOf(LinkedHashSet.class)
                        .containsExactly(SetTestEnum.OPEN);
    }

    @Test
    @DisplayName("Empty set conversion stores empty SS and reads an empty LinkedHashSet")
    void converterFor_whenEmptyStringSet_convertsToEmptySsAndReadsEmptyLinkedHashSet() {
        DefaultAttributeConverterProvider setProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Set<String>> type = EnhancedType.setOf(String.class);
        Set<String> input = new LinkedHashSet<>();

        AttributeConverter<Set<String>> converter = setProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        Set<String> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(SetAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.SS);
        assertThat(stored.ss()).isEmpty();
        assertThat(read).isInstanceOf(LinkedHashSet.class)
                        .isEmpty();
    }

    @Test
    @DisplayName("A null string set member produces a non-string attribute and is rejected")
    void converterFor_whenStringSetWithNullMember_throwsIllegalArgumentException() {
        DefaultAttributeConverterProvider setProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Set<String>> type = EnhancedType.setOf(String.class);
        Set<String> input = new LinkedHashSet<>();
        input.add(null);

        assertThatThrownBy(() -> setProvider.converterFor(type).transformFrom(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attribute value must be S.");
    }

    @Test
    @DisplayName("A list member converter declaring L is rejected as a set member")
    void converterFor_whenListMemberSet_throwsIllegalArgumentException() {
        DefaultAttributeConverterProvider setProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Set<List<String>>> type = EnhancedType.setOf(EnhancedType.listOf(String.class));

        assertThatThrownBy(() -> setProvider.converterFor(type))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(unsupportedSetMemberMessage(List.class));
    }

    @Test
    @DisplayName("A map member converter declaring M is rejected as a set member")
    void converterFor_whenMapMemberSet_throwsIllegalArgumentException() {
        DefaultAttributeConverterProvider setProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Set<Map<String, String>>> type =
            EnhancedType.setOf(EnhancedType.mapOf(String.class, String.class));

        assertThatThrownBy(() -> setProvider.converterFor(type))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(unsupportedSetMemberMessage(Map.class));
    }

    @Test
    @DisplayName("A document member converter declaring M is rejected as a set member")
    void converterFor_whenDocumentMemberSet_throwsIllegalArgumentException() {
        DefaultAttributeConverterProvider setProvider = new DefaultAttributeConverterProvider();
        TableSchema<SetDocumentType> documentSchema =
            StaticTableSchema.builder(SetDocumentType.class).newItemSupplier(SetDocumentType::new).build();
        EnhancedType<Set<SetDocumentType>> type =
            EnhancedType.setOf(EnhancedType.documentOf(SetDocumentType.class, documentSchema));

        assertThatThrownBy(() -> setProvider.converterFor(type))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(unsupportedSetMemberMessage(SetDocumentType.class));
    }

    @Test
    @DisplayName("A custom NULL member converter is rejected as a set member")
    void converterFor_whenNullAttributeTypeMemberSet_throwsIllegalArgumentException() {
        DefaultAttributeConverterProvider setProvider = providerWithDeclaredMemberConverter(
            NullSetMember.class, AttributeValueType.NULL);
        EnhancedType<Set<NullSetMember>> type = EnhancedType.setOf(NullSetMember.class);

        assertThatThrownBy(() -> setProvider.converterFor(type))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(unsupportedSetMemberMessage(NullSetMember.class));
    }

    @Test
    @DisplayName("A custom SS member converter is rejected as a set member")
    void converterFor_whenSsAttributeTypeMemberSet_throwsIllegalArgumentException() {
        DefaultAttributeConverterProvider setProvider = providerWithDeclaredMemberConverter(
            StringSetMember.class, AttributeValueType.SS);
        EnhancedType<Set<StringSetMember>> type = EnhancedType.setOf(StringSetMember.class);

        assertThatThrownBy(() -> setProvider.converterFor(type))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(unsupportedSetMemberMessage(StringSetMember.class));
    }

    @Test
    @DisplayName("A custom NS member converter is rejected as a set member")
    void converterFor_whenNsAttributeTypeMemberSet_throwsIllegalArgumentException() {
        DefaultAttributeConverterProvider setProvider = providerWithDeclaredMemberConverter(
            NumberSetMember.class, AttributeValueType.NS);
        EnhancedType<Set<NumberSetMember>> type = EnhancedType.setOf(NumberSetMember.class);

        assertThatThrownBy(() -> setProvider.converterFor(type))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(unsupportedSetMemberMessage(NumberSetMember.class));
    }

    @Test
    @DisplayName("A custom BS member converter is rejected as a set member")
    void converterFor_whenBsAttributeTypeMemberSet_throwsIllegalArgumentException() {
        DefaultAttributeConverterProvider setProvider = providerWithDeclaredMemberConverter(
            BinarySetMember.class, AttributeValueType.BS);
        EnhancedType<Set<BinarySetMember>> type = EnhancedType.setOf(BinarySetMember.class);

        assertThatThrownBy(() -> setProvider.converterFor(type))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(unsupportedSetMemberMessage(BinarySetMember.class));
    }

    @Test
    @DisplayName("An Object set member fails converter lookup")
    void converterFor_whenObjectMemberSet_throwsConverterNotFound() {
        DefaultAttributeConverterProvider setProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Set<Object>> type = EnhancedType.setOf(Object.class);

        assertThatThrownBy(() -> setProvider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(Object.class));
    }

    @Test
    @DisplayName("A document class without a schema is an unsupported set member")
    void converterFor_whenDocumentClassWithoutSchemaSet_throwsIllegalStateException() {
        DefaultAttributeConverterProvider setProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Set<SetDocumentType>> type = EnhancedType.setOf(SetDocumentType.class);

        assertThatThrownBy(() -> setProvider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(SetDocumentType.class));
    }

    @Test
    @DisplayName("A wildcard set member fails while reading the raw class")
    void converterFor_whenWildcardSetMember_throwsIllegalArgumentException() {
        DefaultAttributeConverterProvider setProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Set<?>> type = new EnhancedType<Set<?>>() {
        };

        assertThatThrownBy(() -> setProvider.converterFor(type))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A wildcard type is not expected here.");
    }

    @Test
    @DisplayName("Collection of String uses set semantics and reads a LinkedHashSet")
    void converterFor_whenStringCollection_convertsToSsAndReadsLinkedHashSet() {
        DefaultAttributeConverterProvider setProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Collection<String>> type = EnhancedType.collectionOf(String.class);
        Collection<String> input = new ArrayList<>();
        input.add("a");
        input.add("b");

        AttributeConverter<Collection<String>> converter = setProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        Collection<String> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(SetAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.SS);
        assertThat(stored.ss()).containsExactly("a", "b");
        assertThat(read).isInstanceOf(LinkedHashSet.class)
                        .containsExactly("a", "b");
    }

    @Test
    @DisplayName("Iterable of String uses set semantics when the value is a LinkedHashSet")
    void converterFor_whenStringIterableWithLinkedHashSet_convertsToSsAndReadsLinkedHashSet() {
        DefaultAttributeConverterProvider setProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Iterable<String>> type = new EnhancedType<Iterable<String>>() {
        };
        Iterable<String> input = linkedSet("a", "b");

        AttributeConverter<Iterable<String>> converter = setProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        Iterable<String> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(SetAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.SS);
        assertThat(stored.ss()).containsExactly("a", "b");
        assertThat(read).isInstanceOf(LinkedHashSet.class)
                        .isEqualTo(linkedSet("a", "b"));
    }

    @Test
    @DisplayName("A non-Collection Iterable is selected as a set converter and fails while converting")
    void converterFor_whenNonCollectionIterable_throwsClassCastException() {
        DefaultAttributeConverterProvider setProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Iterable<String>> type = new EnhancedType<Iterable<String>>() {
        };
        Iterable<String> input = new NonCollectionIterable();

        AttributeConverter<Iterable<String>> converter = setProvider.converterFor(type);

        assertThat(converter).isInstanceOf(SetAttributeConverter.class);
        assertThatThrownBy(() -> converter.transformFrom(input))
            .isInstanceOf(ClassCastException.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unsupportedSetImplementationTypes")
    @DisplayName("Unsupported Set implementations fail converter lookup")
    void converterFor_whenUnsupportedSetImplementation_throwsIllegalStateException(EnhancedType<?> type) {
        DefaultAttributeConverterProvider setProvider = new DefaultAttributeConverterProvider();

        assertThatThrownBy(() -> setProvider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    private static Stream<EnhancedType<?>> unsupportedSetImplementationTypes() {
        return Stream.of(EnhancedType.sortedSetOf(String.class), EnhancedType.navigableSetOf(String.class),
                         new EnhancedType<SetCustomSet<String>>() {
                         }, new EnhancedType<LinkedHashSet<String>>() {
            }, new EnhancedType<TreeSet<String>>() {
            }, new EnhancedType<CopyOnWriteArraySet<String>>() {
            });
    }

    private static <T> DefaultAttributeConverterProvider providerWithDeclaredMemberConverter(
        Class<T> memberClass, AttributeValueType attributeValueType) {
        return DefaultAttributeConverterProvider.builder()
                                                .addConverter(new DeclaredTypeConverter<>(memberClass, attributeValueType))
                                                .build();
    }

    private static String unsupportedSetMemberMessage(Class<?> rawClass) {
        return "SetAttributeConverter cannot be created with a parameterized type of '" + rawClass
               + UNSUPPORTED_SET_MEMBER_SUFFIX;
    }

    @SafeVarargs
    private static <T> LinkedHashSet<T> linkedSet(T... values) {
        LinkedHashSet<T> result = new LinkedHashSet<>();
        result.addAll(Arrays.asList(values));
        return result;
    }

    private static final class DeclaredTypeConverter<T> implements AttributeConverter<T> {
        private final EnhancedType<T> type;
        private final AttributeValueType attributeValueType;

        DeclaredTypeConverter(Class<T> rawClass, AttributeValueType attributeValueType) {
            this.type = EnhancedType.of(rawClass);
            this.attributeValueType = attributeValueType;
        }

        @Override
        public AttributeValue transformFrom(T input) {
            throw new UnsupportedOperationException();
        }

        @Override
        public T transformTo(AttributeValue input) {
            throw new UnsupportedOperationException();
        }

        @Override
        public EnhancedType<T> type() {
            return type;
        }

        @Override
        public AttributeValueType attributeValueType() {
            return attributeValueType;
        }
    }

    static class SetDocumentType {
    }

    static class NullSetMember {
    }

    static class StringSetMember {
    }

    static class NumberSetMember {
    }

    static class BinarySetMember {
    }

    enum SetTestEnum {
        OPEN,
        CLOSED
    }

    static final class SetCustomSet<T> extends HashSet<T> {
    }

    static final class NonCollectionIterable implements Iterable<String> {
        @Override
        public Iterator<String> iterator() {
            return Collections.singletonList("a").iterator();
        }
    }

    // Map declarations, keys, values, and nested map values.
    @Test
    @DisplayName("String-to-Integer map round-trips as a LinkedHashMap")
    void converterFor_whenStringIntegerMap_roundTripsLinkedHashMap() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<String, Integer>> type = EnhancedType.mapOf(String.class, Integer.class);
        Map<String, Integer> input = new LinkedHashMap<>();
        input.put("one", 1);

        AttributeConverter<Map<String, Integer>> converter = mapProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        Map<String, Integer> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(MapAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(stored.m().get("one").n()).isEqualTo("1");
        assertThat(read).isInstanceOf(LinkedHashMap.class).isEqualTo(input);
    }

    @Test
    @DisplayName("An empty String-to-Integer map round-trips as an empty LinkedHashMap")
    void converterFor_whenEmptyStringIntegerMap_roundTripsEmptyLinkedHashMap() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<String, Integer>> type = EnhancedType.mapOf(String.class, Integer.class);
        Map<String, Integer> input = Collections.emptyMap();

        AttributeConverter<Map<String, Integer>> converter = mapProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        Map<String, Integer> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(MapAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(stored.m()).isEmpty();
        assertThat(read).isInstanceOf(LinkedHashMap.class).isEmpty();
    }

    @Test
    @DisplayName("UUID keys and Instant values convert independently through the map converter")
    void converterFor_whenUuidToInstantMap_roundTripsLinkedHashMap() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<UUID, Instant>> type = EnhancedType.mapOf(UUID.class, Instant.class);
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        Instant instant = Instant.parse("2020-01-02T03:04:05Z");
        Map<UUID, Instant> input = new LinkedHashMap<>();
        input.put(uuid, instant);

        AttributeConverter<Map<UUID, Instant>> converter = mapProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        Map<UUID, Instant> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(MapAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(stored.m()).hasSize(1);
        assertThat(stored.m().get(uuid.toString()).s()).isEqualTo("2020-01-02T03:04:05Z");
        assertThat(read).isInstanceOf(LinkedHashMap.class).isEqualTo(input);
    }

    @Test
    @DisplayName("Year is a valid map key through the string-converter registry")
    void converterFor_whenYearToStringMap_roundTripsLinkedHashMapWithYearKey() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<Year, String>> type = EnhancedType.mapOf(Year.class, String.class);
        Year year = Year.of(2026);
        Map<Year, String> input = new LinkedHashMap<>();
        input.put(year, "value");

        AttributeConverter<Map<Year, String>> converter = mapProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        Map<Year, String> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(MapAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(stored.m()).containsOnlyKeys("2026");
        assertThat(read).isInstanceOf(LinkedHashMap.class);
        assertThat(read).containsEntry(year, "value");
    }

    @Test
    @DisplayName("Direct Year lookup has no attribute converter")
    void converterFor_whenYearType_throwsConverterNotFound() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Year> type = EnhancedType.of(Year.class);

        assertThatThrownBy(() -> mapProvider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(Year.class));
    }

    @Test
    @DisplayName("SdkBytes is not a registered map-key string type")
    void converterFor_whenSdkBytesKeyMap_throwsNoStringConverter() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<SdkBytes, String>> type = EnhancedType.mapOf(SdkBytes.class, String.class);

        assertThatThrownBy(() -> mapProvider.converterFor(type))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No string converter exists for class software.amazon.awssdk.core.SdkBytes");
    }

    @Test
    @DisplayName("Enum generation does not apply to map keys")
    void converterFor_whenEnumKeyMap_throwsNoStringConverter() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<MapTestEnum, String>> type = EnhancedType.mapOf(MapTestEnum.class, String.class);

        assertThatThrownBy(() -> mapProvider.converterFor(type))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No string converter exists for " + MapTestEnum.class);
    }

    @Test
    @DisplayName("An attribute-only custom converter cannot enable a map key")
    void converterFor_whenAttributeOnlyKeyMap_throwsNoStringConverter() {
        DefaultAttributeConverterProvider mapProvider = DefaultAttributeConverterProvider.builder()
                                                                                         .addConverter(new AttributeOnlyKeyConverter())
                                                                                         .addConverter(StringAttributeConverter.create())
                                                                                         .build();
        EnhancedType<Map<AttributeOnlyKey, String>> type =
            EnhancedType.mapOf(AttributeOnlyKey.class, String.class);

        assertThatThrownBy(() -> mapProvider.converterFor(type))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No string converter exists for " + AttributeOnlyKey.class);
    }

    @Test
    @DisplayName("Object map keys have no string converter")
    void converterFor_whenObjectKeyMap_throwsNoStringConverter() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<Object, String>> type = EnhancedType.mapOf(Object.class, String.class);

        assertThatThrownBy(() -> mapProvider.converterFor(type))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No string converter exists for class java.lang.Object");
    }

    @Test
    @DisplayName("A nested map token cannot be used as a map key")
    void converterFor_whenMapTokenKey_throwsNoStringConverter() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<Map<String, String>, String>> type =
            EnhancedType.mapOf(EnhancedType.mapOf(String.class, String.class), EnhancedType.of(String.class));

        assertThatThrownBy(() -> mapProvider.converterFor(type))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No string converter exists for interface java.util.Map");
    }

    @Test
    @DisplayName("A list token cannot be used as a map key")
    void converterFor_whenListTokenKey_throwsNoStringConverter() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<List<String>, String>> type =
            EnhancedType.mapOf(EnhancedType.listOf(String.class), EnhancedType.of(String.class));

        assertThatThrownBy(() -> mapProvider.converterFor(type))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No string converter exists for interface java.util.List");
    }

    @Test
    @DisplayName("A set token cannot be used as a map key")
    void converterFor_whenSetTokenKey_throwsNoStringConverter() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<Set<String>, String>> type =
            EnhancedType.mapOf(EnhancedType.setOf(String.class), EnhancedType.of(String.class));

        assertThatThrownBy(() -> mapProvider.converterFor(type))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No string converter exists for interface java.util.Set");
    }

    @Test
    @DisplayName("A document token cannot be used as a map key")
    void converterFor_whenDocumentTokenKey_throwsNoStringConverter() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        TableSchema<MapDocumentType> mapDocumentSchema = mock(TableSchema.class);
        EnhancedType<Map<MapDocumentType, String>> type =
            EnhancedType.mapOf(EnhancedType.documentOf(MapDocumentType.class, mapDocumentSchema),
                               EnhancedType.of(String.class));

        assertThatThrownBy(() -> mapProvider.converterFor(type))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No string converter exists for " + MapDocumentType.class);
        verifyNoInteractions(mapDocumentSchema);
    }

    @Test
    @DisplayName("A wildcard map key fails during raw-class validation")
    void converterFor_whenWildcardKeyMap_throwsWildcardNotExpected() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<?, String>> type = new EnhancedType<Map<?, String>>() {
        };

        assertThatThrownBy(() -> mapProvider.converterFor(type))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A wildcard type is not expected here.");
    }

    @Test
    @DisplayName("An Object map value fails converter lookup")
    void converterFor_whenObjectValueMap_throwsConverterNotFound() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<String, Object>> type = EnhancedType.mapOf(String.class, Object.class);

        assertThatThrownBy(() -> mapProvider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(Object.class));
    }

    @Test
    @DisplayName("A document class without a schema is an unsupported map value")
    void converterFor_whenDocumentValueWithoutSchema_throwsConverterNotFound() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<String, MapDocumentType>> type = EnhancedType.mapOf(String.class, MapDocumentType.class);

        assertThatThrownBy(() -> mapProvider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(MapDocumentType.class));
    }

    @Test
    @DisplayName("A list-valued map preserves list order and duplicates")
    void converterFor_whenListValuedMap_roundTripsLinkedHashMapWithArrayList() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<String, List<String>>> type =
            EnhancedType.mapOf(EnhancedType.of(String.class), EnhancedType.listOf(String.class));
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("a");
        Map<String, List<String>> input = new LinkedHashMap<>();
        input.put("one", list);

        AttributeConverter<Map<String, List<String>>> converter = mapProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        Map<String, List<String>> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(MapAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(stored.m().get("one").l()).extracting(AttributeValue::s).containsExactly("a", "a");
        assertThat(read).isInstanceOf(LinkedHashMap.class);
        assertThat(read.get("one")).isInstanceOf(ArrayList.class).containsExactly("a", "a");
    }

    @Test
    @DisplayName("A set-valued map stores a string set and reads a LinkedHashSet")
    void converterFor_whenSetValuedMap_roundTripsLinkedHashMapWithLinkedHashSet() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<String, Set<String>>> type =
            EnhancedType.mapOf(EnhancedType.of(String.class), EnhancedType.setOf(String.class));
        Set<String> set = new LinkedHashSet<>();
        set.add("a");
        set.add("b");
        Map<String, Set<String>> input = new LinkedHashMap<>();
        input.put("one", set);

        AttributeConverter<Map<String, Set<String>>> converter = mapProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        Map<String, Set<String>> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(MapAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(stored.m().get("one").ss()).containsExactly("a", "b");
        assertThat(read).isInstanceOf(LinkedHashMap.class);
        assertThat(read.get("one")).isInstanceOf(LinkedHashSet.class).containsExactly("a", "b");
    }

    @Test
    @DisplayName("A map-valued map uses M at both levels and reads LinkedHashMap at both levels")
    void converterFor_whenMapValuedMap_roundTripsNestedLinkedHashMaps() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<String, Map<String, Integer>>> type =
            EnhancedType.mapOf(EnhancedType.of(String.class),
                               EnhancedType.mapOf(String.class, Integer.class));
        Map<String, Integer> inner = new LinkedHashMap<>();
        inner.put("one", 1);
        Map<String, Map<String, Integer>> input = new LinkedHashMap<>();
        input.put("nested", inner);

        AttributeConverter<Map<String, Map<String, Integer>>> converter = mapProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        Map<String, Map<String, Integer>> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(MapAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(stored.m().get("nested").m().get("one").n()).isEqualTo("1");
        assertThat(read).isInstanceOf(LinkedHashMap.class);
        assertThat(read.get("nested")).isInstanceOf(LinkedHashMap.class).isEqualTo(inner);
    }

    @Test
    @DisplayName("An enum-valued map stores the constant toString as S")
    void converterFor_whenEnumValuedMap_roundTripsLinkedHashMap() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<String, MapTestEnum>> type = EnhancedType.mapOf(String.class, MapTestEnum.class);
        Map<String, MapTestEnum> input = new LinkedHashMap<>();
        input.put("state", MapTestEnum.OPEN);

        AttributeConverter<Map<String, MapTestEnum>> converter = mapProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        Map<String, MapTestEnum> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(MapAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(stored.m().get("state").s()).isEqualTo(MapTestEnum.OPEN.toString());
        assertThat(read).isInstanceOf(LinkedHashMap.class).containsEntry("state", MapTestEnum.OPEN);
    }

    @Test
    @DisplayName("A document-valued map delegates both conversions to the table schema")
    void converterFor_whenDocumentValuedMap_invokesSchemaAndRoundTripsLinkedHashMap() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        TableSchema<MapDocumentType> schema = mapDocumentSchema();
        MapDocumentType inputDocument = new MapDocumentType();
        inputDocument.setName("doc");
        EnhancedType<Map<String, MapDocumentType>> type =
            EnhancedType.mapOf(EnhancedType.of(String.class),
                               EnhancedType.documentOf(MapDocumentType.class, schema));
        Map<String, MapDocumentType> input = new LinkedHashMap<>();
        input.put("doc", inputDocument);

        AttributeConverter<Map<String, MapDocumentType>> converter = mapProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        Map<String, MapDocumentType> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(MapAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(stored.m().get("doc").m()).containsEntry("name", AttributeValue.fromS("doc"));
        assertThat(read).isInstanceOf(LinkedHashMap.class);
        assertThat(read.get("doc").getName()).isEqualTo("doc");
    }

    @Test
    @DisplayName("A map whose value is a list of sets nests M, L, then SS")
    void converterFor_whenListOfSetsValuedMap_roundTripsNestedCollectionClasses() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<String, List<Set<String>>>> type =
            EnhancedType.mapOf(EnhancedType.of(String.class),
                               EnhancedType.listOf(EnhancedType.setOf(String.class)));
        Set<String> set = new LinkedHashSet<>();
        set.add("a");
        set.add("b");
        List<Set<String>> list = new ArrayList<>();
        list.add(set);
        Map<String, List<Set<String>>> input = new LinkedHashMap<>();
        input.put("one", list);

        AttributeConverter<Map<String, List<Set<String>>>> converter = mapProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        Map<String, List<Set<String>>> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(MapAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(stored.m().get("one").l()).hasSize(1);
        assertThat(stored.m().get("one").l().get(0).ss()).containsExactly("a", "b");
        assertThat(read).isInstanceOf(LinkedHashMap.class);
        assertThat(read.get("one")).isInstanceOf(ArrayList.class);
        assertThat(read.get("one").get(0)).isInstanceOf(LinkedHashSet.class).containsExactly("a", "b");
    }

    @Test
    @DisplayName("A null Java map key is rejected while wrapping the stored map")
    void converterFor_whenNullMapKey_throwsMapMustNotHaveNullKeys() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<String, String>> type = EnhancedType.mapOf(String.class, String.class);
        Map<String, String> input = new HashMap<>();
        input.put(null, "value");
        AttributeConverter<Map<String, String>> converter = mapProvider.converterFor(type);

        assertThatThrownBy(() -> converter.transformFrom(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Map must not have null keys.");
    }

    @Test
    @DisplayName("A null string map value is stored as DynamoDB NULL")
    void converterFor_whenNullStringMapValue_roundTripsNullValue() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<String, String>> type = EnhancedType.mapOf(String.class, String.class);
        Map<String, String> input = new LinkedHashMap<>();
        input.put("key", null);

        AttributeConverter<Map<String, String>> converter = mapProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        Map<String, String> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(MapAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(stored.m().get("key").nul()).isTrue();
        assertThat(read).isInstanceOf(LinkedHashMap.class);
        assertThat(read).containsEntry("key", null);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unsupportedMapImplementationTypes")
    @DisplayName("Unsupported Map implementations fail converter lookup")
    void converterFor_whenUnsupportedMapImplementation_throwsConverterNotFound(EnhancedType<?> type) {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();

        assertThatThrownBy(() -> mapProvider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("A nested list of Object fails converter lookup for the inner Object")
    void converterFor_whenNestedListOfObject_throwsConverterNotFoundForInnerObject() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<List<Object>> listType = EnhancedType.listOf(Object.class);
        EnhancedType<Map<String, List<Object>>> type =
            EnhancedType.mapOf(EnhancedType.of(String.class), listType);

        assertThatThrownBy(() -> mapProvider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(Object.class));
    }

    @Test
    @DisplayName("A nested list of an unsupported member reports that member in the error")
    void converterFor_whenNestedListOfUnsupportedType_throwsConverterNotFoundForMember() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<String, List<MapUnsupportedType>>> type =
            EnhancedType.mapOf(EnhancedType.of(String.class), EnhancedType.listOf(MapUnsupportedType.class));

        assertThatThrownBy(() -> mapProvider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(MapUnsupportedType.class));
    }

    @Test
    @DisplayName("A raw nested Map value fails converter lookup")
    void converterFor_whenRawNestedMapValue_throwsConverterNotFound() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<String, Map>> type =
            EnhancedType.mapOf(EnhancedType.of(String.class), EnhancedType.of(Map.class));

        assertThatThrownBy(() -> mapProvider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(Map.class)
                        + ". Type parameters are required for this type.");
    }

    @Test
    @DisplayName("A wildcard map value fails during nested raw-class validation")
    void converterFor_whenWildcardValueMap_throwsWildcardNotExpected() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<String, ?>> type = new EnhancedType<Map<String, ?>>() {
        };

        assertThatThrownBy(() -> mapProvider.converterFor(type))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A wildcard type is not expected here.");
    }

    @Test
    @DisplayName("An integer map key is stored as the decimal string name")
    void converterFor_whenIntegerKeyMap_roundTripsLinkedHashMap() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<Integer, String>> type = EnhancedType.mapOf(Integer.class, String.class);
        Map<Integer, String> input = new LinkedHashMap<>();
        input.put(7, "value");

        AttributeConverter<Map<Integer, String>> converter = mapProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        Map<Integer, String> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(MapAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(stored.m()).containsOnlyKeys("7");
        assertThat(read).isInstanceOf(LinkedHashMap.class).containsEntry(7, "value");
    }

    @Test
    @DisplayName("A byte-array map key is stored as Base64 text")
    void converterFor_whenByteArrayKeyMap_roundTripsBase64Key() {
        DefaultAttributeConverterProvider mapProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<byte[], String>> type = EnhancedType.mapOf(byte[].class, String.class);
        Map<byte[], String> input = new LinkedHashMap<>();
        input.put(new byte[] {1, 2}, "value");

        AttributeConverter<Map<byte[], String>> converter = mapProvider.converterFor(type);
        AttributeValue stored = converter.transformFrom(input);
        Map<byte[], String> read = converter.transformTo(stored);

        assertThat(converter).isInstanceOf(MapAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(stored.m()).containsOnlyKeys("AQI=");
        assertThat(read).isInstanceOf(LinkedHashMap.class).hasSize(1);
        assertThat(read.keySet().iterator().next()).containsExactly((byte) 1, (byte) 2);
        assertThat(read.values()).containsExactly("value");
    }

    private static Stream<EnhancedType<?>> unsupportedMapImplementationTypes() {
        return Stream.of(EnhancedType.sortedMapOf(String.class, Integer.class),
                         EnhancedType.navigableMapOf(String.class, Integer.class),
                         EnhancedType.concurrentMapOf(String.class, Integer.class),
                         new EnhancedType<MapCustomMap<String, Integer>>() {
                         }, new EnhancedType<LinkedHashMap<String, Integer>>() {
            }, new EnhancedType<TreeMap<String, Integer>>() {
            }, new EnhancedType<ConcurrentHashMap<String, Integer>>() {
            }, new EnhancedType<EnumMap<MapTestEnum, String>>() {
            });
    }

    static class MapUnsupportedType {
    }

    private static TableSchema<MapDocumentType> mapDocumentSchema() {
        return StaticTableSchema.builder(MapDocumentType.class)
                                .newItemSupplier(MapDocumentType::new)
                                .addAttribute(String.class, a -> a.name("name")
                                                                  .getter(MapDocumentType::getName)
                                                                  .setter(MapDocumentType::setName))
                                .build();
    }

    static class MapDocumentType {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    enum MapTestEnum {
        OPEN,
        CLOSED
    }

    static class AttributeOnlyKey {
    }

    static class MapCustomMap<K, V> extends HashMap<K, V> {
    }

    static final class AttributeOnlyKeyConverter implements AttributeConverter<AttributeOnlyKey> {
        @Override
        public AttributeValue transformFrom(AttributeOnlyKey input) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AttributeOnlyKey transformTo(AttributeValue input) {
            throw new UnsupportedOperationException();
        }

        @Override
        public EnhancedType<AttributeOnlyKey> type() {
            return EnhancedType.of(AttributeOnlyKey.class);
        }

        @Override
        public AttributeValueType attributeValueType() {
            return AttributeValueType.S;
        }
    }
}

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

package software.amazon.awssdk.enhanced.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.SetAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Tests converter behavior after lookup and the type tokens used to request converters.
 * <p>
 * The tests cover unsupported type shapes, incorrect DynamoDB attribute forms, Java null values, DynamoDB null values,
 * and enumeration conversion. They also verify propagation of failures from collection members and table schemas,
 * including document conversion options passed to a schema.
 */
public class DefaultAttributeConverterProviderConversionTest {

    @Test
    @DisplayName("Null class fails during token construction before provider lookup")
    void of_whenNullClass_throwsIllegalArgumentExceptionBeforeProviderLookup() {
        assertThatThrownBy(() -> EnhancedType.of((Class<Object>) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("class java.lang.Object isn't parameterized");
    }

    @Test
    @DisplayName("Unbounded wildcard construction succeeds and later rawClass fails")
    void of_whenUnboundedWildcardType_isWildcardAndRawClassThrowsIllegalArgumentException() {
        EnhancedType<?> type = EnhancedType.of(wildcardTypeArgument("unbounded"));

        assertThat(type.isWildcard()).isTrue();
        assertThat(type.toString()).isEqualTo("EnhancedType(?)");
        assertThatThrownBy(type::rawClass)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A wildcard type is not expected here.");
    }

    @Test
    @DisplayName("Upper-bounded wildcard construction succeeds and later rawClass fails")
    void of_whenUpperBoundedWildcardType_isWildcardAndRawClassThrowsIllegalArgumentException() {
        EnhancedType<?> type = EnhancedType.of(wildcardTypeArgument("upperBounded"));

        assertThat(type.isWildcard()).isTrue();
        assertThatThrownBy(type::rawClass)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A wildcard type is not expected here.");
    }

    @Test
    @DisplayName("Lower-bounded wildcard construction succeeds and later rawClass fails")
    void of_whenLowerBoundedWildcardType_isWildcardAndRawClassThrowsIllegalArgumentException() {
        EnhancedType<?> type = EnhancedType.of(wildcardTypeArgument("lowerBounded"));

        assertThat(type.isWildcard()).isTrue();
        assertThatThrownBy(type::rawClass)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A wildcard type is not expected here.");
    }

    @Test
    @DisplayName("Type variable fails during construction")
    void of_whenTypeVariable_throwsIllegalStateException() {
        assertThatThrownBy(() -> EnhancedType.of(genericHolderFieldType("typeVariable")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Type variable type T is not supported.");
    }

    @Test
    @DisplayName("Generic array fails during construction")
    void of_whenGenericArrayType_throwsIllegalStateException() {
        assertThatThrownBy(() -> EnhancedType.of(genericHolderFieldType("genericArray")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Array type T[] is not supported. Use java.util.List instead of arrays.");
    }

    @Test
    @DisplayName("Parameterized Optional has no default converter")
    void converterFor_whenOptionalOfString_throwsIllegalStateException() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        EnhancedType<?> type = EnhancedType.optionalOf(String.class);

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("Primitive int array has no default converter")
    void converterFor_whenIntArray_throwsIllegalStateException() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        EnhancedType<int[]> type = EnhancedType.of(int[].class);

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(int[].class));
    }

    @Test
    @DisplayName("String array has no default converter")
    void converterFor_whenStringArray_throwsIllegalStateException() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        EnhancedType<String[]> type = EnhancedType.of(String[].class);

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(String[].class));
    }

    @Test
    @DisplayName("Abstract Number has no exact registration")
    void converterFor_whenNumberClass_throwsIllegalStateException() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        EnhancedType<Number> type = EnhancedType.of(Number.class);

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(Number.class));
    }

    @Test
    @DisplayName("Unsupported interface reaches normal lookup failure")
    void converterFor_whenUnsupportedInterface_throwsIllegalStateException() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        EnhancedType<UnsupportedInterface> type = EnhancedType.of(UnsupportedInterface.class);

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("Unsupported parameterized application type reaches normal failure")
    void converterFor_whenCapturedEnvelopeString_throwsIllegalStateException() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        EnhancedType<Envelope<String>> type = new EnhancedType<Envelope<String>>() {
        };

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("A mappable class alone is not a document token")
    void converterFor_whenDocumentTypeWithoutAttachedSchema_throwsIllegalStateException() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        EnhancedType<DocumentType> type = EnhancedType.of(DocumentType.class);

        assertThat(type.tableSchema()).isEmpty();
        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("Unsupported abstract class reaches normal lookup failure")
    void converterFor_whenAbstractUnsupportedType_throwsIllegalStateException() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        EnhancedType<AbstractUnsupportedType> type = EnhancedType.of(AbstractUnsupportedType.class);

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("Map converter rejects an N input")
    void transformTo_whenMapConverterWithNumberAttribute_throwsIllegalStateException() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        AttributeConverter<Map<String, Integer>> converter =
            provider.converterFor(EnhancedType.mapOf(String.class, Integer.class));

        assertThatThrownBy(() -> converter.transformTo(AttributeValue.fromN("1")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute."
                        + "MapAttributeConverter cannot convert an attribute of type N into the requested type "
                        + "interface java.util.Map");
    }

    @Test
    @DisplayName("List converter rejects an M input")
    void transformTo_whenListConverterWithEmptyMapAttribute_throwsIllegalStateException() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        AttributeConverter<List<String>> converter = provider.converterFor(EnhancedType.listOf(String.class));

        assertThatThrownBy(() -> converter.transformTo(AttributeValue.fromM(Collections.emptyMap())))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute."
                        + "ListAttributeConverter cannot convert an attribute of type M into the requested type "
                        + "interface java.util.List");
    }

    @Test
    @DisplayName("Set converter rejects a BOOL input")
    void transformTo_whenSetConverterWithBooleanAttribute_throwsIllegalStateException() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        AttributeConverter<Set<String>> converter = provider.converterFor(EnhancedType.setOf(String.class));

        assertThatThrownBy(() -> converter.transformTo(AttributeValue.fromBool(true)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute."
                        + "SetAttributeConverter cannot convert an attribute of type BOOL into the requested type "
                        + "interface java.util.Set");
    }

    @Test
    @DisplayName("List converter also reads SS input")
    void transformTo_whenListConverterWithStringSetAttribute_returnsArrayListContainingValuesInOrder() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        AttributeConverter<List<String>> converter = provider.converterFor(EnhancedType.listOf(String.class));

        List<String> converted = converter.transformTo(AttributeValue.fromSs(Arrays.asList("a", "b")));

        assertThat(converted).isInstanceOf(ArrayList.class).containsExactly("a", "b");
    }

    @Test
    @DisplayName("Set converter also reads L input")
    void transformTo_whenSetConverterWithListAttribute_returnsLinkedHashSetContainingValuesInOrder() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        AttributeConverter<Set<String>> converter = provider.converterFor(EnhancedType.setOf(String.class));
        AttributeValue input = AttributeValue.fromL(Arrays.asList(AttributeValue.fromS("a"),
                                                                  AttributeValue.fromS("b")));

        Set<String> converted = converter.transformTo(input);

        assertThat(converted).isInstanceOf(LinkedHashSet.class).containsExactly("a", "b");
    }

    @Test
    @DisplayName("Document converter passes a wrong input type as an empty map")
    void transformTo_whenDocumentConverterWithNumberAttribute_readsEmptyAutoConstructedMap() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        RecordingDocumentSchema schema = new RecordingDocumentSchema();
        EnhancedType<DocumentType> type = EnhancedType.documentOf(DocumentType.class, schema);
        AttributeValue input = AttributeValue.fromN("1");

        DocumentType converted = provider.converterFor(type).transformTo(input);

        assertThat(input.m()).isEmpty();
        assertThat(schema.lastMapToItemMap()).isSameAs(input.m());
        assertThat(schema.lastMapToItemPreserveEmptyObject()).isFalse();
        assertThat(converted).isNull();
    }

    @Test
    @DisplayName("Integer converter exposes its current wrong-type visitor target")
    void transformTo_whenIntegerConverterWithBooleanAttribute_throwsIllegalStateExceptionForInstant() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        AttributeConverter<Integer> converter = provider.converterFor(EnhancedType.of(Integer.class));

        assertThatThrownBy(() -> converter.transformTo(AttributeValue.fromBool(true)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute."
                        + "IntegerAttributeConverter cannot convert an attribute of type BOOL into the requested type "
                        + "class java.time.Instant");
    }

    @Test
    @DisplayName("Enum converter rejects a non-string input")
    void transformTo_whenEnumConverterWithNumberAttribute_throwsIllegalArgumentException() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        AttributeConverter<TestEnum> converter = provider.converterFor(EnhancedType.of(TestEnum.class));

        assertThatThrownBy(() -> converter.transformTo(AttributeValue.fromN("1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot convert non-string value to enum.");
    }

    @Test
    @DisplayName("Enum converter rejects unknown text")
    void transformTo_whenEnumConverterWithUnknownText_throwsIllegalArgumentException() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        AttributeConverter<TestEnum> converter = provider.converterFor(EnhancedType.of(TestEnum.class));

        assertThatThrownBy(() -> converter.transformTo(AttributeValue.fromS("missing")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unable to convert string value 'missing' to enum type '" + TestEnum.class + "'");
    }

    @Test
    @DisplayName("Generated enum converter persists toString()")
    void transformFrom_whenLowercaseEnumOpen_roundTripsOpenString() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        AttributeConverter<LowercaseEnum> converter =
            provider.converterFor(EnhancedType.of(LowercaseEnum.class));

        assertThat(LowercaseEnum.OPEN.toString()).isEqualTo("open");
        assertThat(converter).isInstanceOf(EnumAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(converter.transformFrom(LowercaseEnum.OPEN).s()).isEqualTo("open");
        assertThat(converter.transformTo(AttributeValue.fromS("open"))).isEqualTo(LowercaseEnum.OPEN);
    }

    @Test
    @DisplayName("Enum name text is not accepted when toString() differs")
    void transformTo_whenLowercaseEnumWithNameText_throwsIllegalArgumentException() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        AttributeConverter<LowercaseEnum> converter =
            provider.converterFor(EnhancedType.of(LowercaseEnum.class));

        assertThatThrownBy(() -> converter.transformTo(AttributeValue.fromS("OPEN")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unable to convert string value 'OPEN' to enum type '" + LowercaseEnum.class + "'");
    }

    @Test
    @DisplayName("String converter maps null Java input to NULL")
    void transformFrom_whenStringConverterWithNullInput_returnsNullAttribute() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        AttributeConverter<String> converter = provider.converterFor(EnhancedType.of(String.class));

        assertThat(converter.transformFrom(null)).isEqualTo(AttributeValue.fromNul(true));
    }

    @Test
    @DisplayName("Map converter rejects null Java input")
    void transformFrom_whenMapConverterWithNullInput_throwsNullPointerException() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        AttributeConverter<Map<String, Integer>> converter =
            provider.converterFor(EnhancedType.mapOf(String.class, Integer.class));

        assertThatThrownBy(() -> converter.transformFrom(null))
            .isInstanceOf(NullPointerException.class)
            .satisfies(ex -> assertThat(ex.getMessage() == null || ex.getMessage().contains("null")).isTrue());
    }

    @Test
    @DisplayName("Set converter rejects null Java input")
    void transformFrom_whenSetConverterWithNullInput_throwsNullPointerException() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        AttributeConverter<Set<String>> converter = provider.converterFor(EnhancedType.setOf(String.class));

        assertThatThrownBy(() -> converter.transformFrom(null))
            .isInstanceOf(NullPointerException.class)
            .satisfies(ex -> assertThat(ex.getMessage() == null || ex.getMessage().contains("null")).isTrue());
    }

    @Test
    @DisplayName("List converter rejects null Java input")
    void transformFrom_whenListConverterWithNullInput_throwsNullPointerException() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        AttributeConverter<List<String>> converter = provider.converterFor(EnhancedType.listOf(String.class));

        assertThatThrownBy(() -> converter.transformFrom(null))
            .isInstanceOf(NullPointerException.class)
            .satisfies(ex -> assertThat(ex.getMessage() == null || ex.getMessage().contains("null")).isTrue());
    }

    @Test
    @DisplayName("Enum converter rejects null Java input")
    void transformFrom_whenEnumConverterWithNullInput_throwsNullPointerException() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        AttributeConverter<TestEnum> converter = provider.converterFor(EnhancedType.of(TestEnum.class));

        assertThatThrownBy(() -> converter.transformFrom(null))
            .isInstanceOf(NullPointerException.class)
            .satisfies(ex -> assertThat(ex.getMessage() == null || ex.getMessage().contains("null")).isTrue());
    }

    @Test
    @DisplayName("Map converter rejects null AttributeValue input")
    void transformTo_whenMapConverterWithNullAttribute_throwsNullPointerException() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        AttributeConverter<Map<String, Integer>> converter =
            provider.converterFor(EnhancedType.mapOf(String.class, Integer.class));

        assertThatThrownBy(() -> converter.transformTo(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Generated attribute value must not contain null values. Use AttributeValue#nul() instead.");
    }

    @Test
    @DisplayName("List converter rejects null AttributeValue input")
    void transformTo_whenListConverterWithNullAttribute_throwsNullPointerException() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        AttributeConverter<List<String>> converter = provider.converterFor(EnhancedType.listOf(String.class));

        assertThatThrownBy(() -> converter.transformTo(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Generated attribute value must not contain null values. Use AttributeValue#nul() instead.");
    }

    @Test
    @DisplayName("Set converter rejects null AttributeValue input")
    void transformTo_whenSetConverterWithNullAttribute_throwsNullPointerException() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        AttributeConverter<Set<String>> converter = provider.converterFor(EnhancedType.setOf(String.class));

        assertThatThrownBy(() -> converter.transformTo(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Generated attribute value must not contain null values. Use AttributeValue#nul() instead.");
    }

    @Test
    @DisplayName("Map converter reads DynamoDB NULL as Java null")
    void transformTo_whenMapConverterWithNullAttributeValue_returnsNull() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        AttributeConverter<Map<String, Integer>> converter =
            provider.converterFor(EnhancedType.mapOf(String.class, Integer.class));

        assertThat(converter.transformTo(AttributeValue.fromNul(true))).isNull();
    }

    @Test
    @DisplayName("List converter reads DynamoDB NULL as Java null")
    void transformTo_whenListConverterWithNullAttributeValue_returnsNull() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        AttributeConverter<List<String>> converter = provider.converterFor(EnhancedType.listOf(String.class));

        assertThat(converter.transformTo(AttributeValue.fromNul(true))).isNull();
    }

    @Test
    @DisplayName("Set converter reads DynamoDB NULL as Java null")
    void transformTo_whenSetConverterWithNullAttributeValue_returnsNull() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        AttributeConverter<Set<String>> converter = provider.converterFor(EnhancedType.setOf(String.class));

        assertThat(converter.transformTo(AttributeValue.fromNul(true))).isNull();
    }

    @Test
    @DisplayName("Primitive optional reads DynamoDB NULL as empty")
    void transformTo_whenOptionalIntConverterWithNullAttributeValue_returnsEmpty() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        AttributeConverter<OptionalInt> converter = provider.converterFor(EnhancedType.of(OptionalInt.class));

        assertThat(converter.transformTo(AttributeValue.fromNul(true))).isEqualTo(OptionalInt.empty());
    }

    @Test
    @DisplayName("Map value converter write failure propagates")
    void transformFrom_whenMapOfThrowingType_throwsSameValueWriteException() {
        ThrowingTypeConverter throwingConverter =
            new ThrowingTypeConverter("Map value converter failed while writing ThrowingType", "unused read message");
        DefaultAttributeConverterProvider provider = providerWith(throwingConverter);
        Map<String, ThrowingType> input = Collections.singletonMap("key", new ThrowingType());

        assertThatThrownBy(() -> provider.converterFor(EnhancedType.mapOf(String.class, ThrowingType.class))
                                         .transformFrom(input))
            .isSameAs(throwingConverter.fromFailure());
    }

    @Test
    @DisplayName("Map value converter read failure propagates")
    void transformTo_whenMapOfThrowingType_throwsSameValueReadException() {
        ThrowingTypeConverter throwingConverter =
            new ThrowingTypeConverter("unused write message", "Map value converter failed while reading ThrowingType");
        DefaultAttributeConverterProvider provider = providerWith(throwingConverter);
        AttributeValue mapAttribute =
            AttributeValue.fromM(Collections.singletonMap("key", AttributeValue.fromS("x")));

        assertThatThrownBy(() -> provider.converterFor(EnhancedType.mapOf(String.class, ThrowingType.class))
                                         .transformTo(mapAttribute))
            .isSameAs(throwingConverter.toFailure());
    }

    @Test
    @DisplayName("List member converter write failure propagates")
    void transformFrom_whenListOfThrowingType_throwsSameMemberWriteException() {
        ThrowingTypeConverter throwingConverter =
            new ThrowingTypeConverter("List member converter failed while writing ThrowingType", "unused read message");
        DefaultAttributeConverterProvider provider = providerWith(throwingConverter);
        List<ThrowingType> input = Collections.singletonList(new ThrowingType());

        assertThatThrownBy(() -> provider.converterFor(EnhancedType.listOf(ThrowingType.class))
                                         .transformFrom(input))
            .isSameAs(throwingConverter.fromFailure());
    }

    @Test
    @DisplayName("List member converter read failure propagates")
    void transformTo_whenListOfThrowingType_throwsSameMemberReadException() {
        ThrowingTypeConverter throwingConverter =
            new ThrowingTypeConverter("unused write message", "List member converter failed while reading ThrowingType");
        DefaultAttributeConverterProvider provider = providerWith(throwingConverter);
        AttributeValue listAttribute =
            AttributeValue.fromL(Collections.singletonList(AttributeValue.fromS("x")));

        assertThatThrownBy(() -> provider.converterFor(EnhancedType.listOf(ThrowingType.class))
                                         .transformTo(listAttribute))
            .isSameAs(throwingConverter.toFailure());
    }

    @Test
    @DisplayName("Set member converter write failure propagates")
    void transformFrom_whenSetOfThrowingType_throwsSameSetWriteException() {
        ThrowingTypeConverter throwingConverter =
            new ThrowingTypeConverter("Set member converter failed while writing ThrowingType", "unused read message");
        DefaultAttributeConverterProvider provider = providerWith(throwingConverter);
        Set<ThrowingType> input = Collections.singleton(new ThrowingType());

        assertThatThrownBy(() -> provider.converterFor(EnhancedType.setOf(ThrowingType.class))
                                         .transformFrom(input))
            .isSameAs(throwingConverter.fromFailure());
    }

    @Test
    @DisplayName("Set member converter read failure propagates")
    void transformTo_whenSetOfThrowingType_throwsSameSetReadException() {
        ThrowingTypeConverter throwingConverter =
            new ThrowingTypeConverter("unused write message", "Set member converter failed while reading ThrowingType");
        DefaultAttributeConverterProvider provider = providerWith(throwingConverter);
        AttributeValue stringSetAttribute = AttributeValue.fromSs(Collections.singletonList("x"));

        assertThatThrownBy(() -> provider.converterFor(EnhancedType.setOf(ThrowingType.class))
                                         .transformTo(stringSetAttribute))
            .isSameAs(throwingConverter.toFailure());
    }

    @Test
    @DisplayName("Set member declaring S but returning NULL fails while flattening")
    void transformFrom_whenSetOfMisreportingStringMember_throwsAttributeValueMustBeS() {
        DefaultAttributeConverterProvider provider = providerWith(new MisreportingStringMemberConverter());
        EnhancedType<Set<MisreportingStringMember>> type = EnhancedType.setOf(MisreportingStringMember.class);
        Set<MisreportingStringMember> input = Collections.singleton(new MisreportingStringMember());

        AttributeConverter<Set<MisreportingStringMember>> converter = provider.converterFor(type);

        assertThat(converter).isInstanceOf(SetAttributeConverter.class);
        assertThatThrownBy(() -> converter.transformFrom(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attribute value must be S.");
    }

    @Test
    @DisplayName("Document schema write failure propagates")
    void transformFrom_whenDocumentConverterItemToMapThrows_throwsSameSchemaWriteException() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        TableSchema<ThrowingWriteDocument> throwingWriteSchema = throwingWriteSchema();
        EnhancedType<ThrowingWriteDocument> type =
            EnhancedType.documentOf(ThrowingWriteDocument.class, throwingWriteSchema);

        assertThatThrownBy(() -> provider.converterFor(type).transformFrom(new ThrowingWriteDocument()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Document schema failed while converting ThrowingWriteDocument to a map");
    }

    @Test
    @DisplayName("Document schema read failure propagates")
    void transformTo_whenDocumentConverterMapToItemThrows_throwsSameSchemaReadException() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        TableSchema<ThrowingReadDocument> throwingReadSchema = throwingReadSchema();
        EnhancedType<ThrowingReadDocument> type =
            EnhancedType.documentOf(ThrowingReadDocument.class, throwingReadSchema);
        AttributeValue mapAttribute =
            AttributeValue.fromM(Collections.singletonMap("name", AttributeValue.fromS("x")));

        assertThatThrownBy(() -> provider.converterFor(type).transformTo(mapAttribute))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Document schema failed while converting a map to ThrowingReadDocument");
    }

    @Test
    @DisplayName("Document converter delegates null Java input to its schema")
    void transformFrom_whenDocumentConverterWithNullInput_recordsNullAndFalseAndStoresEmptyMap() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        RecordingDocumentSchema schema = new RecordingDocumentSchema();
        EnhancedType<DocumentType> type = EnhancedType.documentOf(DocumentType.class, schema);

        AttributeValue stored = provider.converterFor(type).transformFrom(null);

        assertThat(schema.lastItemToMapItem()).isNull();
        assertThat(schema.lastItemToMapIgnoreNulls()).isFalse();
        assertThat(stored.hasM()).isTrue();
        assertThat(stored.m()).isEmpty();
    }

    @Test
    @DisplayName("Integer converter rejects null Java input")
    void transformFrom_whenIntegerConverterWithNullInput_throwsNullPointerException() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        AttributeConverter<Integer> converter = provider.converterFor(EnhancedType.of(Integer.class));

        assertThatThrownBy(() -> converter.transformFrom(null))
            .isInstanceOf(NullPointerException.class)
            .satisfies(ex -> assertThat(ex.getMessage() == null || ex.getMessage().contains("null")).isTrue());
    }

    @Test
    @DisplayName("Null integer map value propagates member-converter failure")
    void transformFrom_whenStringToIntegerMapWithNullValue_throwsNullPointerException() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        AttributeConverter<Map<String, Integer>> converter =
            provider.converterFor(EnhancedType.mapOf(String.class, Integer.class));
        Map<String, Integer> input = new HashMap<>();
        input.put("key", null);

        assertThatThrownBy(() -> converter.transformFrom(input))
            .isInstanceOf(NullPointerException.class)
            .satisfies(ex -> assertThat(ex.getMessage() == null || ex.getMessage().contains("null")).isTrue());
    }

    @Test
    @DisplayName("Document converter forwards ignoreNulls")
    void transformFrom_whenDocumentConverterWithIgnoreNulls_invokesItemToMapWithTrueAndWrapsMap() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        RecordingDocumentSchema schema = new RecordingDocumentSchema();
        Map<String, AttributeValue> schemaMap =
            Collections.singletonMap("name", AttributeValue.fromS("doc"));
        schema.setItemToMapResult(schemaMap);
        DocumentType input = new DocumentType();
        EnhancedType<DocumentType> type =
            EnhancedType.documentOf(DocumentType.class, schema, b -> b.ignoreNulls(true));

        AttributeValue stored = provider.converterFor(type).transformFrom(input);

        assertThat(schema.lastItemToMapItem()).isSameAs(input);
        assertThat(schema.lastItemToMapIgnoreNulls()).isTrue();
        assertThat(stored.hasM()).isTrue();
        assertThat(stored.m()).isEqualTo(schemaMap);
    }

    @Test
    @DisplayName("Document converter forwards preserveEmptyObject")
    void transformTo_whenDocumentConverterWithPreserveEmptyObject_invokesMapToItemWithTrue() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        RecordingDocumentSchema schema = new RecordingDocumentSchema();
        DocumentType reconstructed = new DocumentType();
        schema.setMapToItemResult(reconstructed);
        AttributeValue input = AttributeValue.fromM(Collections.emptyMap());
        EnhancedType<DocumentType> type =
            EnhancedType.documentOf(DocumentType.class, schema, b -> b.preserveEmptyObject(true));

        DocumentType converted = provider.converterFor(type).transformTo(input);

        assertThat(schema.lastMapToItemMap()).isSameAs(input.m());
        assertThat(schema.lastMapToItemPreserveEmptyObject()).isTrue();
        assertThat(converted).isSameAs(reconstructed);
    }

    @Test
    @DisplayName("Null AttributeValue from a list member converter is rejected")
    void transformFrom_whenListWithNullReturningMember_throwsListMustNotHaveNullValues() {
        DefaultAttributeConverterProvider provider = providerWith(new NullReturningMemberConverter());
        List<NullReturningMember> input = Collections.singletonList(new NullReturningMember());

        assertThatThrownBy(() -> provider.converterFor(EnhancedType.listOf(NullReturningMember.class))
                                         .transformFrom(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("List must not have null values.");
    }

    private static DefaultAttributeConverterProvider providerWith(AttributeConverter<?> converter) {
        return DefaultAttributeConverterProvider.builder().addConverter(converter).build();
    }

    private static TableSchema<ThrowingWriteDocument> throwingWriteSchema() {
        return StaticTableSchema.builder(ThrowingWriteDocument.class)
                                .newItemSupplier(ThrowingWriteDocument::new)
                                .addAttribute(String.class, a -> a.name("name")
                                                                  .getter(ThrowingWriteDocument::getName)
                                                                  .setter(ThrowingWriteDocument::setName))
                                .build();
    }

    private static TableSchema<ThrowingReadDocument> throwingReadSchema() {
        return StaticTableSchema.builder(ThrowingReadDocument.class)
                                .newItemSupplier(ThrowingReadDocument::new)
                                .addAttribute(String.class, a -> a.name("name")
                                                                  .getter(ThrowingReadDocument::getName)
                                                                  .setter(ThrowingReadDocument::setName))
                                .build();
    }

    private static Type wildcardTypeArgument(String fieldName) {
        try {
            Type genericType = WildcardHolder.class.getDeclaredField(fieldName).getGenericType();
            return ((ParameterizedType) genericType).getActualTypeArguments()[0];
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Type genericHolderFieldType(String fieldName) {
        try {
            return GenericHolder.class.getDeclaredField(fieldName).getGenericType();
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class WildcardHolder {
        List<?> unbounded;
        List<? extends Number> upperBounded;
        List<? super String> lowerBounded;
    }

    private static class GenericHolder<T> {
        T typeVariable;
        T[] genericArray;
    }

    static class Envelope<T> {
    }

    interface UnsupportedInterface {
    }

    abstract static class AbstractUnsupportedType {
    }

    static class DocumentType {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    static class ThrowingWriteDocument {
        public String getName() {
            throw new IllegalArgumentException("Document schema failed while converting ThrowingWriteDocument to a map");
        }

        public void setName(String name) {
        }
    }

    static class ThrowingReadDocument {
        public String getName() {
            return null;
        }

        public void setName(String name) {
            throw new IllegalArgumentException("Document schema failed while converting a map to ThrowingReadDocument");
        }
    }

    static class ThrowingType {
    }

    static class MisreportingStringMember {
    }

    static class NullReturningMember {
    }

    enum TestEnum {
        OPEN,
        CLOSED
    }

    enum LowercaseEnum {
        OPEN,
        CLOSED;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private static final class ThrowingTypeConverter implements AttributeConverter<ThrowingType> {
        private final IllegalArgumentException fromFailure;
        private final IllegalArgumentException toFailure;

        ThrowingTypeConverter(String fromMessage, String toMessage) {
            this.fromFailure = new IllegalArgumentException(fromMessage);
            this.toFailure = new IllegalArgumentException(toMessage);
        }

        IllegalArgumentException fromFailure() {
            return fromFailure;
        }

        IllegalArgumentException toFailure() {
            return toFailure;
        }

        @Override
        public AttributeValue transformFrom(ThrowingType input) {
            throw fromFailure;
        }

        @Override
        public ThrowingType transformTo(AttributeValue input) {
            throw toFailure;
        }

        @Override
        public EnhancedType<ThrowingType> type() {
            return EnhancedType.of(ThrowingType.class);
        }

        @Override
        public AttributeValueType attributeValueType() {
            return AttributeValueType.S;
        }
    }

    private static final class MisreportingStringMemberConverter
        implements AttributeConverter<MisreportingStringMember> {

        @Override
        public AttributeValue transformFrom(MisreportingStringMember input) {
            return AttributeValue.fromNul(true);
        }

        @Override
        public MisreportingStringMember transformTo(AttributeValue input) {
            throw new UnsupportedOperationException();
        }

        @Override
        public EnhancedType<MisreportingStringMember> type() {
            return EnhancedType.of(MisreportingStringMember.class);
        }

        @Override
        public AttributeValueType attributeValueType() {
            return AttributeValueType.S;
        }
    }

    private static final class RecordingDocumentSchema implements TableSchema<DocumentType> {
        private DocumentType lastItemToMapItem;
        private Boolean lastItemToMapIgnoreNulls;
        private Map<String, AttributeValue> itemToMapResult = Collections.emptyMap();
        private Map<String, AttributeValue> lastMapToItemMap;
        private Boolean lastMapToItemPreserveEmptyObject;
        private DocumentType mapToItemResult;

        void setItemToMapResult(Map<String, AttributeValue> itemToMapResult) {
            this.itemToMapResult = itemToMapResult;
        }

        void setMapToItemResult(DocumentType mapToItemResult) {
            this.mapToItemResult = mapToItemResult;
        }

        DocumentType lastItemToMapItem() {
            return lastItemToMapItem;
        }

        Boolean lastItemToMapIgnoreNulls() {
            return lastItemToMapIgnoreNulls;
        }

        Map<String, AttributeValue> lastMapToItemMap() {
            return lastMapToItemMap;
        }

        Boolean lastMapToItemPreserveEmptyObject() {
            return lastMapToItemPreserveEmptyObject;
        }

        @Override
        public DocumentType mapToItem(Map<String, AttributeValue> attributeMap) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DocumentType mapToItem(Map<String, AttributeValue> attributeMap, boolean preserveEmptyObject) {
            lastMapToItemMap = attributeMap;
            lastMapToItemPreserveEmptyObject = preserveEmptyObject;
            return mapToItemResult;
        }

        @Override
        public Map<String, AttributeValue> itemToMap(DocumentType item, boolean ignoreNulls) {
            lastItemToMapItem = item;
            lastItemToMapIgnoreNulls = ignoreNulls;
            return itemToMapResult;
        }

        @Override
        public Map<String, AttributeValue> itemToMap(DocumentType item, Collection<String> attributes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AttributeValue attributeValue(DocumentType item, String attributeName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TableMetadata tableMetadata() {
            throw new UnsupportedOperationException();
        }

        @Override
        public EnhancedType<DocumentType> itemType() {
            return EnhancedType.of(DocumentType.class);
        }

        @Override
        public List<String> attributeNames() {
            return Collections.emptyList();
        }

        @Override
        public boolean isAbstract() {
            return false;
        }
    }

    private static final class NullReturningMemberConverter implements AttributeConverter<NullReturningMember> {
        @Override
        public AttributeValue transformFrom(NullReturningMember input) {
            return null;
        }

        @Override
        public NullReturningMember transformTo(AttributeValue input) {
            throw new UnsupportedOperationException();
        }

        @Override
        public EnhancedType<NullReturningMember> type() {
            return EnhancedType.of(NullReturningMember.class);
        }

        @Override
        public AttributeValueType attributeValueType() {
            return AttributeValueType.S;
        }
    }
}

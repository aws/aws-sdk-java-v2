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
import static org.assertj.core.api.Assertions.catchThrowable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Tests conversion of partition and sort key values supplied to a key builder.
 * <p>
 * The tests cover supported scalar objects, runtime type selection, primitive optional values, null inputs, and
 * unsupported application or collection types. They also verify that preconverted attribute values bypass provider
 * lookup and retain scalar or nonscalar DynamoDB forms accepted by the builder.
 */
public class KeyConverterTest {

    @ParameterizedTest(name = "{0} object key stores the converted AttributeValue")
    @MethodSource("supportedObjectKeys")
    @DisplayName("Supported built-in object keys convert through addPartitionValue and addSortValue")
    void addPartitionValue_whenSupportedBuiltInType_storesConvertedPartitionAndSortValues(
        String typeName, Object input, AttributeValue expected) {
        Key key = Key.builder().addPartitionValue(input).addSortValue(input).build();

        assertThat(key.partitionKeyValue()).as(typeName).isEqualTo(expected);
        assertThat(key.sortKeyValue()).as(typeName).contains(expected);
    }

    @Test
    @DisplayName("A ByteBuffer runtime subclass is rejected as a partition object key")
    void addPartitionValue_whenByteBufferRuntimeSubclass_throwsUnsupportedTypeWithIllegalStateCause() {
        ByteBuffer input = ByteBuffer.wrap(new byte[] {1, 2});

        assertThatThrownBy(() -> Key.builder().addPartitionValue(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported type: " + input.getClass().getName())
            .hasCauseInstanceOf(IllegalStateException.class)
            .getCause()
            .hasMessage("Converter not found for " + EnhancedType.of(input.getClass()));
    }

    @Test
    @DisplayName("A CharSequence reference selects conversion by the runtime String class")
    void addPartitionValue_whenCharSequenceReferringToString_storesSTextOnPartitionAndSort() {
        CharSequence input = "text";

        Key key = Key.builder().addPartitionValue(input).addSortValue(input).build();

        assertThat(key.partitionKeyValue()).isEqualTo(AttributeValue.fromS("text"));
        assertThat(key.sortKeyValue()).contains(AttributeValue.fromS("text"));
    }

    @Test
    @DisplayName("A ZoneId runtime subclass is rejected as a partition object key")
    void addPartitionValue_whenZoneIdRuntimeSubclass_throwsUnsupportedTypeWithIllegalStateCause() {
        ZoneId input = ZoneId.of("Europe/Paris");

        assertThatThrownBy(() -> Key.builder().addPartitionValue(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported type: " + input.getClass().getName())
            .hasCauseInstanceOf(IllegalStateException.class)
            .getCause()
            .hasMessage("Converter not found for " + EnhancedType.of(input.getClass()));
    }

    @Test
    @DisplayName("The sort object overload also rejects a ByteBuffer runtime subclass")
    void addSortValue_whenByteBufferRuntimeSubclass_throwsUnsupportedType() {
        ByteBuffer input = ByteBuffer.wrap(new byte[] {1, 2});

        assertThatThrownBy(() -> Key.builder().partitionValue("pk").addSortValue(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported type: " + input.getClass().getName())
            .hasCauseInstanceOf(IllegalStateException.class)
            .getCause()
            .hasMessage("Converter not found for " + EnhancedType.of(input.getClass()));
    }

    @Test
    @DisplayName("The sort object overload also rejects a ZoneId runtime subclass")
    void addSortValue_whenZoneIdRuntimeSubclass_throwsUnsupportedType() {
        ZoneId input = ZoneId.of("Europe/Paris");

        assertThatThrownBy(() -> Key.builder().partitionValue("pk").addSortValue(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported type: " + input.getClass().getName())
            .hasCauseInstanceOf(IllegalStateException.class)
            .getCause()
            .hasMessage("Converter not found for " + EnhancedType.of(input.getClass()));
    }

    @Test
    @DisplayName("A custom CharSequence implementation does not inherit the interface registration")
    void addPartitionValue_whenCustomCharSequence_throwsUnsupportedType() {
        CustomCharSequence input = new CustomCharSequence("text");

        assertThatThrownBy(() -> Key.builder().addPartitionValue(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported type: " + CustomCharSequence.class.getName());
    }

    @Test
    @DisplayName("The sort overload also rejects a custom CharSequence implementation")
    void addSortValue_whenCustomCharSequence_throwsUnsupportedType() {
        CustomCharSequence input = new CustomCharSequence("text");

        assertThatThrownBy(() -> Key.builder().partitionValue("pk").addSortValue(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported type: " + CustomCharSequence.class.getName());
    }

    @Test
    @DisplayName("An Object partition value is rejected as unsupported")
    void addPartitionValue_whenObject_throwsUnsupportedType() {
        assertThatThrownBy(() -> Key.builder().addPartitionValue(new Object()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported type: " + Object.class.getName());
    }

    @Test
    @DisplayName("An Object sort value is rejected as unsupported")
    void addSortValue_whenObject_throwsUnsupportedType() {
        assertThatThrownBy(() -> Key.builder().partitionValue("pk").addSortValue(new Object()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported type: " + Object.class.getName());
    }

    @Test
    @DisplayName("A List object key is looked up as its concrete runtime class")
    void addPartitionValue_whenArrayList_throwsUnsupportedTypeForConcreteClass() {
        ArrayList<String> input = new ArrayList<>();

        assertThatThrownBy(() -> Key.builder().addPartitionValue(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported type: java.util.ArrayList");
    }

    @Test
    @DisplayName("A Set object sort key is looked up as its concrete runtime class")
    void addSortValue_whenHashSet_throwsUnsupportedTypeForConcreteClass() {
        HashSet<String> input = new HashSet<>();

        assertThatThrownBy(() -> Key.builder().partitionValue("pk").addSortValue(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported type: java.util.HashSet");
    }

    @Test
    @DisplayName("A Map object key is looked up as its concrete runtime class")
    void addPartitionValue_whenHashMap_throwsUnsupportedTypeForConcreteClass() {
        HashMap<String, Integer> input = new HashMap<>();

        assertThatThrownBy(() -> Key.builder().addPartitionValue(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported type: java.util.HashMap");
    }

    @Test
    @DisplayName("An object key cannot attach a document schema")
    void addPartitionValue_whenDocumentType_throwsUnsupportedType() {
        DocumentType input = new DocumentType();

        assertThatThrownBy(() -> Key.builder().addPartitionValue(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported type: " + DocumentType.class.getName());
    }

    @Test
    @DisplayName("An unsupported application key is wrapped with the provider failure as cause")
    void addPartitionValue_whenUnsupportedType_throwsUnsupportedTypeWithConverterNotFoundCause() {
        UnsupportedType input = new UnsupportedType();

        Throwable thrown = catchThrowable(() -> Key.builder().addPartitionValue(input));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                          .hasMessage("Unsupported type: " + UnsupportedType.class.getName())
                          .hasCauseInstanceOf(IllegalStateException.class);
        assertThat(thrown.getCause())
            .hasMessage("Converter not found for " + EnhancedType.of(UnsupportedType.class));
    }

    @Test
    @DisplayName("An object key ignores an attribute-level-only converter")
    void addPartitionValue_whenAttributeOnlyKey_throwsUnsupportedType() {
        AttributeOnlyKey input = new AttributeOnlyKey();

        assertThatThrownBy(() -> Key.builder().addPartitionValue(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported type: " + AttributeOnlyKey.class.getName());
    }

    @Test
    @DisplayName("A null object partition value has a dedicated message")
    void addPartitionValue_whenNull_throwsPartitionKeyValueCannotBeNull() {
        assertThatThrownBy(() -> Key.builder().addPartitionValue(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Partition key value cannot be null");
    }

    @Test
    @DisplayName("A null object sort value has a dedicated message")
    void addSortValue_whenNull_throwsSortKeyValueCannotBeNull() {
        assertThatThrownBy(() -> Key.builder().partitionValue("pk").addSortValue(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Sort key value cannot be null");
    }

    @Test
    @DisplayName("A preconverted partition value bypasses provider selection")
    void partitionValue_whenPreconvertedStringAttributeValue_storesSameSValue() {
        AttributeValue custom = AttributeValue.fromS("custom");

        Key key = Key.builder().partitionValue(custom).build();

        assertThat(key.partitionKeyValue()).isEqualTo(custom);
    }

    @Test
    @DisplayName("A preconverted sort value bypasses provider selection")
    void sortValue_whenPreconvertedBinaryAttributeValue_storesSameBValue() {
        AttributeValue custom = AttributeValue.fromB(SdkBytes.fromUtf8String("ab"));

        Key key = Key.builder().partitionValue("pk").sortValue(custom).build();

        assertThat(key.sortKeyValue()).contains(custom);
    }

    @Test
    @DisplayName("A preconverted partition overload accepts a non-scalar map")
    void partitionValue_whenPreconvertedMapAttributeValue_storesMapWithoutScalarValidation() {
        AttributeValue custom = AttributeValue.fromM(
            Collections.singletonMap("inner", AttributeValue.fromS("text")));

        Key key = Key.builder().partitionValue(custom).build();

        assertThat(key.partitionKeyValue().hasM()).isTrue();
    }

    @Test
    @DisplayName("A preconverted NULL partition value is rejected")
    void partitionValue_whenNulAttributeValue_throwsPartitionValueShouldNotBeNull() {
        AttributeValue custom = AttributeValue.fromNul(true);

        assertThatThrownBy(() -> Key.builder().partitionValue(custom))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("partitionValue should not be null");
    }

    @Test
    @DisplayName("A preconverted NULL sort value is retained")
    void sortValue_whenNulAttributeValue_retainsNul() {
        AttributeValue custom = AttributeValue.fromNul(true);

        Key key = Key.builder().partitionValue("pk").sortValue(custom).build();

        assertThat(key.sortKeyValue()).contains(AttributeValue.fromNul(true));
    }

    @Test
    @DisplayName("A null AttributeValue partition overload is rejected")
    void partitionValue_whenNullAttributeValue_throwsPartitionValueShouldNotBeNull() {
        assertThatThrownBy(() -> Key.builder().partitionValue((AttributeValue) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("partitionValue should not be null");
    }

    @Test
    @DisplayName("A null AttributeValue sort overload means no sort value")
    void sortValue_whenNullAttributeValue_leavesSortKeyEmpty() {
        Key key = Key.builder().partitionValue("pk").sortValue((AttributeValue) null).build();

        assertThat(key.sortKeyValue()).isEmpty();
    }

    @Test
    @DisplayName("An empty primitive optional produces a NULL object key value")
    void addPartitionValue_whenEmptyOptionalInt_storesNul() {
        OptionalInt input = OptionalInt.empty();

        Key key = Key.builder().addPartitionValue(input).build();

        assertThat(key.partitionKeyValue()).isEqualTo(AttributeValue.fromNul(true));
    }

    static Stream<Arguments> supportedObjectKeys() {
        byte[] bytes = new byte[] {1, 2};
        SdkBytes sdkBytes = SdkBytes.fromUtf8String("ab");
        TestEnum enumConstant = TestEnum.OPEN;
        Instant instant = Instant.parse("2020-01-02T03:04:05Z");
        OffsetDateTime offsetDateTime = OffsetDateTime.parse("2020-01-02T03:04:05+02:00");
        ZonedDateTime zonedDateTime = ZonedDateTime.parse("2020-01-02T03:04:05+01:00[Europe/Paris]");

        return Stream.of(
            Arguments.of("AtomicBoolean", new AtomicBoolean(true), AttributeValue.fromBool(true)),
            Arguments.of("AtomicInteger", new AtomicInteger(7), AttributeValue.fromN("7")),
            Arguments.of("AtomicLong", new AtomicLong(7L), AttributeValue.fromN("7")),
            Arguments.of("BigDecimal", new BigDecimal("1.25"), AttributeValue.fromN("1.25")),
            Arguments.of("BigInteger", new BigInteger("12345678901234567890"),
                         AttributeValue.fromN("12345678901234567890")),
            Arguments.of("Boolean", Boolean.TRUE, AttributeValue.fromBool(true)),
            Arguments.of("byte[]", bytes, AttributeValue.fromB(SdkBytes.fromByteArray(bytes))),
            Arguments.of("Byte", Byte.valueOf((byte) 7), AttributeValue.fromN("7")),
            Arguments.of("char[]", new char[] {'a', 'b'}, AttributeValue.fromS("ab")),
            Arguments.of("Character", Character.valueOf('x'), AttributeValue.fromS("x")),
            Arguments.of("Double", Double.valueOf(1.5d), AttributeValue.fromN("1.5")),
            Arguments.of("Duration", Duration.ofSeconds(90), AttributeValue.fromN("90")),
            Arguments.of("Float", Float.valueOf(1.5f), AttributeValue.fromN("1.5")),
            Arguments.of("Instant", instant, AttributeValue.fromS("2020-01-02T03:04:05Z")),
            Arguments.of("Integer", Integer.valueOf(7), AttributeValue.fromN("7")),
            Arguments.of("LocalDate", LocalDate.parse("2020-01-02"), AttributeValue.fromS("2020-01-02")),
            Arguments.of("LocalDateTime", LocalDateTime.parse("2020-01-02T03:04:05"),
                         AttributeValue.fromS("2020-01-02T03:04:05")),
            Arguments.of("Locale", Locale.forLanguageTag("en-US"), AttributeValue.fromS("en-US")),
            Arguments.of("LocalTime", LocalTime.parse("03:04:05"), AttributeValue.fromS("03:04:05")),
            Arguments.of("Long", Long.valueOf(7L), AttributeValue.fromN("7")),
            Arguments.of("MonthDay", MonthDay.of(1, 2), AttributeValue.fromS("--01-02")),
            Arguments.of("OffsetDateTime", offsetDateTime, AttributeValue.fromS("2020-01-02T03:04:05+02:00")),
            Arguments.of("OptionalDouble", OptionalDouble.of(1.5d), AttributeValue.fromN("1.5")),
            Arguments.of("OptionalInt", OptionalInt.of(7), AttributeValue.fromN("7")),
            Arguments.of("OptionalLong", OptionalLong.of(7L), AttributeValue.fromN("7")),
            Arguments.of("Period", Period.of(1, 2, 3), AttributeValue.fromS("P1Y2M3D")),
            Arguments.of("SdkBytes", sdkBytes, AttributeValue.fromB(sdkBytes)),
            Arguments.of("Short", Short.valueOf((short) 7), AttributeValue.fromN("7")),
            Arguments.of("String", "text", AttributeValue.fromS("text")),
            Arguments.of("StringBuffer", new StringBuffer("text"), AttributeValue.fromS("text")),
            Arguments.of("StringBuilder", new StringBuilder("text"), AttributeValue.fromS("text")),
            Arguments.of("URI", URI.create("https://example.com/a"),
                         AttributeValue.fromS("https://example.com/a")),
            Arguments.of("URL", url("https://example.com/a"), AttributeValue.fromS("https://example.com/a")),
            Arguments.of("UUID", UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                         AttributeValue.fromS("123e4567-e89b-12d3-a456-426614174000")),
            Arguments.of("ZonedDateTime", zonedDateTime,
                         AttributeValue.fromS("2020-01-02T03:04:05+01:00[Europe/Paris]")),
            Arguments.of("ZoneOffset", ZoneOffset.ofHours(2), AttributeValue.fromS("+02:00")),
            Arguments.of("SdkNumber", SdkNumber.fromString("1.25"), AttributeValue.fromN("1.25")),
            Arguments.of("TestEnum", enumConstant, AttributeValue.fromS(enumConstant.toString()))
        );
    }

    private static URL url(String value) {
        try {
            return new URL(value);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    enum TestEnum {
        OPEN,
        CLOSED
    }

    static final class CustomCharSequence implements CharSequence {
        private final String value;

        CustomCharSequence(String value) {
            this.value = value;
        }

        @Override
        public int length() {
            return value.length();
        }

        @Override
        public char charAt(int index) {
            return value.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return value.subSequence(start, end);
        }

        @Override
        public String toString() {
            return value;
        }
    }

    static final class DocumentType {
    }

    static final class UnsupportedType {
    }

    static final class AttributeOnlyKey {
    }
}

package software.amazon.awssdk.enhanced.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.lang.reflect.Type;
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
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.event.Level;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.PrimitiveConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.AtomicBooleanAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.AtomicIntegerAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.AtomicLongAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.BigDecimalAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.BigIntegerAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.BooleanAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ByteArrayAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ByteAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ByteBufferAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.CharSequenceAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.CharacterArrayAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.CharacterAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.DocumentAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.DoubleAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.DurationAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.FloatAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.InstantAsStringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.IntegerAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ListAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.LocalDateAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.LocalDateTimeAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.LocalTimeAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.LocaleAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.LongAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.MapAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.MonthDayAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.OffsetDateTimeAsStringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.OptionalDoubleAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.OptionalIntAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.OptionalLongAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.PeriodAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.SdkBytesAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.SdkNumberAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.SetAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ShortAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.StringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.StringBufferAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.StringBuilderAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.UriAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.UrlAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.UuidAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ZoneIdAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ZoneOffsetAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ZonedDateTimeAsStringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Consolidated lookup, built-in, registry, and cache coverage.
 */
public class DefaultAttributeConverterProviderLookupTest {

    // Built-in converter coverage.
    private static final BiConsumer<Object, Object> EQUAL_TO_INPUT =
        (input, read) -> assertThat(read).isEqualTo(input);

    private final DefaultAttributeConverterProvider builtInProvider = DefaultAttributeConverterProvider.create();

    @ParameterizedTest(name = "{0} registration converts using {1}")
    @MethodSource("builtInTypes")
    @DisplayName("Registered built-in types convert to AttributeValue and back")
    void converterFor_whenRegisteredBuiltInType_convertsToAttributeValueAndBack(String typeName,
                                                                                String converterName,
                                                                                BuiltInCase testCase) {
        AttributeConverter<Object> converter = converterFor(testCase.type);

        AttributeValue stored = converter.transformFrom(testCase.input);
        Object read = converter.transformTo(stored);

        assertThat(converter).as(typeName).isInstanceOf(testCase.converterClass);
        assertThat(converter.getClass().getSimpleName()).isEqualTo(converterName);
        assertThat(converter.type()).isEqualTo(testCase.type);
        assertThat(converter.attributeValueType()).isEqualTo(testCase.attributeValueType);
        assertThat(stored).isEqualTo(testCase.expectedStored);
        if (testCase.input instanceof ByteBuffer) {
            assertThat(stored.b().asByteArray()).containsExactly((byte) 1, (byte) 2);
        }
        testCase.reconstructed.accept(testCase.input, read);
    }

    @ParameterizedTest(name = "{0} registration converts using {1}")
    @MethodSource("primitiveAliases")
    @DisplayName("Primitive aliases share the wrapper converter and convert using the wrapper declared type")
    void converterFor_whenPrimitiveType_sharesWrapperConverterAndConvertsRoundTrip(String typeName,
                                                                                   String converterName,
                                                                                   PrimitiveCase testCase) {
        AttributeConverter<Object> primitive = converterFor(EnhancedType.of(testCase.primitiveClass));
        AttributeConverter<Object> wrapper = converterFor(EnhancedType.of(testCase.wrapperClass));

        AttributeValue stored = primitive.transformFrom(testCase.input);
        Object read = primitive.transformTo(stored);

        assertThat(primitive).as(typeName).isInstanceOf(testCase.converterClass);
        assertThat(primitive.getClass().getSimpleName()).isEqualTo(converterName);
        assertThat(primitive).isSameAs(wrapper);
        assertThat(primitive.type()).isEqualTo(EnhancedType.of(testCase.wrapperClass));
        assertThat(primitive.attributeValueType()).isEqualTo(testCase.attributeValueType);
        assertThat(stored).isEqualTo(testCase.expectedStored);
        assertThat(read).isEqualTo(testCase.input);
    }

    @ParameterizedTest(name = "{0} has no default cache key")
    @MethodSource("unregisteredTypes")
    @DisplayName("Types with a converter class or string converter still fail when they are not default cache keys")
    void converterFor_whenUnregisteredType_throwsIllegalStateException(String typeName, EnhancedType<?> type) {
        assertThatThrownBy(() -> builtInProvider.converterFor(type))
            .as(typeName)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    static Stream<Arguments> builtInTypes() {
        Instant instant = Instant.parse("2020-01-02T03:04:05Z");
        OffsetDateTime offsetDateTime = OffsetDateTime.parse("2020-01-02T03:04:05+02:00");
        ZonedDateTime zonedDateTime = ZonedDateTime.parse("2020-01-02T03:04:05+01:00[Europe/Paris]");
        byte[] bytes = new byte[] {1, 2};
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[] {1, 2});
        SdkBytes sdkBytes = SdkBytes.fromUtf8String("ab");
        URL url = url("https://example.com/a");
        List<String> stringList = new ArrayList<>();
        stringList.add("a");
        stringList.add("b");
        Set<String> stringSet = new LinkedHashSet<>();
        stringSet.add("a");
        stringSet.add("b");
        Map<String, Integer> stringIntegerMap = new LinkedHashMap<>();
        stringIntegerMap.put("one", 1);
        TableSchema<BuiltInDocumentType> documentSchema =
            StaticTableSchema.builder(BuiltInDocumentType.class)
                             .newItemSupplier(BuiltInDocumentType::new)
                             .addAttribute(String.class, a -> a.name("name")
                                                               .getter(BuiltInDocumentType::getName)
                                                               .setter(BuiltInDocumentType::setName))
                             .build();
        EnhancedType<BuiltInDocumentType> documentType = EnhancedType.documentOf(BuiltInDocumentType.class, documentSchema);
        BuiltInDocumentType document = new BuiltInDocumentType();
        document.setName("child");

        return Stream.of(
            builtIn("AtomicBoolean", AtomicBooleanAttributeConverter.class, EnhancedType.of(AtomicBoolean.class),
                    new AtomicBoolean(true), bool(true), AttributeValueType.BOOL,
                    (input, read) -> assertThat(((AtomicBoolean) read).get()).isTrue()),
            builtIn("AtomicInteger", AtomicIntegerAttributeConverter.class, EnhancedType.of(AtomicInteger.class),
                    new AtomicInteger(7), n("7"), AttributeValueType.S,
                    (input, read) -> assertThat(((AtomicInteger) read).get()).isEqualTo(7)),
            builtIn("AtomicLong", AtomicLongAttributeConverter.class, EnhancedType.of(AtomicLong.class),
                    new AtomicLong(7L), n("7"), AttributeValueType.N,
                    (input, read) -> assertThat(((AtomicLong) read).get()).isEqualTo(7L)),
            builtIn("BigDecimal", BigDecimalAttributeConverter.class, EnhancedType.of(BigDecimal.class),
                    new BigDecimal("1.25"), n("1.25"), AttributeValueType.N, EQUAL_TO_INPUT),
            builtIn("BigInteger", BigIntegerAttributeConverter.class, EnhancedType.of(BigInteger.class),
                    new BigInteger("12345678901234567890"), n("12345678901234567890"), AttributeValueType.N,
                    EQUAL_TO_INPUT),
            builtIn("Boolean", BooleanAttributeConverter.class, EnhancedType.of(Boolean.class),
                    Boolean.TRUE, bool(true), AttributeValueType.BOOL, EQUAL_TO_INPUT),
            builtIn("byte[]", ByteArrayAttributeConverter.class, EnhancedType.of(byte[].class),
                    bytes, binary(SdkBytes.fromByteArray(bytes)), AttributeValueType.B,
                    (input, read) -> assertThat((byte[]) read).containsExactly((byte) 1, (byte) 2)),
            builtIn("ByteBuffer", ByteBufferAttributeConverter.class, EnhancedType.of(ByteBuffer.class),
                    byteBuffer, binary(SdkBytes.fromByteArray(new byte[] {1, 2})), AttributeValueType.B,
                    DefaultAttributeConverterProviderLookupTest::assertByteBufferRoundTrip),
            builtIn("Byte", ByteAttributeConverter.class, EnhancedType.of(Byte.class),
                    Byte.valueOf((byte) 7), n("7"), AttributeValueType.N, EQUAL_TO_INPUT),
            builtIn("char[]", CharacterArrayAttributeConverter.class, EnhancedType.of(char[].class),
                    new char[] {'a', 'b'}, s("ab"), AttributeValueType.S,
                    (input, read) -> assertThat((char[]) read).containsExactly('a', 'b')),
            builtIn("Character", CharacterAttributeConverter.class, EnhancedType.of(Character.class),
                    Character.valueOf('x'), s("x"), AttributeValueType.S, EQUAL_TO_INPUT),
            builtIn("CharSequence", CharSequenceAttributeConverter.class, EnhancedType.of(CharSequence.class),
                    "text", s("text"), AttributeValueType.S,
                    (input, read) -> assertThat(read).isInstanceOf(String.class).isEqualTo("text")),
            builtIn("Double", DoubleAttributeConverter.class, EnhancedType.of(Double.class),
                    Double.valueOf(1.5d), n("1.5"), AttributeValueType.N, EQUAL_TO_INPUT),
            builtIn("Duration", DurationAttributeConverter.class, EnhancedType.of(Duration.class),
                    Duration.ofSeconds(90), n("90"), AttributeValueType.N, EQUAL_TO_INPUT),
            builtIn("Float", FloatAttributeConverter.class, EnhancedType.of(Float.class),
                    Float.valueOf(1.5f), n("1.5"), AttributeValueType.N, EQUAL_TO_INPUT),
            builtIn("Instant", InstantAsStringAttributeConverter.class, EnhancedType.of(Instant.class),
                    instant, s("2020-01-02T03:04:05Z"), AttributeValueType.S, EQUAL_TO_INPUT),
            builtIn("Integer", IntegerAttributeConverter.class, EnhancedType.of(Integer.class),
                    Integer.valueOf(7), n("7"), AttributeValueType.N, EQUAL_TO_INPUT),
            builtIn("LocalDate", LocalDateAttributeConverter.class, EnhancedType.of(LocalDate.class),
                    LocalDate.parse("2020-01-02"), s("2020-01-02"), AttributeValueType.S, EQUAL_TO_INPUT),
            builtIn("LocalDateTime", LocalDateTimeAttributeConverter.class, EnhancedType.of(LocalDateTime.class),
                    LocalDateTime.parse("2020-01-02T03:04:05"), s("2020-01-02T03:04:05"), AttributeValueType.S,
                    EQUAL_TO_INPUT),
            builtIn("Locale", LocaleAttributeConverter.class, EnhancedType.of(Locale.class),
                    Locale.forLanguageTag("en-US"), s("en-US"), AttributeValueType.S, EQUAL_TO_INPUT),
            builtIn("LocalTime", LocalTimeAttributeConverter.class, EnhancedType.of(LocalTime.class),
                    LocalTime.parse("03:04:05"), s("03:04:05"), AttributeValueType.S, EQUAL_TO_INPUT),
            builtIn("Long", LongAttributeConverter.class, EnhancedType.of(Long.class),
                    Long.valueOf(7L), n("7"), AttributeValueType.N, EQUAL_TO_INPUT),
            builtIn("MonthDay", MonthDayAttributeConverter.class, EnhancedType.of(MonthDay.class),
                    MonthDay.of(1, 2), s("--01-02"), AttributeValueType.S, EQUAL_TO_INPUT),
            builtIn("OffsetDateTime", OffsetDateTimeAsStringAttributeConverter.class,
                    EnhancedType.of(OffsetDateTime.class), offsetDateTime, s(offsetDateTime.toString()),
                    AttributeValueType.S, EQUAL_TO_INPUT),
            builtIn("OptionalDouble", OptionalDoubleAttributeConverter.class, EnhancedType.of(OptionalDouble.class),
                    OptionalDouble.of(1.5d), n("1.5"), AttributeValueType.N,
                    (input, read) -> assertThat(((OptionalDouble) read).getAsDouble()).isEqualTo(1.5d)),
            builtIn("OptionalInt", OptionalIntAttributeConverter.class, EnhancedType.of(OptionalInt.class),
                    OptionalInt.of(7), n("7"), AttributeValueType.N,
                    (input, read) -> assertThat(((OptionalInt) read).getAsInt()).isEqualTo(7)),
            builtIn("OptionalLong", OptionalLongAttributeConverter.class, EnhancedType.of(OptionalLong.class),
                    OptionalLong.of(7L), n("7"), AttributeValueType.N,
                    (input, read) -> assertThat(((OptionalLong) read).getAsLong()).isEqualTo(7L)),
            builtIn("Period", PeriodAttributeConverter.class, EnhancedType.of(Period.class),
                    Period.of(1, 2, 3), s("P1Y2M3D"), AttributeValueType.S, EQUAL_TO_INPUT),
            builtIn("SdkBytes", SdkBytesAttributeConverter.class, EnhancedType.of(SdkBytes.class),
                    sdkBytes, binary(sdkBytes), AttributeValueType.B, EQUAL_TO_INPUT),
            builtIn("Short", ShortAttributeConverter.class, EnhancedType.of(Short.class),
                    Short.valueOf((short) 7), n("7"), AttributeValueType.N, EQUAL_TO_INPUT),
            builtIn("String", StringAttributeConverter.class, EnhancedType.of(String.class),
                    "text", s("text"), AttributeValueType.S, EQUAL_TO_INPUT),
            builtIn("StringBuffer", StringBufferAttributeConverter.class, EnhancedType.of(StringBuffer.class),
                    new StringBuffer("text"), s("text"), AttributeValueType.S,
                    (input, read) -> assertThat(read.toString()).isEqualTo("text")),
            builtIn("StringBuilder", StringBuilderAttributeConverter.class, EnhancedType.of(StringBuilder.class),
                    new StringBuilder("text"), s("text"), AttributeValueType.S,
                    (input, read) -> assertThat(read.toString()).isEqualTo("text")),
            builtIn("URI", UriAttributeConverter.class, EnhancedType.of(URI.class),
                    URI.create("https://example.com/a"), s("https://example.com/a"), AttributeValueType.S,
                    EQUAL_TO_INPUT),
            builtIn("URL", UrlAttributeConverter.class, EnhancedType.of(URL.class),
                    url, s("https://example.com/a"), AttributeValueType.S,
                    (input, read) -> assertThat(((URL) read).toExternalForm())
                        .isEqualTo(((URL) input).toExternalForm())),
            builtIn("UUID", UuidAttributeConverter.class, EnhancedType.of(UUID.class),
                    UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                    s("123e4567-e89b-12d3-a456-426614174000"), AttributeValueType.S, EQUAL_TO_INPUT),
            builtIn("ZonedDateTime", ZonedDateTimeAsStringAttributeConverter.class, EnhancedType.of(ZonedDateTime.class),
                    zonedDateTime, s(zonedDateTime.toString()), AttributeValueType.S, EQUAL_TO_INPUT),
            builtIn("ZoneId", ZoneIdAttributeConverter.class, EnhancedType.of(ZoneId.class),
                    ZoneId.of("Europe/Paris"), s("Europe/Paris"), AttributeValueType.S, EQUAL_TO_INPUT),
            builtIn("ZoneOffset", ZoneOffsetAttributeConverter.class, EnhancedType.of(ZoneOffset.class),
                    ZoneOffset.ofHours(2), s("+02:00"), AttributeValueType.S, EQUAL_TO_INPUT),
            builtIn("SdkNumber", SdkNumberAttributeConverter.class, EnhancedType.of(SdkNumber.class),
                    SdkNumber.fromString("1.25"), n("1.25"), AttributeValueType.N, EQUAL_TO_INPUT),
            builtIn("List", ListAttributeConverter.class, EnhancedType.listOf(String.class),
                    stringList, l(s("a"), s("b")), AttributeValueType.L,
                    (input, read) -> assertThat(read).isInstanceOf(ArrayList.class).isEqualTo(stringList)),
            builtIn("Set", SetAttributeConverter.class, EnhancedType.setOf(String.class),
                    stringSet, ss("a", "b"), AttributeValueType.SS,
                    (input, read) -> assertThat(read).isInstanceOf(LinkedHashSet.class).isEqualTo(stringSet)),
            builtIn("Map", MapAttributeConverter.class, EnhancedType.mapOf(String.class, Integer.class),
                    stringIntegerMap, m(Collections.singletonMap("one", n("1"))), AttributeValueType.M,
                    (input, read) -> assertThat(read).isInstanceOf(LinkedHashMap.class).isEqualTo(stringIntegerMap)),
            builtIn("Enum", EnumAttributeConverter.class, EnhancedType.of(BuiltInTestEnum.class),
                    BuiltInTestEnum.OPEN, s("OPEN"), AttributeValueType.S, EQUAL_TO_INPUT),
            builtIn("Document", DocumentAttributeConverter.class, documentType,
                    document, m(Collections.singletonMap("name", s("child"))), AttributeValueType.M, EQUAL_TO_INPUT)
        );
    }

    static Stream<Arguments> unregisteredTypes() {
        return Stream.of(
            Arguments.of("Optional", EnhancedType.optionalOf(String.class)),
            Arguments.of("JsonNode", EnhancedType.of(JsonNode.class)),
            Arguments.of("Year", EnhancedType.of(Year.class))
        );
    }

    static Stream<Arguments> primitiveAliases() {
        return Stream.of(
            primitive("boolean", boolean.class, Boolean.class, BooleanAttributeConverter.class,
                      true, bool(true), AttributeValueType.BOOL),
            primitive("byte", byte.class, Byte.class, ByteAttributeConverter.class,
                      (byte) 7, n("7"), AttributeValueType.N),
            primitive("short", short.class, Short.class, ShortAttributeConverter.class,
                      (short) 7, n("7"), AttributeValueType.N),
            primitive("int", int.class, Integer.class, IntegerAttributeConverter.class,
                      7, n("7"), AttributeValueType.N),
            primitive("long", long.class, Long.class, LongAttributeConverter.class,
                      7L, n("7"), AttributeValueType.N),
            primitive("float", float.class, Float.class, FloatAttributeConverter.class,
                      1.5f, n("1.5"), AttributeValueType.N),
            primitive("double", double.class, Double.class, DoubleAttributeConverter.class,
                      1.5d, n("1.5"), AttributeValueType.N),
            primitive("char", char.class, Character.class, CharacterAttributeConverter.class,
                      'x', s("x"), AttributeValueType.S)
        );
    }

    @SuppressWarnings("unchecked")
    private <T> AttributeConverter<T> converterFor(EnhancedType<?> type) {
        return (AttributeConverter<T>) builtInProvider.converterFor(type);
    }

    private static void assertByteBufferRoundTrip(Object input, Object read) {
        ByteBuffer inputBuffer = ((ByteBuffer) input).duplicate();
        ByteBuffer readBuffer = ((ByteBuffer) read).duplicate();
        assertThat(readBuffer).isEqualTo(inputBuffer);
        assertThat(((ByteBuffer) read).equals(input)).isTrue();
    }

    private static Arguments builtIn(String typeName,
                                     Class<?> converterClass,
                                     EnhancedType<?> type,
                                     Object input,
                                     AttributeValue expectedStored,
                                     AttributeValueType attributeValueType,
                                     BiConsumer<Object, Object> reconstructed) {
        return Arguments.of(typeName, converterClass.getSimpleName(),
                            new BuiltInCase(type, converterClass, input, expectedStored, attributeValueType,
                                            reconstructed));
    }

    private static Arguments primitive(String typeName,
                                       Class<?> primitiveClass,
                                       Class<?> wrapperClass,
                                       Class<?> converterClass,
                                       Object input,
                                       AttributeValue expectedStored,
                                       AttributeValueType attributeValueType) {
        return Arguments.of(typeName, converterClass.getSimpleName(),
                            new PrimitiveCase(primitiveClass, wrapperClass, converterClass, input, expectedStored,
                                              attributeValueType));
    }

    private static AttributeValue bool(boolean value) {
        return AttributeValue.builder().bool(value).build();
    }

    private static AttributeValue n(String value) {
        return AttributeValue.builder().n(value).build();
    }

    private static AttributeValue s(String value) {
        return AttributeValue.builder().s(value).build();
    }

    private static AttributeValue binary(SdkBytes value) {
        return AttributeValue.builder().b(value).build();
    }

    private static AttributeValue l(AttributeValue... values) {
        return AttributeValue.builder().l(Arrays.asList(values)).build();
    }

    private static AttributeValue ss(String... values) {
        return AttributeValue.builder().ss(Arrays.asList(values)).build();
    }

    private static AttributeValue m(Map<String, AttributeValue> members) {
        return AttributeValue.builder().m(members).build();
    }

    private static URL url(String value) {
        try {
            return new URL(value);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class BuiltInCase {
        private final EnhancedType<?> type;
        private final Class<?> converterClass;
        private final Object input;
        private final AttributeValue expectedStored;
        private final AttributeValueType attributeValueType;
        private final BiConsumer<Object, Object> reconstructed;

        private BuiltInCase(EnhancedType<?> type,
                            Class<?> converterClass,
                            Object input,
                            AttributeValue expectedStored,
                            AttributeValueType attributeValueType,
                            BiConsumer<Object, Object> reconstructed) {
            this.type = type;
            this.converterClass = converterClass;
            this.input = input;
            this.expectedStored = expectedStored;
            this.attributeValueType = attributeValueType;
            this.reconstructed = reconstructed;
        }
    }

    private static final class PrimitiveCase {
        private final Class<?> primitiveClass;
        private final Class<?> wrapperClass;
        private final Class<?> converterClass;
        private final Object input;
        private final AttributeValue expectedStored;
        private final AttributeValueType attributeValueType;

        private PrimitiveCase(Class<?> primitiveClass,
                              Class<?> wrapperClass,
                              Class<?> converterClass,
                              Object input,
                              AttributeValue expectedStored,
                              AttributeValueType attributeValueType) {
            this.primitiveClass = primitiveClass;
            this.wrapperClass = wrapperClass;
            this.converterClass = converterClass;
            this.input = input;
            this.expectedStored = expectedStored;
            this.attributeValueType = attributeValueType;
        }
    }

    enum BuiltInTestEnum {
        OPEN,
        CLOSED
    }

    static final class BuiltInDocumentType {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof BuiltInDocumentType)) {
                return false;
            }
            return Objects.equals(name, ((BuiltInDocumentType) o).name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name);
        }
    }

    // Generated-converter cache and logging coverage.
    @Test
    @DisplayName("Equal map tokens return the same cached map converter")
    void converterFor_whenEqualMapTokens_returnsSameCachedMapAttributeConverter() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<String, Integer>> firstType = EnhancedType.mapOf(String.class, Integer.class);
        EnhancedType<Map<String, Integer>> secondType = EnhancedType.mapOf(String.class, Integer.class);

        AttributeConverter<Map<String, Integer>> first = provider.converterFor(firstType);
        AttributeConverter<Map<String, Integer>> second = provider.converterFor(secondType);

        assertThat(first).isInstanceOf(MapAttributeConverter.class);
        assertThat(first.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(second).isSameAs(first);
    }

    @Test
    @DisplayName("Equal set tokens return the same cached set converter")
    void converterFor_whenEqualSetTokens_returnsSameCachedSetAttributeConverter() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        EnhancedType<Set<String>> firstType = EnhancedType.setOf(String.class);
        EnhancedType<Set<String>> secondType = EnhancedType.setOf(String.class);

        AttributeConverter<Set<String>> first = provider.converterFor(firstType);
        AttributeConverter<Set<String>> second = provider.converterFor(secondType);

        assertThat(first).isInstanceOf(SetAttributeConverter.class);
        assertThat(first.attributeValueType()).isEqualTo(AttributeValueType.SS);
        assertThat(second).isSameAs(first);
    }

    @Test
    @DisplayName("Equal list tokens return different list converters")
    void converterFor_whenEqualListTokens_returnsDifferentListAttributeConverters() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        EnhancedType<List<String>> firstType = EnhancedType.listOf(String.class);
        EnhancedType<List<String>> secondType = EnhancedType.listOf(String.class);

        AttributeConverter<List<String>> first = provider.converterFor(firstType);
        AttributeConverter<List<String>> second = provider.converterFor(secondType);

        assertThat(first).isInstanceOf(ListAttributeConverter.class);
        assertThat(first.attributeValueType()).isEqualTo(AttributeValueType.L);
        assertThat(second).isInstanceOf(ListAttributeConverter.class);
        assertThat(second.attributeValueType()).isEqualTo(AttributeValueType.L);
        assertThat(second).isNotSameAs(first);
    }

    @Test
    @DisplayName("An enum type looked up twice returns different enum converters")
    void converterFor_whenEnumTypeCalledTwice_returnsDifferentEnumAttributeConverters() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        EnhancedType<CacheTestEnum> type = EnhancedType.of(CacheTestEnum.class);

        AttributeConverter<CacheTestEnum> first = provider.converterFor(type);
        AttributeConverter<CacheTestEnum> second = provider.converterFor(type);

        assertThat(first).isInstanceOf(EnumAttributeConverter.class);
        assertThat(first.attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(second).isInstanceOf(EnumAttributeConverter.class);
        assertThat(second.attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(second).isNotSameAs(first);
    }

    @Test
    @DisplayName("Equal named document tokens return the same cached document converter")
    void converterFor_whenEqualNamedDocumentTokens_returnsSameCachedDocumentAttributeConverter() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        TableSchema<CacheDocumentType> schema = cacheUnusedSchema(CacheDocumentType.class, CacheDocumentType::new);
        EnhancedType<CacheDocumentType> firstType = EnhancedType.documentOf(CacheDocumentType.class, schema);
        EnhancedType<CacheDocumentType> secondType = EnhancedType.documentOf(CacheDocumentType.class, schema);

        AttributeConverter<CacheDocumentType> first = provider.converterFor(firstType);
        AttributeConverter<CacheDocumentType> second = provider.converterFor(secondType);

        assertThat(first).isInstanceOf(DocumentAttributeConverter.class);
        assertThat(first.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(second).isSameAs(first);
    }

    @Test
    @DisplayName("An anonymous Map raw class document is not cached")
    void converterFor_whenAnonymousMapRawClassDocument_returnsUncachedDocumentAttributeConverter() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        LinkedHashMap<String, Integer> anonymousMap = new LinkedHashMap<String, Integer>() {
        };
        TableSchema<LinkedHashMap<String, Integer>> recordingSchema =
            unusedLinkedHashMapSchema(anonymousMap);
        @SuppressWarnings("unchecked")
        EnhancedType<LinkedHashMap<String, Integer>> type =
            EnhancedType.documentOf((Class) anonymousMap.getClass(), recordingSchema);

        AttributeConverter<LinkedHashMap<String, Integer>> first = provider.converterFor(type);
        AttributeConverter<LinkedHashMap<String, Integer>> second = provider.converterFor(type);

        assertThat(first).isInstanceOf(DocumentAttributeConverter.class);
        assertThat(first.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(second).isInstanceOf(DocumentAttributeConverter.class);
        assertThat(second.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(second).isNotSameAs(first);
    }

    @Test
    @DisplayName("An anonymous Set raw class document is not cached")
    void converterFor_whenAnonymousSetRawClassDocument_returnsUncachedDocumentAttributeConverter() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        LinkedHashSet<String> anonymousSet = new LinkedHashSet<String>() {
        };
        TableSchema<LinkedHashSet<String>> recordingSchema = unusedLinkedHashSetSchema(anonymousSet);
        @SuppressWarnings("unchecked")
        EnhancedType<LinkedHashSet<String>> type =
            EnhancedType.documentOf((Class) anonymousSet.getClass(), recordingSchema);

        AttributeConverter<LinkedHashSet<String>> first = provider.converterFor(type);
        AttributeConverter<LinkedHashSet<String>> second = provider.converterFor(type);

        assertThat(first).isInstanceOf(DocumentAttributeConverter.class);
        assertThat(first.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(second).isInstanceOf(DocumentAttributeConverter.class);
        assertThat(second.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(second).isNotSameAs(first);
    }

    @Test
    @DisplayName("A named custom Map document returns the same cached document converter")
    void converterFor_whenNamedCustomMapDocument_returnsSameCachedDocumentAttributeConverter() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        TableSchema<CacheCustomMap> schema = cacheUnusedSchema(CacheCustomMap.class, CacheCustomMap::new);
        EnhancedType<CacheCustomMap> type = EnhancedType.documentOf(CacheCustomMap.class, schema);

        AttributeConverter<CacheCustomMap> first = provider.converterFor(type);
        AttributeConverter<CacheCustomMap> second = provider.converterFor(type);

        assertThat(first).isInstanceOf(DocumentAttributeConverter.class);
        assertThat(first).isNotInstanceOf(MapAttributeConverter.class);
        assertThat(first.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(second).isSameAs(first);
    }

    @Test
    @DisplayName("Separately constructed equal map tokens share one cache key")
    void converterFor_whenSeparatelyConstructedEqualMapTokens_useOneCacheKey() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<String, Integer>> firstType = EnhancedType.mapOf(String.class, Integer.class);
        EnhancedType<Map<String, Integer>> secondType = EnhancedType.mapOf(String.class, Integer.class);

        AttributeConverter<Map<String, Integer>> first = provider.converterFor(firstType);
        AttributeConverter<Map<String, Integer>> second = provider.converterFor(secondType);

        assertThat(firstType).isNotSameAs(secondType);
        assertThat(firstType).isEqualTo(secondType);
        assertThat(first).isInstanceOf(MapAttributeConverter.class);
        assertThat(second).isSameAs(first);
    }

    @Test
    @DisplayName("A UUID key map is not the string key map converter")
    void converterFor_whenUuidKeyMap_isNotStringKeyMapConverter() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<String, Integer>> stringKeyType = EnhancedType.mapOf(String.class, Integer.class);
        EnhancedType<Map<UUID, Integer>> uuidKeyType = EnhancedType.mapOf(UUID.class, Integer.class);

        AttributeConverter<Map<String, Integer>> stringKeyConverter = provider.converterFor(stringKeyType);
        AttributeConverter<Map<UUID, Integer>> uuidKeyConverter = provider.converterFor(uuidKeyType);

        assertThat(uuidKeyConverter).isInstanceOf(MapAttributeConverter.class);
        assertThat(uuidKeyConverter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(uuidKeyConverter).isNotSameAs(stringKeyConverter);
    }

    @Test
    @DisplayName("A Long value map is not the Integer value map converter")
    void converterFor_whenLongValueMap_isNotIntegerValueMapConverter() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<String, Integer>> integerValueType = EnhancedType.mapOf(String.class, Integer.class);
        EnhancedType<Map<String, Long>> longValueType = EnhancedType.mapOf(String.class, Long.class);

        AttributeConverter<Map<String, Integer>> integerValueConverter = provider.converterFor(integerValueType);
        AttributeConverter<Map<String, Long>> longValueConverter = provider.converterFor(longValueType);

        assertThat(longValueConverter).isInstanceOf(MapAttributeConverter.class);
        assertThat(longValueConverter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(longValueConverter).isNotSameAs(integerValueConverter);
    }

    @Test
    @DisplayName("A failed unsupported map value lookup does not cache the failure")
    void converterFor_whenFailedUnsupportedMapValue_doesNotCacheFailure() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<String, CacheUnsupportedType>> type = EnhancedType.mapOf(String.class, CacheUnsupportedType.class);

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(CacheUnsupportedType.class));
        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(CacheUnsupportedType.class));
    }

    @Test
    @DisplayName("Concurrent map misses leave a later lookup matching one concurrent result")
    void converterFor_whenConcurrentMapMisses_laterLookupMatchesOneConcurrentResult() throws Exception {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<String, Integer>> type = EnhancedType.mapOf(String.class, Integer.class);
        int threadCount = 8;
        CyclicBarrier startBarrier = new CyclicBarrier(threadCount + 1);
        List<AttributeConverter<Map<String, Integer>>> concurrentResults =
            Collections.synchronizedList(new ArrayList<>());
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                try {
                    startBarrier.await(5, TimeUnit.SECONDS);
                    concurrentResults.add(provider.converterFor(type));
                } catch (Throwable t) {
                    failures.add(t);
                }
            });
            threads[i].start();
        }

        startBarrier.await(5, TimeUnit.SECONDS);

        for (int i = 0; i < threadCount; i++) {
            threads[i].join(TimeUnit.SECONDS.toMillis(5));
            assertThat(threads[i].isAlive()).isFalse();
        }

        assertThat(failures).isEmpty();
        assertThat(concurrentResults).hasSize(threadCount);
        for (AttributeConverter<Map<String, Integer>> converter : concurrentResults) {
            assertThat(converter).isInstanceOf(MapAttributeConverter.class);
            assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.M);
        }

        AttributeConverter<Map<String, Integer>> later = provider.converterFor(type);

        assertThat(later).isInstanceOf(MapAttributeConverter.class);
        assertThat(later.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(concurrentResults).anySatisfy(result -> assertThat(result).isSameAs(later));
    }

    @Test
    @DisplayName("A successful String lookup emits the exact debug record")
    void converterFor_whenSuccessfulStringLookup_emitsExactDebugRecord() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();

        try (LogCaptor logCaptor = new LogCaptor(DefaultAttributeConverterProvider.class, Level.DEBUG)) {
            provider.converterFor(EnhancedType.of(String.class));

            List<LogEvent> logEvents = logCaptor.loggedEvents();
            assertThat(logEvents).hasSize(1);
            assertThat(logEvents.get(0).getLevel().name()).isEqualTo(Level.DEBUG.name());
            assertThat(logEvents.get(0).getMessage().getFormattedMessage())
                .isEqualTo("Converter for EnhancedType(java.lang.String): software.amazon.awssdk.enhanced.dynamodb.internal"
                           + ".converter.attribute.StringAttributeConverter");
        }
    }

    @Test
    @DisplayName("A missing converter emits the exact debug record and throws")
    void converterFor_whenMissingConverter_emitsExactDebugRecordAndThrows() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        EnhancedType<CacheUnsupportedType> type = EnhancedType.of(CacheUnsupportedType.class);

        try (LogCaptor logCaptor = new LogCaptor(DefaultAttributeConverterProvider.class, Level.DEBUG)) {
            Throwable thrown = catchThrowable(() -> provider.converterFor(type));

            assertThat(thrown).isInstanceOf(IllegalStateException.class)
                              .hasMessage("Converter not found for " + type);
            List<LogEvent> logEvents = logCaptor.loggedEvents();
            assertThat(logEvents).hasSize(1);
            assertThat(logEvents.get(0).getLevel().name()).isEqualTo(Level.DEBUG.name());
            assertThat(logEvents.get(0).getMessage().getFormattedMessage())
                .isEqualTo("No converter available for " + type);
        }
    }

    @Test
    @DisplayName("A failed Boolean set lookup does not cache the failure")
    void converterFor_whenFailedBooleanSet_doesNotCacheFailure() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        EnhancedType<Set<Boolean>> type = EnhancedType.setOf(Boolean.class);
        String expectedMessage =
            "SetAttributeConverter cannot be created with a parameterized type of 'class java.lang.Boolean'. "
            + "Supported parameterized types must convert to B, S or N DynamoDB AttributeValues.";

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(expectedMessage);
        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(expectedMessage);
    }

    @Test
    @DisplayName("A document conversion failure does not evict the cached converter")
    void converterFor_whenDocumentConversionFailure_doesNotEvictCachedConverter() {
        DefaultAttributeConverterProvider provider = new DefaultAttributeConverterProvider();
        TableSchema<ThrowingWriteDocument> schema = throwingWriteSchema();
        EnhancedType<ThrowingWriteDocument> type = EnhancedType.documentOf(ThrowingWriteDocument.class, schema);
        ThrowingWriteDocument input = new ThrowingWriteDocument();

        AttributeConverter<ThrowingWriteDocument> first = provider.converterFor(type);
        assertThatThrownBy(() -> first.transformFrom(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Document schema failed while converting ThrowingWriteDocument to a map");
        AttributeConverter<ThrowingWriteDocument> second = provider.converterFor(type);

        assertThat(first).isInstanceOf(DocumentAttributeConverter.class);
        assertThat(second).isSameAs(first);
    }

    private static <T> TableSchema<T> cacheUnusedSchema(Class<T> type, java.util.function.Supplier<T> supplier) {
        return StaticTableSchema.builder(type).newItemSupplier(supplier).build();
    }

    @SuppressWarnings("unchecked")
    private static TableSchema<LinkedHashMap<String, Integer>> unusedLinkedHashMapSchema(
        LinkedHashMap<String, Integer> instance) {
        return cacheUnusedSchema((Class<LinkedHashMap<String, Integer>>) instance.getClass(), () -> instance);
    }

    @SuppressWarnings("unchecked")
    private static TableSchema<LinkedHashSet<String>> unusedLinkedHashSetSchema(LinkedHashSet<String> instance) {
        return cacheUnusedSchema((Class<LinkedHashSet<String>>) instance.getClass(), () -> instance);
    }

    private static TableSchema<ThrowingWriteDocument> throwingWriteSchema() {
        return StaticTableSchema.builder(ThrowingWriteDocument.class)
                                .newItemSupplier(ThrowingWriteDocument::new)
                                .addAttribute(String.class, a -> a.name("name")
                                                                  .getter(ThrowingWriteDocument::getName)
                                                                  .setter(ThrowingWriteDocument::setName))
                                .build();
    }

    static class CacheUnsupportedType {
    }

    static class CacheDocumentType {
    }

    static class ThrowingWriteDocument {
        public String getName() {
            throw new IllegalArgumentException("Document schema failed while converting ThrowingWriteDocument to a map");
        }

        public void setName(String name) {
        }
    }

    enum CacheTestEnum {
        OPEN,
        CLOSED
    }

    static class CacheCustomMap extends HashMap<String, Integer> {
    }

    // General lookup, raw-type, document, and custom-provider coverage.
    private DefaultAttributeConverterProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DefaultAttributeConverterProvider();
    }

    @Test
    @DisplayName("Null EnhancedType fails during cache lookup")
    void converterFor_whenNullType_throwsNullPointerException() {
        assertThatThrownBy(() -> provider.converterFor(null))
            .isInstanceOf(NullPointerException.class)
            .satisfies(ex -> assertThat(ex.getMessage() == null || ex.getMessage().contains("null")).isTrue());
    }

    @Test
    @DisplayName("Registered String type returns the cached string converter")
    void converterFor_whenRegisteredStringType_returnsCachedStringAttributeConverter() {
        EnhancedType<String> type = EnhancedType.of(String.class);

        AttributeConverter<String> converter = provider.converterFor(type);

        assertThat(converter).isInstanceOf(StringAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(provider.converterFor(type)).isSameAs(converter);
    }

    @Test
    @DisplayName("Unbounded wildcard type fails while reading the raw class")
    void converterFor_whenUnboundedWildcardType_throwsIllegalArgumentException() {
        EnhancedType<?> type = EnhancedType.of(unboundedWildcardType());

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A wildcard type is not expected here.");
    }

    @Test
    @DisplayName("Document token with a null raw class fails during assignability")
    void converterFor_whenDocumentTokenWithNullRawClass_throwsNullPointerException() {
        TableSchema<DocumentType> schema = mock(TableSchema.class);
        EnhancedType<DocumentType> type = EnhancedType.documentOf((Class<DocumentType>) null, schema);

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(NullPointerException.class)
            .satisfies(ex -> assertThat(ex.getMessage() == null || ex.getMessage().contains("null")).isTrue());
        verifyNoInteractions(schema);
    }

    @Test
    @DisplayName("Map of String to Integer selects and caches a map converter")
    void converterFor_whenStringToIntegerMap_returnsCachedMapAttributeConverter() {
        EnhancedType<Map<String, Integer>> type = EnhancedType.mapOf(String.class, Integer.class);

        AttributeConverter<Map<String, Integer>> converter = provider.converterFor(type);

        assertThat(converter).isInstanceOf(MapAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(provider.converterFor(type)).isSameAs(converter);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rawCollectionTypes")
    @DisplayName("A raw collection declaration requires type parameters")
    void converterFor_whenRawCollectionType_throwsConverterNotFound(EnhancedType<?> type) {
        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type + ". Type parameters are required for this type.");
    }

    private static Stream<EnhancedType<?>> rawCollectionTypes() {
        return Stream.of(EnhancedType.of(Map.class), EnhancedType.of(Set.class), EnhancedType.of(List.class));
    }

    @Test
    @DisplayName("A Map document token fails while reading map parameters before the schema is read")
    void converterFor_whenMapDocumentToken_throwsNullPointerExceptionBeforeReadingSchema() {
        TableSchema<Map> mapSchema = mock(TableSchema.class);
        EnhancedType<Map> type = EnhancedType.documentOf(Map.class, mapSchema);

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(Map.class)
                        + ". Type parameters are required for this type.");
        verifyNoInteractions(mapSchema);
    }

    @Test
    @DisplayName("Map with an unsupported key type fails during string-key lookup")
    void converterFor_whenMapWithUnsupportedKey_throwsIllegalArgumentException() {
        EnhancedType<Map<UnsupportedKey, String>> type = EnhancedType.mapOf(UnsupportedKey.class, String.class);

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No string converter exists for " + UnsupportedKey.class);
    }

    @Test
    @DisplayName("Map with an unsupported value type fails during recursive lookup")
    void converterFor_whenMapWithUnsupportedValue_throwsIllegalStateException() {
        EnhancedType<Map<String, UnsupportedType>> type = EnhancedType.mapOf(String.class, UnsupportedType.class);

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(UnsupportedType.class));
    }

    @Test
    @DisplayName("Schema-bearing Map token replaces the generated map converter")
    void converterFor_whenSchemaBearingMapType_returnsCachedDocumentAttributeConverter() {
        TableSchema<Map<String, Integer>> mapSchema = unusedMapSchema();
        EnhancedType<Map<String, Integer>> type = new EnhancedType<Map<String, Integer>>() {
            @Override
            public Optional<TableSchema<Map<String, Integer>>> tableSchema() {
                return Optional.of(mapSchema);
            }
        };

        AttributeConverter<Map<String, Integer>> converter = provider.converterFor(type);

        assertThat(converter).isInstanceOf(DocumentAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(provider.converterFor(type)).isSameAs(converter);
    }

    @Test
    @DisplayName("Plain Object type fails converter lookup")
    void converterFor_whenObjectType_throwsConverterNotFound() {
        EnhancedType<Object> type = EnhancedType.of(Object.class);

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("A schema-bearing Object document token creates and caches a document converter")
    void converterFor_whenObjectDocumentToken_returnsCachedDocumentConverter() {
        TableSchema<Object> objectSchema = mock(TableSchema.class);
        EnhancedType<Object> type = EnhancedType.documentOf(Object.class, objectSchema);

        AttributeConverter<Object> converter = provider.converterFor(type);

        assertThat(converter).isInstanceOf(DocumentAttributeConverter.class);
        assertThat(provider.converterFor(type)).isSameAs(converter);
        verifyNoInteractions(objectSchema);
    }

    @Test
    @DisplayName("Set of String selects and caches a string-set converter")
    void converterFor_whenStringSet_returnsCachedSetAttributeConverter() {
        EnhancedType<Set<String>> type = EnhancedType.setOf(String.class);

        AttributeConverter<Set<String>> converter = provider.converterFor(type);

        assertThat(converter).isInstanceOf(SetAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.SS);
        assertThat(provider.converterFor(type)).isSameAs(converter);
    }

    @Test
    @DisplayName("A Set document token fails while reading set parameters before the schema is read")
    void converterFor_whenSetDocumentToken_throwsNullPointerExceptionBeforeReadingSchema() {
        TableSchema<Set> setSchema = mock(TableSchema.class);
        EnhancedType<Set> type = EnhancedType.documentOf(Set.class, setSchema);

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(Set.class)
                        + ". Type parameters are required for this type.");
        verifyNoInteractions(setSchema);
    }

    @Test
    @DisplayName("Set with an unsupported member type fails during recursive lookup")
    void converterFor_whenSetWithUnsupportedMember_throwsIllegalStateException() {
        EnhancedType<Set<UnsupportedType>> type = EnhancedType.setOf(UnsupportedType.class);

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(UnsupportedType.class));
    }

    @Test
    @DisplayName("Set of Boolean is rejected because BOOL is not a set member type")
    void converterFor_whenBooleanSet_throwsIllegalArgumentException() {
        EnhancedType<Set<Boolean>> type = EnhancedType.setOf(Boolean.class);

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("SetAttributeConverter cannot be created with a parameterized type of 'class java.lang.Boolean'. "
                        + "Supported parameterized types must convert to B, S or N DynamoDB AttributeValues.");
    }

    @Test
    @DisplayName("Schema-bearing Set token replaces the generated set converter")
    void converterFor_whenSchemaBearingSetType_returnsCachedDocumentAttributeConverter() {
        TableSchema<Set<String>> setSchema = unusedSetSchema();
        EnhancedType<Set<String>> type = new EnhancedType<Set<String>>() {
            @Override
            public Optional<TableSchema<Set<String>>> tableSchema() {
                return Optional.of(setSchema);
            }
        };

        AttributeConverter<Set<String>> converter = provider.converterFor(type);

        assertThat(converter).isInstanceOf(DocumentAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(provider.converterFor(type)).isSameAs(converter);
    }

    @Test
    @DisplayName("Collection of String enters the set branch and caches a string-set converter")
    void converterFor_whenStringCollection_returnsCachedSetAttributeConverter() {
        EnhancedType<Collection<String>> type = EnhancedType.collectionOf(String.class);

        AttributeConverter<Collection<String>> converter = provider.converterFor(type);

        assertThat(converter).isInstanceOf(SetAttributeConverter.class);
        assertThat(converter.type()).isEqualTo(EnhancedType.setOf(String.class));
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.SS);
        assertThat(provider.converterFor(type)).isSameAs(converter);
    }

    @Test
    @DisplayName("Iterable of String enters the set branch and caches a string-set converter")
    void converterFor_whenStringIterable_returnsCachedSetAttributeConverter() {
        EnhancedType<Iterable<String>> type = new EnhancedType<Iterable<String>>() {
        };

        AttributeConverter<Iterable<String>> converter = provider.converterFor(type);

        assertThat(converter).isInstanceOf(SetAttributeConverter.class);
        assertThat(converter.type()).isEqualTo(EnhancedType.setOf(String.class));
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.SS);
        assertThat(provider.converterFor(type)).isSameAs(converter);
    }

    @Test
    @DisplayName("List of String selects a list converter without caching it")
    void converterFor_whenStringList_returnsUncachedListAttributeConverter() {
        EnhancedType<List<String>> type = EnhancedType.listOf(String.class);

        AttributeConverter<List<String>> first = provider.converterFor(type);
        AttributeConverter<List<String>> second = provider.converterFor(type);

        assertThat(first).isInstanceOf(ListAttributeConverter.class);
        assertThat(first.attributeValueType()).isEqualTo(AttributeValueType.L);
        assertThat(second).isInstanceOf(ListAttributeConverter.class);
        assertThat(second).isNotSameAs(first);
    }

    @Test
    @DisplayName("A List document token fails while reading list parameters before the schema is read")
    void converterFor_whenListDocumentToken_throwsIllegalStateExceptionBeforeReadingSchema() {
        TableSchema<List> listSchema = mock(TableSchema.class);
        EnhancedType<List> type = EnhancedType.documentOf(List.class, listSchema);

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(List.class)
                        + ". Type parameters are required for this type.");
        verifyNoInteractions(listSchema);
    }

    @Test
    @DisplayName("List with an unsupported member type fails during recursive lookup")
    void converterFor_whenListWithUnsupportedMember_throwsIllegalStateException() {
        EnhancedType<List<UnsupportedType>> type = EnhancedType.listOf(UnsupportedType.class);

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(UnsupportedType.class));
    }

    @Test
    @DisplayName("Enum type selects an enum converter without caching it")
    void converterFor_whenEnumType_returnsUncachedEnumAttributeConverter() {
        EnhancedType<TestEnum> type = EnhancedType.of(TestEnum.class);

        AttributeConverter<TestEnum> first = provider.converterFor(type);
        AttributeConverter<TestEnum> second = provider.converterFor(type);

        assertThat(first).isInstanceOf(EnumAttributeConverter.class);
        assertThat(first.attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(second).isInstanceOf(EnumAttributeConverter.class);
        assertThat(second).isNotSameAs(first);
    }

    @Test
    @DisplayName("Enum type with a table schema still selects the enum converter")
    void converterFor_whenEnumDocumentToken_returnsUncachedEnumAttributeConverter() {
        TableSchema<TestEnum> enumSchema = mock(TableSchema.class);
        EnhancedType<TestEnum> type = EnhancedType.documentOf(TestEnum.class, enumSchema);

        AttributeConverter<TestEnum> converter = provider.converterFor(type);

        assertThat(converter).isInstanceOf(EnumAttributeConverter.class);
        verifyNoInteractions(enumSchema);
        assertThat(provider.converterFor(type)).isNotSameAs(converter);
    }

    @Test
    @DisplayName("Named document type selects and caches a document converter")
    void converterFor_whenNamedDocumentType_returnsCachedDocumentAttributeConverter() {
        TableSchema<DocumentType> documentSchema = unusedSchema(DocumentType.class, DocumentType::new);
        EnhancedType<DocumentType> type = EnhancedType.documentOf(DocumentType.class, documentSchema);

        AttributeConverter<DocumentType> converter = provider.converterFor(type);

        assertThat(converter).isInstanceOf(DocumentAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(provider.converterFor(type)).isSameAs(converter);
    }

    @Test
    @DisplayName("Anonymous document class is not cached")
    void converterFor_whenAnonymousDocumentType_returnsUncachedDocumentAttributeConverter() {
        DocumentType anonymousDocument = new DocumentType() {
        };
        TableSchema<DocumentType> schema = unusedSchema(DocumentType.class, DocumentType::new);
        @SuppressWarnings("unchecked")
        EnhancedType<DocumentType> type = EnhancedType.documentOf((Class) anonymousDocument.getClass(), schema);

        AttributeConverter<DocumentType> first = provider.converterFor(type);
        AttributeConverter<DocumentType> second = provider.converterFor(type);

        assertThat(first).isInstanceOf(DocumentAttributeConverter.class);
        assertThat(first.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(second).isInstanceOf(DocumentAttributeConverter.class);
        assertThat(second.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(second).isNotSameAs(first);
    }

    @Test
    @DisplayName("Named concrete Map class with a schema reaches the document branch")
    void converterFor_whenNamedCustomMapDocument_returnsCachedDocumentAttributeConverter() {
        TableSchema<CustomMap> customMapSchema = unusedSchema(CustomMap.class, CustomMap::new);
        EnhancedType<CustomMap> type = EnhancedType.documentOf(CustomMap.class, customMapSchema);

        AttributeConverter<CustomMap> converter = provider.converterFor(type);

        assertThat(converter).isInstanceOf(DocumentAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(provider.converterFor(type)).isSameAs(converter);
    }

    @Test
    @DisplayName("Named concrete Set class with a schema reaches the document branch")
    void converterFor_whenNamedCustomSetDocument_returnsCachedDocumentAttributeConverter() {
        TableSchema<CustomSet> customSetSchema = unusedSchema(CustomSet.class, CustomSet::new);
        EnhancedType<CustomSet> type = EnhancedType.documentOf(CustomSet.class, customSetSchema);

        AttributeConverter<CustomSet> converter = provider.converterFor(type);

        assertThat(converter).isInstanceOf(DocumentAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(provider.converterFor(type)).isSameAs(converter);
    }

    @Test
    @DisplayName("Named concrete List class with a schema reaches the document branch")
    void converterFor_whenNamedCustomListDocument_returnsCachedDocumentAttributeConverter() {
        TableSchema<CustomList> customListSchema = unusedSchema(CustomList.class, CustomList::new);
        EnhancedType<CustomList> type = EnhancedType.documentOf(CustomList.class, customListSchema);

        AttributeConverter<CustomList> converter = provider.converterFor(type);

        assertThat(converter).isInstanceOf(DocumentAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(provider.converterFor(type)).isSameAs(converter);
    }

    @Test
    @DisplayName("Named class without a converter or schema fails lookup")
    void converterFor_whenUnsupportedType_throwsIllegalStateException() {
        EnhancedType<UnsupportedType> type = EnhancedType.of(UnsupportedType.class);

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("Document token created with a null schema is treated as absent")
    void converterFor_whenDocumentTokenWithNullSchema_throwsIllegalStateException() {
        EnhancedType<DocumentType> type = EnhancedType.documentOf(DocumentType.class, null);

        assertThat(type.tableSchema()).isEmpty();
        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("Deque of String has no default converter")
    void converterFor_whenStringDeque_throwsIllegalStateException() {
        EnhancedType<?> type = EnhancedType.dequeOf(String.class);

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("Concrete HashMap declaration has no default converter")
    void converterFor_whenHashMapType_throwsIllegalStateException() {
        EnhancedType<HashMap<String, Integer>> type = new EnhancedType<HashMap<String, Integer>>() {
        };

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("Concrete HashSet declaration has no default converter")
    void converterFor_whenHashSetType_throwsIllegalStateException() {
        EnhancedType<HashSet<String>> type = new EnhancedType<HashSet<String>>() {
        };

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("Concrete ArrayList declaration has no default converter")
    void converterFor_whenArrayListType_throwsIllegalStateException() {
        EnhancedType<ArrayList<String>> type = new EnhancedType<ArrayList<String>>() {
        };

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    private static <T> TableSchema<T> unusedSchema(Class<T> type, Supplier<T> supplier) {
        return StaticTableSchema.builder(type).newItemSupplier(supplier).build();
    }

    @SuppressWarnings("unchecked")
    private static TableSchema<Map<String, Integer>> unusedMapSchema() {
        return unusedSchema((Class<Map<String, Integer>>) (Class<?>) Map.class, HashMap::new);
    }

    @SuppressWarnings("unchecked")
    private static TableSchema<Set<String>> unusedSetSchema() {
        return unusedSchema((Class<Set<String>>) (Class<?>) Set.class, HashSet::new);
    }

    private static Type unboundedWildcardType() {
        try {
            return WildcardHolder.class.getDeclaredField("unbounded").getGenericType();
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class WildcardHolder {
        List<?> unbounded;
    }

    static class UnsupportedType {
    }

    static class UnsupportedKey {
    }

    static class DocumentType {
    }

    enum TestEnum {
        OPEN,
        CLOSED
    }

    static class CustomMap extends HashMap<String, Integer> {
    }

    static class CustomSet extends HashSet<String> {
    }

    static class CustomList extends ArrayList<String> {
    }

    // Provider construction, registration, and precedence coverage.
    @Test
    @DisplayName("defaultProvider and create return the same singleton and cached string converter")
    void create_whenDefaultProviderAlsoCalled_returnsSameSingletonAndCachedStringConverter() {
        AttributeConverterProvider defaultProvider = AttributeConverterProvider.defaultProvider();
        DefaultAttributeConverterProvider created = DefaultAttributeConverterProvider.create();
        EnhancedType<String> type = EnhancedType.of(String.class);

        AttributeConverter<String> defaultConverter = defaultProvider.converterFor(type);
        AttributeConverter<String> createdConverter = created.converterFor(type);

        assertThat(defaultProvider).isSameAs(created);
        assertThat(defaultConverter).isInstanceOf(StringAttributeConverter.class);
        assertThat(createdConverter).isSameAs(defaultConverter);
    }

    @Test
    @DisplayName("Repeated create calls share the generated map converter cache")
    void create_whenCalledTwice_sharesGeneratedMapConverterCache() {
        DefaultAttributeConverterProvider firstProvider = DefaultAttributeConverterProvider.create();
        DefaultAttributeConverterProvider secondProvider = DefaultAttributeConverterProvider.create();
        EnhancedType<Map<String, Integer>> type = EnhancedType.mapOf(String.class, Integer.class);

        AttributeConverter<Map<String, Integer>> firstConverter = firstProvider.converterFor(type);
        AttributeConverter<Map<String, Integer>> secondConverter = secondProvider.converterFor(type);

        assertThat(firstProvider).isSameAs(secondProvider);
        assertThat(firstConverter).isInstanceOf(MapAttributeConverter.class);
        assertThat(secondConverter).isSameAs(firstConverter);
    }

    @Test
    @DisplayName("The public constructor creates an isolated default registry")
    void converterFor_whenPubliclyConstructedProvider_isIsolatedFromSingletonRegistry() {
        DefaultAttributeConverterProvider singleton = DefaultAttributeConverterProvider.create();
        DefaultAttributeConverterProvider constructed = new DefaultAttributeConverterProvider();
        EnhancedType<String> type = EnhancedType.of(String.class);

        AttributeConverter<String> singletonConverter = singleton.converterFor(type);
        AttributeConverter<String> constructedConverter = constructed.converterFor(type);

        assertThat(singleton).isNotSameAs(constructed);
        assertThat(singletonConverter).isInstanceOf(StringAttributeConverter.class);
        assertThat(singletonConverter.attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(constructedConverter).isInstanceOf(StringAttributeConverter.class);
        assertThat(constructedConverter.attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(constructedConverter).isNotSameAs(singletonConverter);
    }

    @Test
    @DisplayName("Publicly constructed providers own separate generated map caches")
    void converterFor_whenTwoPubliclyConstructedProviders_ownSeparateGeneratedMapCaches() {
        DefaultAttributeConverterProvider firstProvider = new DefaultAttributeConverterProvider();
        DefaultAttributeConverterProvider secondProvider = new DefaultAttributeConverterProvider();
        EnhancedType<Map<String, Integer>> type = EnhancedType.mapOf(String.class, Integer.class);

        AttributeConverter<Map<String, Integer>> firstConverter = firstProvider.converterFor(type);
        AttributeConverter<Map<String, Integer>> secondConverter = secondProvider.converterFor(type);

        assertThat(firstProvider).isNotSameAs(secondProvider);
        assertThat(firstConverter).isInstanceOf(MapAttributeConverter.class);
        assertThat(firstConverter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(secondConverter).isInstanceOf(MapAttributeConverter.class);
        assertThat(secondConverter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(secondConverter).isNotSameAs(firstConverter);
    }

    @Test
    @DisplayName("An empty builder has no converter for String")
    void converterFor_whenEmptyBuilder_throwsConverterNotFoundForString() {
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.builder().build();
        EnhancedType<String> type = EnhancedType.of(String.class);

        assertThatThrownBy(() -> provider.converterFor(type))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(String.class));
    }

    @Test
    @DisplayName("A builder with a custom converter returns the supplied reference")
    void converterFor_whenBuilderWithCustomConverter_returnsSuppliedReference() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        TestAttributeConverter<CustomType> customConverter = new TestAttributeConverter<>(type);
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.builder()
                                                                                      .addConverter(customConverter)
                                                                                      .build();

        AttributeConverter<CustomType> converter = provider.converterFor(type);

        assertThat(converter).isSameAs(customConverter);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.S);
    }

    @Test
    @DisplayName("A primitive converter alias serves both int and Integer")
    void converterFor_whenBuilderWithPrimitiveAlias_returnsSameConverterForIntAndInteger() {
        TestPrimitiveConverter<Integer> primitiveConverter =
            new TestPrimitiveConverter<>(EnhancedType.of(Integer.class), EnhancedType.of(int.class));
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.builder()
                                                                                      .addConverter(primitiveConverter)
                                                                                      .build();

        AttributeConverter<Integer> integerConverter = provider.converterFor(EnhancedType.of(Integer.class));
        AttributeConverter<Integer> intConverter = provider.converterFor(EnhancedType.of(int.class));

        assertThat(intConverter).isSameAs(primitiveConverter);
        assertThat(integerConverter).isSameAs(intConverter);
    }

    @Test
    @DisplayName("The first added converter wins for a duplicate type")
    void converterFor_whenFirstAddedDuplicateRegistration_wins() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        TestAttributeConverter<CustomType> firstConverter = new TestAttributeConverter<>(type);
        TestAttributeConverter<CustomType> secondConverter = new TestAttributeConverter<>(type);
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.builder()
                                                                                      .addConverter(firstConverter)
                                                                                      .addConverter(secondConverter)
                                                                                      .build();

        AttributeConverter<CustomType> converter = provider.converterFor(type);

        assertThat(converter).isSameAs(firstConverter);
        assertThat(converter).isNotSameAs(secondConverter);
    }

    @Test
    @DisplayName("Adding a null converter is rejected")
    void addConverter_whenNullConverter_throwsNullPointerException() {
        DefaultAttributeConverterProvider.Builder builder = DefaultAttributeConverterProvider.builder();

        assertThatThrownBy(() -> builder.addConverter(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("converter must not be null.");
    }

    @Test
    @DisplayName("A reused builder creates separate providers that share the supplied converter")
    void build_whenReusedBuilder_createsSeparateProvidersSharingSuppliedConverter() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        TestAttributeConverter<CustomType> customConverter = new TestAttributeConverter<>(type);
        DefaultAttributeConverterProvider.Builder builder = DefaultAttributeConverterProvider.builder()
                                                                                             .addConverter(customConverter);

        DefaultAttributeConverterProvider firstProvider = builder.build();
        DefaultAttributeConverterProvider secondProvider = builder.build();
        AttributeConverter<CustomType> firstConverter = firstProvider.converterFor(type);
        AttributeConverter<CustomType> secondConverter = secondProvider.converterFor(type);

        assertThat(firstProvider).isNotSameAs(secondProvider);
        assertThat(firstConverter).isSameAs(customConverter);
        assertThat(secondConverter).isSameAs(customConverter);
    }

    @Test
    @DisplayName("An exact custom map registration precedes generated map selection")
    void converterFor_whenExactCustomMapRegistration_precedesGeneratedMapSelection() {
        EnhancedType<Map<String, Integer>> type = EnhancedType.mapOf(String.class, Integer.class);
        TestAttributeConverter<Map<String, Integer>> customConverter = new TestAttributeConverter<>(type);
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.builder()
                                                                                      .addConverter(customConverter)
                                                                                      .build();

        AttributeConverter<Map<String, Integer>> converter = provider.converterFor(type);

        assertThat(converter).isSameAs(customConverter);
        assertThat(converter).isNotInstanceOf(MapAttributeConverter.class);
    }

    @Test
    @DisplayName("An exact custom set registration precedes generated set selection")
    void converterFor_whenExactCustomSetRegistration_precedesGeneratedSetSelection() {
        EnhancedType<Set<String>> type = EnhancedType.setOf(String.class);
        TestAttributeConverter<Set<String>> customConverter = new TestAttributeConverter<>(type);
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.builder()
                                                                                      .addConverter(customConverter)
                                                                                      .build();

        AttributeConverter<Set<String>> converter = provider.converterFor(type);

        assertThat(converter).isSameAs(customConverter);
        assertThat(converter).isNotInstanceOf(SetAttributeConverter.class);
    }

    @Test
    @DisplayName("An exact custom list registration precedes generated list selection")
    void converterFor_whenExactCustomListRegistration_precedesGeneratedListSelection() {
        EnhancedType<List<String>> type = EnhancedType.listOf(String.class);
        TestAttributeConverter<List<String>> customConverter = new TestAttributeConverter<>(type);
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.builder()
                                                                                      .addConverter(customConverter)
                                                                                      .build();

        AttributeConverter<List<String>> converter = provider.converterFor(type);

        assertThat(converter).isSameAs(customConverter);
        assertThat(converter).isNotInstanceOf(ListAttributeConverter.class);
    }

    @Test
    @DisplayName("An exact custom enum registration precedes generated enum selection")
    void converterFor_whenExactCustomEnumRegistration_precedesGeneratedEnumSelection() {
        EnhancedType<RegistryTestEnum> type = EnhancedType.of(RegistryTestEnum.class);
        TestAttributeConverter<RegistryTestEnum> customConverter = new TestAttributeConverter<>(type);
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.builder()
                                                                                      .addConverter(customConverter)
                                                                                      .build();

        AttributeConverter<RegistryTestEnum> converter = provider.converterFor(type);

        assertThat(converter).isSameAs(customConverter);
        assertThat(converter).isNotInstanceOf(EnumAttributeConverter.class);
    }

    @Test
    @DisplayName("An exact custom document registration precedes generated document selection")
    void converterFor_whenExactCustomDocumentRegistration_precedesGeneratedDocumentSelection() {
        TableSchema<RegistryDocumentType> schema =
            StaticTableSchema.builder(RegistryDocumentType.class).newItemSupplier(RegistryDocumentType::new).build();
        EnhancedType<RegistryDocumentType> type = EnhancedType.documentOf(RegistryDocumentType.class, schema);
        TestAttributeConverter<RegistryDocumentType> customConverter = new TestAttributeConverter<>(type);
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.builder()
                                                                                      .addConverter(customConverter)
                                                                                      .build();

        AttributeConverter<RegistryDocumentType> converter = provider.converterFor(type);

        assertThat(converter).isSameAs(customConverter);
        assertThat(converter).isNotInstanceOf(DocumentAttributeConverter.class);
    }

    @Test
    @DisplayName("A built provider is isolated from later builder mutation")
    void converterFor_whenBuilderMutatedAfterFirstBuild_throwsThenReturnsLaterConverter() {
        TestAttributeConverter<CustomType> firstConverter =
            new TestAttributeConverter<>(EnhancedType.of(CustomType.class));
        DefaultAttributeConverterProvider.Builder builder = DefaultAttributeConverterProvider.builder()
                                                                                             .addConverter(firstConverter);
        DefaultAttributeConverterProvider firstProvider = builder.build();
        EnhancedType<SecondCustomType> secondType = EnhancedType.of(SecondCustomType.class);
        TestAttributeConverter<SecondCustomType> secondConverter = new TestAttributeConverter<>(secondType);
        builder.addConverter(secondConverter);
        DefaultAttributeConverterProvider secondProvider = builder.build();

        assertThatThrownBy(() -> firstProvider.converterFor(secondType))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + secondType);
        assertThat(secondProvider.converterFor(secondType)).isSameAs(secondConverter);
    }

    @Test
    @DisplayName("An exact custom registration intercepts a schema-bearing Object type")
    void converterFor_whenExactCustomRegistration_interceptsSchemaBearingObject() {
        TableSchema<Object> objectSchema =
            StaticTableSchema.builder(Object.class).newItemSupplier(Object::new).build();
        EnhancedType<Object> type = EnhancedType.documentOf(Object.class, objectSchema);
        TestAttributeConverter<Object> customConverter = new TestAttributeConverter<>(type);
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.builder()
                                                                                      .addConverter(customConverter)
                                                                                      .build();

        AttributeConverter<Object> converter = provider.converterFor(type);

        assertThat(converter).isSameAs(customConverter);
        assertThat(converter).isNotInstanceOf(MapAttributeConverter.class);
    }

    private static final class TestAttributeConverter<T> implements AttributeConverter<T> {
        private final EnhancedType<T> type;

        TestAttributeConverter(EnhancedType<T> type) {
            this.type = type;
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
            return AttributeValueType.S;
        }
    }

    private static final class TestPrimitiveConverter<T> implements AttributeConverter<T>, PrimitiveConverter<T> {
        private final EnhancedType<T> type;
        private final EnhancedType<T> primitiveType;

        TestPrimitiveConverter(EnhancedType<T> type, EnhancedType<T> primitiveType) {
            this.type = type;
            this.primitiveType = primitiveType;
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
            return AttributeValueType.S;
        }

        @Override
        public EnhancedType<T> primitiveType() {
            return primitiveType;
        }
    }

    static class RegistryDocumentType {
    }

    enum RegistryTestEnum {
        OPEN,
        CLOSED
    }

    static class CustomType {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    static class SecondCustomType {
    }
}

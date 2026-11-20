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

package software.amazon.awssdk.mapper.dynamodb.jsonfixtures;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Object shapes and JSON inputs for checking that {@link software.amazon.awssdk.mapper.dynamodb.DynamoDBTypeConvertedJson}
 * writes and reads the same JSON as the v1 mapper.
 *
 * <p>The shapes follow how customers used the v1 annotation: Lombok-style POJOs with private fields and generated
 * accessors, nested beans and collections, named {@code ArrayList} subclasses passed as {@code targetType()}, numbers,
 * dates and strings that need escaping, and classes with {@code @JsonProperty}, {@code @JsonIgnore} and
 * {@code @JsonPropertyOrder}.
 */
public final class JsonModelFixtures {

    private JsonModelFixtures() {
    }

    /**
     * Every value whose serialized form must match jackson-databind exactly.
     */
    public static List<Object> writeCorpus() {
        List<Object> corpus = new ArrayList<>();

        // --- The example from the annotation's own javadoc. If nothing else matches, this must. ---
        corpus.add(new Currency(79.99, "USD"));
        corpus.add(new Currency(null, null));

        // --- Lombok-shaped beans: the dominant real-world shape. ---
        corpus.add(LombokShaped.populated());
        corpus.add(new LombokShaped());

        // --- Nesting and collections. ---
        corpus.add(new Nested(new Currency(1.0, "EUR"), Arrays.asList(new Currency(2.0, "GBP"), null)));
        corpus.add(new Nested(null, Collections.emptyList()));
        corpus.add(currencyList());
        corpus.add(new CurrencyList());
        corpus.add(Arrays.asList(new Currency(3.0, "JPY"), new Currency(4.0, "CHF")));
        corpus.add(new HasCollections());
        corpus.add(HasCollections.populated());

        // --- Maps: key stringification and iteration order. ---
        corpus.add(stringKeyedMap());
        corpus.add(integerKeyedMap());
        corpus.add(enumKeyedMap());
        corpus.add(sortedMap());
        corpus.add(new HasMaps(stringKeyedMap()));

        // --- Numbers, where a hand-written encoder is most likely to drift. ---
        corpus.add(Numbers.populated());
        corpus.add(new Numbers());
        corpus.add(new Doubles(0.0, -0.0, 1.0));
        corpus.add(new Doubles(1.0E20, 1.0E-20, 1.0 / 3.0));
        corpus.add(new Doubles(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY));
        corpus.add(new Doubles(Double.MIN_VALUE, Double.MAX_VALUE, Double.MIN_NORMAL));
        corpus.add(new Floats(0.0f, 1.0f / 3.0f, Float.MAX_VALUE));
        corpus.add(new Bigs(new BigDecimal("1.2300"), BigInteger.valueOf(42)));
        corpus.add(new Bigs(new BigDecimal("1E+10"), new BigInteger("123456789012345678901234567890")));
        corpus.add(new Bigs(new BigDecimal("0.000000000000000000001"), BigInteger.ZERO));
        corpus.add(new Bigs(BigDecimal.ZERO.setScale(5), BigInteger.valueOf(-1)));

        // --- Strings and escaping, which is jackson-core's job and so should be free. ---
        for (String string : STRINGS) {
            corpus.add(new HasString(string));
        }
        corpus.add(new HasChar('a'));
        corpus.add(new HasChar('"'));
        corpus.add(new HasChar('\n'));

        // --- Other scalars with dedicated databind serializers. ---
        corpus.add(new HasDate(new Date(0L), new Date(1_700_000_000_123L)));
        corpus.add(new HasDate(null, null));
        corpus.add(new HasCalendar(calendar(1_700_000_000_123L)));
        corpus.add(new HasUuid(UUID.fromString("c2a4fcd3-0c3b-4cfb-9ff7-c1cb03e2e9f6")));
        corpus.add(new HasEnum(Color.RED));
        corpus.add(new HasEnum(null));
        corpus.add(new HasBytes(new byte[] {0, 1, 2, -1}));
        corpus.add(new HasBytes(new byte[0]));
        corpus.add(new HasBytes(null));
        corpus.add(new HasChars(new char[] {'h', 'i'}));
        corpus.add(new HasArrays());
        corpus.add(HasArrays.populated());

        // --- Property discovery and ordering. ---
        corpus.add(new PublicFields());
        corpus.add(new MixedVisibility());
        corpus.add(new SkipsStaticAndTransient());
        corpus.add(new SubclassWithFieldsAndGetters());
        corpus.add(new OverridesGetter());
        corpus.add(new GetterOnly());
        corpus.add(new ManglesGetterNames());
        corpus.add(new IsGetters());
        corpus.add(new PrivateNoArgConstructor());

        // --- @JsonProperty, @JsonIgnore and @JsonPropertyOrder, each case measured against databind. ---
        corpus.add(new RenameOnField());
        corpus.add(new RenameOnGetter());
        corpus.add(new RenameMiddle());
        corpus.add(new TwoRenamed());
        corpus.add(new SameNameAsDefault());
        corpus.add(new RenameOnSetterOnly());
        corpus.add(new PrivateFieldNoGetter());
        corpus.add(new PrivateFieldRenamedNoGetter());
        corpus.add(new PrivateGetter());
        corpus.add(new NonBeanMethod());
        corpus.add(new SubOverridesRenamed());
        corpus.add(new IgnoreField());
        corpus.add(new IgnoreGetter());
        corpus.add(new IgnoreSetterOnly());
        corpus.add(new IgnoreFalse());
        corpus.add(new IgnoreFieldButPropertyOnGetter());
        corpus.add(new SubOverridesIgnored());
        corpus.add(new OrderFull());
        corpus.add(new OrderPartial());
        corpus.add(new OrderUnknownName());
        corpus.add(new OrderAlpha());
        corpus.add(new OrderPartialAlpha());
        corpus.add(new OrderByRenamed());
        corpus.add(new OrderByImplicitOfRenamed());
        corpus.add(new OrderInherited());
        corpus.add(new OrderOverridden());
        corpus.add(new AlphaWithRename());
        corpus.add(new Nested(null, Arrays.asList(new Currency(1.0, "EUR")))); // unannotated still unaffected

        // --- Top-level values that are not beans at all. ---
        corpus.add("a plain string");
        corpus.add(Integer.valueOf(42));
        corpus.add(Boolean.TRUE);
        corpus.add(null);
        corpus.add(Color.GREEN);
        corpus.add(new BigDecimal("1.2300"));
        corpus.add(new Date(1_700_000_000_123L));
        corpus.add(new byte[] {1, 2, 3});
        corpus.add(Arrays.asList(1, "two", null, true));
        corpus.add(stringKeyedMap());

        return corpus;
    }

    /**
     * JSON documents that must deserialize to the same object databind produces, paired with the target type.
     */
    public static List<ReadCase> readCorpus() {
        List<ReadCase> corpus = new ArrayList<>();

        corpus.add(new ReadCase("{\"amount\":79.99,\"unit\":\"USD\"}", Currency.class));
        corpus.add(new ReadCase("{\"unit\":\"USD\",\"amount\":79.99}", Currency.class));

        // The one behaviour the deleted v1 TypeConvertedJsonTest actually asserted: an item carrying a property the
        // model does not have must still load, because the v1 converter disabled FAIL_ON_UNKNOWN_PROPERTIES.
        corpus.add(new ReadCase("{\"amount\":1.0,\"unit\":\"USD\",\"unknownField\":\"unknownValue\"}", Currency.class));
        corpus.add(new ReadCase("{\"unknownOnly\":1}", Currency.class));
        corpus.add(new ReadCase("{}", Currency.class));
        corpus.add(new ReadCase("{\"amount\":null,\"unit\":null}", Currency.class));
        corpus.add(new ReadCase("null", Currency.class));

        // Numeric coercion.
        corpus.add(new ReadCase("{\"i\":1.0,\"l\":2.0,\"s\":3,\"b\":4,\"d\":5,\"f\":6}", Numbers.class));
        corpus.add(new ReadCase("{\"i\":1.5,\"l\":-2.5}", Numbers.class));
        corpus.add(new ReadCase("{\"i\":\"5\",\"l\":\"6\"}", Numbers.class));
        corpus.add(new ReadCase("{\"i\":null,\"l\":null,\"d\":null}", Numbers.class));
        corpus.add(new ReadCase("{\"decimal\":1.2300,\"integer\":123456789012345678901234567890}", Bigs.class));

        // Both forms databind can write for a Date.
        corpus.add(new ReadCase("{\"epoch\":0,\"later\":1700000000123}", HasDate.class));
        corpus.add(new ReadCase("{\"epoch\":\"1970-01-01T00:00:00.000+0000\"}", HasDate.class));

        corpus.add(new ReadCase("{\"bytes\":\"AAEC/w==\"}", HasBytes.class));
        corpus.add(new ReadCase("{\"color\":\"RED\"}", HasEnum.class));
        corpus.add(new ReadCase("{\"color\":null}", HasEnum.class));
        corpus.add(new ReadCase("{\"id\":\"c2a4fcd3-0c3b-4cfb-9ff7-c1cb03e2e9f6\"}", HasUuid.class));
        corpus.add(new ReadCase("{\"text\":\"line1\\nline2\\u0000\\\"quoted\\\" \\u00e9 \\u4e2d\"}", HasString.class));
        corpus.add(new ReadCase("{\"text\":42}", HasString.class));

        corpus.add(new ReadCase("{\"one\":{\"amount\":1.0,\"unit\":\"EUR\"},"
                                + "\"many\":[{\"amount\":2.0,\"unit\":\"GBP\"},null]}", Nested.class));
        corpus.add(new ReadCase("{\"one\":null,\"many\":[]}", Nested.class));

        // The named-ArrayList-subclass trick v1 documented for generic element types.
        corpus.add(new ReadCase("[{\"amount\":1.0,\"unit\":\"EUR\"},{\"amount\":2.0,\"unit\":\"GBP\"}]",
                                CurrencyList.class));
        corpus.add(new ReadCase("[]", CurrencyList.class));

        corpus.add(new ReadCase("{\"list\":[\"a\",\"b\"],\"set\":[\"a\",\"a\",\"b\"],\"nested\":[[1,2],[3]]}",
                                HasCollections.class));
        corpus.add(new ReadCase("{\"byName\":{\"a\":{\"amount\":1.0,\"unit\":\"EUR\"}}}", HasMaps.class));

        corpus.add(new ReadCase("{\"ints\":[1,2,3],\"strings\":[\"a\",null],"
                                + "\"currencies\":[{\"amount\":1.0,\"unit\":\"EUR\"}]}", HasArrays.class));

        // Untyped targets: databind's own choice of Integer / Long / BigInteger / Double / LinkedHashMap / ArrayList.
        corpus.add(new ReadCase("{\"any\":{\"i\":1,\"big\":123456789012345678901234567890,\"l\":4294967296,"
                                + "\"d\":1.5,\"s\":\"x\",\"b\":true,\"n\":null,\"a\":[1,[2]],\"o\":{\"k\":\"v\"}}}",
                                HasObject.class));
        corpus.add(new ReadCase("{\"any\":[1,2,3]}", HasObject.class));

        corpus.add(new ReadCase("{\"flag\":true,\"boxed\":false,\"ready\":true}", IsGetters.class));
        corpus.add(new ReadCase("{\"id\":7,\"name\":\"n\"}", PrivateNoArgConstructor.class));
        corpus.add(new ReadCase("{\"baseField\":\"b\",\"subField\":\"s\"}", SubclassWithFieldsAndGetters.class));

        // Read-only properties: a getter with no setter, and a private field with no setter. With
        // FAIL_ON_UNKNOWN_PROPERTIES disabled both are simply skipped.
        corpus.add(new ReadCase("{\"readOnly\":\"ignored\",\"writable\":\"kept\"}", PartlyWritable.class));

        corpus.add(new ReadCase("{\"x_a\":\"1\",\"b\":\"2\"}", ReadRenamed.class));
        corpus.add(new ReadCase("{\"a\":\"1\",\"b\":\"2\"}", ReadRenamed.class));
        corpus.add(new ReadCase("{\"h\":\"1\"}", ReadPrivateField.class));
        corpus.add(new ReadCase("{\"a\":\"1\",\"b\":\"2\"}", ReadIgnored.class));
        corpus.add(new ReadCase("{\"f\":\"fromjson\"}", ReadFinalField.class));
        corpus.add(new ReadCase("\"top level string\"", String.class));
        corpus.add(new ReadCase("42", Integer.class));
        corpus.add(new ReadCase("true", Boolean.class));

        return corpus;
    }

    /**
     * Values whose serialized form a round trip must preserve exactly. Same objects as {@link #writeCorpus()} minus
     * the ones that cannot round trip through a declared type (top-level scalars and raw collections).
     */
    public static List<Object> roundTripCorpus() {
        List<Object> corpus = new ArrayList<>();
        for (Object value : writeCorpus()) {
            if (value != null && isRoundTrippable(value.getClass())) {
                corpus.add(value);
            }
        }
        return corpus;
    }

    private static boolean isRoundTrippable(Class<?> type) {
        return type.getName().startsWith(JsonModelFixtures.class.getName())
               || type == CurrencyList.class
               || type == Currency.class;
    }

    /**
     * Strings chosen for escaping behaviour: quoting, the characters jackson-core does and does not escape, C0
     * controls, non-ASCII, and a surrogate pair.
     */
    private static final String[] STRINGS = {
        "",
        "plain",
        "with \"quotes\"",
        "back\\slash",
        "forward/slash",
        "new\nline",
        "tab\there",
        "carriage\rreturn",
        "\u0000\u0001\u001f",
        "\u007f",
        "caf\u00e9",
        "\u4e2d\u6587",
        "emoji \ud83d\ude00",
        "   leading and trailing   ",
        "a\u2028b\u2029c",
    };

    private static CurrencyList currencyList() {
        CurrencyList list = new CurrencyList();
        list.add(new Currency(1.0, "EUR"));
        list.add(new Currency(2.0, "GBP"));
        return list;
    }

    private static Map<String, Currency> stringKeyedMap() {
        Map<String, Currency> map = new LinkedHashMap<>();
        map.put("zulu", new Currency(1.0, "EUR"));
        map.put("alpha", new Currency(2.0, "GBP"));
        map.put("", null);
        return map;
    }

    private static Map<Integer, String> integerKeyedMap() {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(2, "two");
        map.put(1, "one");
        return map;
    }

    private static Map<Color, String> enumKeyedMap() {
        Map<Color, String> map = new LinkedHashMap<>();
        map.put(Color.RED, "r");
        map.put(Color.GREEN, "g");
        return map;
    }

    private static Map<String, Integer> sortedMap() {
        Map<String, Integer> map = new TreeMap<>();
        map.put("zulu", 1);
        map.put("alpha", 2);
        return map;
    }

    private static Calendar calendar(long millis) {
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(millis);
        return calendar;
    }

    /**
     * A JSON document plus the type to read it as.
     */
    public static final class ReadCase {
        private final String json;
        private final Class<?> targetType;

        public ReadCase(String json, Class<?> targetType) {
            this.json = json;
            this.targetType = targetType;
        }

        public String json() {
            return json;
        }

        public Class<?> targetType() {
            return targetType;
        }

        @Override
        public String toString() {
            return targetType.getSimpleName() + " <- " + json;
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Models
    // -------------------------------------------------------------------------------------------------------------

    /** The annotation javadoc's own example, character for character. */
    public static class Currency {
        private Double amount;
        private String unit;

        public Currency() {
        }

        public Currency(Double amount, String unit) {
            this.amount = amount;
            this.unit = unit;
        }

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }
    }

    /** v1 documented this shape - a named ArrayList subclass - as the way to get a generic element type. */
    public static class CurrencyList extends ArrayList<Currency> {
        private static final long serialVersionUID = 1L;
    }

    /**
     * What Lombok's {@code @Data} generates: private fields, public getters and setters, declaration-ordered.
     * The survey puts this shape at ~83% of call sites, which is also why it is the shape with stable ordering.
     */
    public static class LombokShaped {
        private String id;
        private int count;
        private boolean active;
        private Double ratio;
        private List<String> tags;

        static LombokShaped populated() {
            LombokShaped value = new LombokShaped();
            value.setId("abc");
            value.setCount(7);
            value.setActive(true);
            value.setRatio(0.5);
            value.setTags(Arrays.asList("x", "y"));
            return value;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public Double getRatio() {
            return ratio;
        }

        public void setRatio(Double ratio) {
            this.ratio = ratio;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }
    }

    public static class Nested {
        private Currency one;
        private List<Currency> many;

        public Nested() {
        }

        public Nested(Currency one, List<Currency> many) {
            this.one = one;
            this.many = many;
        }

        public Currency getOne() {
            return one;
        }

        public void setOne(Currency one) {
            this.one = one;
        }

        public List<Currency> getMany() {
            return many;
        }

        public void setMany(List<Currency> many) {
            this.many = many;
        }
    }

    public static class HasCollections {
        private List<String> list;
        private Set<String> set;
        private List<List<Integer>> nested;

        static HasCollections populated() {
            HasCollections value = new HasCollections();
            value.setList(Arrays.asList("a", null, "b"));
            value.setSet(new LinkedHashSet<>(Arrays.asList("a", "b")));
            value.setNested(Arrays.asList(Arrays.asList(1, 2), Collections.singletonList(3)));
            return value;
        }

        public List<String> getList() {
            return list;
        }

        public void setList(List<String> list) {
            this.list = list;
        }

        public Set<String> getSet() {
            return set;
        }

        public void setSet(Set<String> set) {
            this.set = set;
        }

        public List<List<Integer>> getNested() {
            return nested;
        }

        public void setNested(List<List<Integer>> nested) {
            this.nested = nested;
        }
    }

    public static class HasMaps {
        private Map<String, Currency> byName;

        public HasMaps() {
        }

        public HasMaps(Map<String, Currency> byName) {
            this.byName = byName;
        }

        public Map<String, Currency> getByName() {
            return byName;
        }

        public void setByName(Map<String, Currency> byName) {
            this.byName = byName;
        }
    }

    public static class Numbers {
        private int i;
        private long l;
        private short s;
        private byte b;
        private double d;
        private float f;
        private Integer boxedInt;
        private Long boxedLong;

        static Numbers populated() {
            Numbers value = new Numbers();
            value.setI(Integer.MIN_VALUE);
            value.setL(Long.MAX_VALUE);
            value.setS((short) -1);
            value.setB((byte) -128);
            value.setD(1.5);
            value.setF(2.5f);
            value.setBoxedInt(0);
            value.setBoxedLong(-1L);
            return value;
        }

        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }

        public long getL() {
            return l;
        }

        public void setL(long l) {
            this.l = l;
        }

        public short getS() {
            return s;
        }

        public void setS(short s) {
            this.s = s;
        }

        public byte getB() {
            return b;
        }

        public void setB(byte b) {
            this.b = b;
        }

        public double getD() {
            return d;
        }

        public void setD(double d) {
            this.d = d;
        }

        public float getF() {
            return f;
        }

        public void setF(float f) {
            this.f = f;
        }

        public Integer getBoxedInt() {
            return boxedInt;
        }

        public void setBoxedInt(Integer boxedInt) {
            this.boxedInt = boxedInt;
        }

        public Long getBoxedLong() {
            return boxedLong;
        }

        public void setBoxedLong(Long boxedLong) {
            this.boxedLong = boxedLong;
        }
    }

    public static class Doubles {
        private double a;
        private double b;
        private Double c;

        public Doubles() {
        }

        public Doubles(double a, double b, Double c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        public double getA() {
            return a;
        }

        public void setA(double a) {
            this.a = a;
        }

        public double getB() {
            return b;
        }

        public void setB(double b) {
            this.b = b;
        }

        public Double getC() {
            return c;
        }

        public void setC(Double c) {
            this.c = c;
        }
    }

    public static class Floats {
        private float a;
        private float b;
        private Float c;

        public Floats() {
        }

        public Floats(float a, float b, Float c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        public float getA() {
            return a;
        }

        public void setA(float a) {
            this.a = a;
        }

        public float getB() {
            return b;
        }

        public void setB(float b) {
            this.b = b;
        }

        public Float getC() {
            return c;
        }

        public void setC(Float c) {
            this.c = c;
        }
    }

    public static class Bigs {
        private BigDecimal decimal;
        private BigInteger integer;

        public Bigs() {
        }

        public Bigs(BigDecimal decimal, BigInteger integer) {
            this.decimal = decimal;
            this.integer = integer;
        }

        public BigDecimal getDecimal() {
            return decimal;
        }

        public void setDecimal(BigDecimal decimal) {
            this.decimal = decimal;
        }

        public BigInteger getInteger() {
            return integer;
        }

        public void setInteger(BigInteger integer) {
            this.integer = integer;
        }
    }

    public static class HasString {
        private String text;

        public HasString() {
        }

        public HasString(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public static class HasChar {
        private char c;
        private Character boxed;

        public HasChar() {
        }

        public HasChar(char c) {
            this.c = c;
            this.boxed = c;
        }

        public char getC() {
            return c;
        }

        public void setC(char c) {
            this.c = c;
        }

        public Character getBoxed() {
            return boxed;
        }

        public void setBoxed(Character boxed) {
            this.boxed = boxed;
        }
    }

    public static class HasDate {
        private Date epoch;
        private Date later;

        public HasDate() {
        }

        public HasDate(Date epoch, Date later) {
            this.epoch = epoch;
            this.later = later;
        }

        public Date getEpoch() {
            return epoch;
        }

        public void setEpoch(Date epoch) {
            this.epoch = epoch;
        }

        public Date getLater() {
            return later;
        }

        public void setLater(Date later) {
            this.later = later;
        }
    }

    public static class HasCalendar {
        private Calendar when;

        public HasCalendar() {
        }

        public HasCalendar(Calendar when) {
            this.when = when;
        }

        public Calendar getWhen() {
            return when;
        }

        public void setWhen(Calendar when) {
            this.when = when;
        }
    }

    public static class HasUuid {
        private UUID id;

        public HasUuid() {
        }

        public HasUuid(UUID id) {
            this.id = id;
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }
    }

    public enum Color {
        RED, GREEN
    }

    public static class HasEnum {
        private Color color;

        public HasEnum() {
        }

        public HasEnum(Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
        }
    }

    public static class HasBytes {
        private byte[] bytes;

        public HasBytes() {
        }

        public HasBytes(byte[] bytes) {
            this.bytes = bytes;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public void setBytes(byte[] bytes) {
            this.bytes = bytes;
        }
    }

    public static class HasChars {
        private char[] chars;

        public HasChars() {
        }

        public HasChars(char[] chars) {
            this.chars = chars;
        }

        public char[] getChars() {
            return chars;
        }

        public void setChars(char[] chars) {
            this.chars = chars;
        }
    }

    public static class HasArrays {
        private int[] ints;
        private String[] strings;
        private Currency[] currencies;

        static HasArrays populated() {
            HasArrays value = new HasArrays();
            value.setInts(new int[] {1, 2, 3});
            value.setStrings(new String[] {"a", null});
            value.setCurrencies(new Currency[] {new Currency(1.0, "EUR")});
            return value;
        }

        public int[] getInts() {
            return ints;
        }

        public void setInts(int[] ints) {
            this.ints = ints;
        }

        public String[] getStrings() {
            return strings;
        }

        public void setStrings(String[] strings) {
            this.strings = strings;
        }

        public Currency[] getCurrencies() {
            return currencies;
        }

        public void setCurrencies(Currency[] currencies) {
            this.currencies = currencies;
        }
    }

    public static class HasObject {
        private Object any;

        public Object getAny() {
            return any;
        }

        public void setAny(Object any) {
            this.any = any;
        }
    }

    /** Public fields with no getters are still properties, since the default field visibility is PUBLIC_ONLY. */
    public static class PublicFields {
        public String first = "1";
        public int second = 2;
        public final String third = "3";
    }

    /** A private field with no getter is invisible, but it still occupies an ordering slot. */
    public static class MixedVisibility {
        private String invisible = "hidden";
        public String visibleField = "shown";
        private String withGetter = "also shown";

        public String getWithGetter() {
            return withGetter;
        }
    }

    public static class SkipsStaticAndTransient {
        public static String staticField = "skipped";
        public transient String transientField = "skipped";
        public String kept = "kept";
    }

    /** Pins the measured ordering rule: field slots first (superclass first), then getter-only slots. */
    public static class BaseWithFieldAndGetter {
        public String baseField = "bf";

        public String getBaseGetter() {
            return "bg";
        }
    }

    public static class SubclassWithFieldsAndGetters extends BaseWithFieldAndGetter {
        public String subField = "sf";

        public String getSubGetter() {
            return "sg";
        }
    }

    public static class BaseWithGetter {
        public String getValue() {
            return "base";
        }
    }

    /** An overridden getter is one property, not two. */
    public static class OverridesGetter extends BaseWithGetter {
        @Override
        public String getValue() {
            return "sub";
        }
    }

    /**
     * No backing fields at all, so every property's position comes from {@code getDeclaredMethods()}. Included on
     * purpose: whatever order the JVM hands back, both encoders must agree on it, since they ask the same JVM.
     */
    public static class GetterOnly {
        public String getZulu() {
            return "z";
        }

        public String getAlpha() {
            return "a";
        }

        public int getMike() {
            return 13;
        }
    }

    /** Pins databind's legacy name mangling, which lowercases the whole leading uppercase run. */
    public static class ManglesGetterNames {
        public String getURL() {
            return "u";
        }

        public String getPTLevelRequestIds() {
            return "p";
        }

        public String getXMLHttpRequest() {
            return "x";
        }

        public String getA() {
            return "a";
        }

        public String getAB() {
            return "ab";
        }

        public String getABc() {
            return "abc";
        }

        public String getaLowerFirst() {
            return "l";
        }
    }

    public static class IsGetters {
        private boolean flag = true;
        private Boolean boxed = Boolean.FALSE;

        public boolean isFlag() {
            return flag;
        }

        public void setFlag(boolean flag) {
            this.flag = flag;
        }

        public Boolean isBoxed() {
            return boxed;
        }

        public void setBoxed(Boolean boxed) {
            this.boxed = boxed;
        }

        public boolean isReady() {
            return true;
        }
    }

    /** databind will use a non-public no-arg constructor, and Lombok's {@code @NoArgsConstructor} often makes one. */
    public static class PrivateNoArgConstructor {
        private int id;
        private String name;

        private PrivateNoArgConstructor() {
        }

        public PrivateNoArgConstructor(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /** A read-only property alongside a writable one, to pin what happens on the read path. */
    public static class PartlyWritable {
        private String writable;

        public String getReadOnly() {
            return "constant";
        }

        public String getWritable() {
            return writable;
        }

        public void setWritable(String writable) {
            this.writable = writable;
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Jackson-annotated shapes. The expected bytes are whatever databind writes; see the measured notes in
    // JsonBeanModel. Renamed properties move to the end, even when the new name equals the old one.
    // ---------------------------------------------------------------------------------------------------------------

    public static class RenameOnField {
        @JsonProperty("x_a") private String a = "A";
        private String b = "B";
        public String getA() { return a; }
        public String getB() { return b; }
    }

    public static class RenameOnGetter {
        private String a = "A";
        private String b = "B";
        @JsonProperty("x_a") public String getA() { return a; }
        public String getB() { return b; }
    }

    public static class RenameMiddle {
        private String a = "A";
        @JsonProperty("z") private String b = "B";
        private String c = "C";
        public String getA() { return a; }
        public String getB() { return b; }
        public String getC() { return c; }
    }

    public static class TwoRenamed {
        @JsonProperty("q") private String a = "A";
        private String b = "B";
        @JsonProperty("p") private String c = "C";
        public String getA() { return a; }
        public String getB() { return b; }
        public String getC() { return c; }
    }

    public static class SameNameAsDefault {
        @JsonProperty("a") private String a = "A";
        private String b = "B";
        public String getA() { return a; }
        public String getB() { return b; }
    }

    public static class RenameOnSetterOnly {
        private String a = "A";
        private String b = "B";
        public String getA() { return a; }
        @JsonProperty("x_a") public void setA(String v) { a = v; }
        public String getB() { return b; }
    }

    public static class PrivateFieldNoGetter {
        @JsonProperty private String hidden = "H";
        private String b = "B";
        public String getB() { return b; }
    }

    public static class PrivateFieldRenamedNoGetter {
        @JsonProperty("h") private String hidden = "H";
        private String b = "B";
        public String getB() { return b; }
    }

    public static class PrivateGetter {
        private String a = "A";
        private String b = "B";
        @JsonProperty private String getA() { return a; }
        public String getB() { return b; }
    }

    public static class NonBeanMethod {
        private String b = "B";
        public String getB() { return b; }
        @JsonProperty public String computed() { return "C"; }
    }

    public static class SuperRenamed {
        private String a = "A";
        private String b = "B";
        @JsonProperty("x_a") public String getA() { return a; }
        public String getB() { return b; }
    }

    public static class SubOverridesRenamed extends SuperRenamed {
        @Override public String getA() { return "SUB"; }
    }

    public static class IgnoreField {
        @JsonIgnore private String a = "A";
        private String b = "B";
        public String getA() { return a; }
        public String getB() { return b; }
    }

    public static class IgnoreGetter {
        private String a = "A";
        private String b = "B";
        @JsonIgnore public String getA() { return a; }
        public String getB() { return b; }
    }

    public static class IgnoreSetterOnly {
        private String a = "A";
        private String b = "B";
        public String getA() { return a; }
        @JsonIgnore public void setA(String v) { a = v; }
        public String getB() { return b; }
    }

    public static class IgnoreFalse {
        @JsonIgnore(false) private String a = "A";
        private String b = "B";
        public String getA() { return a; }
        public String getB() { return b; }
    }

    public static class IgnoreFieldButPropertyOnGetter {
        @JsonIgnore private String a = "A";
        private String b = "B";
        @JsonProperty public String getA() { return a; }
        public String getB() { return b; }
    }

    public static class SuperIgnored {
        private String a = "A";
        private String b = "B";
        @JsonIgnore public String getA() { return a; }
        public String getB() { return b; }
    }

    public static class SubOverridesIgnored extends SuperIgnored {
        @Override public String getA() { return "SUB"; }
    }

    @JsonPropertyOrder({"c", "a", "b"})
    public static class OrderFull {
        private String a = "A";
        private String b = "B";
        private String c = "C";
        public String getA() { return a; }
        public String getB() { return b; }
        public String getC() { return c; }
    }

    @JsonPropertyOrder({"c"})
    public static class OrderPartial {
        private String a = "A";
        private String b = "B";
        private String c = "C";
        public String getA() { return a; }
        public String getB() { return b; }
        public String getC() { return c; }
    }

    @JsonPropertyOrder({"nope", "b"})
    public static class OrderUnknownName {
        private String a = "A";
        private String b = "B";
        public String getA() { return a; }
        public String getB() { return b; }
    }

    @JsonPropertyOrder(alphabetic = true)
    public static class OrderAlpha {
        private String c = "C";
        private String a = "A";
        private String b = "B";
        public String getA() { return a; }
        public String getB() { return b; }
        public String getC() { return c; }
    }

    @JsonPropertyOrder(value = {"c"}, alphabetic = true)
    public static class OrderPartialAlpha {
        private String c2 = "x";
        private String b = "B";
        private String a = "A";
        private String c = "C";
        public String getC2() { return c2; }
        public String getA() { return a; }
        public String getB() { return b; }
        public String getC() { return c; }
    }

    @JsonPropertyOrder({"x_a", "b"})
    public static class OrderByRenamed {
        private String b = "B";
        @JsonProperty("x_a") private String a = "A";
        public String getA() { return a; }
        public String getB() { return b; }
    }

    @JsonPropertyOrder({"a", "b"})
    public static class OrderByImplicitOfRenamed {
        private String b = "B";
        @JsonProperty("x_a") private String a = "A";
        public String getA() { return a; }
        public String getB() { return b; }
    }

    @JsonPropertyOrder({"b", "a"})
    public static class OrderBase {
        private String a = "A";
        private String b = "B";
        public String getA() { return a; }
        public String getB() { return b; }
    }

    public static class OrderInherited extends OrderBase {
        private String c = "C";
        public String getC() { return c; }
    }

    @JsonPropertyOrder({"c"})
    public static class OrderOverridden extends OrderBase {
        private String c = "C";
        public String getC() { return c; }
    }

    @JsonPropertyOrder(alphabetic = true)
    public static class AlphaWithRename {
        @JsonProperty("m") private String z = "Z";
        private String b = "B";
        private String a = "A";
        public String getZ() { return z; }
        public String getB() { return b; }
        public String getA() { return a; }
    }

    public static class ReadRenamed {
        @JsonProperty("x_a") private String a;
        private String b;
        public String getA() { return a; }
        public void setA(String v) { a = v; }
        public String getB() { return b; }
        public void setB(String v) { b = v; }
    }

    public static class ReadPrivateField {
        @JsonProperty("h") private String hidden;
    }

    public static class ReadIgnored {
        @JsonIgnore private String a;
        private String b;
        public String getA() { return a; }
        public void setA(String v) { a = v; }
        public String getB() { return b; }
        public void setB(String v) { b = v; }
    }

    public static class ReadFinalField {
        @JsonProperty("f") private final String f;
        private ReadFinalField() { f = "init"; }
    }
}

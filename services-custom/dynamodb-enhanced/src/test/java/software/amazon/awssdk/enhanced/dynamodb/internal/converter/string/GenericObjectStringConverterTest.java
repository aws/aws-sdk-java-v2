package software.amazon.awssdk.enhanced.dynamodb.internal.converter.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.LocalDate;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.CustomType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.StringConverter;

public class GenericObjectStringConverterTest {

    @Test
    public void testSerializeAndDeserializeCustomObject() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        StringConverter<CustomType> converter = GenericObjectStringConverter.create(type);

        CustomType original = createCustomType();
        String json = converter.toString(original);
        CustomType result = converter.fromString(json);

        assertNotNull(json);
        assertNotNull(result);
        assertEquals(original.getBooleanAttribute(), result.getBooleanAttribute());
        assertEquals(original.getIntegerAttribute(), result.getIntegerAttribute());
        assertEquals(original.getDoubleAttribute(), result.getDoubleAttribute());
        assertEquals(original.getStringAttribute(), result.getStringAttribute());
        assertEquals(original.getLocalDateAttribute(), result.getLocalDateAttribute());
    }

    @Test
    public void serializeNullObject_returnsNull() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        StringConverter<CustomType> converter = GenericObjectStringConverter.create(type);

        assertNull(converter.toString(null));
    }

    @Test
    public void serializeNullJsonString_returnsNull() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        StringConverter<CustomType> converter = GenericObjectStringConverter.create(type);

        assertNull(converter.fromString(null));
    }

    @Test
    public void deserializeMalformedJson_returnsNull() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        StringConverter<CustomType> converter = GenericObjectStringConverter.create(type);

        assertNull(converter.fromString("{malformed"));
    }

    private CustomType createCustomType() {
        return new CustomType()
            .setBooleanAttribute(Boolean.TRUE)
            .setIntegerAttribute(1)
            .setDoubleAttribute(100.0)
            .setStringAttribute("test1")
            .setLocalDateAttribute(LocalDate.of(2025, 1, 1));
    }
}

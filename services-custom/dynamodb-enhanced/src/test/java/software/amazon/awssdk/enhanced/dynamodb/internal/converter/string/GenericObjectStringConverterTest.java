package software.amazon.awssdk.enhanced.dynamodb.internal.converter.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.CustomType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.StringConverter;

@RunWith(MockitoJUnitRunner.class)
public class GenericObjectStringConverterTest {

    @Test
    public void testSerializeAndDeserializeCustomObject() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        StringConverter<CustomType> converter = GenericObjectStringConverter.create(type);
        CustomType original = createCustomType();

        String json = converter.toString(original);
        CustomType result = converter.fromString(json);

        assertNotNull("Serialized JSON should not be null", json);
        assertNotNull("Deserialized object should not be null", result);
        assertEquals(original.getBooleanAttribute(), result.getBooleanAttribute());
        assertEquals(original.getIntegerAttribute(), result.getIntegerAttribute());
        assertEquals(original.getDoubleAttribute(), result.getDoubleAttribute());
        assertEquals(original.getStringAttribute(), result.getStringAttribute());
        assertEquals(original.getLocalDateAttribute(), result.getLocalDateAttribute());
    }

    @Test
    public void testSerialize_withNullObject_returnsNull() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        StringConverter<CustomType> converter = GenericObjectStringConverter.create(type);

        assertNull("Serialization of null should return null", converter.toString(null));
    }

    @Test
    public void testSerialize_withUnserializableType_throwsIllegalArgumentException() {
        class UnserializableType {
            private Thread thread = new Thread();
        }

        UnserializableType instance = new UnserializableType();
        EnhancedType<UnserializableType> type = EnhancedType.of(UnserializableType.class);
        StringConverter<UnserializableType> converter = GenericObjectStringConverter.create(type);

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> converter.toString(instance)
        );

        assertTrue(ex.getMessage().contains("Unable to serialize object of type UnserializableType"));
        assertNotNull("Expected cause for serialization exception", ex.getCause());
    }

    @Test
    public void testDeserialize_withNullInput_returnsNull() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        StringConverter<CustomType> converter = GenericObjectStringConverter.create(type);

        assertNull("Deserialization of null should return null", converter.fromString(null));
    }

    @Test
    public void testDeserialize_withMalformedJson_throwsIllegalArgumentException() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        StringConverter<CustomType> converter = GenericObjectStringConverter.create(type);

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> converter.fromString("{malformed")
        );

        assertTrue(ex.getMessage().contains("Unable to deserialize string to object of type CustomType"));
        assertNotNull("Expected cause for deserialization exception", ex.getCause());
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

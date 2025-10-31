package software.amazon.awssdk.enhanced.dynamodb.internal.converter.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.CustomType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.StringConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.StringConverterProvider;

@RunWith(MockitoJUnitRunner.class)
public class FallbackStringConverterProviderTest {

    @Mock
    private StringConverterProvider mockDelegate;

    private FallbackStringConverterProvider fallbackProvider;

    @Before
    public void setUp() {
        fallbackProvider = new FallbackStringConverterProvider(mockDelegate);
    }

    @Test
    public void testUsesDelegateWhenAvailable() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        StringConverter<CustomType> mockConverter = mock(StringConverter.class);
        when(mockConverter.toString(any())).thenReturn("mock-serialized-result");
        when(mockDelegate.converterFor(type)).thenReturn(mockConverter);

        StringConverter<CustomType> result = fallbackProvider.converterFor(type);
        assertEquals("mock-serialized-result", result.toString(createCustomType()));
        verify(mockDelegate).converterFor(type);
    }

    @Test
    public void testFallbackSerializationAndDeserialization() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        when(mockDelegate.converterFor(type)).thenThrow(new IllegalArgumentException("Not found"));

        StringConverter<CustomType> converter = fallbackProvider.converterFor(type);
        CustomType original = createCustomType();
        String json = converter.toString(original);
        CustomType parsed = converter.fromString(json);

        assertNotNull(json);
        assertNotNull(parsed);
        assertEquals(original.getBooleanAttribute(), parsed.getBooleanAttribute());
        assertEquals(original.getIntegerAttribute(), parsed.getIntegerAttribute());
        assertEquals(original.getDoubleAttribute(), parsed.getDoubleAttribute());
        assertEquals(original.getStringAttribute(), parsed.getStringAttribute());
        assertEquals(original.getLocalDateAttribute(), parsed.getLocalDateAttribute());
    }

    @Test
    public void testFallbackCaching() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        when(mockDelegate.converterFor(type)).thenThrow(new IllegalArgumentException("Not found"));

        StringConverter<CustomType> first = fallbackProvider.converterFor(type);
        StringConverter<CustomType> second = fallbackProvider.converterFor(type);

        assertSame(first, second);
    }


    @Test
    public void testFallback_serializeWithNullObject_returnsNull() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        StringConverter<CustomType> converter = GenericObjectStringConverter.create(type);

        assertNull("Serialization of null should return null", converter.toString(null));
    }

    @Test
    public void testFallback_serializeWithUnserializableType_throwsIllegalArgumentException() {
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
    public void testFallback_deserializeWithNullInput_returnsNull() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        StringConverter<CustomType> converter = GenericObjectStringConverter.create(type);

        assertNull("Deserialization of null should return null", converter.fromString(null));
    }

    @Test
    public void testFallback_deserializeWithMalformedJson_throwsIllegalArgumentException() {
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

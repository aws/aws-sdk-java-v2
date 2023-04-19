package software.amazon.awssdk.enhanced.dynamodb.functionaltests.models;


import java.util.Map;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomClassForDocumentAPI;
import software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomClassForDocumentAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomIntegerAttributeConverter;
import software.amazon.awssdk.utils.ImmutableMap;

/**
 * InnerAttribConverterProvider  to save the InnerAttribConverter on the class.
 */
public class InnerAttribConverterProvider<T> implements AttributeConverterProvider {


    private final Map<EnhancedType<?>, AttributeConverter<?>> converterCache = ImmutableMap.of(
        EnhancedType.of(InnerAttributeRecord.class),  new InnerAttribConverter<T>()
    );


    @Override
    public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
        return (AttributeConverter<T>) converterCache.get(enhancedType);
    }
}
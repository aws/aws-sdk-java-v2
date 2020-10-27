package software.amazon.awssdk.enhanced.dynamodb.functionaltests.models;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

/**
 * Event Payload Converter to save the record on the class
 */
public class InnerAttribConverter<T> implements AttributeConverter<T> {

    private final ObjectMapper objectMapper;

    /**
     * This No Args constuctor is needed by the DynamoDbConvertedBy annotation
     */
    public InnerAttribConverter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        final AttributeValue dd = stringValue("dd");
        AttributeConverter<String> attributeConverter = null;
        AttributeValueType attributeValueType = null;
        EnhancedType enhancedType = null;
        // add this to preserve the same offset (don't convert to UTC)
        this.objectMapper.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);
    }

        @Override
    public AttributeValue transformFrom(final T input) {

        Map<String, AttributeValue> map = null;
        if (input != null) {
            map = new HashMap<>();
            InnerAttributeRecord innerAttributeRecord = (InnerAttributeRecord) input;
            if (innerAttributeRecord.getAttribOne() != null) {

                final AttributeValue attributeValue = stringValue(innerAttributeRecord.getAttribOne());
                map.put("attribOne", stringValue(innerAttributeRecord.getAttribOne()));
            }
            if (innerAttributeRecord.getAttribTwo() != null) {
                map.put("attribTwo", stringValue(String.valueOf(innerAttributeRecord.getAttribTwo())));
            }
        }
        return AttributeValue.builder().m(map).build();

    }

    @Override
    public T transformTo(final AttributeValue attributeValue) {
        InnerAttributeRecord innerMetadata = new InnerAttributeRecord();
        if (attributeValue.m().get("attribOne") != null) {
            innerMetadata.setAttribOne(attributeValue.m().get("attribOne").s());
        }
        if (attributeValue.m().get("attribTwo") != null) {
            innerMetadata.setAttribTwo(Integer.valueOf(attributeValue.m().get("attribTwo").s()));
        }
        return (T) innerMetadata;
    }

    @Override
    public EnhancedType<T> type() {
        return (EnhancedType<T>) EnhancedType.of(InnerAttributeRecord.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }


}
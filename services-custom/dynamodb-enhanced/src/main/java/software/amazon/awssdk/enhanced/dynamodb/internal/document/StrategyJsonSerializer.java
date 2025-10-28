package software.amazon.awssdk.enhanced.dynamodb.internal.document;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.protocols.json.SdkJsonGenerator;
import software.amazon.awssdk.thirdparty.jackson.core.JsonFactory;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkInternalApi
public final class StrategyJsonSerializer {
    private static final JsonFactory jsonFactory = new JsonFactory();

    private StrategyJsonSerializer() {
    }

    private enum JsonSerializationStrategy {
        NULL,
        STRING,
        NUMBER,
        BOOLEAN,
        BYTES,
        LIST,
        MAP,
        STRING_SET,
        NUMBER_SET,
        BYTES_SET
    }

    public static String serializeAttributeValueMap(Map<String, AttributeValue> map) {
        SdkJsonGenerator jsonGen = new SdkJsonGenerator(jsonFactory, "application/json"); // re-use factory

        jsonGen.writeStartObject();
        map.forEach((key, value) -> {
            jsonGen.writeFieldName(key);
            serializeAttributeValue(jsonGen, value);
        });
        jsonGen.writeEndObject();

        return new String(jsonGen.getBytes(), StandardCharsets.UTF_8);
    }

    public static String serializeSingleAttributeValue(AttributeValue av) {
        SdkJsonGenerator jsonGen = new SdkJsonGenerator(jsonFactory, "application/json"); // re-use factory
        serializeAttributeValue(jsonGen, av);
        return new String(jsonGen.getBytes(), StandardCharsets.UTF_8);
    }

    public static void serializeAttributeValue(SdkJsonGenerator generator, AttributeValue av) {
        JsonSerializationStrategy strategy = getStrategy(av);

        switch (strategy) {
            case NULL:
                generator.writeNull();
                break;
            case STRING:
                generator.writeValue(av.s());
                break;
            case NUMBER:
                generator.writeNumber(av.n());
                break;
            case BOOLEAN:
                generator.writeValue(av.bool());
                break;
            case BYTES:
                generator.writeValue(av.b().asByteBuffer());
                break;
            case LIST:
                generator.writeStartArray();
                for (AttributeValue item : av.l()) {
                    serializeAttributeValue(generator, item);
                }
                generator.writeEndArray();
                break;
            case MAP:
                generator.writeStartObject();
                for (Map.Entry<String, AttributeValue> entry : av.m().entrySet()) {
                    generator.writeFieldName(entry.getKey());
                    serializeAttributeValue(generator, entry.getValue());
                }
                generator.writeEndObject();
                break;
            case STRING_SET:
                generator.writeStartArray();
                for (String s : av.ss()) {
                    generator.writeValue(s);
                }
                generator.writeEndArray();
                break;
            case NUMBER_SET:
                generator.writeStartArray();
                for (String n : av.ns()) {
                    generator.writeNumber(n);
                }
                generator.writeEndArray();
                break;
            case BYTES_SET:
                generator.writeStartArray();
                for (SdkBytes b : av.bs()) {
                    generator.writeValue(b.asByteBuffer());
                }
                generator.writeEndArray();
                break;
            default:
                throw new IllegalStateException("Unsupported strategy: " + strategy);
        }
    }

    private static JsonSerializationStrategy getStrategy(AttributeValue av) {
        if (av.nul() != null && av.nul()) return JsonSerializationStrategy.NULL;
        if (av.s() != null) return JsonSerializationStrategy.STRING;
        if (av.n() != null) return JsonSerializationStrategy.NUMBER;
        if (av.bool() != null) return JsonSerializationStrategy.BOOLEAN;
        if (av.b() != null) return JsonSerializationStrategy.BYTES;
        if (av.hasL()) return JsonSerializationStrategy.LIST;
        if (av.hasM()) return JsonSerializationStrategy.MAP;
        if (av.hasSs()) return JsonSerializationStrategy.STRING_SET;
        if (av.hasNs()) return JsonSerializationStrategy.NUMBER_SET;
        if (av.hasBs()) return JsonSerializationStrategy.BYTES_SET;
        throw new IllegalStateException("Unknown AttributeValue type: " + av);
    }
}
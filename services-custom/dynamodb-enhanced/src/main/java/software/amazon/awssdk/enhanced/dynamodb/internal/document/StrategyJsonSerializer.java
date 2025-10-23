package software.amazon.awssdk.enhanced.dynamodb.internal.document;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.protocols.json.SdkJsonGenerator;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.thirdparty.jackson.core.JsonFactory;

@SdkInternalApi
public final class StrategyJsonSerializer {

    private enum JsonSerializationStrategy {
        NULL {
            @Override
            public void serialize(SdkJsonGenerator generator, AttributeValue av) {
                generator.writeNull();
            }
        },
        STRING {
            @Override
            public void serialize(SdkJsonGenerator generator, AttributeValue av) {
                generator.writeValue(av.s());
            }
        },
        NUMBER {
            @Override
            public void serialize(SdkJsonGenerator generator, AttributeValue av) {
                generator.writeNumber(av.n());
            }
        },
        BOOLEAN {
            @Override
            public void serialize(SdkJsonGenerator generator, AttributeValue av) {
                generator.writeValue(av.bool());
            }
        },
        BYTES {
            @Override
            public void serialize(SdkJsonGenerator generator, AttributeValue av) {
                generator.writeValue(av.b().asByteBuffer());
            }
        },
        LIST {
            @Override
            public void serialize(SdkJsonGenerator generator, AttributeValue av) {
                generator.writeStartArray();
                for (AttributeValue item : av.l()) {
                    serializeAttributeValue(generator, item);
                }
                generator.writeEndArray();
            }
        },
        MAP {
            @Override
            public void serialize(SdkJsonGenerator generator, AttributeValue av) {
                generator.writeStartObject();
                for (Map.Entry<String, AttributeValue> entry : av.m().entrySet()) {
                    generator.writeFieldName(entry.getKey());
                    serializeAttributeValue(generator, entry.getValue());
                }
                generator.writeEndObject();
            }
        },
        STRING_SET {
            @Override
            public void serialize(SdkJsonGenerator generator, AttributeValue av) {
                generator.writeStartArray();
                for (String s : av.ss()) {
                    generator.writeValue(s);
                }
                generator.writeEndArray();
            }
        },
        NUMBER_SET {
            @Override
            public void serialize(SdkJsonGenerator generator, AttributeValue av) {
                generator.writeStartArray();
                for (String n : av.ns()) {
                    generator.writeNumber(n);
                }
                generator.writeEndArray();
            }
        },
        BYTES_SET {
            @Override
            public void serialize(SdkJsonGenerator generator, AttributeValue av) {
                generator.writeStartArray();
                for (SdkBytes b : av.bs()) {
                    generator.writeValue(b.asByteBuffer());
                }
                generator.writeEndArray();
            }
        };

        public abstract void serialize(SdkJsonGenerator generator, AttributeValue av);
    }

    public static String serializeAttributeValueMap(Map<String, AttributeValue> map) {
        SdkJsonGenerator jsonGen = new SdkJsonGenerator(new JsonFactory(), "application/json");

        jsonGen.writeStartObject();
        map.forEach((key, value) -> {
            jsonGen.writeFieldName(key);
            serializeAttributeValue(jsonGen, value);
        });
        jsonGen.writeEndObject();

        return new String(jsonGen.getBytes(), StandardCharsets.UTF_8);
    }

    public static void serializeAttributeValue(SdkJsonGenerator generator, AttributeValue av) {
        JsonSerializationStrategy strategy = getStrategy(av);
        strategy.serialize(generator, av);
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

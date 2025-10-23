package software.amazon.awssdk.enhanced.dynamodb.internal.document;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.thirdparty.jackson.core.JsonFactory;
import software.amazon.awssdk.thirdparty.jackson.core.JsonGenerator;

@SdkInternalApi
public final class StrategyJsonSerializer {

    private StrategyJsonSerializer() {
    }

    private enum JsonSerializationStrategy {
        NULL {
            @Override
            public void serialize(JsonGenerator generator, AttributeValue av) throws IOException {
                generator.writeNull();
            }
        },
        STRING {
            @Override
            public void serialize(JsonGenerator generator, AttributeValue av) throws IOException {
                generator.writeString(av.s());
            }
        },
        NUMBER {
            @Override
            public void serialize(JsonGenerator generator, AttributeValue av) throws IOException {
                generator.writeNumber(av.n());
            }
        },
        BOOLEAN {
            @Override
            public void serialize(JsonGenerator generator, AttributeValue av) throws IOException {
                generator.writeBoolean(av.bool());
            }
        },
        BYTES {
            @Override
            public void serialize(JsonGenerator generator, AttributeValue av) throws IOException {
                generator.writeBinary(av.b().asByteArray());
            }
        },
        LIST {
            @Override
            public void serialize(JsonGenerator generator, AttributeValue av) throws IOException {
                generator.writeStartArray();
                for (AttributeValue item : av.l()) {
                    serializeAttributeValue(generator, item);
                }
                generator.writeEndArray();
            }
        },
        MAP {
            @Override
            public void serialize(JsonGenerator generator, AttributeValue av) throws IOException {
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
            public void serialize(JsonGenerator generator, AttributeValue av) throws IOException {
                generator.writeStartArray();
                for (String s : av.ss()) {
                    generator.writeString(s);
                }
                generator.writeEndArray();
            }
        },
        NUMBER_SET {
            @Override
            public void serialize(JsonGenerator generator, AttributeValue av) throws IOException {
                generator.writeStartArray();
                for (String n : av.ns()) {
                    generator.writeNumber(n);
                }
                generator.writeEndArray();
            }
        },
        BYTES_SET {
            @Override
            public void serialize(JsonGenerator generator, AttributeValue av) throws IOException {
                generator.writeStartArray();
                for (SdkBytes b : av.bs()) {
                    generator.writeBinary(b.asByteArray());
                }
                generator.writeEndArray();
            }
        };

        public abstract void serialize(JsonGenerator generator, AttributeValue av) throws IOException;
    }

    public static String serializeAttributeValueMap(Map<String, AttributeValue> map) {
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = new JsonFactory().createGenerator(writer)) {
            generator.writeStartObject();
            for (Map.Entry<String, AttributeValue> entry : map.entrySet()) {
                generator.writeFieldName(entry.getKey());
                serializeAttributeValue(generator, entry.getValue());
            }
            generator.writeEndObject();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize AttributeValue map to JSON", e);
        }
        return writer.toString();
    }

    private static void serializeAttributeValue(JsonGenerator generator, AttributeValue av) throws IOException {
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

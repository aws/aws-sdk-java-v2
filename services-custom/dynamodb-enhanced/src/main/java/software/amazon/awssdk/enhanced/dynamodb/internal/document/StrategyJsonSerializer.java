package software.amazon.awssdk.enhanced.dynamodb.internal.document;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
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
            throw new UncheckedIOException(e);
        }
        return writer.toString();
    }

    private static void serializeAttributeValue(JsonGenerator generator, AttributeValue av) throws IOException {
        if (av.nul() != null && av.nul()) {
            generator.writeNull();
        } else if (av.s() != null) {
            generator.writeString(av.s());
        } else if (av.n() != null) {
            generator.writeNumber(av.n());
        } else if (av.bool() != null) {
            generator.writeBoolean(av.bool());
        } else if (av.b() != null) {
            generator.writeBinary(av.b().asByteArray());
        } else if (av.hasL()) {
            serializeList(generator, av);
        } else if (av.hasM()) {
            serializeMap(generator, av);
        } else if (av.hasSs()) {
            serializeStringSet(generator, av);
        } else if (av.hasNs()) {
            serializeNumberSet(generator, av);
        } else if (av.hasBs()) {
            serializeBytesSet(generator, av);
        } else {
            throw new IllegalStateException("Unknown AttributeValue type: " + av);
        }
    }

    private static void serializeList(JsonGenerator generator, AttributeValue av) throws IOException {
        generator.writeStartArray();
        for (AttributeValue item : av.l()) {
            serializeAttributeValue(generator, item);
        }
        generator.writeEndArray();
    }

    private static void serializeMap(JsonGenerator generator, AttributeValue av) throws IOException {
        generator.writeStartObject();
        for (Map.Entry<String, AttributeValue> entry : av.m().entrySet()) {
            generator.writeFieldName(entry.getKey());
            serializeAttributeValue(generator, entry.getValue());
        }
        generator.writeEndObject();
    }

    private static void serializeStringSet(JsonGenerator generator, AttributeValue av) throws IOException {
        generator.writeStartArray();
        for (String s : av.ss()) {
            generator.writeString(s);
        }
        generator.writeEndArray();
    }

    private static void serializeNumberSet(JsonGenerator generator, AttributeValue av) throws IOException {
        generator.writeStartArray();
        for (String n : av.ns()) {
            generator.writeNumber(n);
        }
        generator.writeEndArray();
    }

    private static void serializeBytesSet(JsonGenerator generator, AttributeValue av) throws IOException {
        generator.writeStartArray();
        for (SdkBytes b : av.bs()) {
            generator.writeBinary(b.asByteArray());
        }
        generator.writeEndArray();
    }
}

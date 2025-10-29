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

package software.amazon.awssdk.enhanced.dynamodb.internal.document;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.protocols.json.SdkJsonGenerator;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.thirdparty.jackson.core.JsonFactory;
import software.amazon.awssdk.thirdparty.jackson.core.JsonGenerator;

/**
 * JSON serializer for DynamoDB Enhanced Client document operations.
 */
@SdkInternalApi
public final class DocumentJsonSerializer {
    private static final JsonFactory JSON_FACTORY = new JsonFactory()   {
        @Override
        public JsonGenerator createGenerator(OutputStream out) throws IOException {
            return super.createGenerator(out).enable(JsonGenerator.Feature.COMBINE_UNICODE_SURROGATES_IN_UTF8);
        }
    };

    private DocumentJsonSerializer() {
    }

    public static String serializeAttributeValueMap(Map<String, AttributeValue> map) {
        SdkJsonGenerator jsonGen = new SdkJsonGenerator(JSON_FACTORY, "application/json");

        jsonGen.writeStartObject();
        map.forEach((key, value) -> {
            jsonGen.writeFieldName(key);
            serializeAttributeValue(jsonGen, value);
        });
        jsonGen.writeEndObject();

        return new String(jsonGen.getBytes(), StandardCharsets.UTF_8);
    }

    public static String serializeSingleAttributeValue(AttributeValue av) {
        SdkJsonGenerator jsonGen = new SdkJsonGenerator(JSON_FACTORY, "application/json");
        serializeAttributeValue(jsonGen, av);
        return new String(jsonGen.getBytes(), StandardCharsets.UTF_8);
    }

    public static void serializeAttributeValue(SdkJsonGenerator generator, AttributeValue av) {
        switch (av.type()) {
            case NUL:
                generator.writeNull();
                break;
            case S:
                generator.writeValue(av.s());
                break;
            case N:
                generator.writeNumber(av.n());
                break;
            case BOOL:
                generator.writeValue(av.bool());
                break;
            case B:
                generator.writeValue(av.b().asByteBuffer());
                break;
            case L:
                generator.writeStartArray();
                for (AttributeValue item : av.l()) {
                    serializeAttributeValue(generator, item);
                }
                generator.writeEndArray();
                break;
            case M:
                generator.writeStartObject();
                for (Map.Entry<String, AttributeValue> entry : av.m().entrySet()) {
                    generator.writeFieldName(entry.getKey());
                    serializeAttributeValue(generator, entry.getValue());
                }
                generator.writeEndObject();
                break;
            case SS:
                generator.writeStartArray();
                for (String s : av.ss()) {
                    generator.writeValue(s);
                }
                generator.writeEndArray();
                break;
            case NS:
                generator.writeStartArray();
                for (String n : av.ns()) {
                    generator.writeNumber(n);
                }
                generator.writeEndArray();
                break;
            case BS:
                generator.writeStartArray();
                for (SdkBytes b : av.bs()) {
                    generator.writeValue(b.asByteBuffer());
                }
                generator.writeEndArray();
                break;
            default:
                throw new IllegalArgumentException("Unsupported AttributeValue type: " + av.type());
        }
    }

}
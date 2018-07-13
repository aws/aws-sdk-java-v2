/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.internal.protocol.json;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.StructuredPojo;
import software.amazon.awssdk.core.protocol.json.StructuredJsonGenerator;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

@SdkInternalApi
public final class SimpleTypeJsonMarshaller {

    public static final JsonMarshaller<Void> NULL = (val, context, paramName) -> {
        // If paramName is non null then we are emitting a field of an object, in that
        // we just don't write the field. If param name is null then we are either in a container
        // or the thing being marshalled is the payload itself in which case we want to preserve
        // the JSON null.
        if (paramName == null) {
            context.jsonGenerator().writeNull();
        }
    };

    public static final JsonMarshaller<String> STRING = new BaseJsonMarshaller<String>() {
        @Override
        public void marshall(String val, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context) {
            jsonGenerator.writeValue(val);
        }
    };

    public static final JsonMarshaller<Integer> INTEGER = new BaseJsonMarshaller<Integer>() {
        @Override
        public void marshall(Integer val, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context) {
            jsonGenerator.writeValue(val);
        }
    };

    public static final JsonMarshaller<Long> LONG = new BaseJsonMarshaller<Long>() {
        @Override
        public void marshall(Long val, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context) {
            jsonGenerator.writeValue(val);
        }
    };

    public static final JsonMarshaller<Float> FLOAT = new BaseJsonMarshaller<Float>() {
        @Override
        public void marshall(Float val, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context) {
            jsonGenerator.writeValue(val);
        }
    };

    public static final JsonMarshaller<BigDecimal> BIG_DECIMAL = new BaseJsonMarshaller<BigDecimal>() {
        @Override
        public void marshall(BigDecimal val, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context) {
            jsonGenerator.writeValue(val);
        }
    };

    public static final JsonMarshaller<Double> DOUBLE = new BaseJsonMarshaller<Double>() {
        @Override
        public void marshall(Double val, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context) {
            jsonGenerator.writeValue(val);
        }
    };

    public static final JsonMarshaller<Boolean> BOOLEAN = new BaseJsonMarshaller<Boolean>() {
        @Override
        public void marshall(Boolean val, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context) {
            jsonGenerator.writeValue(val);
        }
    };

    public static final JsonMarshaller<Instant> INSTANT = new BaseJsonMarshaller<Instant>() {
        @Override
        public void marshall(Instant val, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context) {
            jsonGenerator.writeValue(val);
        }
    };

    public static final JsonMarshaller<SdkBytes> SDK_BYTES = new BaseJsonMarshaller<SdkBytes>() {
        @Override
        public void marshall(SdkBytes val, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context) {
            jsonGenerator.writeValue(val.asByteBuffer());
        }
    };

    public static final JsonMarshaller<StructuredPojo> STRUCTURED = new BaseJsonMarshaller<StructuredPojo>() {
        @Override
        public void marshall(StructuredPojo val, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context) {
            jsonGenerator.writeStartObject();
            val.marshall(context.protocolHandler());
            jsonGenerator.writeEndObject();

        }
    };

    public static final JsonMarshaller<List> LIST = new BaseJsonMarshaller<List>() {
        @Override
        public void marshall(List list, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context) {
            jsonGenerator.writeStartArray();
            for (Object listValue : list) {
                context.marshall(MarshallLocation.PAYLOAD, listValue);
            }
            jsonGenerator.writeEndArray();
        }

        @Override
        protected boolean shouldEmit(List list) {
            return !list.isEmpty() || !(list instanceof SdkAutoConstructList);

        }
    };

    /**
     * Marshalls a Map as a JSON object where each key becomes a field.
     */
    public static final JsonMarshaller<Map> MAP = new BaseJsonMarshaller<Map>() {
        @Override
        public void marshall(Map map, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context) {
            jsonGenerator.writeStartObject();
            for (Map.Entry<String, ?> entry : ((Map<String, ?>) map).entrySet()) {
                if (entry.getValue() != null) {
                    final Object value = entry.getValue();
                    jsonGenerator.writeFieldName(entry.getKey());
                    context.marshall(MarshallLocation.PAYLOAD, value);
                }
            }
            jsonGenerator.writeEndObject();
        }

        @Override
        protected boolean shouldEmit(Map map) {
            return !map.isEmpty() || !(map instanceof SdkAutoConstructMap);
        }
    };

    private SimpleTypeJsonMarshaller() {
    }

    /**
     * Base marshaller that emits the field name if present. The field name may be null in cases like
     * marshalling something inside a list or if the object is the explicit payload member.
     *
     * @param <T> Type to marshall.
     */
    private abstract static class BaseJsonMarshaller<T> implements JsonMarshaller<T> {

        @Override
        public final void marshall(T val, JsonMarshallerContext context, String paramName) {
            if (!shouldEmit(val)) {
                return;
            }
            if (paramName != null) {
                context.jsonGenerator().writeFieldName(paramName);
            }
            marshall(val, context.jsonGenerator(), context);
        }

        public abstract void marshall(T val, StructuredJsonGenerator jsonGenerator, JsonMarshallerContext context);

        protected boolean shouldEmit(T val) {
            return true;
        }
    }

}

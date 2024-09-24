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

package software.amazon.awssdk.protocols.rpcv2.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.protocols.core.NumberToInstant;
import software.amazon.awssdk.protocols.core.StringToInstant;
import software.amazon.awssdk.protocols.core.StringToValueConverter;
import software.amazon.awssdk.protocols.json.internal.unmarshall.DefaultJsonUnmarshallerRegistry;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonProtocolUnmarshaller;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonUnmarshaller;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonUnmarshallerContext;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonUnmarshallerRegistry;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.EmbeddedObjectJsonNode;

/**
 * Unmarshalling support for the Smithy RPCv2 protocol.
 */
@SdkInternalApi
public final class SdkRpcV2CborUnmarshaller {

    private SdkRpcV2CborUnmarshaller() {
    }

    public static JsonUnmarshallerRegistry timestampFormatRegistryFactory(
        Map<MarshallLocation, TimestampFormatTrait.Format> formats
    ) {
        DefaultJsonUnmarshallerRegistry.Builder builder =
            JsonProtocolUnmarshaller.createSharedRegistry()
                .toBuilder();
        StringToValueConverter.StringToValue<Instant> instantStringToValue = StringToInstant
            .create(formats.isEmpty() ?
                    new EnumMap<>(MarshallLocation.class) :
                    new EnumMap<>(formats));

        NumberToInstant instantNumberToValue = NumberToInstant
            .create(formats.isEmpty() ?
                    new EnumMap<>(MarshallLocation.class) :
                    new EnumMap<>(formats));

        SimpleTypeInstantJsonUnmarshaller<Instant> payloadUnmarshaller =
            new SimpleTypeInstantJsonUnmarshaller<>(instantStringToValue, instantNumberToValue);

        builder.payloadUnmarshaller(MarshallingType.INSTANT, payloadUnmarshaller);
        return setPayloadOverrides(builder).build();
    }

    /**
     * Creates an unmarshalling registry that knows how to read embedded values from the parse result.
     */
    public static DefaultJsonUnmarshallerRegistry.Builder setPayloadOverrides(DefaultJsonUnmarshallerRegistry.Builder builder) {
        builder.payloadUnmarshaller(MarshallingType.INTEGER, forEmbeddable(Number.class, Number::intValue,
                                                                           StringToValueConverter.TO_INTEGER))
               .payloadUnmarshaller(MarshallingType.LONG, forEmbeddable(Number.class, Number::longValue,
                                                                        StringToValueConverter.TO_LONG))
               .payloadUnmarshaller(MarshallingType.BYTE, forEmbeddable(Number.class, Number::byteValue,
                                                                        StringToValueConverter.TO_BYTE))
               .payloadUnmarshaller(MarshallingType.SHORT, forEmbeddable(Number.class, Number::shortValue,
                                                                         StringToValueConverter.TO_SHORT))
               .payloadUnmarshaller(MarshallingType.FLOAT, forEmbeddable(Number.class, Number::floatValue,
                                                                         StringToValueConverter.TO_FLOAT))
               .payloadUnmarshaller(MarshallingType.DOUBLE, forEmbeddable(Number.class, Number::doubleValue,
                                                                          StringToValueConverter.TO_DOUBLE))
               .payloadUnmarshaller(MarshallingType.BIG_DECIMAL, forEmbeddable(BigDecimal.class,
                                                                               StringToValueConverter.TO_BIG_DECIMAL))
               .payloadUnmarshaller(MarshallingType.BOOLEAN, forEmbeddable(Boolean.class,
                                                                           StringToValueConverter.TO_BOOLEAN));
        return builder;
    }

    /**
     * Creates an unmarshaller that expects the {@code embeddedType} from an {@link EmbeddedObjectJsonNode}. If the node given for
     * unmarshalling is of this type, and the type of its value is instance of {@code embeddedType}, then the {@code transformer}
     * is used to convert the expected type into the target type. For instance, to read an integer from an
     * {@link EmbeddedObjectJsonNode} the caller will use.
     * <pre>
     *     forEmbeddable(Number.class, Number::intValue, StringToValueConverter.TO_INTEGER)
     * </pre>
     * Using Java numbers to convert to other numbers gives a seamless way to upcast when the number is encoded using a smaller
     * type because those fit. The last argument allows a final fallback to parse from a string value.
     */
    private static <T, V> EmbeddableTypeTransformingJsonUnmarshaller<T, V> forEmbeddable(
        Class<V> embeddedType,
        Function<V, T> transformer,
        StringToValueConverter.StringToValue<T> stringToValue
    ) {
        return new EmbeddableTypeTransformingJsonUnmarshaller<>(embeddedType, transformer, stringToValue);
    }

    private static <T> EmbeddableTypeTransformingJsonUnmarshaller<T, T> forEmbeddable(
        Class<T> embeddedType,
        StringToValueConverter.StringToValue<T> stringToValue
    ) {
        return new EmbeddableTypeTransformingJsonUnmarshaller<>(embeddedType, Function.identity(), stringToValue);
    }

    private static class EmbeddableTypeTransformingJsonUnmarshaller<T, V> implements JsonUnmarshaller<T> {

        private final StringToValueConverter.StringToValue<T> stringToValue;
        private final Class<V> embeddedType;
        private final Function<V, T> typeConverter;

        private EmbeddableTypeTransformingJsonUnmarshaller(
            Class<V> embeddedType,
            Function<V, T> typeConverter,
            StringToValueConverter.StringToValue<T> stringToValue
        ) {
            this.stringToValue = stringToValue;
            this.typeConverter = typeConverter;
            this.embeddedType = embeddedType;
        }

        @Override
        public T unmarshall(JsonUnmarshallerContext context,
                            JsonNode jsonContent,
                            SdkField<T> field) {
            if (jsonContent == null || jsonContent.isNull()) {
                return null;
            }
            String text = null;
            if (jsonContent.isEmbeddedObject()) {
                Object embedded = jsonContent.asEmbeddedObject();
                if (embedded == null) {
                    return null;
                }
                if (embeddedType.isAssignableFrom(embedded.getClass())) {
                    return typeConverter.apply((V) embedded);
                }
                // Fallback in case that the embedded object is not what
                // we were looking for.
                text = embedded.toString();
            }
            if (text == null) {
                text = jsonContent.text();
            }
            return stringToValue.convert(text, field);
        }
    }

    private static class SimpleTypeInstantJsonUnmarshaller<T> implements JsonUnmarshaller<T> {

        private final StringToValueConverter.StringToValue<T> stringToValue;
        private final NumberToInstant numberToInstant;

        private SimpleTypeInstantJsonUnmarshaller(
            StringToValueConverter.StringToValue<T> stringToValue,
            NumberToInstant numberToInstant
        ) {
            this.stringToValue = stringToValue;
            this.numberToInstant = numberToInstant;
        }

        @Override
        public T unmarshall(JsonUnmarshallerContext context,
                            JsonNode jsonContent,
                            SdkField<T> field) {
            if (jsonContent == null || jsonContent.isNull()) {
                return null;
            }
            String text = null;
            if (jsonContent.isEmbeddedObject()) {
                Object embedded = jsonContent.asEmbeddedObject();
                if (embedded == null) {
                    return null;
                }
                if (Number.class.isAssignableFrom(embedded.getClass())) {
                    return (T) numberToInstant.convert((Number) embedded, (SdkField<Instant>) field);
                }
                // Fallback in case that the embedded object is not what
                // we were looking for.
                text = embedded.toString();
            }
            if (text == null) {
                text = jsonContent.text();
            }
            return stringToValue.convert(text, field);
        }
    }
}

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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.function.BiFunction;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.protocol.json.StructuredJsonGenerator;
import software.amazon.awssdk.core.runtime.transform.JsonUnmarshallerContext;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;
import software.amazon.awssdk.core.util.ImmutableMapParameter;

/**
 * Creates generators and protocol handlers for CBOR wire format.
 */
@SdkInternalApi
public abstract class SdkStructuredCborFactory {

    protected static final JsonFactory CBOR_FACTORY = new CBORFactory();

    protected static final CborGeneratorSupplier CBOR_GENERATOR_SUPPLIER =
        SdkCborGenerator::new;

    /**
     * cbor unmarshallers for scalar types.
     */
    protected static final Map<Class<?>, Unmarshaller<?, JsonUnmarshallerContext>> CBOR_SCALAR_UNMARSHALLERS =
        new ImmutableMapParameter.Builder<Class<?>, Unmarshaller<?, JsonUnmarshallerContext>>()
            .put(String.class, SimpleTypeCborUnmarshallers.StringCborUnmarshaller.getInstance())
            .put(Double.class, SimpleTypeCborUnmarshallers.DoubleCborUnmarshaller.getInstance())
            .put(Integer.class, SimpleTypeCborUnmarshallers.IntegerCborUnmarshaller.getInstance())
            .put(BigInteger.class, SimpleTypeCborUnmarshallers.BigIntegerCborUnmarshaller.getInstance())
            .put(BigDecimal.class, SimpleTypeCborUnmarshallers.BigDecimalCborUnmarshaller.getInstance())
            .put(Boolean.class, SimpleTypeCborUnmarshallers.BooleanCborUnmarshaller.getInstance())
            .put(Float.class, SimpleTypeCborUnmarshallers.FloatCborUnmarshaller.getInstance())
            .put(Long.class, SimpleTypeCborUnmarshallers.LongCborUnmarshaller.getInstance())
            .put(Byte.class, SimpleTypeCborUnmarshallers.ByteCborUnmarshaller.getInstance())
            .put(Date.class, SimpleTypeCborUnmarshallers.DateCborUnmarshaller.getInstance())
            .put(SdkBytes.class, SimpleTypeCborUnmarshallers.SdkBytesCborUnmarshaller.getInstance())
            .put(Instant.class, SimpleTypeCborUnmarshallers.InstantCborUnmarshaller.getInstance())
            .put(Short.class, SimpleTypeCborUnmarshallers.ShortCborUnmarshaller.getInstance()).build();

    protected SdkStructuredCborFactory() {
    }

    @FunctionalInterface
    protected interface CborGeneratorSupplier extends BiFunction<JsonFactory, String, StructuredJsonGenerator> {
        @Override
        StructuredJsonGenerator apply(JsonFactory jsonFactory, String contentType);
    }
}

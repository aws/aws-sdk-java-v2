/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.protocol.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.runtime.transform.JsonUnmarshallerContext;
import software.amazon.awssdk.runtime.transform.SimpleTypeCborUnmarshallers;
import software.amazon.awssdk.runtime.transform.Unmarshaller;
import software.amazon.awssdk.util.ImmutableMapParameter;

/**
 * Creates generators and protocol handlers for CBOR wire format.
 */
@SdkInternalApi
class SdkStructuredCborFactory {

    private static final JsonFactory CBOR_FACTORY = new CBORFactory();

    /**
     * cbor unmarshallers for scalar types.
     */
    private static final Map<Class<?>, Unmarshaller<?, JsonUnmarshallerContext>> CBOR_SCALAR_UNMARSHALLERS =
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
            .put(ByteBuffer.class, SimpleTypeCborUnmarshallers.ByteBufferCborUnmarshaller.getInstance())
            .put(Instant.class, SimpleTypeCborUnmarshallers.InstantCborUnmarshaller.getInstance())
            .put(Short.class, SimpleTypeCborUnmarshallers.ShortCborUnmarshaller.getInstance()).build();

    public static final SdkStructuredJsonFactory SDK_CBOR_FACTORY = new SdkStructuredJsonFactoryImpl(
            CBOR_FACTORY, CBOR_SCALAR_UNMARSHALLERS) {
        @Override
        protected StructuredJsonGenerator createWriter(JsonFactory jsonFactory,
                                                       String contentType) {
            return new SdkCborGenerator(jsonFactory, contentType);
        }
    };

}

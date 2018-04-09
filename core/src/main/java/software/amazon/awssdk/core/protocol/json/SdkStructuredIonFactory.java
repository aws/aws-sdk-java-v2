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

package software.amazon.awssdk.core.protocol.json;

import com.fasterxml.jackson.core.JsonFactory;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.http.CompositeErrorCodeParser;
import software.amazon.awssdk.core.internal.http.ErrorCodeParser;
import software.amazon.awssdk.core.internal.http.IonErrorCodeParser;
import software.amazon.awssdk.core.internal.http.JsonErrorCodeParser;
import software.amazon.awssdk.core.runtime.transform.JsonUnmarshallerContext;
import software.amazon.awssdk.core.runtime.transform.SimpleTypeIonUnmarshallers;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;
import software.amazon.awssdk.core.util.ImmutableMapParameter;
import software.amazon.ion.IonSystem;
import software.amazon.ion.system.IonBinaryWriterBuilder;
import software.amazon.ion.system.IonSystemBuilder;
import software.amazon.ion.system.IonTextWriterBuilder;
import software.amazon.ion.system.IonWriterBuilder;

@SdkInternalApi
class SdkStructuredIonFactory extends SdkStructuredJsonFactoryImpl {
    private static final IonSystem ION_SYSTEM = IonSystemBuilder.standard().build();
    private static final JsonFactory JSON_FACTORY = new IonFactory(ION_SYSTEM);
    private static final Map<Class<?>, Unmarshaller<?, JsonUnmarshallerContext>> UNMARSHALLERS =
            new ImmutableMapParameter.Builder<Class<?>, Unmarshaller<?, JsonUnmarshallerContext>>()
            .put(BigDecimal.class, SimpleTypeIonUnmarshallers.BigDecimalIonUnmarshaller.getInstance())
            .put(BigInteger.class, SimpleTypeIonUnmarshallers.BigIntegerIonUnmarshaller.getInstance())
            .put(Boolean.class, SimpleTypeIonUnmarshallers.BooleanIonUnmarshaller.getInstance())
            .put(ByteBuffer.class, SimpleTypeIonUnmarshallers.ByteBufferIonUnmarshaller.getInstance())
            .put(Byte.class, SimpleTypeIonUnmarshallers.ByteIonUnmarshaller.getInstance())
            .put(Date.class, SimpleTypeIonUnmarshallers.DateIonUnmarshaller.getInstance())
            .put(Double.class, SimpleTypeIonUnmarshallers.DoubleIonUnmarshaller.getInstance())
            .put(Float.class, SimpleTypeIonUnmarshallers.FloatIonUnmarshaller.getInstance())
            .put(Integer.class, SimpleTypeIonUnmarshallers.IntegerIonUnmarshaller.getInstance())
            .put(Long.class, SimpleTypeIonUnmarshallers.LongIonUnmarshaller.getInstance())
            .put(Short.class, SimpleTypeIonUnmarshallers.ShortIonUnmarshaller.getInstance())
            .put(String.class, SimpleTypeIonUnmarshallers.StringIonUnmarshaller.getInstance())
            .build();
    private static final IonBinaryWriterBuilder BINARY_WRITER_BUILDER = IonBinaryWriterBuilder.standard().immutable();
    public static final SdkStructuredIonFactory SDK_ION_BINARY_FACTORY = new SdkStructuredIonFactory(BINARY_WRITER_BUILDER);
    private static final IonTextWriterBuilder TEXT_WRITER_BUILDER = IonTextWriterBuilder.standard().immutable();
    public static final SdkStructuredIonFactory SDK_ION_TEXT_FACTORY = new SdkStructuredIonFactory(TEXT_WRITER_BUILDER);

    private final IonWriterBuilder builder;

    private SdkStructuredIonFactory(IonWriterBuilder builder) {
        super(JSON_FACTORY, UNMARSHALLERS);
        this.builder = builder;
    }

    @Override
    protected StructuredJsonGenerator createWriter(JsonFactory jsonFactory, String contentType) {
        return SdkIonGenerator.create(builder, contentType);
    }

    @Override
    protected ErrorCodeParser getErrorCodeParser(String customErrorCodeFieldName) {
        return new CompositeErrorCodeParser(
                new IonErrorCodeParser(ION_SYSTEM),
                new JsonErrorCodeParser(customErrorCodeFieldName));
    }
}

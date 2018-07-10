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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Map;
import java.util.function.BiFunction;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.protocol.json.StructuredJsonGenerator;
import software.amazon.awssdk.core.runtime.transform.JsonUnmarshallerContext;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;
import software.amazon.awssdk.core.util.ImmutableMapParameter;
import software.amazon.ion.IonSystem;
import software.amazon.ion.system.IonSystemBuilder;
import software.amazon.ion.system.IonWriterBuilder;

@SdkInternalApi
public abstract class SdkStructuredIonFactory {

    protected static final IonSystem ION_SYSTEM = IonSystemBuilder.standard().build();

    protected static final JsonFactory JSON_FACTORY = new IonFactory(ION_SYSTEM);

    protected static final IonGeneratorSupplier ION_GENERATOR_SUPPLIER = SdkIonGenerator::create;

    protected static final Map<Class<?>, Unmarshaller<?, JsonUnmarshallerContext>> UNMARSHALLERS =
        new ImmutableMapParameter.Builder<Class<?>, Unmarshaller<?, JsonUnmarshallerContext>>()
            .put(BigDecimal.class, SimpleTypeIonUnmarshallers.BigDecimalIonUnmarshaller.getInstance())
            .put(BigInteger.class, SimpleTypeIonUnmarshallers.BigIntegerIonUnmarshaller.getInstance())
            .put(Boolean.class, SimpleTypeIonUnmarshallers.BooleanIonUnmarshaller.getInstance())
            .put(SdkBytes.class, SimpleTypeIonUnmarshallers.SdkBytesIonUnmarshaller.getInstance())
            .put(Byte.class, SimpleTypeIonUnmarshallers.ByteIonUnmarshaller.getInstance())
            .put(Date.class, SimpleTypeIonUnmarshallers.DateIonUnmarshaller.getInstance())
            .put(Double.class, SimpleTypeIonUnmarshallers.DoubleIonUnmarshaller.getInstance())
            .put(Float.class, SimpleTypeIonUnmarshallers.FloatIonUnmarshaller.getInstance())
            .put(Integer.class, SimpleTypeIonUnmarshallers.IntegerIonUnmarshaller.getInstance())
            .put(Long.class, SimpleTypeIonUnmarshallers.LongIonUnmarshaller.getInstance())
            .put(Short.class, SimpleTypeIonUnmarshallers.ShortIonUnmarshaller.getInstance())
            .put(String.class, SimpleTypeIonUnmarshallers.StringIonUnmarshaller.getInstance())
            .build();

    protected SdkStructuredIonFactory() {
    }

    @FunctionalInterface
    protected interface IonGeneratorSupplier extends BiFunction<IonWriterBuilder, String, StructuredJsonGenerator> {
        StructuredJsonGenerator apply(IonWriterBuilder writerBuilder, String contentType);
    }
}

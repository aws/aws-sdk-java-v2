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
import java.time.Instant;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.runtime.transform.JsonUnmarshallerContext;
import software.amazon.awssdk.core.runtime.transform.SimpleTypeJsonUnmarshallers;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;
import software.amazon.awssdk.core.util.ImmutableMapParameter;

/**
 * Creates generators and protocol handlers for plain text JSON wire format.
 */
@SdkProtectedApi
public final class SdkStructuredPlainJsonFactory {

    /**
     * Recommended to share JsonFactory instances per http://wiki.fasterxml
     * .com/JacksonBestPracticesPerformance
     */
    public static final JsonFactory JSON_FACTORY = new JsonFactory();

    @SdkTestInternalApi
    public static final Map<Class<?>, Unmarshaller<?, JsonUnmarshallerContext>> JSON_SCALAR_UNMARSHALLERS =
            new ImmutableMapParameter.Builder<Class<?>, Unmarshaller<?, JsonUnmarshallerContext>>()
            .put(String.class, SimpleTypeJsonUnmarshallers.StringJsonUnmarshaller.getInstance())
            .put(Double.class, SimpleTypeJsonUnmarshallers.DoubleJsonUnmarshaller.getInstance())
            .put(Integer.class, SimpleTypeJsonUnmarshallers.IntegerJsonUnmarshaller.getInstance())
            .put(BigInteger.class, SimpleTypeJsonUnmarshallers.BigIntegerJsonUnmarshaller.getInstance())
            .put(BigDecimal.class, SimpleTypeJsonUnmarshallers.BigDecimalJsonUnmarshaller.getInstance())
            .put(Boolean.class, SimpleTypeJsonUnmarshallers.BooleanJsonUnmarshaller.getInstance())
            .put(Float.class, SimpleTypeJsonUnmarshallers.FloatJsonUnmarshaller.getInstance())
            .put(Long.class, SimpleTypeJsonUnmarshallers.LongJsonUnmarshaller.getInstance())
            .put(Byte.class, SimpleTypeJsonUnmarshallers.ByteJsonUnmarshaller.getInstance())
            .put(Instant.class, SimpleTypeJsonUnmarshallers.InstantJsonUnmarshaller.getInstance())
            .put(ByteBuffer.class, SimpleTypeJsonUnmarshallers.ByteBufferJsonUnmarshaller.getInstance())
            .put(Character.class, SimpleTypeJsonUnmarshallers.CharacterJsonUnmarshaller.getInstance())
            .put(Short.class, SimpleTypeJsonUnmarshallers.ShortJsonUnmarshaller.getInstance()).build();

    public static final SdkStructuredJsonFactory SDK_JSON_FACTORY = new SdkStructuredJsonFactoryImpl(
            JSON_FACTORY, JSON_SCALAR_UNMARSHALLERS) {
        @Override
        protected StructuredJsonGenerator createWriter(JsonFactory jsonFactory,
                                                       String contentType) {
            return new SdkJsonGenerator(jsonFactory, contentType);
        }
    };

    private SdkStructuredPlainJsonFactory() {
    }
}

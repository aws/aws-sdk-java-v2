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
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Date;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.runtime.transform.JsonUnmarshallerContext;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;

@SdkInternalApi
public class SimpleTypeIonUnmarshallers {
    public static class StringIonUnmarshaller implements Unmarshaller<String, JsonUnmarshallerContext> {
        private static final StringIonUnmarshaller INSTANCE = new StringIonUnmarshaller();

        public static StringIonUnmarshaller getInstance() {
            return INSTANCE;
        }

        @Override
        public String unmarshall(JsonUnmarshallerContext context) throws Exception {
            return context.readText();
        }
    }


    public static class DoubleIonUnmarshaller implements Unmarshaller<Double, JsonUnmarshallerContext> {
        private static final DoubleIonUnmarshaller INSTANCE = new DoubleIonUnmarshaller();

        public static DoubleIonUnmarshaller getInstance() {
            return INSTANCE;
        }

        @Override
        public Double unmarshall(JsonUnmarshallerContext context) throws Exception {
            return context.getJsonParser().getDoubleValue();
        }
    }

    public static class IntegerIonUnmarshaller implements Unmarshaller<Integer, JsonUnmarshallerContext> {
        private static final IntegerIonUnmarshaller INSTANCE = new IntegerIonUnmarshaller();

        public static IntegerIonUnmarshaller getInstance() {
            return INSTANCE;
        }

        @Override
        public Integer unmarshall(JsonUnmarshallerContext context) throws Exception {
            return context.getJsonParser().getIntValue();
        }
    }

    public static class BigIntegerIonUnmarshaller implements Unmarshaller<BigInteger, JsonUnmarshallerContext> {
        private static final BigIntegerIonUnmarshaller INSTANCE = new BigIntegerIonUnmarshaller();

        public static BigIntegerIonUnmarshaller getInstance() {
            return INSTANCE;
        }

        @Override
        public BigInteger unmarshall(JsonUnmarshallerContext context) throws Exception {
            return context.getJsonParser().getBigIntegerValue();
        }
    }

    public static class BigDecimalIonUnmarshaller implements Unmarshaller<BigDecimal, JsonUnmarshallerContext> {
        private static final BigDecimalIonUnmarshaller INSTANCE = new BigDecimalIonUnmarshaller();

        public static BigDecimalIonUnmarshaller getInstance() {
            return INSTANCE;
        }

        @Override
        public BigDecimal unmarshall(JsonUnmarshallerContext context) throws Exception {
            return context.getJsonParser().getDecimalValue();
        }
    }

    public static class BooleanIonUnmarshaller implements Unmarshaller<Boolean, JsonUnmarshallerContext> {
        private static final BooleanIonUnmarshaller INSTANCE = new BooleanIonUnmarshaller();

        public static BooleanIonUnmarshaller getInstance() {
            return INSTANCE;
        }

        @Override
        public Boolean unmarshall(JsonUnmarshallerContext context) throws Exception {
            return context.getJsonParser().getBooleanValue();
        }
    }

    public static class FloatIonUnmarshaller implements Unmarshaller<Float, JsonUnmarshallerContext> {
        private static final FloatIonUnmarshaller INSTANCE = new FloatIonUnmarshaller();

        public static FloatIonUnmarshaller getInstance() {
            return INSTANCE;
        }

        @Override
        public Float unmarshall(JsonUnmarshallerContext context) throws Exception {
            return context.getJsonParser().getFloatValue();
        }
    }

    public static class LongIonUnmarshaller implements Unmarshaller<Long, JsonUnmarshallerContext> {
        private static final LongIonUnmarshaller INSTANCE = new LongIonUnmarshaller();

        public static LongIonUnmarshaller getInstance() {
            return INSTANCE;
        }

        @Override
        public Long unmarshall(JsonUnmarshallerContext context) throws Exception {
            return context.getJsonParser().getLongValue();
        }
    }

    public static class ByteIonUnmarshaller implements Unmarshaller<Byte, JsonUnmarshallerContext> {
        private static final ByteIonUnmarshaller INSTANCE = new ByteIonUnmarshaller();

        public static ByteIonUnmarshaller getInstance() {
            return INSTANCE;
        }

        @Override
        public Byte unmarshall(JsonUnmarshallerContext context) throws Exception {
            return context.getJsonParser().getByteValue();
        }
    }

    public static class DateIonUnmarshaller implements Unmarshaller<Date, JsonUnmarshallerContext> {
        private static final DateIonUnmarshaller INSTANCE = new DateIonUnmarshaller();

        public static DateIonUnmarshaller getInstance() {
            return INSTANCE;
        }

        @Override
        public Date unmarshall(JsonUnmarshallerContext context) throws Exception {
            return (Date) context.getJsonParser().getEmbeddedObject();
        }
    }

    public static class SdkBytesIonUnmarshaller implements Unmarshaller<SdkBytes, JsonUnmarshallerContext> {
        private static final SdkBytesIonUnmarshaller INSTANCE = new SdkBytesIonUnmarshaller();

        public static SdkBytesIonUnmarshaller getInstance() {
            return INSTANCE;
        }

        @Override
        public SdkBytes unmarshall(JsonUnmarshallerContext context) throws Exception {
            return SdkBytes.fromByteBuffer((ByteBuffer) context.getJsonParser().getEmbeddedObject());
        }
    }

    public static class ShortIonUnmarshaller implements Unmarshaller<Short, JsonUnmarshallerContext> {
        private static final ShortIonUnmarshaller INSTANCE = new ShortIonUnmarshaller();

        public static ShortIonUnmarshaller getInstance() {
            return INSTANCE;
        }

        @Override
        public Short unmarshall(JsonUnmarshallerContext context) throws Exception {
            return context.getJsonParser().getShortValue();
        }
    }
}

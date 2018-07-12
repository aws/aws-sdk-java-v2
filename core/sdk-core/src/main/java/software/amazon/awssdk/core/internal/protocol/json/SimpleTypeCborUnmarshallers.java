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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Date;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.runtime.transform.JsonUnmarshallerContext;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;

@SdkInternalApi
public class SimpleTypeCborUnmarshallers {
    /**
     * Unmarshaller for String values.
     */
    public static class StringCborUnmarshaller implements Unmarshaller<String, JsonUnmarshallerContext> {
        private static final StringCborUnmarshaller INSTANCE = new StringCborUnmarshaller();

        public static StringCborUnmarshaller getInstance() {
            return INSTANCE;
        }

        public String unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            return unmarshallerContext.readText();
        }
    }

    /**
     * Unmarshaller for Double values.
     */
    public static class DoubleCborUnmarshaller implements Unmarshaller<Double, JsonUnmarshallerContext> {
        private static final DoubleCborUnmarshaller INSTANCE = new DoubleCborUnmarshaller();

        public static DoubleCborUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Double unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            return unmarshallerContext.getJsonParser().getDoubleValue();
        }
    }

    /**
     * Unmarshaller for Integer values.
     */
    public static class IntegerCborUnmarshaller implements Unmarshaller<Integer, JsonUnmarshallerContext> {
        private static final IntegerCborUnmarshaller INSTANCE = new IntegerCborUnmarshaller();

        public static IntegerCborUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Integer unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            return unmarshallerContext.getJsonParser().getIntValue();
        }
    }

    public static class BigIntegerCborUnmarshaller implements Unmarshaller<BigInteger, JsonUnmarshallerContext> {
        private static final BigIntegerCborUnmarshaller INSTANCE = new BigIntegerCborUnmarshaller();

        public static BigIntegerCborUnmarshaller getInstance() {
            return INSTANCE;
        }

        public BigInteger unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            JsonParser parser = unmarshallerContext.getJsonParser();
            JsonToken current = parser.getCurrentToken();
            if (current == JsonToken.VALUE_NUMBER_INT) {
                return parser.getBigIntegerValue();
            } else if (current == JsonToken.VALUE_EMBEDDED_OBJECT) {
                Object embedded = parser.getEmbeddedObject();
                return new BigInteger((byte[]) embedded);
            } else {
                throw SdkClientException.builder().message("Invalid BigInteger Format.").build();
            }
        }
    }

    public static class BigDecimalCborUnmarshaller implements Unmarshaller<BigDecimal, JsonUnmarshallerContext> {
        private static final BigDecimalCborUnmarshaller INSTANCE = new BigDecimalCborUnmarshaller();

        public static BigDecimalCborUnmarshaller getInstance() {
            return INSTANCE;
        }

        public BigDecimal unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            JsonParser parser = unmarshallerContext.getJsonParser();
            Unmarshaller<BigInteger, JsonUnmarshallerContext> bigIntegerUnmarshaller =
                    unmarshallerContext.getUnmarshaller(BigInteger.class);

            JsonToken current = parser.getCurrentToken();
            if (current != JsonToken.START_ARRAY) {
                throw SdkClientException.builder().message("Invalid BigDecimal Format.").build();
            }
            parser.nextToken();
            int exponent = parser.getIntValue();
            parser.nextToken();
            BigInteger mantissa = bigIntegerUnmarshaller.unmarshall(unmarshallerContext);
            return new BigDecimal(mantissa, exponent);
        }
    }

    /**
     * Unmarshaller for Boolean values.
     */
    public static class BooleanCborUnmarshaller implements Unmarshaller<Boolean, JsonUnmarshallerContext> {
        private static final BooleanCborUnmarshaller INSTANCE = new BooleanCborUnmarshaller();

        public static BooleanCborUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Boolean unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            return unmarshallerContext.getJsonParser().getBooleanValue();
        }
    }

    /**
     * Unmarshaller for Float values.
     */
    public static class FloatCborUnmarshaller implements Unmarshaller<Float, JsonUnmarshallerContext> {
        private static final FloatCborUnmarshaller INSTANCE = new FloatCborUnmarshaller();

        public static FloatCborUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Float unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            return unmarshallerContext.getJsonParser().getFloatValue();
        }
    }

    /**
     * Unmarshaller for Long values.
     */
    public static class LongCborUnmarshaller implements Unmarshaller<Long, JsonUnmarshallerContext> {
        private static final LongCborUnmarshaller INSTANCE = new LongCborUnmarshaller();

        public static LongCborUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Long unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            return unmarshallerContext.getJsonParser().getLongValue();
        }
    }

    /**
     * Unmarshaller for Byte values.
     */
    public static class ByteCborUnmarshaller implements Unmarshaller<Byte, JsonUnmarshallerContext> {
        private static final ByteCborUnmarshaller INSTANCE = new ByteCborUnmarshaller();

        public static ByteCborUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Byte unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            return unmarshallerContext.getJsonParser().getByteValue();
        }
    }

    /**
     * Unmarshaller for Date values - JSON dates come in as epoch seconds.
     */
    public static class DateCborUnmarshaller implements Unmarshaller<Date, JsonUnmarshallerContext> {
        private static final DateCborUnmarshaller INSTANCE = new DateCborUnmarshaller();

        public static DateCborUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Date unmarshall(JsonUnmarshallerContext unmarshallerContext)
                throws Exception {
            return new Date(unmarshallerContext.getJsonParser().getLongValue());
        }
    }

    /**
     * Unmarshaller for ByteBuffer values.
     */
    public static class SdkBytesCborUnmarshaller implements Unmarshaller<SdkBytes, JsonUnmarshallerContext> {
        private static final SdkBytesCborUnmarshaller INSTANCE = new SdkBytesCborUnmarshaller();

        public static SdkBytesCborUnmarshaller getInstance() {
            return INSTANCE;
        }

        public SdkBytes unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            return SdkBytes.fromByteArray(unmarshallerContext.getJsonParser().getBinaryValue());

        }
    }

    public static class InstantCborUnmarshaller implements Unmarshaller<Instant, JsonUnmarshallerContext> {
        private static final InstantCborUnmarshaller INSTANCE = new InstantCborUnmarshaller();

        public static InstantCborUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Instant unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            return Instant.ofEpochMilli(unmarshallerContext.getJsonParser().getLongValue());
        }
    }

    /**
     * Unmarshaller for Short values.
     */
    public static class ShortCborUnmarshaller implements Unmarshaller<Short, JsonUnmarshallerContext> {
        private static final ShortCborUnmarshaller INSTANCE = new ShortCborUnmarshaller();

        public static ShortCborUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Short unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            return unmarshallerContext.getJsonParser().getShortValue();
        }
    }
}

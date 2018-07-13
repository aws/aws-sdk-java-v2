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

package software.amazon.awssdk.core.runtime.transform;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.util.AwsDateUtils;
import software.amazon.awssdk.utils.BinaryUtils;

@SdkProtectedApi
public final class SimpleTypeJsonUnmarshallers {
    private SimpleTypeJsonUnmarshallers() {
    }

    /**
     * Unmarshaller for String values.
     */
    public static class StringJsonUnmarshaller implements Unmarshaller<String, JsonUnmarshallerContext> {
        private static final StringJsonUnmarshaller INSTANCE = new StringJsonUnmarshaller();

        public static StringJsonUnmarshaller getInstance() {
            return INSTANCE;
        }

        public String unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            return unmarshallerContext.readText();
        }
    }

    /**
     * Unmarshaller for Double values.
     */
    public static class DoubleJsonUnmarshaller implements Unmarshaller<Double, JsonUnmarshallerContext> {
        private static final DoubleJsonUnmarshaller INSTANCE = new DoubleJsonUnmarshaller();

        public static DoubleJsonUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Double unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            String doubleString = unmarshallerContext.readText();
            return (doubleString == null) ? null : Double.parseDouble(doubleString);
        }
    }

    /**
     * Unmarshaller for Integer values.
     */
    public static class IntegerJsonUnmarshaller implements Unmarshaller<Integer, JsonUnmarshallerContext> {
        private static final IntegerJsonUnmarshaller INSTANCE = new IntegerJsonUnmarshaller();

        public static IntegerJsonUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Integer unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            String intString = unmarshallerContext.readText();
            return (intString == null) ? null : Integer.parseInt(intString);
        }
    }

    public static class BigIntegerJsonUnmarshaller implements Unmarshaller<BigInteger, JsonUnmarshallerContext> {
        private static final BigIntegerJsonUnmarshaller INSTANCE = new BigIntegerJsonUnmarshaller();

        public static BigIntegerJsonUnmarshaller getInstance() {
            return INSTANCE;
        }

        public BigInteger unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            String intString = unmarshallerContext.readText();
            return (intString == null) ? null : new BigInteger(intString);
        }
    }

    public static class BigDecimalJsonUnmarshaller implements Unmarshaller<BigDecimal, JsonUnmarshallerContext> {
        private static final BigDecimalJsonUnmarshaller INSTANCE = new BigDecimalJsonUnmarshaller();

        public static BigDecimalJsonUnmarshaller getInstance() {
            return INSTANCE;
        }

        public BigDecimal unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            String s = unmarshallerContext.readText();
            return (s == null) ? null : new BigDecimal(s);
        }
    }

    /**
     * Unmarshaller for Boolean values.
     */
    public static class BooleanJsonUnmarshaller implements Unmarshaller<Boolean, JsonUnmarshallerContext> {
        private static final BooleanJsonUnmarshaller INSTANCE = new BooleanJsonUnmarshaller();

        public static BooleanJsonUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Boolean unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            String booleanString = unmarshallerContext.readText();
            return (booleanString == null) ? null : Boolean.parseBoolean(booleanString);
        }
    }

    /**
     * Unmarshaller for Float values.
     */
    public static class FloatJsonUnmarshaller implements Unmarshaller<Float, JsonUnmarshallerContext> {
        private static final FloatJsonUnmarshaller INSTANCE = new FloatJsonUnmarshaller();

        public static FloatJsonUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Float unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            String floatString = unmarshallerContext.readText();
            return (floatString == null) ? null : Float.valueOf(floatString);
        }
    }

    /**
     * Unmarshaller for Long values.
     */
    public static class LongJsonUnmarshaller implements Unmarshaller<Long, JsonUnmarshallerContext> {
        private static final LongJsonUnmarshaller INSTANCE = new LongJsonUnmarshaller();

        public static LongJsonUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Long unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            String longString = unmarshallerContext.readText();
            return (longString == null) ? null : Long.parseLong(longString);
        }
    }

    /**
     * Unmarshaller for Byte values.
     */
    public static class ByteJsonUnmarshaller implements Unmarshaller<Byte, JsonUnmarshallerContext> {
        private static final ByteJsonUnmarshaller INSTANCE = new ByteJsonUnmarshaller();

        public static ByteJsonUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Byte unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            String byteString = unmarshallerContext.readText();
            return (byteString == null) ? null : Byte.valueOf(byteString);
        }
    }

    public static class InstantJsonUnmarshaller implements Unmarshaller<Instant, JsonUnmarshallerContext> {
        private static final InstantJsonUnmarshaller INSTANCE = new InstantJsonUnmarshaller();

        public static InstantJsonUnmarshaller getInstance() {
            return INSTANCE;
        }

        @ReviewBeforeRelease("This date parsing is coupled to AWS's format. Should we generalize it?")
        public Instant unmarshall(JsonUnmarshallerContext unmarshallerContext)
                throws Exception {
            return AwsDateUtils.parseServiceSpecificInstant(unmarshallerContext
                    .readText());
        }
    }

    /**
     * Unmarshaller for ByteBuffer values.
     */
    public static class SdkBytesJsonUnmarshaller implements Unmarshaller<SdkBytes, JsonUnmarshallerContext> {
        private static final SdkBytesJsonUnmarshaller INSTANCE = new SdkBytesJsonUnmarshaller();

        public static SdkBytesJsonUnmarshaller getInstance() {
            return INSTANCE;
        }

        public SdkBytes unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            String base64EncodedString = unmarshallerContext.readText();
            if (base64EncodedString == null) {
                return null;
            }
            byte[] decodedBytes = BinaryUtils.fromBase64(base64EncodedString);
            return SdkBytes.fromByteArray(decodedBytes);

        }
    }

    /**
     * Unmarshaller for Character values.
     */
    public static class CharacterJsonUnmarshaller implements Unmarshaller<Character, JsonUnmarshallerContext> {
        private static final CharacterJsonUnmarshaller INSTANCE = new CharacterJsonUnmarshaller();

        public static CharacterJsonUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Character unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            String charString = unmarshallerContext.readText();

            if (charString == null) {
                return null;
            }

            charString = charString.trim();
            if (charString.isEmpty() || charString.length() > 1) {
                throw SdkClientException.builder()
                                        .message("'" + charString + "' cannot be converted to Character")
                                        .build();
            }
            return Character.valueOf(charString.charAt(0));
        }
    }

    /**
     * Unmarshaller for Short values.
     */
    public static class ShortJsonUnmarshaller implements Unmarshaller<Short, JsonUnmarshallerContext> {
        private static final ShortJsonUnmarshaller INSTANCE = new ShortJsonUnmarshaller();

        public static ShortJsonUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Short unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            String shortString = unmarshallerContext.readText();
            return (shortString == null) ? null : Short.valueOf(shortString);
        }
    }
}

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

package software.amazon.awssdk.runtime.transform;

import java.nio.ByteBuffer;
import java.util.Date;
import org.w3c.dom.Node;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.util.XpathUtils;

/**
 * Collection of unmarshallers for simple data types.
 */
@SdkProtectedApi
public class SimpleTypeUnmarshallers {

    /**
     * Unmarshaller for String values.
     */
    public static class StringUnmarshaller implements Unmarshaller<String, Node> {
        private static volatile StringUnmarshaller instance;

        public static StringUnmarshaller getInstance() {
            if (instance == null) {
                synchronized (StringUnmarshaller.class) {
                    if (instance == null) {
                        instance = new StringUnmarshaller();
                    }
                }
            }
            return instance;
        }

        public String unmarshall(Node in) throws Exception {
            return XpathUtils.asString(".", in);
        }
    }

    /**
     * Unmarshaller for Double values.
     */
    public static class DoubleUnmarshaller implements Unmarshaller<Double, Node> {
        private static volatile DoubleUnmarshaller instance;

        public static DoubleUnmarshaller getInstance() {
            if (instance == null) {
                synchronized (DoubleUnmarshaller.class) {
                    if (instance == null) {
                        instance = new DoubleUnmarshaller();
                    }
                }
            }
            return instance;
        }

        public Double unmarshall(Node in) throws Exception {
            return XpathUtils.asDouble(".", in);
        }
    }

    /**
     * Unmarshaller for Integer values.
     */
    public static class IntegerUnmarshaller implements Unmarshaller<Integer, Node> {
        private static volatile IntegerUnmarshaller instance;

        public static IntegerUnmarshaller getInstance() {
            if (instance == null) {
                synchronized (IntegerUnmarshaller.class) {
                    if (instance == null) {
                        instance = new IntegerUnmarshaller();
                    }
                }
            }
            return instance;
        }

        public Integer unmarshall(Node in) throws Exception {
            return XpathUtils.asInteger(".", in);
        }
    }

    /**
     * Unmarshaller for Boolean values.
     */
    public static class BooleanUnmarshaller implements Unmarshaller<Boolean, Node> {
        private static volatile BooleanUnmarshaller instance;

        public static BooleanUnmarshaller getInstance() {
            if (instance == null) {
                synchronized (BooleanUnmarshaller.class) {
                    if (instance == null) {
                        instance = new BooleanUnmarshaller();
                    }
                }
            }
            return instance;
        }

        public Boolean unmarshall(Node in) throws Exception {
            return XpathUtils.asBoolean(".", in);
        }
    }

    /**
     * Unmarshaller for Float values.
     */
    public static class FloatUnmarshaller implements Unmarshaller<Float, Node> {
        private static volatile FloatUnmarshaller instance;

        public static FloatUnmarshaller getInstance() {
            if (instance == null) {
                synchronized (FloatUnmarshaller.class) {
                    if (instance == null) {
                        instance = new FloatUnmarshaller();
                    }
                }
            }
            return instance;
        }

        public Float unmarshall(Node in) throws Exception {
            return XpathUtils.asFloat(".", in);
        }
    }

    /**
     * Unmarshaller for Long values.
     */
    public static class LongUnmarshaller implements Unmarshaller<Long, Node> {
        private static volatile LongUnmarshaller instance;

        public static LongUnmarshaller getInstance() {
            if (instance == null) {
                synchronized (LongUnmarshaller.class) {
                    if (instance == null) {
                        instance = new LongUnmarshaller();
                    }
                }
            }
            return instance;
        }

        public Long unmarshall(Node in) throws Exception {
            return XpathUtils.asLong(".", in);
        }
    }

    /**
     * Unmarshaller for Byte values.
     */
    public static class ByteUnmarshaller implements Unmarshaller<Byte, Node> {
        private static volatile ByteUnmarshaller instance;

        public static ByteUnmarshaller getInstance() {
            if (instance == null) {
                synchronized (ByteUnmarshaller.class) {
                    if (instance == null) {
                        instance = new ByteUnmarshaller();
                    }
                }
            }
            return instance;
        }

        public Byte unmarshall(Node in) throws Exception {
            return XpathUtils.asByte(".", in);
        }
    }

    /**
     * Unmarshaller for Date values.
     */
    public static class DateUnmarshaller implements Unmarshaller<Date, Node> {
        private static volatile DateUnmarshaller instance;

        public static DateUnmarshaller getInstance() {
            if (instance == null) {
                synchronized (DateUnmarshaller.class) {
                    if (instance == null) {
                        instance = new DateUnmarshaller();
                    }
                }
            }
            return instance;
        }

        public Date unmarshall(Node in) throws Exception {
            return XpathUtils.asDate(".", in);
        }
    }

    /**
     * Unmarshaller for ByteBuffer values.
     */
    public static class ByteBufferUnmarshaller implements Unmarshaller<ByteBuffer, Node> {
        private static volatile ByteBufferUnmarshaller instance;

        public static ByteBufferUnmarshaller getInstance() {
            if (instance == null) {
                synchronized (ByteBufferUnmarshaller.class) {
                    if (instance == null) {
                        instance = new ByteBufferUnmarshaller();
                    }
                }
            }
            return instance;
        }

        public ByteBuffer unmarshall(Node in) throws Exception {
            return XpathUtils.asByteBuffer(".", in);
        }
    }

}

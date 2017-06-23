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

package software.amazon.awssdk.protocol.asserts.marshalling;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.math.BigInteger;
import java.util.Objects;
import software.amazon.ion.Decimal;
import software.amazon.ion.IonReader;
import software.amazon.ion.IonSystem;
import software.amazon.ion.IonType;
import software.amazon.ion.Timestamp;
import software.amazon.ion.system.IonSystemBuilder;

public class IonBodyAssertion extends MarshallingAssertion {
    private static final double DOUBLE_DELTA = 0.0001d;
    private static final IonSystem ION_SYSTEM = IonSystemBuilder.standard().build();

    private final String ionEquals;

    public IonBodyAssertion(String ionEquals) {
        this.ionEquals = ionEquals;
    }

    @Override
    protected void doAssert(LoggedRequest request) throws Exception {
        IonReader expected = ION_SYSTEM.newReader(ionEquals);
        IonReader actual = ION_SYSTEM.newReader(request.getBody());
        assertIonReaderEquals(expected, actual);
    }

    private void assertIonReaderEquals(IonReader x, IonReader y) {
        for (int token = 0; ; token++) {
            IonType xType = x.next();
            IonType yType = y.next();

            if (xType == null && yType == null) {
                if (x.getDepth() == 0 && y.getDepth() == 0) {
                    return;
                } else {
                    x.stepOut();
                    y.stepOut();
                    continue;
                }
            }

            if (!Objects.equals(xType, yType)) {
                fail(String.format("Types (%s, %s) are unequal at token %s", xType, yType, token));
            }

            if (x.isInStruct() && y.isInStruct()) {
                String xFieldName = x.getFieldName();
                String yFieldName = y.getFieldName();
                assertEquals(
                        String.format("Unequal field names (%s, %s) at token %s", xFieldName, yFieldName, token),
                        xFieldName,
                        yFieldName);
            }

            boolean xNull = x.isNullValue();
            boolean yNull = y.isNullValue();
            if ((xNull && !yNull) || (yNull && !xNull)) {
                fail(String.format("One value is null but the other is not at token %s", token));
            } else if (xNull && yNull) {
                continue;
            }

            switch (xType) {
                case BLOB:
                case CLOB:
                    int sizeX = x.byteSize();
                    int sizeY = y.byteSize();
                    assertEquals(
                            String.format("Unequal LOB sizes (%s, %s) at token %s", sizeX, sizeY, token),
                            sizeX,
                            sizeY);

                    byte[] bufferX = new byte[sizeX];
                    byte[] bufferY = new byte[sizeY];

                    x.getBytes(bufferX, 0, sizeX);
                    y.getBytes(bufferY, 0, sizeY);

                    assertArrayEquals(
                            String.format("Unequal LOBs at token %s", token),
                            bufferX,
                            bufferY);
                    break;

                case BOOL:
                    boolean xBoolean = x.booleanValue();
                    boolean yBoolean = y.booleanValue();
                    assertEquals(
                            String.format("Unequal boolean values (%s, %s) at token %s", xBoolean, yBoolean, token),
                            xBoolean,
                            yBoolean);
                    break;

                case DECIMAL:
                    Decimal xDecimal = x.decimalValue();
                    Decimal yDecimal = y.decimalValue();
                    assertEquals(
                            String.format("Unequal decimal values (%s, %s) at token %s", xDecimal, yDecimal, token),
                            xDecimal,
                            yDecimal);
                    break;

                case FLOAT:
                    double xDouble = x.doubleValue();
                    double yDouble = y.doubleValue();
                    assertEquals(
                            String.format("Unequal float values (%s, %s) at token %s", xDouble, yDouble, token),
                            xDouble,
                            yDouble,
                            DOUBLE_DELTA);
                    break;

                case INT:
                    BigInteger xInteger = x.bigIntegerValue();
                    BigInteger yInteger = y.bigIntegerValue();
                    assertEquals(
                            String.format("Unequal integer values (%s, %s) at token %s", xInteger, yInteger, token),
                            xInteger,
                            yInteger);
                    break;

                case LIST:
                case SEXP:
                    x.stepIn();
                    y.stepIn();
                    break;

                case NULL:
                    throw new IllegalStateException("We should never fall through to the IonType.NULL block due to previous " +
                                                    "assertions for equal types and nullness");

                case STRING:
                case SYMBOL:
                    String xString = x.stringValue();
                    String yString = y.stringValue();
                    assertEquals(
                            String.format("Unequal string values (%s, %s) at token %s", xString, yString, token),
                            xString,
                            yString);
                    break;

                case STRUCT:
                    x.stepIn();
                    y.stepIn();
                    break;

                case TIMESTAMP:
                    Timestamp xTimestamp = x.timestampValue();
                    Timestamp yTimestamp = y.timestampValue();
                    assertEquals(
                            String.format("Unequal timestamp values (%s, %s) at token %s", xTimestamp, yTimestamp, token),
                            xTimestamp,
                            yTimestamp);
                    break;

                default:
                    fail(String.format("Unrecognized IonType %s", xType));
            }
        }
    }
}

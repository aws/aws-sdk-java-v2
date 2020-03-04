/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans;

import java.util.Objects;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class PrimitiveTypesBean {
    private String id;
    private boolean booleanAttribute;
    private int integerAttribute;
    private long longAttribute;
    private short shortAttribute;
    private byte byteAttribute;
    private double doubleAttribute;
    private float floatAttribute;

    @DynamoDbPartitionKey
    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public boolean isBooleanAttribute() {
        return booleanAttribute;
    }

    public void setBooleanAttribute(boolean booleanAttribute) {
        this.booleanAttribute = booleanAttribute;
    }

    public int getIntegerAttribute() {
        return integerAttribute;
    }

    public void setIntegerAttribute(int integerAttribute) {
        this.integerAttribute = integerAttribute;
    }

    public long getLongAttribute() {
        return longAttribute;
    }

    public void setLongAttribute(long longAttribute) {
        this.longAttribute = longAttribute;
    }

    public short getShortAttribute() {
        return shortAttribute;
    }

    public void setShortAttribute(short shortAttribute) {
        this.shortAttribute = shortAttribute;
    }

    public byte getByteAttribute() {
        return byteAttribute;
    }

    public void setByteAttribute(byte byteAttribute) {
        this.byteAttribute = byteAttribute;
    }

    public double getDoubleAttribute() {
        return doubleAttribute;
    }

    public void setDoubleAttribute(double doubleAttribute) {
        this.doubleAttribute = doubleAttribute;
    }

    public float getFloatAttribute() {
        return floatAttribute;
    }

    public void setFloatAttribute(float floatAttribute) {
        this.floatAttribute = floatAttribute;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrimitiveTypesBean that = (PrimitiveTypesBean) o;
        return booleanAttribute == that.booleanAttribute &&
            integerAttribute == that.integerAttribute &&
            longAttribute == that.longAttribute &&
            shortAttribute == that.shortAttribute &&
            byteAttribute == that.byteAttribute &&
            Double.compare(that.doubleAttribute, doubleAttribute) == 0 &&
            Float.compare(that.floatAttribute, floatAttribute) == 0 &&
            Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, booleanAttribute, integerAttribute, longAttribute, shortAttribute, byteAttribute, doubleAttribute, floatAttribute);
    }
}

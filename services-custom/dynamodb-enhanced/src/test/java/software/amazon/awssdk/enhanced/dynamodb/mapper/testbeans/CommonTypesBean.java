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
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class CommonTypesBean {
    private String id;
    private Boolean booleanAttribute;
    private Integer integerAttribute;
    private Long longAttribute;
    private Short shortAttribute;
    private Byte byteAttribute;
    private Double doubleAttribute;
    private Float floatAttribute;
    private SdkBytes binaryAttribute;

    @DynamoDbPartitionKey
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getBooleanAttribute() {
        return booleanAttribute;
    }

    public void setBooleanAttribute(Boolean booleanAttribute) {
        this.booleanAttribute = booleanAttribute;
    }

    public Integer getIntegerAttribute() {
        return integerAttribute;
    }

    public void setIntegerAttribute(Integer integerAttribute) {
        this.integerAttribute = integerAttribute;
    }

    public Long getLongAttribute() {
        return longAttribute;
    }

    public void setLongAttribute(Long longAttribute) {
        this.longAttribute = longAttribute;
    }

    public Short getShortAttribute() {
        return shortAttribute;
    }

    public void setShortAttribute(Short shortAttribute) {
        this.shortAttribute = shortAttribute;
    }

    public Byte getByteAttribute() {
        return byteAttribute;
    }

    public void setByteAttribute(Byte byteAttribute) {
        this.byteAttribute = byteAttribute;
    }

    public Double getDoubleAttribute() {
        return doubleAttribute;
    }

    public void setDoubleAttribute(Double doubleAttribute) {
        this.doubleAttribute = doubleAttribute;
    }

    public Float getFloatAttribute() {
        return floatAttribute;
    }

    public void setFloatAttribute(Float floatAttribute) {
        this.floatAttribute = floatAttribute;
    }

    public SdkBytes getBinaryAttribute() {
        return binaryAttribute;
    }

    public void setBinaryAttribute(SdkBytes binaryAttribute) {
        this.binaryAttribute = binaryAttribute;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommonTypesBean that = (CommonTypesBean) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(booleanAttribute, that.booleanAttribute) &&
            Objects.equals(integerAttribute, that.integerAttribute) &&
            Objects.equals(longAttribute, that.longAttribute) &&
            Objects.equals(shortAttribute, that.shortAttribute) &&
            Objects.equals(byteAttribute, that.byteAttribute) &&
            Objects.equals(doubleAttribute, that.doubleAttribute) &&
            Objects.equals(floatAttribute, that.floatAttribute) &&
            Objects.equals(binaryAttribute, that.binaryAttribute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, booleanAttribute, integerAttribute, longAttribute, shortAttribute, byteAttribute, doubleAttribute, floatAttribute, binaryAttribute);
    }
}

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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests.models;

import java.time.LocalDate;
import java.util.Objects;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
public class CustomType {
    private String stringAttribute;
    private Boolean booleanAttribute;
    private Integer integerAttribute;
    private Double doubleAttribute;
    private LocalDate localDateAttribute;

    public String getStringAttribute() {
        return stringAttribute;
    }

    public CustomType setStringAttribute(String stringAttribute) {
        this.stringAttribute = stringAttribute;
        return this;
    }

    public LocalDate getLocalDateAttribute() {
        return localDateAttribute;
    }

    public CustomType setLocalDateAttribute(LocalDate localDateAttribute) {
        this.localDateAttribute = localDateAttribute;
        return this;
    }

    public Double getDoubleAttribute() {
        return doubleAttribute;
    }

    public CustomType setDoubleAttribute(Double doubleAttribute) {
        this.doubleAttribute = doubleAttribute;
        return this;
    }

    public Integer getIntegerAttribute() {
        return integerAttribute;
    }

    public CustomType setIntegerAttribute(Integer integerAttribute) {
        this.integerAttribute = integerAttribute;
        return this;
    }

    public Boolean getBooleanAttribute() {
        return booleanAttribute;
    }

    public CustomType setBooleanAttribute(Boolean booleanAttribute) {
        this.booleanAttribute = booleanAttribute;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CustomType that = (CustomType) o;
        return Objects.equals(stringAttribute, that.stringAttribute) && Objects.equals(booleanAttribute, that.booleanAttribute) && Objects.equals(integerAttribute, that.integerAttribute) && Objects.equals(doubleAttribute, that.doubleAttribute) && Objects.equals(localDateAttribute, that.localDateAttribute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stringAttribute, booleanAttribute, integerAttribute, doubleAttribute, localDateAttribute);
    }
}

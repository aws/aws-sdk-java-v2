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
import java.util.Set;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class SetBean {
    private String id;
    private Set<String> stringSet;
    private Set<Integer> integerSet;
    private Set<Long> longSet;
    private Set<Short> shortSet;
    private Set<Byte> byteSet;
    private Set<Double> doubleSet;
    private Set<Float> floatSet;
    private Set<SdkBytes> binarySet;

    @DynamoDbPartitionKey
    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public Set<String> getStringSet() {
        return stringSet;
    }
    public void setStringSet(Set<String> stringSet) {
        this.stringSet = stringSet;
    }

    public Set<Integer> getIntegerSet() {
        return integerSet;
    }

    public void setIntegerSet(Set<Integer> integerSet) {
        this.integerSet = integerSet;
    }

    public Set<Long> getLongSet() {
        return longSet;
    }

    public void setLongSet(Set<Long> longSet) {
        this.longSet = longSet;
    }

    public Set<Short> getShortSet() {
        return shortSet;
    }

    public void setShortSet(Set<Short> shortSet) {
        this.shortSet = shortSet;
    }

    public Set<Byte> getByteSet() {
        return byteSet;
    }

    public void setByteSet(Set<Byte> byteSet) {
        this.byteSet = byteSet;
    }

    public Set<Double> getDoubleSet() {
        return doubleSet;
    }

    public void setDoubleSet(Set<Double> doubleSet) {
        this.doubleSet = doubleSet;
    }

    public Set<Float> getFloatSet() {
        return floatSet;
    }

    public void setFloatSet(Set<Float> floatSet) {
        this.floatSet = floatSet;
    }

    public Set<SdkBytes> getBinarySet() {
        return binarySet;
    }

    public void setBinarySet(Set<SdkBytes> binarySet) {
        this.binarySet = binarySet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SetBean setBean = (SetBean) o;
        return Objects.equals(id, setBean.id) &&
            Objects.equals(stringSet, setBean.stringSet) &&
            Objects.equals(integerSet, setBean.integerSet) &&
            Objects.equals(longSet, setBean.longSet) &&
            Objects.equals(shortSet, setBean.shortSet) &&
            Objects.equals(byteSet, setBean.byteSet) &&
            Objects.equals(doubleSet, setBean.doubleSet) &&
            Objects.equals(floatSet, setBean.floatSet) &&
            Objects.equals(binarySet, setBean.binarySet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, stringSet, integerSet, longSet, shortSet, byteSet, doubleSet, floatSet, binarySet);
    }
}

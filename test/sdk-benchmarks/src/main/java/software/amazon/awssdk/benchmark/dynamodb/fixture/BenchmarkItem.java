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

package software.amazon.awssdk.benchmark.dynamodb.fixture;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/**
 * Representative typed item for DynamoDB performance benchmarks.
 * Values are populated deterministically by {@link DynamoDbBenchmarkFixture}.
 */
@DynamoDbBean
public class BenchmarkItem {

    private String pk;
    private String stringAttr;
    private Integer numberAttr;
    private Boolean boolAttr;
    private List<String> stringList;
    private Map<String, String> stringMap;
    private NestedAttrs nested;

    @DynamoDbPartitionKey
    public String getPk() {
        return pk;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    public String getStringAttr() {
        return stringAttr;
    }

    public void setStringAttr(String stringAttr) {
        this.stringAttr = stringAttr;
    }

    public Integer getNumberAttr() {
        return numberAttr;
    }

    public void setNumberAttr(Integer numberAttr) {
        this.numberAttr = numberAttr;
    }

    public Boolean getBoolAttr() {
        return boolAttr;
    }

    public void setBoolAttr(Boolean boolAttr) {
        this.boolAttr = boolAttr;
    }

    public List<String> getStringList() {
        return stringList;
    }

    public void setStringList(List<String> stringList) {
        this.stringList = stringList;
    }

    public Map<String, String> getStringMap() {
        return stringMap;
    }

    public void setStringMap(Map<String, String> stringMap) {
        this.stringMap = stringMap;
    }

    public NestedAttrs getNested() {
        return nested;
    }

    public void setNested(NestedAttrs nested) {
        this.nested = nested;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BenchmarkItem)) {
            return false;
        }
        BenchmarkItem that = (BenchmarkItem) o;
        return Objects.equals(pk, that.pk)
               && Objects.equals(stringAttr, that.stringAttr)
               && Objects.equals(numberAttr, that.numberAttr)
               && Objects.equals(boolAttr, that.boolAttr)
               && Objects.equals(stringList, that.stringList)
               && Objects.equals(stringMap, that.stringMap)
               && Objects.equals(nested, that.nested);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(pk);
        result = 31 * result + Objects.hashCode(stringAttr);
        result = 31 * result + Objects.hashCode(numberAttr);
        result = 31 * result + Objects.hashCode(boolAttr);
        result = 31 * result + Objects.hashCode(stringList);
        result = 31 * result + Objects.hashCode(stringMap);
        result = 31 * result + Objects.hashCode(nested);
        return result;
    }

    /**
     * Nested document attributes for representative object complexity.
     */
    @DynamoDbBean
    public static class NestedAttrs {
        private String nestedString;
        private Integer nestedNumber;

        public String getNestedString() {
            return nestedString;
        }

        public void setNestedString(String nestedString) {
            this.nestedString = nestedString;
        }

        public Integer getNestedNumber() {
            return nestedNumber;
        }

        public void setNestedNumber(Integer nestedNumber) {
            this.nestedNumber = nestedNumber;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof NestedAttrs)) {
                return false;
            }
            NestedAttrs that = (NestedAttrs) o;
            return Objects.equals(nestedString, that.nestedString)
                   && Objects.equals(nestedNumber, that.nestedNumber);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(nestedString);
            result = 31 * result + Objects.hashCode(nestedNumber);
            return result;
        }
    }
}

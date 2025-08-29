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

import java.util.Objects;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSubtypeDiscriminator;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSupertype;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSupertype.Subtype;

@DynamoDbBean
@DynamoDbSupertype( {
    @Subtype(discriminatorValue = "no_version", subtypeClass = PolymorphicItemWithVersionSubtype.SubtypeWithoutVersion.class),
    @Subtype(discriminatorValue = "with_version", subtypeClass = PolymorphicItemWithVersionSubtype.SubtypeWithVersion.class)})
public abstract class PolymorphicItemWithVersionSubtype {
    private String id;
    private String type;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbSubtypeDiscriminator
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @DynamoDbBean
    public static class SubtypeWithoutVersion extends PolymorphicItemWithVersionSubtype {
        private String attributeOne;

        public String getAttributeOne() {
            return attributeOne;
        }

        public void setAttributeOne(String attributeOne) {
            this.attributeOne = attributeOne;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            SubtypeWithoutVersion that = (SubtypeWithoutVersion) o;
            return Objects.equals(attributeOne, that.attributeOne);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), attributeOne);
        }
    }

    @DynamoDbBean
    public static class SubtypeWithVersion extends PolymorphicItemWithVersionSubtype {
        private String attributeTwo;
        private Integer version;

        public String getAttributeTwo() {
            return attributeTwo;
        }

        public void setAttributeTwo(String attributeTwo) {
            this.attributeTwo = attributeTwo;
        }

        @DynamoDbVersionAttribute
        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }

            SubtypeWithVersion that = (SubtypeWithVersion) o;

            if (attributeTwo != null ? !attributeTwo.equals(that.attributeTwo) : that.attributeTwo != null) {
                return false;
            }
            return version != null ? version.equals(that.version) : that.version == null;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (attributeTwo != null ? attributeTwo.hashCode() : 0);
            result = 31 * result + (version != null ? version.hashCode() : 0);
            return result;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PolymorphicItemWithVersionSubtype that = (PolymorphicItemWithVersionSubtype) o;
        return Objects.equals(id, that.id) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }
}


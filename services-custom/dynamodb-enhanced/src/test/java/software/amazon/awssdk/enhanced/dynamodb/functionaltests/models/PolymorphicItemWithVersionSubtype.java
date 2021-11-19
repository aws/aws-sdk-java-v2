package software.amazon.awssdk.enhanced.dynamodb.functionaltests.models;

import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSubtypeName;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSubtypes;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSubtypes.Subtype;

@DynamoDbBean
@DynamoDbSubtypes({
        @Subtype(name = "no_version", subtypeClass = PolymorphicItemWithVersionSubtype.SubtypeWithoutVersion.class),
        @Subtype(name = "with_version", subtypeClass = PolymorphicItemWithVersionSubtype.SubtypeWithVersion.class)})
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

    @DynamoDbSubtypeName
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PolymorphicItemWithVersionSubtype that = (PolymorphicItemWithVersionSubtype) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        return type != null ? type.equals(that.type) : that.type == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
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
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            SubtypeWithoutVersion that = (SubtypeWithoutVersion) o;

            return attributeOne != null ? attributeOne.equals(that.attributeOne) : that.attributeOne == null;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (attributeOne != null ? attributeOne.hashCode() : 0);
            return result;
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
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            SubtypeWithVersion that = (SubtypeWithVersion) o;

            if (attributeTwo != null ? !attributeTwo.equals(that.attributeTwo) : that.attributeTwo != null)
                return false;
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
}

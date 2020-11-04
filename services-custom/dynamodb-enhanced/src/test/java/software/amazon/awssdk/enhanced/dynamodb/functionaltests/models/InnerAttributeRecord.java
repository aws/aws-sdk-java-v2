package software.amazon.awssdk.enhanced.dynamodb.functionaltests.models;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;


public class InnerAttributeRecord {
    private String attribOne;
    private Integer attribTwo;

    @DynamoDbPartitionKey
    public String getAttribOne() {
        return attribOne;
    }

    public void setAttribOne(String attribOne) {
        this.attribOne = attribOne;
    }

    public Integer getAttribTwo() {
        return attribTwo;
    }

    public void setAttribTwo(Integer attribTwo) {
        this.attribTwo = attribTwo;
    }

    @Override
    public String toString() {
        return "InnerAttributeRecord{" +
                "attribOne='" + attribOne + '\'' +
                ", attribTwo=" + attribTwo +
                '}';
    }
}

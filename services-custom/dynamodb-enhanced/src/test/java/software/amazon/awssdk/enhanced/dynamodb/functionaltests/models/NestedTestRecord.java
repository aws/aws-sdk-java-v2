package software.amazon.awssdk.enhanced.dynamodb.functionaltests.models;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;


@DynamoDbBean
public class NestedTestRecord {
    private String outerAttribOne;
    private Integer sort;
    private InnerAttributeRecord innerAttributeRecord;

    @DynamoDbPartitionKey
    public String getOuterAttribOne() {
        return outerAttribOne;
    }

    public void setOuterAttribOne(String outerAttribOne) {
        this.outerAttribOne = outerAttribOne;
    }

    @DynamoDbSortKey
    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    @DynamoDbConvertedBy(InnerAttribConverter.class)
    public InnerAttributeRecord getInnerAttributeRecord() {
        return innerAttributeRecord;
    }

    public void setInnerAttributeRecord(InnerAttributeRecord innerAttributeRecord) {
        this.innerAttributeRecord = innerAttributeRecord;
    }


    @Override
    public String toString() {
        return "NestedTestRecord{" +
                "outerAttribOne='" + outerAttribOne + '\'' +
                ", outerAttribTwo='" + sort + '\'' +
                ", innerAttributeRecord=" + innerAttributeRecord +
                '}';
    }
}
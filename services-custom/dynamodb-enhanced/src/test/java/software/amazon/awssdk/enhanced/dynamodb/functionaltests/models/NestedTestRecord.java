package software.amazon.awssdk.enhanced.dynamodb.functionaltests.models;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;


@DynamoDbBean
public class NestedTestRecord {
    private String outerAttribOne;
    private Integer sort;
    private InnerAttributeRecord innerAttributeRecord;

    private String dotVariable;


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

    @DynamoDbAttribute("test.com")
    public String getDotVariable() {
        return dotVariable;
    }

    public void setDotVariable(String dotVariable) {
        this.dotVariable = dotVariable;
    }

    @Override
    public String toString() {
        return "NestedTestRecord{" +
                "outerAttribOne='" + outerAttribOne + '\'' +
                ", sort=" + sort +
                ", innerAttributeRecord=" + innerAttributeRecord +
                ", dotVariable='" + dotVariable + '\'' +
                '}';
    }
}
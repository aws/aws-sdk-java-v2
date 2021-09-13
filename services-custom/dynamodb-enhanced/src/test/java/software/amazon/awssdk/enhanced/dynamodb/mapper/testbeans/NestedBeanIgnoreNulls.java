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

import java.util.List;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnoreNulls;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class NestedBeanIgnoreNulls {
    private String id;
    private AbstractBean innerBean1;
    private AbstractBean innerBean2;
    private List<AbstractBean> innerBeanList1;
    private List<AbstractBean> innerBeanList2;

    @DynamoDbPartitionKey
    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbIgnoreNulls
    public AbstractBean getInnerBean1() {
        return innerBean1;
    }
    public void setInnerBean1(AbstractBean innerBean) {
        this.innerBean1 = innerBean;
    }

    public AbstractBean getInnerBean2() {
        return innerBean2;
    }
    public void setInnerBean2(AbstractBean innerBean) {
        this.innerBean2 = innerBean;
    }

    @DynamoDbIgnoreNulls
    public List<AbstractBean> getInnerBeanList1() {
        return innerBeanList1;
    }
    public void setInnerBeanList1(List<AbstractBean> innerBeanList1) {
        this.innerBeanList1 = innerBeanList1;
    }

    public List<AbstractBean> getInnerBeanList2() {
        return innerBeanList2;
    }
    public void setInnerBeanList2(List<AbstractBean> innerBeanList2) {
        this.innerBeanList2 = innerBeanList2;
    }
}

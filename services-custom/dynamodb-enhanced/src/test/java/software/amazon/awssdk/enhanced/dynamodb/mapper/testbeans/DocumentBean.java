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
import java.util.Map;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class DocumentBean {
    private String id;
    private String attribute1;
    private AbstractBean abstractBean;
    private AbstractImmutable abstractImmutable;
    private List<AbstractBean> abstractBeanList;
    private List<AbstractImmutable> abstractImmutableList;
    private Map<String, AbstractBean> abstractBeanMap;
    private Map<String, AbstractImmutable> abstractImmutableMap;

    @DynamoDbPartitionKey
    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getAttribute1() {
        return attribute1;
    }
    public void setAttribute1(String attribute1) {
        this.attribute1 = attribute1;
    }

    public AbstractBean getAbstractBean() {
        return abstractBean;
    }
    public void setAbstractBean(AbstractBean abstractBean) {
        this.abstractBean = abstractBean;
    }

    public List<AbstractBean> getAbstractBeanList() {
        return abstractBeanList;
    }
    public void setAbstractBeanList(List<AbstractBean> abstractBeanList) {
        this.abstractBeanList = abstractBeanList;
    }

    public Map<String, AbstractBean> getAbstractBeanMap() {
        return abstractBeanMap;
    }
    public void setAbstractBeanMap(Map<String, AbstractBean> abstractBeanMap) {
        this.abstractBeanMap = abstractBeanMap;
    }

    public AbstractImmutable getAbstractImmutable() {
        return abstractImmutable;
    }
    public void setAbstractImmutable(AbstractImmutable abstractImmutable) {
        this.abstractImmutable = abstractImmutable;
    }

    public List<AbstractImmutable> getAbstractImmutableList() {
        return abstractImmutableList;
    }
    public void setAbstractImmutableList(List<AbstractImmutable> abstractImmutableList) {
        this.abstractImmutableList = abstractImmutableList;
    }

    public Map<String, AbstractImmutable> getAbstractImmutableMap() {
        return abstractImmutableMap;
    }
    public void setAbstractImmutableMap(Map<String, AbstractImmutable> abstractImmutableMap) {
        this.abstractImmutableMap = abstractImmutableMap;
    }
}

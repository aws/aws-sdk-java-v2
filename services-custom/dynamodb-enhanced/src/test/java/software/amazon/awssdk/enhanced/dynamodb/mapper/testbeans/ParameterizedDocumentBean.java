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
public class ParameterizedDocumentBean {
    private String id;
    private String attribute1;
    private ParameterizedAbstractBean<String> abstractBean;
    private List<ParameterizedAbstractBean<String>> abstractBeanList;
    private Map<String, ParameterizedAbstractBean<String>> abstractBeanMap;

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

    public ParameterizedAbstractBean<String> getAbstractBean() {
        return abstractBean;
    }
    public void setAbstractBean(ParameterizedAbstractBean<String> abstractBean) {
        this.abstractBean = abstractBean;
    }

    public List<ParameterizedAbstractBean<String>> getAbstractBeanList() {
        return abstractBeanList;
    }

    public void setAbstractBeanList(List<ParameterizedAbstractBean<String>> abstractBeanList) {
        this.abstractBeanList = abstractBeanList;
    }

    public Map<String, ParameterizedAbstractBean<String>> getAbstractBeanMap() {
        return abstractBeanMap;
    }

    public void setAbstractBeanMap(Map<String, ParameterizedAbstractBean<String>> abstractBeanMap) {
        this.abstractBeanMap = abstractBeanMap;
    }
}

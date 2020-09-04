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

import java.util.List;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
public final class RecursiveRecordBean {
    private int attribute;
    private RecursiveRecordBean recursiveRecordBean;
    private RecursiveRecordImmutable recursiveRecordImmutable;
    private List<RecursiveRecordBean> recursiveRecordBeanList;

    public int getAttribute() {
        return attribute;
    }

    public void setAttribute(int attribute) {
        this.attribute = attribute;
    }

    public RecursiveRecordBean getRecursiveRecordBean() {
        return recursiveRecordBean;
    }

    public void setRecursiveRecordBean(RecursiveRecordBean recursiveRecordBean) {
        this.recursiveRecordBean = recursiveRecordBean;
    }

    public RecursiveRecordImmutable getRecursiveRecordImmutable() {
        return recursiveRecordImmutable;
    }

    public void setRecursiveRecordImmutable(RecursiveRecordImmutable recursiveRecordImmutable) {
        this.recursiveRecordImmutable = recursiveRecordImmutable;
    }

    public List<RecursiveRecordBean> getRecursiveRecordList() {
        return recursiveRecordBeanList;
    }

    public void setRecursiveRecordList(List<RecursiveRecordBean> recursiveRecordBeanList) {
        this.recursiveRecordBeanList = recursiveRecordBeanList;
    }
}

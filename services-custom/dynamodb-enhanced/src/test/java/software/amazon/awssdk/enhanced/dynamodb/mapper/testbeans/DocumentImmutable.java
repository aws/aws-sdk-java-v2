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
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbImmutable(builder = DocumentImmutable.Builder.class)
public class DocumentImmutable {
    private final String id;
    private final String attribute1;
    private final AbstractBean abstractBean;
    private final AbstractImmutable abstractImmutable;
    private final List<AbstractBean> abstractBeanList;
    private final List<AbstractImmutable> abstractImmutableList;
    private final Map<String, AbstractBean> abstractBeanMap;
    private final Map<String, AbstractImmutable> abstractImmutableMap;

    private DocumentImmutable(Builder b) {
        this.id = b.id;
        this.attribute1 = b.attribute1;
        this.abstractBean = b.abstractBean;
        this.abstractImmutable = b.abstractImmutable;
        this.abstractBeanList = b.abstractBeanList;
        this.abstractImmutableList = b.abstractImmutableList;
        this.abstractBeanMap = b.abstractBeanMap;
        this.abstractImmutableMap = b.abstractImmutableMap;
    }

    @DynamoDbPartitionKey
    public String id() {
        return this.id;
    }

    public String attribute1() {
        return attribute1;
    }

    public AbstractBean abstractBean() {
        return abstractBean;
    }

    public List<AbstractBean> abstractBeanList() {
        return abstractBeanList;
    }

    public Map<String, AbstractBean> abstractBeanMap() {
        return abstractBeanMap;
    }

    public AbstractImmutable abstractImmutable() {
        return abstractImmutable;
    }

    public List<AbstractImmutable> abstractImmutableList() {
        return abstractImmutableList;
    }

    public Map<String, AbstractImmutable> abstractImmutableMap() {
        return abstractImmutableMap;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private String attribute1;
        private AbstractBean abstractBean;
        private AbstractImmutable abstractImmutable;
        private List<AbstractBean> abstractBeanList;
        private List<AbstractImmutable> abstractImmutableList;
        private Map<String, AbstractBean> abstractBeanMap;
        private Map<String, AbstractImmutable> abstractImmutableMap;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder attribute1(String attribute1) {
            this.attribute1 = attribute1;
            return this;
        }

        public Builder abstractBean(AbstractBean abstractBean) {
            this.abstractBean = abstractBean;
            return this;
        }

        public Builder abstractImmutable(AbstractImmutable abstractImmutable) {
            this.abstractImmutable = abstractImmutable;
            return this;
        }

        public Builder abstractBeanList(List<AbstractBean> abstractBeanList) {
            this.abstractBeanList = abstractBeanList;
            return this;
        }

        public Builder abstractImmutableList(List<AbstractImmutable> abstractImmutableList) {
            this.abstractImmutableList = abstractImmutableList;
            return this;
        }

        public Builder abstractBeanMap(Map<String, AbstractBean> abstractBeanMap) {
            this.abstractBeanMap = abstractBeanMap;
            return this;
        }

        public Builder abstractImmutableMap(Map<String, AbstractImmutable> abstractImmutableMap) {
            this.abstractImmutableMap = abstractImmutableMap;
            return this;
        }

        public DocumentImmutable build() {
            return new DocumentImmutable(this);
        }
    }
}

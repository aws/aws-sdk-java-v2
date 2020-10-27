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

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbFlatten;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbImmutable(builder = FlattenedBeanImmutable.Builder.class)
public class FlattenedBeanImmutable {
    private final String id;
    private final String attribute1;
    private final AbstractBean abstractBean;

    private FlattenedBeanImmutable(Builder b) {
        this.id = b.id;
        this.attribute1 = b.attribute1;
        this.abstractBean = b.abstractBean;
    }

    @DynamoDbPartitionKey
    public String getId() {
        return this.id;
    }

    public String getAttribute1() {
        return attribute1;
    }

    @DynamoDbFlatten
    public AbstractBean getAbstractBean() {
        return abstractBean;
    }

    public static final class Builder {
        private String id;
        private String attribute1;
        private AbstractBean abstractBean;

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setAttribute1(String attribute1) {
            this.attribute1 = attribute1;
            return this;
        }

        public Builder setAbstractBean(AbstractBean abstractBean) {
            this.abstractBean = abstractBean;
            return this;
        }

        public FlattenedBeanImmutable build() {
            return new FlattenedBeanImmutable(this);
        }
    }
}

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

import static software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior.WRITE_IF_NOT_EXISTS;

import java.time.Instant;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbUpdateBehavior;

@DynamoDbBean
public class InvalidNestedAttributeBean {

    private String id;
    private InvalidNestedAttributeChild nestedChildAttribute;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public InvalidNestedAttributeBean setId(String id) {
        this.id = id;
        return this;
    }

    public InvalidNestedAttributeChild getNestedChildAttribute() {
        return nestedChildAttribute;
    }

    public InvalidNestedAttributeBean setNestedChildAttribute(
        InvalidNestedAttributeChild nestedChildAttribute) {
        this.nestedChildAttribute = nestedChildAttribute;
        return this;
    }


    @DynamoDbBean
    public static class InvalidNestedAttributeChild {

        private String id;
        private InvalidNestedAttributeChild nestedChildAttribute;
        private Instant childAttr_NESTED_ATTR_UPDATE_;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public InvalidNestedAttributeChild setId(String id) {
            this.id = id;
            return this;
        }

        public InvalidNestedAttributeChild getNestedChildAttribute() {
            return nestedChildAttribute;
        }

            public InvalidNestedAttributeChild setNestedChildAttribute(
            InvalidNestedAttributeChild nestedChildAttribute) {
            this.nestedChildAttribute = nestedChildAttribute;
            return this;
        }

        @DynamoDbUpdateBehavior(WRITE_IF_NOT_EXISTS)
        public Instant getAttr_NESTED_ATTR_UPDATE_() {
            return childAttr_NESTED_ATTR_UPDATE_;
        }

        public InvalidNestedAttributeChild setAttr_NESTED_ATTR_UPDATE_(Instant attr_NESTED_ATTR_UPDATE_) {
            this.childAttr_NESTED_ATTR_UPDATE_ = attr_NESTED_ATTR_UPDATE_;
            return this;
        }
    }
}
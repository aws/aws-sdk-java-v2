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

import java.time.Instant;

public class RecordWithInvalidAttributeNameOnNestedLevel {

    private String id;
    private RecordWithReservedMarkerNestedChildAttribute nestedChildAttribute;

    public String getId() {
        return id;
    }

    public RecordWithInvalidAttributeNameOnNestedLevel setId(String id) {
        this.id = id;
        return this;
    }

    public RecordWithReservedMarkerNestedChildAttribute getNestedChildAttribute() {
        return nestedChildAttribute;
    }

    public RecordWithInvalidAttributeNameOnNestedLevel setNestedChildAttribute(
        RecordWithReservedMarkerNestedChildAttribute nestedChildAttribute) {
        this.nestedChildAttribute = nestedChildAttribute;
        return this;
    }


    public static class RecordWithReservedMarkerNestedChildAttribute {

        private String id;
        private RecordWithReservedMarkerNestedChildAttribute nestedChildAttribute;
        private Instant childAttr_NESTED_ATTR_UPDATE_;

        public String getId() {
            return id;
        }

        public RecordWithReservedMarkerNestedChildAttribute setId(String id) {
            this.id = id;
            return this;
        }

        public RecordWithReservedMarkerNestedChildAttribute getNestedChildAttribute() {
            return nestedChildAttribute;
        }

        public RecordWithReservedMarkerNestedChildAttribute setNestedChildAttribute(
            RecordWithReservedMarkerNestedChildAttribute nestedChildAttribute) {
            this.nestedChildAttribute = nestedChildAttribute;
            return this;
        }

        public Instant getAttr_NESTED_ATTR_UPDATE_() {
            return childAttr_NESTED_ATTR_UPDATE_;
        }

        public RecordWithReservedMarkerNestedChildAttribute setAttr_NESTED_ATTR_UPDATE_(Instant attr_NESTED_ATTR_UPDATE_) {
            this.childAttr_NESTED_ATTR_UPDATE_ = attr_NESTED_ATTR_UPDATE_;
            return this;
        }
    }
}

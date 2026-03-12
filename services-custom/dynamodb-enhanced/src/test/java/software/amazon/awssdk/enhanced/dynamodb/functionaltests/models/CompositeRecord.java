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

import java.util.Objects;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
public class CompositeRecord {
    private NestedRecordWithUpdateBehavior nestedRecord;

    public void setNestedRecord(NestedRecordWithUpdateBehavior nestedRecord) {
        this.nestedRecord = nestedRecord;
    }

    public NestedRecordWithUpdateBehavior getNestedRecord() {
        return nestedRecord;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CompositeRecord that = (CompositeRecord) o;
        return Objects.equals(that, this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nestedRecord);
    }
}

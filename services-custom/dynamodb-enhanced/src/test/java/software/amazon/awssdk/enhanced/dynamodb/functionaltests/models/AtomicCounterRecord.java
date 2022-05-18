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

import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbAtomicCounter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class AtomicCounterRecord {
    private String id;
    private Long defaultCounter;
    private Long customCounter;
    private Long decreasingCounter;
    private String attribute1;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbAtomicCounter
    public Long getDefaultCounter() {
        return defaultCounter;
    }
    public void setDefaultCounter(Long counter) {
        this.defaultCounter = counter;
    }

    @DynamoDbAtomicCounter(delta = 5, startValue = 10)
    public Long getCustomCounter() {
        return customCounter;
    }
    public void setCustomCounter(Long counter) {
        this.customCounter = counter;
    }

    @DynamoDbAtomicCounter(delta = -1, startValue = -20)
    public Long getDecreasingCounter() {
        return decreasingCounter;
    }
    public void setDecreasingCounter(Long counter) {
        this.decreasingCounter = counter;
    }

    public String getAttribute1() {
        return attribute1;
    }
    public void setAttribute1(String attribute1) {
        this.attribute1 = attribute1;
    }
}

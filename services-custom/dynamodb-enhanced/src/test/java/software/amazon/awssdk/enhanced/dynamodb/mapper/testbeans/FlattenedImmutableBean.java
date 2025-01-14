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

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbFlatten;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class FlattenedImmutableBean {
    private String id;
    private String attribute1;
    private AbstractImmutable abstractImmutable;
    private AbstractImmutable explicitPrefixImmutable;
    private AbstractImmutable autoPrefixImmutable;
    private AbstractImmutable customPrefixImmutable;

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

    @DynamoDbFlatten
    public AbstractImmutable getAbstractImmutable() {
        return abstractImmutable;
    }
    public void setAbstractImmutable(AbstractImmutable abstractImmutable) {
        this.abstractImmutable = abstractImmutable;
    }

    @DynamoDbFlatten(prefix = "prefix-")
    public AbstractImmutable getExplicitPrefixImmutable() {
        return explicitPrefixImmutable;
    }
    public void setExplicitPrefixImmutable(AbstractImmutable explicitPrefixImmutable) {
        this.explicitPrefixImmutable = explicitPrefixImmutable;
    }

    @DynamoDbFlatten(prefix = DynamoDbFlatten.AUTO_PREFIX)
    public AbstractImmutable getAutoPrefixImmutable() {
        return autoPrefixImmutable;
    }
    public void setAutoPrefixImmutable(AbstractImmutable autoPrefixImmutable) {
        this.autoPrefixImmutable = autoPrefixImmutable;
    }

    @DynamoDbAttribute("custom")
    @DynamoDbFlatten(prefix = DynamoDbFlatten.AUTO_PREFIX)
    public AbstractImmutable getCustomPrefixImmutable() {
        return customPrefixImmutable;
    }
    public void setCustomPrefixImmutable(AbstractImmutable customPrefixImmutable) {
        this.customPrefixImmutable = customPrefixImmutable;
    }
}

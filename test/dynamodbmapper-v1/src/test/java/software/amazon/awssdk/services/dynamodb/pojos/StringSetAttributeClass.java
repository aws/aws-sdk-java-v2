/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.pojos;

import java.util.Set;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbAttribute;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbHashKey;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbTable;

/**
 * Test domain class with a string set attribute and a string key
 */
@DynamoDbTable(tableName = "aws-java-sdk-util")
public class StringSetAttributeClass {

    private String key;
    private Set<String> stringSetAttribute;
    private Set<String> StringSetAttributeRenamed;

    @DynamoDbHashKey
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @DynamoDbAttribute
    public Set<String> getStringSetAttribute() {
        return stringSetAttribute;
    }

    public void setStringSetAttribute(Set<String> stringSetAttribute) {
        this.stringSetAttribute = stringSetAttribute;
    }

    @DynamoDbAttribute(attributeName = "originalName")
    public Set<String> getStringSetAttributeRenamed() {
        return StringSetAttributeRenamed;
    }

    public void setStringSetAttributeRenamed(Set<String> stringSetAttributeRenamed) {
        StringSetAttributeRenamed = stringSetAttributeRenamed;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((StringSetAttributeRenamed == null) ? 0 : StringSetAttributeRenamed.hashCode());
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((stringSetAttribute == null) ? 0 : stringSetAttribute.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        StringSetAttributeClass other = (StringSetAttributeClass) obj;
        if (StringSetAttributeRenamed == null) {
            if (other.StringSetAttributeRenamed != null) {
                return false;
            }
        } else if (!StringSetAttributeRenamed.equals(other.StringSetAttributeRenamed)) {
            return false;
        }
        if (key == null) {
            if (other.key != null) {
                return false;
            }
        } else if (!key.equals(other.key)) {
            return false;
        }
        if (stringSetAttribute == null) {
            if (other.stringSetAttribute != null) {
                return false;
            }
        } else if (!stringSetAttribute.equals(other.stringSetAttribute)) {
            return false;
        }
        return true;
    }


}

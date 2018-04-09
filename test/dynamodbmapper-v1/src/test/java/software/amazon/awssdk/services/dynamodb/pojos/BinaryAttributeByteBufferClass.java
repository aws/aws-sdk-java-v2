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

import java.nio.ByteBuffer;
import java.util.Set;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbAttribute;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbHashKey;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbTable;

/**
 * Test domain class with byteBuffer attribute, byteBuffer set and a string key
 */
@DynamoDbTable(tableName = "aws-java-sdk-util")
public class BinaryAttributeByteBufferClass {

    private String key;
    private ByteBuffer binaryAttribute;
    private Set<ByteBuffer> binarySetAttribute;

    @DynamoDbHashKey(attributeName = "key")
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @DynamoDbAttribute(attributeName = "binaryAttribute")
    public ByteBuffer getBinaryAttribute() {
        return binaryAttribute;
    }

    public void setBinaryAttribute(ByteBuffer binaryAttribute) {
        this.binaryAttribute = binaryAttribute;
    }

    @DynamoDbAttribute(attributeName = "binarySetAttribute")
    public Set<ByteBuffer> getBinarySetAttribute() {
        return binarySetAttribute;
    }

    public void setBinarySetAttribute(Set<ByteBuffer> binarySetAttribute) {
        this.binarySetAttribute = binarySetAttribute;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((binaryAttribute == null) ? 0 : binaryAttribute.hashCode());
        result = prime * result + ((binarySetAttribute == null) ? 0 : binarySetAttribute.hashCode());
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
        BinaryAttributeByteBufferClass other = (BinaryAttributeByteBufferClass) obj;
        if (key == null) {
            if (other.key != null) {
                return false;
            }
        } else if (!key.equals(other.key)) {
            return false;
        }
        if (binaryAttribute == null) {
            if (other.binaryAttribute != null) {
                return false;
            }
        } else if (!binaryAttribute.equals(other.binaryAttribute)) {
            return false;
        }
        if (binarySetAttribute == null) {
            if (other.binarySetAttribute != null) {
                return false;
            }
        } else if (!binarySetAttribute.equals(other.binarySetAttribute)) {
            return false;
        }
        return true;
    }

}

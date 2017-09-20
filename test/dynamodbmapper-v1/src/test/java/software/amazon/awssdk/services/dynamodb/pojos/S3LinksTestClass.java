/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbHashKey;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.datamodeling.S3Link;

/**
 * Test domain class with a single string key, and two S3Links
 */
@DynamoDbTable(tableName = "aws-java-sdk-util")
public class S3LinksTestClass {

    private String key;
    private S3Link s3LinkWest;
    private S3Link s3LinkEast;

    @DynamoDbHashKey
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public S3LinksTestClass withKey(String key) {
        setKey(key);
        return this;
    }

    public S3Link s3LinkWest() {
        return s3LinkWest;
    }

    public void setS3LinkWest(S3Link s3LinkAttribute) {
        this.s3LinkWest = s3LinkAttribute;
    }

    public S3Link s3LinkEast() {
        return s3LinkEast;
    }

    public void setS3LinkEast(S3Link s3LinkEast) {
        this.s3LinkEast = s3LinkEast;
    }
}

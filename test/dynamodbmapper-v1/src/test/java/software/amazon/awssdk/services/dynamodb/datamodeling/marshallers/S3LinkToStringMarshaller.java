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

package software.amazon.awssdk.services.dynamodb.datamodeling.marshallers;

import software.amazon.awssdk.services.dynamodb.datamodeling.ArgumentMarshaller.StringAttributeMarshaller;
import software.amazon.awssdk.services.dynamodb.datamodeling.S3Link;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A marshaller that marshals {@code S3Link} objects to DynamoDB Strings,
 * using a JSON encoding. For example: {"s3":{"region":"us-west-2",
 *  "bucket":"my-bucket-name", "key": "foo/bar/baz.txt"}}.
 */
public class S3LinkToStringMarshaller implements StringAttributeMarshaller {

    private static final S3LinkToStringMarshaller INSTANCE =
            new S3LinkToStringMarshaller();

    private S3LinkToStringMarshaller() {
    }

    public static S3LinkToStringMarshaller instance() {
        return INSTANCE;
    }

    @Override
    public AttributeValue marshall(Object obj) {
        S3Link s3link = (S3Link) obj;

        if (s3link.bucketName() == null || s3link.getKey() == null) {
            // insufficient S3 resource specification
            return null;
        }

        return AttributeValue.builder().s(s3link.toJson()).build();
    }
}

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

package software.amazon.awssdk.services.dynamodb.datamodeling.unmarshallers;

import software.amazon.awssdk.services.dynamodb.datamodeling.S3ClientCache;
import software.amazon.awssdk.services.dynamodb.datamodeling.S3Link;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class S3LinkUnmarshaller extends SUnmarshaller {

    private static final S3LinkUnmarshaller INSTANCE = new S3LinkUnmarshaller();
    private final S3ClientCache clientCache;


    private S3LinkUnmarshaller() {
        this(null);
    }

    public S3LinkUnmarshaller(S3ClientCache clientCache) {
        this.clientCache = clientCache;
    }

    public static S3LinkUnmarshaller instance() {
        return INSTANCE;
    }

    @Override
    public Object unmarshall(AttributeValue value) {
        if (clientCache == null) {
            throw new IllegalStateException(
                    "Mapper must be constructed with S3 AWS Credentials to "
                    + "load S3Link");
        }

        return S3Link.fromJson(clientCache, value.s());
    }
}

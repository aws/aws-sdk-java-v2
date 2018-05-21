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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import java.lang.reflect.Method;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class UnmarshallerTest extends StandardModelFactoriesV2UnconvertTest {

    private static final ItemConverter CONVERTER = CONFIG.getConversionSchema().getConverter(
            new ConversionSchema.Dependencies().with(S3ClientCache.class, new S3ClientCache((AwsCredentialsProvider) null)));

    @Override
    protected <T> Object unconvert(Class<T> clazz, Method getter, Method setter, AttributeValue value) {
        return CONVERTER.unconvert(getter, setter, value);
    }

}

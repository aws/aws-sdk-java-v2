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

package software.amazon.awssdk.mapper.dynamodb.internal.unmarshallers;

import software.amazon.awssdk.annotations.SdkInternalApi;
import java.lang.reflect.Method;

import software.amazon.awssdk.mapper.dynamodb.ArgumentUnmarshaller;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMappingException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkInternalApi
abstract class NSUnmarshaller implements ArgumentUnmarshaller {

    @Override
    public void typeCheck(AttributeValue value, Method setter) {
        if ( !value.hasNs() ) {
            throw new DynamoDBMappingException("Expected NS in value " + value + " when invoking " + setter);
        }
    }

}

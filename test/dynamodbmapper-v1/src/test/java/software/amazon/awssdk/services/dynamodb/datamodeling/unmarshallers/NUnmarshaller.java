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

import java.lang.reflect.Method;
import software.amazon.awssdk.services.dynamodb.datamodeling.ArgumentUnmarshaller;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMappingException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

abstract class NUnmarshaller implements ArgumentUnmarshaller {

    @Override
    public void typeCheck(AttributeValue value, Method setter) {
        if (value.n() == null) {
            throw new DynamoDbMappingException("Expected N in value " + value + " when invoking " + setter);
        }
    }

}

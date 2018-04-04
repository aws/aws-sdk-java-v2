/*
 * Copyright 2013-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.kinesis.transform;

import javax.annotation.Generated;
import software.amazon.awssdk.core.runtime.transform.JsonUnmarshallerContext;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardOutput;

/**
 * SubscribeToShardOutput JSON Unmarshaller
 */
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class SubscribeToShardOutputUnmarshaller implements Unmarshaller<SubscribeToShardOutput, JsonUnmarshallerContext> {

    public SubscribeToShardOutput unmarshall(JsonUnmarshallerContext context) throws Exception {
        return SubscribeToShardOutput.builder().build();
    }

    private static final SubscribeToShardOutputUnmarshaller INSTANCE = new SubscribeToShardOutputUnmarshaller();

    public static SubscribeToShardOutputUnmarshaller getInstance() {
        return INSTANCE;
    }
}

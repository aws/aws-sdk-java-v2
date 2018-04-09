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

import java.math.*;
import java.nio.ByteBuffer;
import javax.annotation.Generated;

import software.amazon.awssdk.services.kinesis.model.*;
import software.amazon.awssdk.core.runtime.transform.SimpleTypeJsonUnmarshallers.*;
import software.amazon.awssdk.core.runtime.transform.*;

import com.fasterxml.jackson.core.JsonToken;
import static com.fasterxml.jackson.core.JsonToken.*;

/**
 * SubscribeToShardResponse JSON Unmarshaller
 */
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class SubscribeToShardResponseUnmarshaller implements Unmarshaller<SubscribeToShardResponse, JsonUnmarshallerContext> {

    public SubscribeToShardResponse unmarshall(JsonUnmarshallerContext context) throws Exception {
        return SubscribeToShardResponse.builder().build();
    }

    private static final SubscribeToShardResponseUnmarshaller INSTANCE = new SubscribeToShardResponseUnmarshaller();

    public static SubscribeToShardResponseUnmarshaller getInstance() {
        return INSTANCE;
    }
}

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

import javax.annotation.Generated;

import software.amazon.awssdk.services.kinesis.model.*;
import software.amazon.awssdk.core.runtime.transform.SimpleTypeJsonUnmarshallers.*;
import software.amazon.awssdk.core.runtime.transform.*;

import com.fasterxml.jackson.core.JsonToken;
import static com.fasterxml.jackson.core.JsonToken.*;

/**
 * RecordBatchEvent JSON Unmarshaller
 */
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class RecordBatchEventUnmarshaller implements Unmarshaller<RecordBatchEvent, JsonUnmarshallerContext> {

    public RecordBatchEvent unmarshall(JsonUnmarshallerContext context) throws Exception {
        RecordBatchEvent.Builder recordBatchEventBuilder = RecordBatchEvent.builder();

        int originalDepth = context.getCurrentDepth();
        String currentParentElement = context.getCurrentParentElement();
        int targetDepth = originalDepth + 1;

        JsonToken token = context.getCurrentToken();
        if (token == null)
            token = context.nextToken();
        if (token == VALUE_NULL) {
            return null;
        }

        while (true) {
            if (token == null)
                break;

            if (token == FIELD_NAME || token == START_OBJECT) {
                if (context.testExpression("Records", targetDepth)) {
                    context.nextToken();
                    recordBatchEventBuilder.records(new ListUnmarshaller<Record>(RecordUnmarshaller.getInstance())
                            .unmarshall(context));
                }
                if (context.testExpression("MillisBehindLatest", targetDepth)) {
                    context.nextToken();
                    recordBatchEventBuilder.millisBehindLatest(context.getUnmarshaller(Long.class).unmarshall(context));
                }
            } else if (token == END_ARRAY || token == END_OBJECT) {
                if (context.getLastParsedParentElement() == null
                        || context.getLastParsedParentElement().equals(currentParentElement)) {
                    if (context.getCurrentDepth() <= originalDepth)
                        break;
                }
            }
            token = context.nextToken();
        }

        return recordBatchEventBuilder.build();
    }

    private static final RecordBatchEventUnmarshaller INSTANCE = new RecordBatchEventUnmarshaller();

    public static RecordBatchEventUnmarshaller getInstance() {
        return INSTANCE;
    }
}

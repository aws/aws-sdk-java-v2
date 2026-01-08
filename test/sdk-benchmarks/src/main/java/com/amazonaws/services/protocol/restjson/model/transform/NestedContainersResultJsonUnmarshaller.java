/*
 * Copyright 2021-2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.services.protocol.restjson.model.transform;

import java.math.*;



import com.amazonaws.services.protocol.restjson.model.*;
import com.amazonaws.transform.SimpleTypeJsonUnmarshallers.*;
import com.amazonaws.transform.*;

import com.fasterxml.jackson.core.JsonToken;
import static com.fasterxml.jackson.core.JsonToken.*;

/**
 * NestedContainersResult JSON Unmarshaller
 */

public class NestedContainersResultJsonUnmarshaller implements Unmarshaller<NestedContainersResult, JsonUnmarshallerContext> {

    public NestedContainersResult unmarshall(JsonUnmarshallerContext context) throws Exception {
        NestedContainersResult nestedContainersResult = new NestedContainersResult();

        int originalDepth = context.getCurrentDepth();
        String currentParentElement = context.getCurrentParentElement();
        int targetDepth = originalDepth + 1;

        JsonToken token = context.getCurrentToken();
        if (token == null)
            token = context.nextToken();
        if (token == VALUE_NULL) {
            return nestedContainersResult;
        }

        while (true) {
            if (token == null)
                break;

            if (token == FIELD_NAME || token == START_OBJECT) {
                if (context.testExpression("ListOfListsOfStrings", targetDepth)) {
                    context.nextToken();
                    nestedContainersResult.setListOfListsOfStrings(new ListUnmarshaller<java.util.List<String>>(new ListUnmarshaller<String>(context
                            .getUnmarshaller(String.class))

                    )

                    .unmarshall(context));
                }
                if (context.testExpression("ListOfListsOfStructs", targetDepth)) {
                    context.nextToken();
                    nestedContainersResult.setListOfListsOfStructs(new ListUnmarshaller<java.util.List<SimpleStruct>>(new ListUnmarshaller<SimpleStruct>(
                            SimpleStructJsonUnmarshaller.getInstance())

                    )

                    .unmarshall(context));
                }
                if (context.testExpression("ListOfListsOfAllTypesStructs", targetDepth)) {
                    context.nextToken();
                    nestedContainersResult.setListOfListsOfAllTypesStructs(new ListUnmarshaller<java.util.List<AllTypesStructure>>(
                            new ListUnmarshaller<AllTypesStructure>(AllTypesStructureJsonUnmarshaller.getInstance())

                    )

                    .unmarshall(context));
                }
                if (context.testExpression("ListOfListOfListsOfStrings", targetDepth)) {
                    context.nextToken();
                    nestedContainersResult.setListOfListOfListsOfStrings(new ListUnmarshaller<java.util.List<java.util.List<String>>>(
                            new ListUnmarshaller<java.util.List<String>>(new ListUnmarshaller<String>(context.getUnmarshaller(String.class))

                            )

                    )

                    .unmarshall(context));
                }
                if (context.testExpression("MapOfStringToListOfListsOfStrings", targetDepth)) {
                    context.nextToken();
                    nestedContainersResult.setMapOfStringToListOfListsOfStrings(new MapUnmarshaller<String, java.util.List<java.util.List<String>>>(context
                            .getUnmarshaller(String.class), new ListUnmarshaller<java.util.List<String>>(new ListUnmarshaller<String>(context
                            .getUnmarshaller(String.class))

                    )

                    ).unmarshall(context));
                }
                if (context.testExpression("StringMember", targetDepth)) {
                    context.nextToken();
                    nestedContainersResult.setStringMember(context.getUnmarshaller(String.class).unmarshall(context));
                }
            } else if (token == END_ARRAY || token == END_OBJECT) {
                if (context.getLastParsedParentElement() == null || context.getLastParsedParentElement().equals(currentParentElement)) {
                    if (context.getCurrentDepth() <= originalDepth)
                        break;
                }
            }
            token = context.nextToken();
        }

        return nestedContainersResult;
    }

    private static NestedContainersResultJsonUnmarshaller instance;

    public static NestedContainersResultJsonUnmarshaller getInstance() {
        if (instance == null)
            instance = new NestedContainersResultJsonUnmarshaller();
        return instance;
    }
}

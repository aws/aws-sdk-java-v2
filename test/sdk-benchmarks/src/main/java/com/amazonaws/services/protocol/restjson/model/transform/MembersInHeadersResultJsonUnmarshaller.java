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

import static com.fasterxml.jackson.core.JsonToken.*;

/**
 * MembersInHeadersResult JSON Unmarshaller
 */

public class MembersInHeadersResultJsonUnmarshaller implements Unmarshaller<MembersInHeadersResult, JsonUnmarshallerContext> {

    public MembersInHeadersResult unmarshall(JsonUnmarshallerContext context) throws Exception {
        MembersInHeadersResult membersInHeadersResult = new MembersInHeadersResult();

        if (context.isStartOfDocument()) {
            if (context.getHeader("x-amz-string") != null) {
                context.setCurrentHeader("x-amz-string");
                membersInHeadersResult.setStringMember(context.getUnmarshaller(String.class).unmarshall(context));
            }
            if (context.getHeader("x-amz-boolean") != null) {
                context.setCurrentHeader("x-amz-boolean");
                membersInHeadersResult.setBooleanMember(context.getUnmarshaller(Boolean.class).unmarshall(context));
            }
            if (context.getHeader("x-amz-integer") != null) {
                context.setCurrentHeader("x-amz-integer");
                membersInHeadersResult.setIntegerMember(context.getUnmarshaller(Integer.class).unmarshall(context));
            }
            if (context.getHeader("x-amz-long") != null) {
                context.setCurrentHeader("x-amz-long");
                membersInHeadersResult.setLongMember(context.getUnmarshaller(Long.class).unmarshall(context));
            }
            if (context.getHeader("x-amz-float") != null) {
                context.setCurrentHeader("x-amz-float");
                membersInHeadersResult.setFloatMember(context.getUnmarshaller(Float.class).unmarshall(context));
            }
            if (context.getHeader("x-amz-double") != null) {
                context.setCurrentHeader("x-amz-double");
                membersInHeadersResult.setDoubleMember(context.getUnmarshaller(Double.class).unmarshall(context));
            }
            if (context.getHeader("x-amz-timestamp") != null) {
                context.setCurrentHeader("x-amz-timestamp");
                membersInHeadersResult.setTimestampMember(DateJsonUnmarshallerFactory.getInstance("rfc822").unmarshall(context));
            }
        }

        return membersInHeadersResult;
    }

    private static MembersInHeadersResultJsonUnmarshaller instance;

    public static MembersInHeadersResultJsonUnmarshaller getInstance() {
        if (instance == null)
            instance = new MembersInHeadersResultJsonUnmarshaller();
        return instance;
    }
}

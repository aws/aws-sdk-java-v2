/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.codecatalyst.endpoints.internal;

import java.util.Locale;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;

@SdkInternalApi
public enum ParameterType {
    STRING("String"),
    BOOLEAN("Boolean"), ;

    private final String name;

    ParameterType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static ParameterType fromNode(JsonNode node) {
        return fromValue(node.asString());
    }

    public static ParameterType fromValue(String value) {
        switch (value.toLowerCase(Locale.ENGLISH)) {
        case "string":
            return STRING;
        case "boolean":
            return BOOLEAN;
        default:
            throw SdkClientException.create("Unknown parameter type: " + value);
        }
    }
}

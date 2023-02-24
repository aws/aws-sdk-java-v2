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

package software.amazon.awssdk.enhanced.dynamodb.internal.document;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeVisitor;

public class IdentityJsonNodeVisitor implements JsonNodeVisitor<Object> {
    @Override
    public Object visitNull() {
        return null;
    }

    @Override
    public Object visitBoolean(boolean bool) {
        return bool;
    }

    @Override
    public Object visitNumber(String number) {
        return number;
    }

    @Override
    public Object visitString(String string) {
        return string;
    }

    @Override
    public Object visitArray(List<JsonNode> array) {
        return array.stream()
                    .map(n -> n.visit(this))
                    .collect(Collectors.toList());
    }

    @Override
    public Object visitObject(Map<String, JsonNode> object) {
        Map<String, Object> result = new LinkedHashMap<>(object.size());
        object.forEach((k, v) -> {
            result.put(k, v.visit(this));
        });
        return result;
    }

    @Override
    public Object visitEmbeddedObject(Object embeddedObject) {
        return embeddedObject;
    }
}

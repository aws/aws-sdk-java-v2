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

package software.amazon.awssdk.protocol.reflect.document;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.core.document.Document;

public class JsonNodeToDocumentConvertor implements JsonNodeVisitor<Document> {

    @Override
    public Document visit(JsonNode jsonNode) {
        if (jsonNode.isObject()) {
            return visitMap(jsonNode);
        } else if (jsonNode.isArray()) {
            return visitList(jsonNode);
        } else if (jsonNode.isBoolean()) {
            return Document.fromBoolean(jsonNode.asBoolean());
        } else if (jsonNode.isNumber()) {
            return visitNumber(jsonNode);
        } else if (jsonNode.isTextual()) {
            return visitString(jsonNode);
        } else if (jsonNode.isNull()) {
            return visitNull();
        } else {
            throw new IllegalArgumentException("Unknown Type");
        }
    }

    private Document visitMap(JsonNode jsonContent) {
        Map<String, Document> documentMap = new LinkedHashMap<>();
        jsonContent.fieldNames().forEachRemaining(s ->
                documentMap.put(s, visit(jsonContent.get(s))));
        return Document.fromMap(documentMap);
    }

    private Document visitList(JsonNode jsonContent) {
        List<Document> documentList = new ArrayList<>();
        jsonContent.elements().forEachRemaining(s -> documentList.add(visit(s)));
        return Document.fromList(documentList);
    }

    private Document visitNull() {
        return Document.fromNull();
    }

    private Document visitNumber(JsonNode jsonNode) {
        return Document.fromNumber(SdkNumber.fromString(jsonNode.numberValue().toString()));
    }

    private Document visitString(JsonNode jsonNode) {
        return Document.fromString(jsonNode.asText());
    }
}

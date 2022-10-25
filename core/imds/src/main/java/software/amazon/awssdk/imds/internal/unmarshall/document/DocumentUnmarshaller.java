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

package software.amazon.awssdk.imds.internal.unmarshall.document;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeVisitor;

@SdkInternalApi
public final class DocumentUnmarshaller implements JsonNodeVisitor<Document> {
    @Override
    public Document visitNull() {
        return Document.fromNull();
    }

    @Override
    public Document visitBoolean(boolean bool) {
        return Document.fromBoolean(bool);
    }

    @Override
    public Document visitNumber(String number) {
        return Document.fromNumber(number);
    }

    @Override
    public Document visitString(String string) {
        return Document.fromString(string);
    }

    @Override
    public Document visitArray(List<JsonNode> array) {
        return Document.fromList(array.stream()
                                      .map(node -> node.visit(this))
                                      .collect(Collectors.toList()));
    }

    @Override
    public Document visitObject(Map<String, JsonNode> object) {
        return Document.fromMap(object.entrySet()
                                      .stream().collect(Collectors.toMap(Map.Entry::getKey,
                                                                         entry -> entry.getValue().visit(this),
                                                                         (left, right) -> left,
                                                                         LinkedHashMap::new)));
    }

    @Override
    public Document visitEmbeddedObject(Object embeddedObject) {
        throw new UnsupportedOperationException("Embedded objects are not supported within Document types.");
    }
}

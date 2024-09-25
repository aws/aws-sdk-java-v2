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

package software.amazon.awssdk.enhanced.dynamodb;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;

public class JsonTestUtils {

    public static List<JsonNode> toJsonNode(List<EnhancedDocument> documents) {
        List<JsonNode> list = new ArrayList<>(documents.size());
        for (EnhancedDocument document : documents) {
            String toJson = document.toJson();
            JsonNode jsonNode = parseJson(toJson);
            list.add(jsonNode);
        }
        return list;
    }

    public static JsonNode parseJson(String json) {
        JsonNodeParser parser = JsonNodeParser.create();
        return parser.parse(json);
    }

}

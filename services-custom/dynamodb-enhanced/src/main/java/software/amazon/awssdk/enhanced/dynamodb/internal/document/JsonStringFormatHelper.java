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

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;

@SdkInternalApi
public final class JsonStringFormatHelper {

    private JsonStringFormatHelper() {
    }

    /**
     * Helper function to convert a JsonNode to Json String representation
     *
     * @param jsonNode The JsonNode that needs to be converted to Json String.
     * @return Json String of Json Node.
     */
    public static String stringValue(JsonNode jsonNode) {
        if (jsonNode.isArray()) {
            return StreamSupport.stream(jsonNode.asArray().spliterator(), false)
                                .map(JsonStringFormatHelper::stringValue)
                                .collect(Collectors.joining(",", "[", "]"));
        }
        if (jsonNode.isObject()) {
            return mapToString(jsonNode);
        }
        return jsonNode.isString() ? "\"" + addEscapeCharacters(jsonNode.text()) + "\"" : jsonNode.toString();
    }

    /**
     * Escapes characters for a give given string
     *
     * @param input Input string
     * @return String with escaped characters.
     */
    public static String addEscapeCharacters(String input) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            switch (ch) {
                case '\\':
                    output.append("\\\\"); // escape backslash with a backslash
                    break;
                case '\n':
                    output.append("\\n"); // newline character
                    break;
                case '\r':
                    output.append("\\r"); // carriage return character
                    break;
                case '\t':
                    output.append("\\t"); // tab character
                    break;
                case '\f':
                    output.append("\\f"); // form feed
                    break;
                case '\"':
                    output.append("\\\""); // double-quote character
                    break;
                default:
                    output.append(ch);
                    break;
            }
        }
        return output.toString();
    }

    private static String mapToString(JsonNode jsonNode) {
        Map<String, JsonNode> value = jsonNode.asObject();

        if (value.isEmpty()) {
            return "{}";
        }

        StringBuilder output = new StringBuilder();
        output.append("{");
        value.forEach((k, v) -> output.append("\"").append(k).append("\":")
                                      .append(stringValue(v)).append(","));
        output.setCharAt(output.length() - 1, '}');
        return output.toString();
    }
}

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
     * Escapes characters for a given string
     *
     * @param input Input string
     * @return String with escaped characters.
     */
    public static String addEscapeCharacters(String input) {
        StringBuilder output = null;
        int len = input.length();

        for (int i = 0; i < len; i++) {
            char ch = input.charAt(i);
            if (needsEscaping(ch)) {
                if (output == null) {
                    output = initializeStringBuilder(input, i);
                }
                appendEscapeChar(output, ch);
            } else if (output != null) {
                output.append(ch);
            }
        }

        return output == null ? input : output.toString();
    }

    private static boolean needsEscaping(char ch) {
        return Character.isISOControl(ch) || ch == '"' || ch == '\\';
    }

    /**
     * Initialize StringBuilder with the unescaped portion of the input.
     */
    private static StringBuilder initializeStringBuilder(String input, int position) {
        // Arbitrary buffer of 16
        StringBuilder builder = new StringBuilder(input.length() + 16);
        builder.append(input, 0, position);
        return builder;
    }

    private static void appendEscapeChar(StringBuilder output, char ch) {
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
                if (Character.isISOControl(ch)) {
                    output.append(String.format("\\u%04X", (int) ch));
                } else {
                    output.append(ch);
                }
                break;
        }
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

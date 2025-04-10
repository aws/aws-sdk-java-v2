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

package software.amazon.awssdk.codegen.model.config.customization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class UseLegacyEventSchemeDeserializer
    extends JsonDeserializer<Map<String, CustomizationConfig.LegacyEventGenerationMode>> {
    @Override
    public Map<String, CustomizationConfig.LegacyEventGenerationMode> deserialize(
        JsonParser p, DeserializationContext ctxt) throws IOException {

        Map<String, CustomizationConfig.LegacyEventGenerationMode> result = new HashMap<>();

        JsonNode rootNode = p.getCodec().readTree(p);
        Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            JsonNode valueNode = field.getValue();

            if (valueNode.isArray()) {
                // Old format: List<String>
                result.put(key, CustomizationConfig.LegacyEventGenerationMode.NO_ES_EVENT_IMPL);
            } else if (valueNode.isTextual()) {
                // New format: Enum name as string
                try {
                    CustomizationConfig.LegacyEventGenerationMode value =
                        CustomizationConfig.LegacyEventGenerationMode.valueOf(
                            valueNode.asText().toUpperCase(Locale.US));
                    result.put(key, value);
                } catch (IllegalArgumentException e) {
                    throw new IOException("Invalid enum value: " + valueNode.asText(), e);
                }
            } else {
                throw new IOException("Unexpected format for key: " + key);
            }
        }

        return result;
    }
}

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

package software.amazon.awssdk.codegen.lite.defaultsmode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeVisitor;
import software.amazon.awssdk.utils.Logger;

/**
 * Loads sdk-default-configuration.json into memory. It filters out unsupported configuration options from the file
 */
@SdkInternalApi
public final class DefaultsLoader {
    private static final Logger log = Logger.loggerFor(DefaultsLoader.class);

    private static final Set<String> UNSUPPORTED_OPTIONS = new HashSet<>();

    static {
        UNSUPPORTED_OPTIONS.add("stsRegionalEndpoints");
    }

    private DefaultsLoader() {
    }

    public static DefaultConfiguration load(File path) {
        return loadDefaultsFromFile(path);
    }

    private static DefaultConfiguration loadDefaultsFromFile(File path) {
        DefaultConfiguration defaultsResolution = new DefaultConfiguration();
        Map<String, Map<String, String>> resolvedDefaults = new HashMap<>();

        try (FileInputStream fileInputStream = new FileInputStream(path)) {
            JsonNodeParser jsonNodeParser = JsonNodeParser.builder().build();

            Map<String, JsonNode> sdkDefaultConfiguration = jsonNodeParser.parse(fileInputStream)
                                                                          .asObject();

            Map<String, JsonNode> base = sdkDefaultConfiguration.get("base").asObject();
            Map<String, JsonNode> modes = sdkDefaultConfiguration.get("modes").asObject();

            modes.forEach((mode, modifiers) -> applyModificationToOneMode(resolvedDefaults, base, mode, modifiers));

            Map<String, JsonNode> documentation = sdkDefaultConfiguration.get("documentation").asObject();
            Map<String, JsonNode> modesDocumentation = documentation.get("modes").asObject();
            Map<String, JsonNode> configDocumentation = documentation.get("configuration").asObject();

            defaultsResolution.modesDocumentation(
                modesDocumentation.entrySet()
                                  .stream()
                                  .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue().asString()), Map::putAll));
            defaultsResolution.configurationDocumentation(
                configDocumentation.entrySet()
                                   .stream()
                                   .filter(e -> !UNSUPPORTED_OPTIONS.contains(e.getKey()))
                                   .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue().asString()), Map::putAll));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        defaultsResolution.modeDefaults(resolvedDefaults);

        return defaultsResolution;
    }

    private static void applyModificationToOneConfigurationOption(Map<String, String> resolvedDefaultsForCurrentMode,
                                                                  String option,
                                                                  JsonNode modifier) {
        String resolvedValue;
        String baseValue = resolvedDefaultsForCurrentMode.get(option);

        if (UNSUPPORTED_OPTIONS.contains(option)) {
            return;
        }

        Map<String, JsonNode> modifierMap = modifier.asObject();

        if (modifierMap.size() != 1) {
            throw new IllegalStateException("More than one modifier exists for option " + option);
        }

        String modifierString = modifierMap.keySet().iterator().next();

        switch (modifierString) {
            case "override":
                resolvedValue = modifierMap.get("override").visit(new StringJsonNodeVisitor());
                break;
            case "multiply":
                resolvedValue = processMultiply(baseValue, modifierMap);
                break;
            case "add":
                resolvedValue = processAdd(baseValue, modifierMap);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported modifier: " + modifierString);
        }

        resolvedDefaultsForCurrentMode.put(option, resolvedValue);
    }

    private static void applyModificationToOneMode(Map<String, Map<String, String>> resolvedDefaults,
                                                   Map<String, JsonNode> base,
                                                   String mode,
                                                   JsonNode modifiers) {

        log.info(() -> "Apply modification for mode: " + mode);
        Map<String, String> resolvedDefaultsForCurrentMode =
            base.entrySet().stream().filter(e -> !UNSUPPORTED_OPTIONS.contains(e.getKey()))
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(),
                                                       e.getValue().visit(new StringJsonNodeVisitor())), Map::putAll);


        // Iterate the configuration options and apply modification.
        modifiers.asObject().forEach((option, modifier) -> applyModificationToOneConfigurationOption(
            resolvedDefaultsForCurrentMode, option, modifier));

        resolvedDefaults.put(mode, resolvedDefaultsForCurrentMode);
    }

    private static String processAdd(String baseValue, Map<String, JsonNode> modifierMap) {
        String resolvedValue;
        String add = modifierMap.get("add").asNumber();
        int parsedAdd = Integer.parseInt(add);
        int number = Math.addExact(Integer.parseInt(baseValue), parsedAdd);
        resolvedValue = String.valueOf(number);
        return resolvedValue;
    }

    private static String processMultiply(String baseValue, Map<String, JsonNode> modifierMap) {
        String resolvedValue;
        String multiply = modifierMap.get("multiply").asNumber();
        double parsedValue = Double.parseDouble(multiply);

        double resolvedNumber = Integer.parseInt(baseValue) * parsedValue;
        int castValue = (int) resolvedNumber;

        if (castValue != resolvedNumber) {
            throw new IllegalStateException("The transformed value must be be a float number: " + castValue);
        }

        resolvedValue = String.valueOf(castValue);
        return resolvedValue;
    }

    private static final class StringJsonNodeVisitor implements JsonNodeVisitor<String> {
        @Override
        public String visitNull() {
            throw new IllegalStateException("Invalid type encountered");
        }

        @Override
        public String visitBoolean(boolean b) {
            throw new IllegalStateException("Invalid type (boolean) encountered " + b);
        }

        @Override
        public String visitNumber(String s) {
            return s;
        }

        @Override
        public String visitString(String s) {
            return s;
        }

        @Override
        public String visitArray(List<JsonNode> list) {
            throw new IllegalStateException("Invalid type (list) encountered: " + list);
        }

        @Override
        public String visitObject(Map<String, JsonNode> map) {
            throw new IllegalStateException("Invalid type (map) encountered: " + map);
        }

        @Override
        public String visitEmbeddedObject(Object o) {
            throw new IllegalStateException("Invalid type (embedded) encountered: " + o);
        }
    }
}

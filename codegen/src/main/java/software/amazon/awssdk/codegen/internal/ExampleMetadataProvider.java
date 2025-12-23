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

package software.amazon.awssdk.codegen.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.Logger;

/**
 * Class for loading and caching code example metadata from JSON files.
 */
public final class ExampleMetadataProvider {

    private static final Logger log = Logger.loggerFor(ExampleMetadataProvider.class);

    private static final ConcurrentMap<String, ExampleMetadataProvider> INSTANCE_CACHE = new ConcurrentHashMap<>();

    private final String exampleMetaPath;
    private final JsonNodeParser jsonParser;
    private final Lazy<Map<String, JsonNode>> serviceNodeCache;
    private final Lazy<Map<String, String>> normalizedServiceKeyMap;

    private ExampleMetadataProvider(String exampleMetaPath) {
        this.exampleMetaPath = exampleMetaPath;
        this.jsonParser = JsonNodeParser.create();
        this.serviceNodeCache = new Lazy<>(this::buildServiceNodeCache);
        this.normalizedServiceKeyMap = new Lazy<>(this::buildNormalizedServiceKeyMap);
    }

    /**
     * Creates an ExampleMetadataProvider instance for the given JSON file path.
     *
     * @param exampleMetaPath path to the example metadata JSON file
     * @return ExampleMetadataProvider instance for this path
     */
    public static ExampleMetadataProvider getInstance(String exampleMetaPath) {
        if (exampleMetaPath == null) {
            throw new IllegalArgumentException("exampleMetaPath cannot be null");
        }
        return INSTANCE_CACHE.computeIfAbsent(exampleMetaPath, ExampleMetadataProvider::new);
    }
    
    /**
     * Clears the instance cache.
     */
    public static void clearCache() {
        INSTANCE_CACHE.clear();
    }

    /**
     * Creates a link to a code example for the given operation.
     *
     * @param metadata the service metadata
     * @param operationName the name of the operation to find an example for
     * @return Optional containing the HTML link to the code example, or empty if no example found
     */
    public Optional<String> createLinkToCodeExample(Metadata metadata, String operationName) {
        try {
            if (metadata == null || operationName == null) {
                return Optional.empty();
            }

            String actualServiceKey = resolveActualServiceKey(metadata);
            if (actualServiceKey == null) {
                return Optional.empty();
            }

            return findExampleUrl(actualServiceKey, operationName)
                    .map(url -> String.format("<a href=\"%s\" target=\"_top\">Code Example</a>", url));
        } catch (RuntimeException e) {
            log.error(() -> "Failed to create code example link for "
                            + metadata.getServiceName() + "." + operationName, e);
            return Optional.empty();
        }
    }

    private String resolveActualServiceKey(Metadata metadata) {
        String normalizedServiceName = metadata.getServiceName().toLowerCase(Locale.ROOT);
        return normalizedServiceKeyMap.getValue().get(normalizedServiceName);
    }

    /**
     * Finds the URL for a code example given the service key and operation name.
     */
    private Optional<String> findExampleUrl(String serviceKey, String operationName) {
        JsonNode serviceNode = serviceNodeCache.getValue().get(serviceKey);

        if (serviceNode != null) {
            String targetExampleId = serviceKey + "_" + operationName;
            return findOperationUrl(serviceNode, targetExampleId);
        }

        return Optional.empty();
    }

    /**
     * Gets all code examples for a specific service.
     *
     * @param metadata the service metadata
     * @return a list of examples for the service
     */
    public List<ExampleData> getServiceCodeExamples(Metadata metadata) {
        List<ExampleData> examples = new ArrayList<>();

        try {
            String normalizedServiceName = metadata.getServiceName().toLowerCase(Locale.ROOT);
            String actualServiceKey = normalizedServiceKeyMap.getValue().get(normalizedServiceName);

            if (actualServiceKey != null) {
                JsonNode serviceNode = serviceNodeCache.getValue().get(actualServiceKey);
                if (serviceNode != null) {
                    examples = parseServiceExamples(serviceNode);
                }
            }
        } catch (Exception e) {
            log.debug(() -> "Failed to load examples for " + metadata.getServiceName(), e);
        }

        return examples;
    }

    /**
     * Builds the service node cache from the example metadata file.
     * @return map of service keys to their JSON node data
     */
    private Map<String, JsonNode> buildServiceNodeCache() {
        Map<String, JsonNode> nodeCache = new HashMap<>();
        
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(exampleMetaPath)) {
            if (inputStream == null) {
                log.debug(() -> exampleMetaPath + " not found in classpath");
            } else {
                JsonNode root = jsonParser.parse(inputStream);
                JsonNode servicesNode = root.field("services").orElse(null);
                
                if (servicesNode != null && servicesNode.isObject()) {
                    Map<String, JsonNode> servicesMap = servicesNode.asObject();
                    servicesMap.forEach((serviceKey, serviceNode) -> {
                        if (serviceNode != null) {
                            nodeCache.put(serviceKey, serviceNode);
                        }
                    });
                }
            }
        } catch (IOException | RuntimeException e) {
            log.warn(() -> "Failed to load " + exampleMetaPath, e);
        }
        
        return nodeCache;
    }
    
    /**
     * Builds the normalized service key mapping from the example metadata file.
     * @return map of normalized service names to actual service keys
     */
    private Map<String, String> buildNormalizedServiceKeyMap() {
        Map<String, String> normalizedMap = new HashMap<>();

        serviceNodeCache.getValue().keySet().forEach(serviceKey -> {
            String normalizedKey = serviceKey.replace("-", "").toLowerCase(Locale.ROOT);
            normalizedMap.put(normalizedKey, serviceKey);
        });
        
        return normalizedMap;
    }
    
    /**
     * Finds the URL for a specific operation ID within a service node.
     */
    private Optional<String> findOperationUrl(JsonNode serviceNode, String targetExampleId) {
        JsonNode examplesNode = serviceNode.field("examples").orElse(null);
        if (examplesNode != null && examplesNode.isArray()) {
            for (JsonNode example : examplesNode.asArray()) {
                JsonNode idNode = example.field("id").orElse(null);
                JsonNode urlNode = example.field("url").orElse(null);

                if (idNode != null && urlNode != null) {
                    String id = idNode.asString();
                    if (targetExampleId.equals(id)) {
                        return Optional.of(urlNode.asString());
                    }
                }
            }
        }
        return Optional.empty();
    }
    
    /**
     * Parses examples from a service node in the JSON.
     */
    private List<ExampleData> parseServiceExamples(JsonNode serviceNode) {
        List<ExampleData> examples = new ArrayList<>();
        JsonNode examplesNode = serviceNode.field("examples").orElse(null);

        if (examplesNode != null && examplesNode.isArray()) {
            for (JsonNode example : examplesNode.asArray()) {
                JsonNode idNode = example.field("id").orElse(null);
                JsonNode titleNode = example.field("title").orElse(null);
                JsonNode categoryNode = example.field("category").orElse(null);
                JsonNode urlNode = example.field("url").orElse(null);

                if (idNode != null && titleNode != null && urlNode != null) {
                    String id = idNode.asString();
                    String title = titleNode.asString();
                    String category = categoryNode != null ? categoryNode.asString() : "Api";
                    String url = urlNode.asString();
                    
                    if (!id.isEmpty() && !title.isEmpty() && !url.isEmpty()) {
                        examples.add(new ExampleData(id, title, category, url));
                    }
                }
            }
        }
        
        return examples;
    }

    public static final class ExampleData {
        private final String id;
        private final String title;
        private final String category;
        private final String url;

        public ExampleData(String id, String title, String category, String url) {
            this.id = id;
            this.title = title;
            this.category = category;
            this.url = url;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getCategory() {
            return category;
        }

        public String getUrl() {
            return url;
        }
    }
}

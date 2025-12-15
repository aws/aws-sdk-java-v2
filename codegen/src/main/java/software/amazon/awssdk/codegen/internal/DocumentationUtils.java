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

import static software.amazon.awssdk.codegen.internal.Constant.AWS_DOCS_HOST;
import static software.amazon.awssdk.codegen.model.intermediate.ShapeType.Model;
import static software.amazon.awssdk.codegen.model.intermediate.ShapeType.Request;
import static software.amazon.awssdk.codegen.model.intermediate.ShapeType.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.utils.Logger;

public final class DocumentationUtils {

    private static final String DEFAULT_SETTER = "Sets the value of the %s property for this object.";

    private static final String DEFAULT_SETTER_PARAM = "The new value for the %s property for this object.";

    private static final String DEFAULT_GETTER = "Returns the value of the %s property for this object.";

    private static final String DEFAULT_GETTER_PARAM = "The value of the %s property for this object.";

    private static final String DEFAULT_EXISTENCE_CHECK =
        "For responses, this returns true if the service returned a value for the %s property. This DOES NOT check that the "
        + "value is non-empty (for which, you should check the {@code isEmpty()} method on the property). This is useful because "
        + "the SDK will never return a null collection or map, but you may need to differentiate between the service returning "
        + "nothing (or null) and the service returning an empty collection or map. For requests, this returns true if a value "
        + "for the property was specified in the request builder, and false if a value was not specified.";

    private static final String DEFAULT_FLUENT_RETURN =
            "Returns a reference to this object so that method calls can be chained together.";

    //TODO probably should move this to a custom config in each service
    private static final Set<String> SERVICES_EXCLUDED_FROM_CROSS_LINKING = new HashSet<>(Arrays.asList(
            "apigateway", "budgets", "cloudsearch", "cloudsearchdomain",
            "discovery", "elastictranscoder", "es", "glacier",
            "iot", "data.iot", "machinelearning", "rekognition", "s3", "sdb", "swf"
                                                                                                       ));
    private static final Pattern COMMENT_DELIMITER = Pattern.compile("\\*\\/");
    
    private static final Logger log = Logger.loggerFor(DocumentationUtils.class);
    private static Map<String, String> exampleUrlMap;

    private DocumentationUtils() {
    }

    /**
     * Returns a documentation with HTML tags prefixed and suffixed removed, or
     * returns empty string if the input is empty or null. This method is to be
     * used when constructing documentation for method parameters.
     *
     * @param documentation
     *            unprocessed input documentation
     * @return HTML tag stripped documentation or empty string if input was
     *         null.
     */
    public static String stripHtmlTags(String documentation) {
        if (documentation == null) {
            return "";
        }

        if (documentation.startsWith("<")) {
            int startTagIndex = documentation.indexOf('>');
            int closingTagIndex = documentation.lastIndexOf('<');
            if (closingTagIndex > startTagIndex) {
                documentation = stripHtmlTags(documentation.substring(startTagIndex + 1, closingTagIndex));
            } else {
                documentation = stripHtmlTags(documentation.substring(startTagIndex + 1));
            }
        }

        return documentation.trim();
    }

    /**
     * Escapes Java comment breaking illegal character sequences.
     *
     * @param documentation
     *            unprocessed input documentation
     * @return escaped documentation, or empty string if input was null
     */
    public static String escapeIllegalCharacters(String documentation) {
        if (documentation == null) {
            return "";
        }

        /*
         * this specifically handles a case where a '* /' sequence may
         * be present in documentation and inadvertently terminate that Java
         * comment line, resulting in broken code.
         */
        documentation = COMMENT_DELIMITER.matcher(documentation).replaceAll("*&#47;");

        return documentation;
    }

    /**
     * Create the HTML for a link to the operation/shape core AWS docs site
     *
     * @param metadata  the UID for the service from that services metadata
     * @param name the name of the shape/request/operation
     *
     * @return a '@see also' HTML link to the doc
     */
    public static String createLinkToServiceDocumentation(Metadata metadata, String name) {
        if (isCrossLinkingEnabledForService(metadata)) {
            return String.format("<a href=\"https://%s/goto/WebAPI/%s/%s\" target=\"_top\">AWS API Documentation</a>",
                                 AWS_DOCS_HOST,
                                 metadata.getUid(),
                                 name);
        }
        return "";
    }

    /**
     * Create the HTML for a link to the operation/shape core AWS docs site
     *
     * @param metadata  the UID for the service from that services metadata
     * @param shapeModel the model of the shape
     *
     * @return a '@see also' HTML link to the doc
     */
    public static String createLinkToServiceDocumentation(Metadata metadata, ShapeModel shapeModel) {
        return isRequestResponseOrModel(shapeModel) ? createLinkToServiceDocumentation(metadata,
                                                                                       shapeModel.getDocumentationShapeName())
                                                    : "";
    }

    /**
     * Create a link to a code example for the given operation.
     *
     * @param metadata the service metadata containing service name information
     * @param operationName the name of the operation to find an example for
     * @return a '@see also' HTML link to the code example, or empty string if no example found
     */
    public static String createLinkToCodeExample(Metadata metadata, String operationName) {
        try {
            String serviceKey = mapServiceNameToExampleKey(metadata.getServiceName());
            String targetExampleId = serviceKey + "_" + operationName;

            Map<String, String> urlMap = getExampleUrlMap();
            String url = urlMap.get(targetExampleId);

            if (url != null) {
                return String.format("<a href=\"%s\" target=\"_top\">Code Example</a>", url);
            }

            return "";
        } catch (Exception e) {
            log.debug(() -> "Failed to create code example link for " + metadata.getServiceName() + "." + operationName, e);
            return "";
        }
    }

    public static String removeFromEnd(String string, String stringToRemove) {
        return string.endsWith(stringToRemove) ? string.substring(0, string.length() - stringToRemove.length()) : string;
    }

    private static boolean isRequestResponseOrModel(ShapeModel shapeModel) {
        return shapeModel.getShapeType() == Model || shapeModel.getShapeType() == Request ||
               shapeModel.getShapeType() == Response;
    }

    private static boolean isCrossLinkingEnabledForService(Metadata metadata) {
        return metadata.getUid() != null && metadata.getEndpointPrefix() != null &&
               !SERVICES_EXCLUDED_FROM_CROSS_LINKING.contains(metadata.getEndpointPrefix());
    }

    public static String defaultSetter() {
        return DEFAULT_SETTER;
    }

    public static String defaultSetterParam() {
        return DEFAULT_SETTER_PARAM;
    }

    public static String defaultGetter() {
        return DEFAULT_GETTER;
    }

    public static String defaultGetterParam() {
        return DEFAULT_GETTER_PARAM;
    }

    public static String defaultFluentReturn() {
        return DEFAULT_FLUENT_RETURN;
    }

    public static String defaultExistenceCheck() {
        return DEFAULT_EXISTENCE_CHECK;
    }

    /**
     * Maps service names from codegen format (e.g., "AutoScaling") to example-meta.json keys (e.g., "auto-scaling").
     */
    private static String mapServiceNameToExampleKey(String serviceName) {
        if (serviceName == null) {
            return "";
        }

        return serviceName
            .replaceAll("([a-z0-9])([A-Z])", "$1-$2")
            .toLowerCase(Locale.ROOT);
    }

    /**
     * Gets the cached example URL map for fast operation ID -> URL lookups.
     */
    private static Map<String, String> getExampleUrlMap() {
        if (exampleUrlMap == null) {
            exampleUrlMap = buildExampleUrlMap();
        }
        return exampleUrlMap;
    }

    /**
     * Builds a flat lookup map from example-meta.json: operation ID -> URL
     */
    private static Map<String, String> buildExampleUrlMap() {
        Map<String, String> urlMap = new HashMap<>();
        
        try (InputStream inputStream = DocumentationUtils.class.getClassLoader()
                .getResourceAsStream("software/amazon/awssdk/codegen/example-meta.json")) {
            
            if (inputStream == null) {
                log.debug(() -> "example-meta.json not found in classpath");
                return urlMap;
            }
            
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(inputStream);
            
            JsonNode servicesNode = root.get("services");
            if (servicesNode != null) {
                for (JsonNode serviceNode : servicesNode) {
                    JsonNode examplesNode = serviceNode.get("examples");
                    if (examplesNode != null && examplesNode.isArray()) {
                        for (JsonNode example : examplesNode) {
                            JsonNode idNode = example.get("id");
                            JsonNode urlNode = example.get("url");
                            
                            if (idNode != null && urlNode != null) {
                                String id = idNode.asText();
                                String url = urlNode.asText();
                                if (!id.isEmpty() && !url.isEmpty()) {
                                    urlMap.put(id, url);
                                }
                            }
                        }
                    }
                }
            }

            return urlMap;
            
        } catch (IOException e) {
            log.warn(() -> "Failed to load example-meta.json", e);
            return urlMap;
        }
    }
}

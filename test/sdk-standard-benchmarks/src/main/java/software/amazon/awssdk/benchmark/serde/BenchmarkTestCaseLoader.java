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

package software.amazon.awssdk.benchmark.serde;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Loads protocol test case JSON files and extracts test case data for
 * benchmarks.
 *
 * <p>
 * The test data files use the c2j protocol test format, where the top-level
 * structure is an array of operation-group objects. Each group contains
 * a {@code cases} array with individual test cases.
 * </p>
 */
public final class BenchmarkTestCaseLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int DEFAULT_STATUS_CODE = 200;

    private BenchmarkTestCaseLoader() {
    }

    /**
     * Represents a single marshalling test case loaded from the input JSON.
     */
    public static final class MarshallTestCase {
        private final String id;
        private final String operationName;
        private final JsonNode inputData;
        private final String httpMethod;
        private final String requestUri;

        MarshallTestCase(String id, String operationName, JsonNode inputData,
                String httpMethod, String requestUri) {
            this.id = id;
            this.operationName = operationName;
            this.inputData = inputData;
            this.httpMethod = httpMethod;
            this.requestUri = requestUri;
        }

        public String getId() {
            return id;
        }

        public String getOperationName() {
            return operationName;
        }

        public JsonNode getInputData() {
            return inputData;
        }

        public String getHttpMethod() {
            return httpMethod;
        }

        public String getRequestUri() {
            return requestUri;
        }
    }

    /**
     * Represents a single unmarshalling test case loaded from the output JSON.
     */
    public static final class UnmarshallTestCase {
        private final String id;
        private final String operationName;
        private final String responseBody;
        private final Integer statusCode;
        private final Map<String, String> headers;

        UnmarshallTestCase(String id, String operationName, String responseBody, Integer statusCode,
                Map<String, String> headers) {
            this.id = id;
            this.operationName = operationName;
            this.responseBody = responseBody;
            this.statusCode = statusCode;
            this.headers = headers;
        }

        public String getId() {
            return id;
        }

        public String getOperationName() {
            return operationName;
        }

        public String getResponseBody() {
            return responseBody;
        }

        public Integer getStatusCode() {
            return statusCode;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }
    }

    /**
     * Load all marshall test cases from the given resource path.
     *
     * <p>
     * Iterates through the top-level array of operation groups, then through each
     * group's {@code cases} array. Extracts {@code id} from the case,
     * {@code operationName}
     * from {@code case.given.name}, and {@code inputData} from {@code case.params}.
     * </p>
     *
     * @param resourcePath classpath resource path to the input test JSON file
     * @return list of marshall test cases
     * @throws IllegalStateException    if the resource is not found
     * @throws IllegalArgumentException if the JSON is malformed or missing required
     *                                  fields
     */
    public static List<MarshallTestCase> loadMarshallTestCases(String resourcePath) {
        JsonNode root = loadJsonResource(resourcePath);
        validateTopLevelArray(root, resourcePath);

        List<MarshallTestCase> testCases = new ArrayList<>();
        for (JsonNode operationGroup : root) {
            JsonNode casesNode = operationGroup.get("cases");
            if (casesNode == null || !casesNode.isArray()) {
                continue;
            }
            for (JsonNode caseNode : casesNode) {
                String id = getRequiredTextField(caseNode, "id", resourcePath);
                String operationName = getOperationName(caseNode, resourcePath);
                JsonNode inputData = getRequiredNode(caseNode, "params", resourcePath);
                JsonNode givenNode = caseNode.get("given");
                JsonNode httpNode = givenNode != null ? givenNode.get("http") : null;
                String httpMethod = httpNode != null && httpNode.has("method")
                        ? httpNode.get("method").asText()
                        : "POST";
                String requestUri = httpNode != null && httpNode.has("requestUri")
                        ? httpNode.get("requestUri").asText()
                        : "/";
                testCases.add(new MarshallTestCase(id, operationName, inputData, httpMethod, requestUri));
            }
        }
        return testCases;
    }

    /**
     * Load all unmarshall test cases from the given resource path.
     *
     * <p>
     * Iterates through the top-level array of operation groups, then through each
     * group's {@code cases} array. Extracts {@code id} from the case,
     * {@code operationName}
     * from {@code case.given.name}, {@code responseBody} from
     * {@code case.response.body},
     * {@code statusCode} from {@code case.response.status_code} (defaults to 200 if
     * absent),
     * and {@code headers} from {@code case.response.headers} (null if absent).
     * </p>
     *
     * @param resourcePath classpath resource path to the output test JSON file
     * @return list of unmarshall test cases
     * @throws IllegalStateException    if the resource is not found
     * @throws IllegalArgumentException if the JSON is malformed or missing required
     *                                  fields
     */
    public static List<UnmarshallTestCase> loadUnmarshallTestCases(String resourcePath) {
        JsonNode root = loadJsonResource(resourcePath);
        validateTopLevelArray(root, resourcePath);

        List<UnmarshallTestCase> testCases = new ArrayList<>();
        for (JsonNode operationGroup : root) {
            JsonNode casesNode = operationGroup.get("cases");
            if (casesNode == null || !casesNode.isArray()) {
                continue;
            }
            for (JsonNode caseNode : casesNode) {
                String id = getRequiredTextField(caseNode, "id", resourcePath);
                String operationName = getOperationName(caseNode, resourcePath);

                JsonNode responseNode = getRequiredNode(caseNode, "response", resourcePath);

                JsonNode bodyNode = responseNode.get("body");
                String responseBody = bodyNode != null && bodyNode.isTextual() ? bodyNode.asText() : "";

                Integer statusCode = DEFAULT_STATUS_CODE;
                JsonNode statusCodeNode = responseNode.get("status_code");
                if (statusCodeNode != null && statusCodeNode.isNumber()) {
                    statusCode = statusCodeNode.intValue();
                }

                Map<String, String> headers = extractHeaders(responseNode);

                testCases.add(new UnmarshallTestCase(id, operationName, responseBody, statusCode, headers));
            }
        }
        return testCases;
    }

    /**
     * Load an {@link IntermediateModel} from the classpath and patch member names
     * so that
     * {@code ShapeModelReflector} resolves the correct fluent setter method names.
     *
     * <p>
     * The codegen-generated fluent setter for a member named {@code "SS"} is
     * {@code ss()},
     * but {@code ShapeModelReflector} derives the setter name by calling
     * {@code StringUtils.uncapitalize(member.getName())} which produces
     * {@code "sS"}.
     * This method patches each member's {@code name} to be the capitalized form of
     * {@code fluentSetterMethodName} so that uncapitalize produces the correct
     * result.
     * </p>
     *
     * @param resourcePath classpath resource path to the intermediate model JSON
     * @return the loaded and patched IntermediateModel
     */
    public static IntermediateModel loadIntermediateModel(String resourcePath) {
        ObjectMapper mapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        URL resource = BenchmarkTestCaseLoader.class.getClassLoader().getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("IntermediateModel not found on classpath: " + resourcePath);
        }
        try {
            IntermediateModel model = mapper.readValue(resource, IntermediateModel.class);
            patchMemberNames(model);
            return model;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load IntermediateModel from: " + resourcePath, e);
        }
    }

    /**
     * Patch member names so that {@code StringUtils.uncapitalize(name)} matches the
     * fluent setter method name. For example, member "SS" with fluentSetter "ss"
     * gets its name changed to "Ss" so uncapitalize("Ss") = "ss".
     */
    private static void patchMemberNames(IntermediateModel model) {
        for (ShapeModel shape : model.getShapes().values()) {
            if (shape.getMembers() == null) {
                continue;
            }
            for (MemberModel member : shape.getMembers()) {
                String fluentSetter = member.getFluentSetterMethodName();
                if (fluentSetter == null || fluentSetter.isEmpty()) {
                    continue;
                }
                String derivedName = StringUtils.uncapitalize(member.getName());
                if (!derivedName.equals(fluentSetter)) {
                    // Capitalize the fluent setter name so uncapitalize produces the correct result
                    member.setName(StringUtils.capitalize(fluentSetter));
                }
            }
        }
    }

    /**
     * Build an {@link OperationInfo} for the given operation by inspecting the
     * intermediate model's
     * input shape to determine payload flags. This replicates the logic the codegen
     * uses when
     * generating per-operation marshallers.
     *
     * @param model    the loaded IntermediateModel
     * @param testCase the test case (provides operationName, httpMethod,
     *                 requestUri)
     * @return correctly configured OperationInfo
     */
    public static OperationInfo buildOperationInfo(IntermediateModel model, MarshallTestCase testCase) {
        String operationName = testCase.getOperationName();
        String inputShapeName = operationName + "Request";
        ShapeModel inputShape = model.getShapes().get(inputShapeName);

        boolean hasExplicitPayload = false;
        boolean hasImplicitPayload = false;
        boolean hasPayloadMembers = false;

        if (inputShape != null && inputShape.getMembers() != null) {
            for (MemberModel member : inputShape.getMembers()) {
                if (member.getHttp() != null && member.getHttp().getIsPayload()) {
                    hasExplicitPayload = true;
                    hasPayloadMembers = true;
                } else if (member.getHttp() == null || member.getHttp().getLocation() == null) {
                    hasImplicitPayload = true;
                    hasPayloadMembers = true;
                }
            }
        }

        return OperationInfo.builder()
                .httpMethod(SdkHttpMethod.valueOf(testCase.getHttpMethod()))
                .requestUri(testCase.getRequestUri())
                .operationIdentifier(operationName)
                .hasExplicitPayloadMember(hasExplicitPayload)
                .hasImplicitPayloadMembers(hasImplicitPayload)
                .hasPayloadMembers(hasPayloadMembers)
                .build();
    }

    private static JsonNode loadJsonResource(String resourcePath) {
        InputStream is = BenchmarkTestCaseLoader.class.getResourceAsStream("/" + resourcePath);
        if (is == null) {
            throw new IllegalStateException("Resource not found on classpath: " + resourcePath);
        }
        try {
            return MAPPER.readTree(is);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse JSON from resource: " + resourcePath, e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // best-effort close
            }
        }
    }

    private static void validateTopLevelArray(JsonNode root, String resourcePath) {
        if (!root.isArray()) {
            throw new IllegalArgumentException(
                    "Expected top-level JSON array in resource: " + resourcePath);
        }
    }

    private static String getOperationName(JsonNode caseNode, String resourcePath) {
        JsonNode givenNode = caseNode.get("given");
        if (givenNode == null) {
            throw new IllegalArgumentException(
                    "Test case missing 'given' field in resource: " + resourcePath
                            + ", case: " + caseNode.get("id"));
        }
        JsonNode nameNode = givenNode.get("name");
        if (nameNode == null || !nameNode.isTextual()) {
            throw new IllegalArgumentException(
                    "Test case missing 'given.name' field in resource: " + resourcePath
                            + ", case: " + caseNode.get("id"));
        }
        return nameNode.asText();
    }

    private static String getRequiredTextField(JsonNode node, String fieldName, String resourcePath) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || !fieldNode.isTextual()) {
            throw new IllegalArgumentException(
                    "Missing or non-text required field '" + fieldName + "' in resource: " + resourcePath);
        }
        return fieldNode.asText();
    }

    private static JsonNode getRequiredNode(JsonNode node, String fieldName, String resourcePath) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null) {
            throw new IllegalArgumentException(
                    "Missing required field '" + fieldName + "' in resource: " + resourcePath);
        }
        return fieldNode;
    }

    private static Map<String, String> extractHeaders(JsonNode responseNode) {
        JsonNode headersNode = responseNode.get("headers");
        if (headersNode == null || !headersNode.isObject()) {
            return null;
        }
        Map<String, String> headers = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = headersNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            headers.put(entry.getKey(), entry.getValue().asText());
        }
        return Collections.unmodifiableMap(headers);
    }
}

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

package software.amazon.awssdk.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import junit.framework.ComparisonFailure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.smithy.SmithyIntermediateModelBuilder;
import software.amazon.awssdk.codegen.smithy.SmithyModelWithCustomizations;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

public class SmithyIntermediateModelBuilderTest {

    private IntermediateModel c2jIm;
    private IntermediateModel smithyIm;

    @BeforeEach
    public void setUp() {
        final File c2jModelFile = new File(IntermediateModelBuilderTest.class
                                            .getResource("poet/client/smithy/basic/service-2.json").getFile());
        c2jIm = new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(ModelLoaderUtils.loadModel(ServiceModel.class, c2jModelFile))
                     .customizationConfig(CustomizationConfig.create())
                     .build())
            .build();

        final File smithyModelFile = new File(IntermediateModelBuilderTest.class
                                            .getResource("poet/client/smithy/basic/model.json").getFile());
        SmithyModelWithCustomizations smithyModel = SmithyModelWithCustomizations.builder()
                                                                                 .smithyModel(smithyModelFile.toPath())
                                                                                 .build();
        smithyIm = new SmithyIntermediateModelBuilder(smithyModel).build();
    }

    @Test
    public void testTranslate() {
        // Assert that both models have the same operations
        assertOperationsMatch(c2jIm, smithyIm);

        // Assert that both models have the same shapes
        assertShapesMatch(c2jIm, smithyIm);

        // Assert that the Smithy model has endpoint rules loaded from traits
        assertEndpointModelsPopulated(smithyIm);
    }

    /**
     * Serializes both intermediate models to JSON and computes a structured diff.
     * This test always passes - it prints the differences to stdout for inspection.
     */
    @Test
    public void compareJsonModels() throws Exception {
        ObjectMapper mapper = createOrderedMapper();

        JsonNode c2jTree = mapper.valueToTree(c2jIm);
        JsonNode smithyTree = mapper.valueToTree(smithyIm);

        List<String> diffs = new ArrayList<>();
        computeDiffs("", c2jTree, smithyTree, diffs);

        System.out.println("=== IntermediateModel JSON Diff (C2J vs Smithy) ===");
        System.out.println("Total differences: " + diffs.size());
        System.out.println();
        for (String diff : diffs) {
            System.out.println(diff);
        }
        System.out.println("=== End of Diff ===");
    }

    /**
     * Serializes both intermediate models to pretty-printed JSON and throws a ComparisonFailure
     * so the IDE can display a side-by-side diff. Run this test directly from the IDE to inspect
     * the full JSON differences.
     *
     * <p>Disabled by default since it is expected to fail until the Smithy translation is complete.
     */
    @Disabled("Expected to fail - run manually from IDE to see side-by-side JSON diff")
    @Test
    public void compareFullJsonOutput() throws Exception {
        ObjectMapper mapper = createOrderedMapper();
        String c2jJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(c2jIm);
        String smithyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(smithyIm);

        if (!c2jJson.equals(smithyJson)) {
            // IDEs know how to nicely display JUnit's ComparisonFailure - makes debugging much easier
            throw new ComparisonFailure("Smithy IM does not match C2J", c2jJson, smithyJson);
        }
    }

    private static ObjectMapper createOrderedMapper() {
        return new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .registerModule(new Jdk8Module());
    }

    /**
     * Recursively computes differences between two JSON trees.
     * Differences are collected as human-readable strings with JSON paths.
     */
    private void computeDiffs(String path, JsonNode c2j, JsonNode smithy, List<String> diffs) {
        if (c2j == null && smithy == null) {
            return;
        }
        if (c2j == null) {
            diffs.add(String.format("ADDED   %s: %s", path, truncate(smithy.toString())));
            return;
        }
        if (smithy == null) {
            diffs.add(String.format("MISSING %s: %s", path, truncate(c2j.toString())));
            return;
        }
        if (c2j.getNodeType() != smithy.getNodeType()) {
            diffs.add(String.format("TYPE    %s: c2j=%s, smithy=%s", path, c2j.getNodeType(), smithy.getNodeType()));
            return;
        }

        if (c2j.isObject()) {
            ObjectNode c2jObj = (ObjectNode) c2j;
            ObjectNode smithyObj = (ObjectNode) smithy;

            // Check all fields in c2j
            Iterator<String> c2jFields = c2jObj.fieldNames();
            while (c2jFields.hasNext()) {
                String field = c2jFields.next();
                String childPath = path.isEmpty() ? field : path + "." + field;
                if (!smithyObj.has(field)) {
                    diffs.add(String.format("MISSING %s: %s", childPath, truncate(c2jObj.get(field).toString())));
                } else {
                    computeDiffs(childPath, c2jObj.get(field), smithyObj.get(field), diffs);
                }
            }

            // Check for fields only in smithy
            Iterator<String> smithyFields = smithyObj.fieldNames();
            while (smithyFields.hasNext()) {
                String field = smithyFields.next();
                if (!c2jObj.has(field)) {
                    String childPath = path.isEmpty() ? field : path + "." + field;
                    diffs.add(String.format("ADDED   %s: %s", childPath, truncate(smithyObj.get(field).toString())));
                }
            }
        } else if (c2j.isArray()) {
            // Sort arrays before comparing to eliminate ordering differences.
            // Member arrays sort by c2jName, exception arrays by exceptionName,
            // string arrays sort alphabetically.
            List<JsonNode> c2jSorted = sortJsonArray(c2j);
            List<JsonNode> smithySorted = sortJsonArray(smithy);
            int maxLen = Math.max(c2jSorted.size(), smithySorted.size());
            for (int i = 0; i < maxLen; i++) {
                String childPath = path + "[" + i + "]";
                JsonNode c2jElem = i < c2jSorted.size() ? c2jSorted.get(i) : null;
                JsonNode smithyElem = i < smithySorted.size() ? smithySorted.get(i) : null;
                computeDiffs(childPath, c2jElem, smithyElem, diffs);
            }
        } else {
            // Value node comparison
            if (!c2j.equals(smithy)) {
                diffs.add(String.format("CHANGED %s: c2j=%s, smithy=%s", path,
                                       truncate(c2j.toString()), truncate(smithy.toString())));
            }
        }
    }

    private String truncate(String value) {
        int maxLen = 120;
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen) + "...(" + value.length() + " chars)";
    }

    /**
     * Sorts a JSON array for stable comparison. Objects are sorted by a sort key
     * (c2jName, exceptionName, or the full JSON text), strings are sorted alphabetically.
     */
    private List<JsonNode> sortJsonArray(JsonNode arrayNode) {
        List<JsonNode> elements = new ArrayList<>();
        for (JsonNode element : arrayNode) {
            elements.add(element);
        }
        elements.sort((a, b) -> {
            String keyA = getArrayElementSortKey(a);
            String keyB = getArrayElementSortKey(b);
            return keyA.compareTo(keyB);
        });
        return elements;
    }

    /**
     * Extracts a sort key from a JSON array element for stable ordering.
     * For objects: uses c2jName, exceptionName, name, or falls back to full JSON text.
     * For value nodes: uses the text value.
     */
    private String getArrayElementSortKey(JsonNode node) {
        if (node.isObject()) {
            // Try common identifying fields in priority order
            for (String field : new String[]{"c2jName", "exceptionName", "name", "value"}) {
                JsonNode fieldNode = node.get(field);
                if (fieldNode != null && fieldNode.isTextual()) {
                    return fieldNode.asText();
                }
            }
            // Fall back to full JSON text for deterministic ordering
            return node.toString();
        }
        return node.asText();
    }

    /**
     * Asserts that shapes in both models match in terms of names and key properties.
     */
    private void assertShapesMatch(IntermediateModel expected, IntermediateModel actual) {
        assertThat(actual.getShapes().keySet())
            .as("Shape names should match")
            .containsExactlyInAnyOrderElementsOf(expected.getShapes().keySet());

        // For each shape, compare basic properties
        for (String shapeName : expected.getShapes().keySet()) {
            ShapeModel expectedShape = expected.getShapes().get(shapeName);
            ShapeModel actualShape = actual.getShapes().get(shapeName);

            assertThat(actualShape)
                .as("Shape %s should exist in both models", shapeName)
                .isNotNull();

            assertShapeDetailsMatch(shapeName, expectedShape, actualShape);
        }
    }

    /**
     * Asserts that two shape models match in their key properties.
     */
    private void assertShapeDetailsMatch(String shapeName, ShapeModel expected, ShapeModel actual) {
        assertThat(actual.getShapeName())
            .as("Shape name for %s", shapeName)
            .isEqualTo(expected.getShapeName());

        assertThat(actual.getType())
            .as("Shape type for %s", shapeName)
            .isEqualTo(expected.getType());

        // Documentation can be null in Smithy or empty string in C2J when not present
        String expectedDoc = expected.getDocumentation();
        String actualDoc = actual.getDocumentation();
        if ((expectedDoc == null || expectedDoc.isEmpty()) && (actualDoc == null || actualDoc.isEmpty())) {
            // Both are effectively empty - this is fine
        } else {
            assertThat(actualDoc)
                .as("Documentation for shape %s", shapeName)
                .isEqualTo(expectedDoc);
        }

        // Compare members
        if (expected.getMembers() != null && !expected.getMembers().isEmpty()) {
            assertThat(actual.getMembers())
                .as("Members for shape %s should not be null or empty", shapeName)
                .isNotEmpty();

            assertThat(actual.getMembers())
                .as("Number of members for shape %s", shapeName)
                .hasSameSizeAs(expected.getMembers());

            // Create maps by c2jName for easier comparison
            Map<String, MemberModel> expectedMemberMap = expected.getMembers().stream()
                .collect(Collectors.toMap(MemberModel::getC2jName, m -> m));
            Map<String, MemberModel> actualMemberMap = actual.getMembers().stream()
                .collect(Collectors.toMap(MemberModel::getC2jName, m -> m));

            assertThat(actualMemberMap.keySet())
                .as("Member names for shape %s", shapeName)
                .containsExactlyInAnyOrderElementsOf(expectedMemberMap.keySet());

            // For each member, compare basic properties
            for (String memberC2jName : expectedMemberMap.keySet()) {
                assertMemberDetailsMatch(shapeName, memberC2jName,
                                       expectedMemberMap.get(memberC2jName),
                                       actualMemberMap.get(memberC2jName));
            }
        } else {
            assertThat(actual.getMembers())
                .as("Members for shape %s should be empty", shapeName)
                .isEmpty();
        }

        // TODO: Add more shape property assertions as we implement them:
        // - Required fields
        // - Deprecation
        // - Enums
        // - etc.
    }

    /**
     * Asserts that two member models match in their key properties.
     */
    private void assertMemberDetailsMatch(String shapeName, String memberName,
                                         MemberModel expected, MemberModel actual) {
        assertThat(actual)
            .as("Member %s in shape %s should exist", memberName, shapeName)
            .isNotNull();

        assertThat(actual.getName())
            .as("Member name for %s.%s", shapeName, memberName)
            .isEqualTo(expected.getName());

        assertThat(actual.getC2jName())
            .as("C2J name for %s.%s", shapeName, memberName)
            .isEqualTo(expected.getC2jName());

        assertThat(actual.getVariable().getVariableType())
            .as("Variable type for %s.%s", shapeName, memberName)
            .isEqualTo(expected.getVariable().getVariableType());

        // Compare timestamp format
        assertThat(actual.getTimestampFormat())
            .as("Timestamp format for %s.%s", shapeName, memberName)
            .isEqualTo(expected.getTimestampFormat());

        // TODO: Add more member property assertions as we implement them:
        // - HTTP mappings
        // - List/Map models
        // - Enum types
        // - Required flag
        // - Deprecation
        // - etc.
    }

    /**
     * Asserts that operations in both models match in terms of names and key properties.
     */
    private void assertOperationsMatch(IntermediateModel expected, IntermediateModel actual) {
        assertThat(actual.getOperations().keySet())
            .as("Operation names should match")
            .containsExactlyInAnyOrderElementsOf(expected.getOperations().keySet());

        // For each operation, compare the details
        for (String operationName : expected.getOperations().keySet()) {
            OperationModel expectedOp = expected.getOperation(operationName);
            OperationModel actualOp = actual.getOperation(operationName);

            assertThat(actualOp)
                .as("Operation %s should exist in both models", operationName)
                .isNotNull();

            assertOperationDetailsMatch(operationName, expectedOp, actualOp);
        }
    }

    /**
     * Asserts that two operation models match in their key properties.
     */
    private void assertOperationDetailsMatch(String operationName, OperationModel expected, OperationModel actual) {
        assertThat(actual.getOperationName())
            .as("Operation name for %s", operationName)
            .isEqualTo(expected.getOperationName());

        assertThat(actual.getDocumentation())
            .as("Documentation for operation %s", operationName)
            .isEqualTo(expected.getDocumentation());

        assertThat(actual.isDeprecated())
            .as("Deprecated flag for operation %s", operationName)
            .isEqualTo(expected.isDeprecated());

        if (expected.isDeprecated()) {
            assertThat(actual.getDeprecatedMessage())
                .as("Deprecated message for operation %s", operationName)
                .isEqualTo(expected.getDeprecatedMessage());
        }

        assertThat(actual.isPaginated())
            .as("Paginated flag for operation %s", operationName)
            .isEqualTo(expected.isPaginated());

        assertThat(actual.isEndpointOperation())
            .as("Endpoint operation flag for operation %s", operationName)
            .isEqualTo(expected.isEndpointOperation());

        assertThat(actual.isHttpChecksumRequired())
            .as("HTTP checksum required flag for operation %s", operationName)
            .isEqualTo(expected.isHttpChecksumRequired());

        assertThat(actual.isUnsignedPayload())
            .as("Unsigned payload flag for operation %s", operationName)
            .isEqualTo(expected.isUnsignedPayload());

        // Compare input
        if (expected.getInput() != null) {
            assertThat(actual.getInput())
                .as("Input for operation %s should not be null", operationName)
                .isNotNull();
            assertThat(actual.getInput().getVariableType())
                .as("Input type for operation %s", operationName)
                .isEqualTo(expected.getInput().getVariableType());
        } else {
            assertThat(actual.getInput())
                .as("Input for operation %s should be null", operationName)
                .isNull();
        }

        // Compare output/return type
        if (expected.getReturnType() != null) {
            assertThat(actual.getReturnType())
                .as("Return type for operation %s should not be null", operationName)
                .isNotNull();
            assertThat(actual.getReturnType().getReturnType())
                .as("Return type name for operation %s", operationName)
                .isEqualTo(expected.getReturnType().getReturnType());
        } else {
            assertThat(actual.getReturnType())
                .as("Return type for operation %s should be null", operationName)
                .isNull();
        }

        // Compare exceptions
        assertThat(actual.getExceptions())
            .as("Number of exceptions for operation %s", operationName)
            .hasSameSizeAs(expected.getExceptions());

        // Compare exception names as a set since order may differ between C2J and Smithy
        Set<String> expectedExceptionNames = expected.getExceptions().stream()
            .map(e -> e.getExceptionName())
            .collect(Collectors.toSet());
        Set<String> actualExceptionNames = actual.getExceptions().stream()
            .map(e -> e.getExceptionName())
            .collect(Collectors.toSet());

        assertThat(actualExceptionNames)
            .as("Exception names for operation %s", operationName)
            .isEqualTo(expectedExceptionNames);

        // Compare endpoint discovery
        if (expected.getEndpointDiscovery() != null) {
            assertThat(actual.getEndpointDiscovery())
                .as("Endpoint discovery for operation %s should not be null", operationName)
                .isNotNull();
            assertThat(actual.getEndpointDiscovery().isRequired())
                .as("Endpoint discovery required flag for operation %s", operationName)
                .isEqualTo(expected.getEndpointDiscovery().isRequired());
        } else {
            assertThat(actual.getEndpointDiscovery())
                .as("Endpoint discovery for operation %s should be null", operationName)
                .isNull();
        }

        // Compare endpoint trait
        if (expected.getEndpointTrait() != null) {
            assertThat(actual.getEndpointTrait())
                .as("Endpoint trait for operation %s should not be null", operationName)
                .isNotNull();
            assertThat(actual.getEndpointTrait().getHostPrefix())
                .as("Endpoint trait host prefix for operation %s", operationName)
                .isEqualTo(expected.getEndpointTrait().getHostPrefix());
        } else {
            assertThat(actual.getEndpointTrait())
                .as("Endpoint trait for operation %s should be null", operationName)
                .isNull();
        }

        // Compare HTTP checksum
        if (expected.getHttpChecksum() != null) {
            assertThat(actual.getHttpChecksum())
                .as("HTTP checksum for operation %s should not be null", operationName)
                .isNotNull();
            assertThat(actual.getHttpChecksum().isRequestChecksumRequired())
                .as("HTTP checksum request required for operation %s", operationName)
                .isEqualTo(expected.getHttpChecksum().isRequestChecksumRequired());
        } else {
            assertThat(actual.getHttpChecksum())
                .as("HTTP checksum for operation %s should be null", operationName)
                .isNull();
        }

        // Compare request compression
        if (expected.getRequestcompression() != null) {
            assertThat(actual.getRequestcompression())
                .as("Request compression for operation %s should not be null", operationName)
                .isNotNull();
            assertThat(actual.getRequestcompression().getEncodings())
                .as("Request compression encodings for operation %s", operationName)
                .isEqualTo(expected.getRequestcompression().getEncodings());
        } else {
            assertThat(actual.getRequestcompression())
                .as("Request compression for operation %s should be null", operationName)
                .isNull();
        }
    }

    /**
     * Asserts that the Smithy intermediate model has endpoint rule set and test suite populated
     * from the service's Smithy traits.
     */
    private void assertEndpointModelsPopulated(IntermediateModel smithyModel) {
        assertThat(smithyModel.getEndpointRuleSetModel())
            .as("Endpoint rule set model should be populated from Smithy traits")
            .isNotNull();
        assertThat(smithyModel.getEndpointRuleSetModel().getRules())
            .as("Endpoint rule set should have rules")
            .isNotEmpty();
        assertThat(smithyModel.getEndpointRuleSetModel().getParameters())
            .as("Endpoint rule set should have parameters")
            .isNotEmpty();

        assertThat(smithyModel.getEndpointTestSuiteModel())
            .as("Endpoint test suite model should be populated from Smithy traits")
            .isNotNull();
        assertThat(smithyModel.getEndpointTestSuiteModel().getTestCases())
            .as("Endpoint test suite should have test cases")
            .isNotEmpty();
    }
}

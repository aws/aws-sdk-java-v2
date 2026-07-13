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

package software.amazon.awssdk.codegen.parity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;

class IntermediateModelParityCheckerTest {

    private final IntermediateModelParityChecker checker = new IntermediateModelParityChecker();

    @Test
    void compare_whenModelsIdentical_reportsNoDiffs() {
        IntermediateModel a = simpleModel("Foo", "1.0");
        IntermediateModel b = simpleModel("Foo", "1.0");

        ParityResult result = checker.compare("foo", a, b);

        assertThat(result.allDiffs()).isEmpty();
        assertThat(result.unexpectedDiffs()).isEmpty();
    }

    @Test
    void compare_whenMetadataDiffers_reportsChangedDiff() {
        IntermediateModel a = simpleModel("Foo", "1.0");
        IntermediateModel b = simpleModel("Foo", "2.0");

        ParityResult result = checker.compare("foo", a, b);

        assertThat(result.unexpectedDiffs())
            .extracting(ParityDiff::path)
            .contains("metadata.apiVersion");
        assertThat(result.unexpectedDiffs())
            .extracting(ParityDiff::type)
            .contains(ParityDiff.Type.CHANGED);
    }

    @Test
    void compare_whenSameFieldDifferentJsonType_reportsTypeDiff() throws Exception {
        String c2jJson = "{ \"metadata\": { \"awsQueryCompatible\": {} } }";
        String smithyJson = "{ \"metadata\": { \"awsQueryCompatible\": \"true\" } }";

        com.fasterxml.jackson.databind.JsonNode c2jTree =
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(c2jJson);
        com.fasterxml.jackson.databind.JsonNode smithyTree =
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(smithyJson);

        ParityResult result = checker.compareTrees("foo", c2jTree, smithyTree, Collections.emptyList());

        assertThat(result.unexpectedDiffs())
            .anyMatch(d -> d.path().equals("metadata.awsQueryCompatible")
                           && d.type() == ParityDiff.Type.TYPE_MISMATCH);
    }

    @Test
    void compare_whenDiffHasPerServiceAllowlistEntry_ignored() {
        IntermediateModel a = simpleModel("Foo", "1.0");
        IntermediateModel b = simpleModel("Foo", "2.0");
        List<ParityAllowlistEntry> allowlist = Collections.singletonList(
            new ParityAllowlistEntry("metadata.apiVersion", "Smithy uses compact date"));

        ParityResult result = checker.compare("foo", a, b, allowlist);

        assertThat(result.unexpectedDiffs()).isEmpty();
        assertThat(result.allDiffs())
            .extracting(ParityDiff::path)
            .contains("metadata.apiVersion");
    }

    @Test
    void compare_whenAllowlistGlobPatternMatches_ignored() {
        IntermediateModel a = modelWithShapes("FooShape");
        IntermediateModel b = modelWithShapes("BarShape");

        List<ParityAllowlistEntry> allowlist = Collections.singletonList(
            new ParityAllowlistEntry("shapes.**", "all shape diffs allowed for this synthetic test"));

        ParityResult result = checker.compare("foo", a, b, allowlist);

        assertThat(result.unexpectedDiffs()).as(result.summary()).isEmpty();
        assertThat(result.allDiffs()).as(result.summary()).isNotEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidAllowlistEntries")
    void newAllowlistEntry_whenInputInvalid_throws(String label, String path, String reason, String expectedMessage) {
        assertThatThrownBy(() -> new ParityAllowlistEntry(path, reason))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(expectedMessage);
    }

    static Stream<Arguments> invalidAllowlistEntries() {
        return Stream.of(
            Arguments.of("path missing",  "",             "reason",   "path"),
            Arguments.of("reason null",   "some.path",    null,       "reason"),
            Arguments.of("reason blank",  "some.path",    "  ",       "reason")
        );
    }

    @Test
    void loadAllowlist_whenJsonValid_returnsEntries() throws Exception {
        String json = "{ \"metadata.apiVersion\": \"Smithy uses compact date\","
                      + " \"shapes.Foo.documentation\": \"docs rephrased\" }";
        List<ParityAllowlistEntry> entries =
            checker.loadAllowlist(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        assertThat(entries).hasSize(2);
        assertThat(entries).extracting(ParityAllowlistEntry::path)
            .containsExactly("metadata.apiVersion", "shapes.Foo.documentation");
    }

    @Test
    void loadAllowlist_whenReasonBlank_throws() {
        String json = "{ \"a.b\": \"\" }";
        assertThatThrownBy(() ->
            checker.loadAllowlist(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))))
            .hasMessageContaining("reason");
    }

    @Test
    void loadAllowlist_whenStreamNull_returnsEmpty() throws Exception {
        assertThat(checker.loadAllowlist(null)).isEmpty();
    }

    @Test
    void compare_whenMapEntriesInDifferentInsertionOrder_reportsNoDiffs() {
        IntermediateModel a = new IntermediateModel();
        a.setMetadata(metadata("Foo", "1.0"));
        Map<String, ShapeModel> aShapes = new HashMap<>();
        aShapes.put("Zeta", shape("Zeta"));
        aShapes.put("Alpha", shape("Alpha"));
        a.setShapes(aShapes);
        a.setCustomizationConfig(CustomizationConfig.create());

        IntermediateModel b = new IntermediateModel();
        b.setMetadata(metadata("Foo", "1.0"));
        Map<String, ShapeModel> bShapes = new HashMap<>();
        bShapes.put("Alpha", shape("Alpha"));
        bShapes.put("Zeta", shape("Zeta"));
        b.setShapes(bShapes);
        b.setCustomizationConfig(CustomizationConfig.create());

        ParityResult result = checker.compare("foo", a, b);

        assertThat(result.unexpectedDiffs())
            .as(result.summary())
            .isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("diffCases")
    void diffEngine_producesExpectedDiff(String label,
                                         IntermediateModel c2j,
                                         IntermediateModel smithy,
                                         ParityDiff.Type expectedType,
                                         String expectedPath) {
        ParityResult result = checker.compare("test", c2j, smithy);

        assertThat(result.unexpectedDiffs())
            .as(result.summary())
            .anyMatch(d -> d.path().equals(expectedPath) && d.type() == expectedType);
    }

    static Stream<Arguments> diffCases() {
        return Stream.of(
            Arguments.of("apiVersion value changed",
                         simpleModel("Foo", "1.0"), simpleModel("Foo", "2.0"),
                         ParityDiff.Type.CHANGED, "metadata.apiVersion"),
            Arguments.of("shape missing from smithy",
                         modelWithShapes("FooShape", "BarShape"), modelWithShapes("FooShape"),
                         ParityDiff.Type.MISSING, "shapes.BarShape"),
            Arguments.of("shape added in smithy",
                         modelWithShapes("FooShape"), modelWithShapes("FooShape", "ExtraShape"),
                         ParityDiff.Type.ADDED, "shapes.ExtraShape"),
            Arguments.of("shape names differ by case",
                         modelWithShapes("Foo"), modelWithShapes("foo"),
                         ParityDiff.Type.MISSING, "shapes.Foo"),
            Arguments.of("trailing space in value",
                         simpleModel("Foo", "1.0"), simpleModel("Foo", "1.0 "),
                         ParityDiff.Type.CHANGED, "metadata.apiVersion"),
            Arguments.of("internal space in value",
                         simpleModel("Foo", "1.0"), simpleModel("Foo", "1. 0"),
                         ParityDiff.Type.CHANGED, "metadata.apiVersion"),
            Arguments.of("empty string vs non-empty",
                         simpleModel("Foo", ""), simpleModel("Foo", "1.0"),
                         ParityDiff.Type.CHANGED, "metadata.apiVersion")
        );
    }

    @Test
    void result_summary_includesServiceAndDiffCount() {
        IntermediateModel a = simpleModel("Foo", "1.0");
        IntermediateModel b = simpleModel("Foo", "2.0");

        ParityResult result = checker.compare("foo", a, b);

        assertThat(result.summary())
            .contains("foo")
            .contains("unexpected");
    }

    private static IntermediateModel simpleModel(String serviceName, String apiVersion) {
        IntermediateModel model = new IntermediateModel();
        model.setMetadata(metadata(serviceName, apiVersion));
        model.setShapes(Collections.emptyMap());
        model.setOperations(Collections.emptyMap());
        model.setCustomizationConfig(CustomizationConfig.create());
        return model;
    }

    private static IntermediateModel modelWithShapes(String... shapeNames) {
        IntermediateModel model = simpleModel("Foo", "1.0");
        Map<String, ShapeModel> shapes = new HashMap<>();
        for (String name : shapeNames) {
            shapes.put(name, shape(name));
        }
        model.setShapes(shapes);
        return model;
    }

    private static Metadata metadata(String serviceName, String apiVersion) {
        Metadata m = new Metadata();
        m.setServiceName(serviceName);
        m.setApiVersion(apiVersion);
        return m;
    }

    private static ShapeModel shape(String name) {
        ShapeModel shape = new ShapeModel(name);
        shape.setShapeName(name);
        shape.setType("Structure");
        return shape;
    }
}

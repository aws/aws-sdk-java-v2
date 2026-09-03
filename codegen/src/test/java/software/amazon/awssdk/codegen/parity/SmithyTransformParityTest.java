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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;

/**
 * Measures what declarative Smithy transforms carry on the Smithy codegen path.
 *
 * <p>A straight C2J-versus-Smithy diff cannot answer that question, because nine of the sixteen C2J
 * customization processors run unconditionally and so the two paths differ for reasons unrelated to
 * the customizations under test. This compares three models instead:
 *
 * <ul>
 *   <li>{@code c2j} — the C2J path with its real customization.config and full processor chain.</li>
 *   <li>{@code untransformed} — the Smithy path with no transforms.</li>
 *   <li>{@code transformed} — the Smithy path with the transforms declared beside the model.</li>
 * </ul>
 *
 * <p>The difference between the second and third is exactly what the transforms accomplished, with
 * none of the unconditional-processor noise. The difference between the first and third is the
 * residual gap.
 */
class SmithyTransformParityTest {

    private static final String SERVICES = "marketplacecommerceanalytics, elasticloadbalancing";

    private final IntermediateModelParityChecker checker = new IntermediateModelParityChecker();

    /**
     * The transforms must achieve what the service's own {@code customization.config} asks for. Both
     * POC services use {@code renameShapes} and nothing else of the five in-scope settings, so the
     * config's rename map is the specification the transform is held to.
     */
    @ParameterizedTest(name = "renameShapes lands for {0}")
    @ValueSource(strings = {"marketplacecommerceanalytics", "elasticloadbalancing"})
    void declaredRenames_areAppliedToTheSmithyModel(String service) {
        Map<String, String> renames = Fixtures.loadCustomization(service).getRenameShapes();
        assertThat(renames).as("POC service is expected to use renameShapes").isNotEmpty();

        Set<String> before = c2jNames(Fixtures.buildFromSmithy(service, false));
        Set<String> after = c2jNames(Fixtures.buildFromSmithy(service, true));
        Set<String> c2j = c2jNames(Fixtures.buildFromC2j(service));

        assertThat(before).as("old names present before transforms").containsAll(renames.keySet());
        assertThat(before).as("new names absent before transforms").doesNotContainAnyElementsOf(renames.values());

        assertThat(after).as("new names present after transforms").containsAll(renames.values());
        assertThat(after).as("old names gone after transforms").doesNotContainAnyElementsOf(renames.keySet());

        assertThat(c2j).as("C2J agrees on the new names").containsAll(renames.values());
    }

    /**
     * Renaming a shape is only useful if every reference to it moves too. C2J hand-walks list members,
     * map keys and values, structure members, operation input, output and error entries to do this.
     * Smithy's {@code renameShapes} claims to do it as a side effect.
     *
     * <p>Rather than enumerate reference sites, this asserts the old name appears nowhere in the diff
     * against C2J. A reference left pointing at the old name would surface there, because C2J carries
     * the new name everywhere.
     */
    @ParameterizedTest(name = "references are rewired for {0}")
    @ValueSource(strings = {"marketplacecommerceanalytics", "elasticloadbalancing"})
    void renamedShapeReferences_areRewiredWithoutHandWrittenWalking(String service) {
        Map<String, String> renames = Fixtures.loadCustomization(service).getRenameShapes();

        ParityResult residual = checker.compare(service,
                                                Fixtures.buildFromC2j(service),
                                                Fixtures.buildFromSmithy(service, true));

        List<String> survivingOldNames =
            residual.allDiffs().stream()
                    .filter(d -> mentionsAny(d.smithyValue(), renames.keySet()))
                    .map(Object::toString)
                    .collect(Collectors.toList());

        assertThat(survivingOldNames)
            .as("no reference to a renamed shape's old name should survive on the Smithy side")
            .isEmpty();
    }

    /**
     * Guards the wiring hazard. {@code SmithyIntermediateModelBuilder} derives its naming strategy in
     * its constructor from the model it is handed, so a model customized any later would leave the
     * naming strategy reading the uncustomized model. The generated Java class name is produced by
     * that naming strategy, so if it carries the new name, the transform demonstrably ran before
     * construction.
     */
    @ParameterizedTest(name = "naming strategy sees the transformed model for {0}")
    @ValueSource(strings = {"marketplacecommerceanalytics", "elasticloadbalancing"})
    void namingStrategy_seesTheTransformedModel(String service) {
        Map<String, String> renames = Fixtures.loadCustomization(service).getRenameShapes();
        IntermediateModel transformed = Fixtures.buildFromSmithy(service, true);

        Set<String> javaClassNames = transformed.getShapes().values().stream()
                                                .map(ShapeModel::getShapeName)
                                                .collect(Collectors.toSet());

        assertThat(javaClassNames).as("new names reached the naming strategy").containsAll(renames.values());
        assertThat(javaClassNames).as("no old name reached the naming strategy")
                                  .doesNotContainAnyElementsOf(renames.keySet());
    }

    /**
     * The measurement. Raw diff counts are not usable here: the checker pairs array elements by index
     * after sorting them by content key, so renaming a shape re-sorts every {@code exceptions} array
     * that mentions it and reports a cascade of CHANGED entries that are index shifts rather than
     * content differences. Renaming also converts a whole-shape MISSING/ADDED pair into a key match,
     * which exposes intra-shape diffs that were previously collapsed into one line. Both effects move
     * the raw total in directions unrelated to whether the customization landed.
     *
     * <p>So the measurement counts only diffs attributable to the renamed names. Those must be present
     * before the transforms and gone afterwards.
     */
    @ParameterizedTest(name = "transforms close the rename-attributable gap for {0}")
    @ValueSource(strings = {"marketplacecommerceanalytics", "elasticloadbalancing"})
    void transforms_closeTheRenameAttributableGap(String service) {
        Map<String, String> renames = Fixtures.loadCustomization(service).getRenameShapes();

        IntermediateModel c2j = Fixtures.buildFromC2j(service);
        IntermediateModel untransformed = Fixtures.buildFromSmithy(service, false);
        IntermediateModel transformed = Fixtures.buildFromSmithy(service, true);

        ParityResult baseline = checker.compare(service, c2j, untransformed);
        ParityResult residual = checker.compare(service, c2j, transformed);
        ParityResult effect = checker.compare(service, untransformed, transformed);

        long staleBefore = oldNameOccurrences(baseline, renames.keySet());
        long staleAfter = oldNameOccurrences(residual, renames.keySet());

        System.out.printf("%n=== transform measurement: %s (POC services: %s) ===%n", service, SERVICES);
        System.out.printf("  renames declared                    : %d%n", renames.size());
        System.out.printf("  diffs carrying a pre-rename name, before : %d%n", staleBefore);
        System.out.printf("  diffs carrying a pre-rename name, after  : %d%n", staleAfter);
        System.out.printf("  raw C2J vs Smithy, no transforms    : %d  (baseline noise: unconditional "
                          + "processors + translation gaps)%n", baseline.allDiffs().size());
        System.out.printf("  raw C2J vs Smithy, transformed      : %d%n", residual.allDiffs().size());
        System.out.printf("  raw untransformed vs transformed    : %d  (mostly index shifts, see javadoc)%n",
                          effect.allDiffs().size());
        System.out.printf("  residual under renamed shapes       : %d  (deferred RemoveExceptionMessageProperty, "
                          + "not rename-related)%n", residualUnderRenamedShapes(residual, renames.values()));

        assertThat(staleBefore).as("the customization is genuinely missing before transforms").isGreaterThan(0);
        assertThat(staleAfter).as("no pre-rename name survives anywhere after transforms").isZero();
    }

    /**
     * Counts diffs mentioning a pre-rename name anywhere. C2J never carries an old name, having renamed
     * it, so any occurrence is a rename the Smithy side failed to apply. Zero is the target.
     *
     * <p>Deliberately not matched on the post-rename name: once a rename succeeds, the renamed shape's
     * subtree is compared key-to-key, and unrelated baseline diffs inside it sit under a path that
     * contains the new name. Counting those would penalise a rename for working.
     */
    private static long oldNameOccurrences(ParityResult result, Set<String> oldNames) {
        return result.allDiffs().stream()
                     .filter(d -> mentionsAny(d.path(), oldNames)
                                  || mentionsAny(d.c2jValue(), oldNames)
                                  || mentionsAny(d.smithyValue(), oldNames))
                     .count();
    }

    private static long residualUnderRenamedShapes(ParityResult result, java.util.Collection<String> newNames) {
        return result.allDiffs().stream()
                     .filter(d -> newNames.stream().anyMatch(n -> d.path().startsWith("shapes." + n + ".")))
                     .count();
    }

    private static Set<String> c2jNames(IntermediateModel model) {
        return model.getShapes().values().stream()
                    .map(ShapeModel::getC2jName)
                    .collect(Collectors.toSet());
    }

    private static boolean mentionsAny(String value, Set<String> names) {
        if (value == null) {
            return false;
        }
        return names.stream().anyMatch(value::contains);
    }
}

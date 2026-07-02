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

package software.amazon.awssdk.codegen.poet.crac;

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.utils.Logger;

/**
 * Selects which service operation a generated CRaC warm-up provider should invoke for its synthetic priming call.
 *
 * <p>The selection is a pure, deterministic function of the {@link IntermediateModel}. It prefers an operation whose
 * synthetic {@code Request.builder().build()} marshals, signs and unmarshals cleanly (so the warm-up call does not throw)
 * and that exercises the canonical read path of the service (e.g. {@code s3 -> ListBuckets},
 * {@code dynamodb -> ListTables}).
 */
@SdkInternalApi
public final class WarmUpOperationSelector {

    /**
     * Sentinel value for {@code warmUpOperation} customization that disables warm-up for a service entirely.
     */
    private static final String DISABLE_WARM_UP = "NONE";

    /**
     * Read-only verbs preferred for the synthetic warm-up call, in priority order. Lower index = more preferred.
     * Modeled after the named priority lists already used in codegen (e.g. {@code HttpChecksumTrait}'s checksum
     * algorithm priority) so that the ranking carries no magic numbers.
     */
    private static final List<String> PREFERRED_VERBS = Arrays.asList("List", "Describe", "Get");

    // === To change warm-up operation selection, edit THIS list. Order = priority; earlier wins. ===
    // Each comparator sorts the preferred candidate first ("false sorts before true", "fewer sorts before more").
    // Clauses 1-3 are HARD gates: an operation flagged by any of them cannot be warmed up at all, and
    // isWarmUpSafe() rejects it so the provider falls back to a no-op (a fully-deprecated service warms nothing).
    // Clause 4 prefers operations the SDK has already hand-vetted as safe to call with an empty request (the
    // "simple methods" that get the no-arg client overload, e.g. client.listTables()). Clauses 5-9 steer to the
    // canonical API (e.g. s3->ListBuckets, dynamodb->ListTables). Preferences never override the hard gates.
    private static final List<Comparator<OperationModel>> SELECTION_RULES = Arrays.asList(
        comparing(WarmUpOperationSelector::failsClientSideRequestValidation), // 1. avoid URI-path / host-prefix
        comparing(WarmUpOperationSelector::isStreaming),            // 2. avoid streaming / event-stream
        comparing(OperationModel::isDeprecated),                    // 3. avoid deprecated
        comparing(WarmUpOperationSelector::isNotSimpleMethod),      // 4. prefer a hand-vetted no-arg "simple method"
        comparingInt(WarmUpOperationSelector::requiredMemberCount), // 5. fewest required members (simplest request)
        comparing(WarmUpOperationSelector::hasNoOutput),            // 6. prefer an output to unmarshal
        comparingInt(WarmUpOperationSelector::verbRank),            // 7. prefer list/describe/get
        comparingInt(op -> op.getOperationName().length()),         // 8. prefer the primary (shortest) collection
        comparing(OperationModel::getOperationName)                 // 9. deterministic tie-break
    );

    private static final Comparator<OperationModel> WARMUP_PREFERENCE =
        SELECTION_RULES.stream()
                       .reduce(Comparator::thenComparing)
                       .orElseThrow(() -> new IllegalStateException("No warm-up selection rules defined"));

    private static final Logger log = Logger.loggerFor(WarmUpOperationSelector.class);

    private WarmUpOperationSelector() {
    }

    /**
     * Selects the operation a service's generated warm-up provider should invoke for its synthetic priming call.
     *
     * <p>An explicit {@code warmUpOperation} customization takes precedence over the algorithm: the literal value
     * {@code "NONE"} disables warm-up, and any other value pins that operation when it exists. When no override applies,
     * the operation that sorts first under the ordered {@code SELECTION_RULES} comparator is chosen.
     *
     * <p>The result is fully deterministic for a given model snapshot: {@link IntermediateModel#getOperations()} is a
     * name-sorted {@code TreeMap} and the final selection rule breaks ties on the operation name, so the same model
     * always yields the same operation across regenerations.
     *
     * @param model the intermediate model of the service being generated.
     * @return the operation to warm up, or {@link Optional#empty()} when warm-up is disabled via
     *         {@code warmUpOperation: "NONE"}, or when the service models no operations at all.
     */
    public static Optional<OperationModel> selectWarmUpOperation(IntermediateModel model) {
        String override = model.getCustomizationConfig().getWarmUpOperation();

        // 1. Explicit override / disable (escape hatch).
        if (DISABLE_WARM_UP.equals(override)) {
            return Optional.empty();
        }
        if (override != null) {
            if (model.getOperations().containsKey(override)) {
                return Optional.of(model.getOperations().get(override));
            }
            log.warn(() -> "Configured warmUpOperation '" + override + "' is not an operation of service '"
                           + model.getMetadata().getServiceName() + "'. Falling back to automatic selection.");
        }

        // 2. Otherwise pick the best candidate by the ordered rule list above. getOperations() is a sorted TreeMap and
        //    the last rule is the operation name, so the result is fully deterministic across regenerations.
        return model.getOperations().values().stream().min(WARMUP_PREFERENCE);
    }

    /**
     * Whether a synthetic {@code Request.builder().build()} call against the given operation can be safely generated for
     * the warm-up provider in this version of the SDK.
     *
     * <p>An operation is <em>not</em> safe to warm up when:
     * <ul>
     *     <li>it would throw at marshalling (a required member bound to the URI path, or a host-prefix label);</li>
     *     <li>it streams its input or output (the generated method has a different signature that cannot be invoked
     *     with an empty request);</li>
     *     <li>it is deprecated (there is no value in priming deprecated code; a fully-deprecated service therefore
     *     warms nothing and its provider is a no-op).</li>
     * </ul>
     *
     * <p>{@link #selectWarmUpOperation(IntermediateModel)} still returns the best candidate even when every operation is
     * unsafe, so the safety decision lives here, in one place, and the caller emits a no-op provider when it returns
     * {@code false}. A future version may inject placeholder values for the hazardous members instead of skipping.
     *
     * @param op the operation chosen by {@link #selectWarmUpOperation(IntermediateModel)}.
     * @return {@code true} when a synthetic warm-up call can be generated for the operation.
     */
    static boolean isWarmUpSafe(OperationModel op) {
        return !failsClientSideRequestValidation(op) && !isStreaming(op) && !op.isDeprecated();
    }

    // --- Rule predicates (referenced by SELECTION_RULES above). Each is small, named, and unit-testable. ---
    // "false sorts before true", so each boolean predicate names the thing we want to AVOID.

    // A member bound to the URI path (its marshaller throws on a null value) or a host-prefix label (validated
    // against a hostname pattern while the request is built). An empty request leaves these null, so building it
    // fails client-side before any call is made.
    private static boolean failsClientSideRequestValidation(OperationModel op) {
        return hasRequiredPathMember(op) || hasHostPrefix(op);
    }

    private static boolean hasRequiredPathMember(OperationModel op) {
        ShapeModel in = op.getInputShape();
        if (in == null || in.getRequired() == null) {
            return false;
        }
        return in.getRequired().stream()
                 .map(name -> memberByC2jName(in, name))
                 .anyMatch(m -> m != null && m.getHttp() != null && m.getHttp().isUri());
    }

    private static boolean hasHostPrefix(OperationModel op) {
        return op.getEndpointTrait() != null && op.getEndpointTrait().getHostPrefix() != null;
    }

    // Both raw streaming (blob payload) and event streams: neither can be exercised with an empty Request.builder()
    // through the synthetic sync call, and the generated event-stream method has a different signature
    // (it takes a request handler / publisher), so it must not be selected.
    private static boolean isStreaming(OperationModel op) {
        return op.hasStreamingInput() || op.hasStreamingOutput()
               || op.hasEventStreamInput() || op.hasEventStreamOutput();
    }

    // A "simple method" is an operation the SDK has hand-vetted (via the service's verifiedSimpleMethods
    // customization) as callable with an empty request; it is exactly the set that gets the no-arg client overload
    // such as client.listTables(). Preferring these picks an operation a human already confirmed is safe and
    // representative. isSimpleMethod() is precomputed on the input shape by IntermediateModelBuilder.setSimpleMethods().
    private static boolean isNotSimpleMethod(OperationModel op) {
        ShapeModel in = op.getInputShape();
        return in == null || !in.isSimpleMethod();
    }

    private static int requiredMemberCount(OperationModel op) {
        ShapeModel in = op.getInputShape();
        return in == null || in.getRequired() == null ? 0 : in.getRequired().size();
    }

    private static boolean hasNoOutput(OperationModel op) {
        return op.getOutputShape() == null;
    }

    // Prefer read-only list/describe/get verbs (the canonical warm-up ops). Lower = preferred.
    private static int verbRank(OperationModel op) {
        String name = op.getOperationName();
        for (int i = 0; i < PREFERRED_VERBS.size(); i++) {
            if (name.startsWith(PREFERRED_VERBS.get(i))) {
                return i;
            }
        }
        return PREFERRED_VERBS.size();
    }

    private static MemberModel memberByC2jName(ShapeModel shape, String c2jName) {
        return shape.getMembers().stream()
                    .filter(m -> c2jName.equals(m.getC2jName()))
                    .findFirst()
                    .orElse(null);
    }
}

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

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.NumericUtils;

/**
 * Selects the operation used for the CRaC warm-up call: filters out streaming/event-stream and deprecated
 * operations, plus operations that cannot be dummy-filled (see {@link #membersRequiringDummyValue}), then ranks the
 * rest (see {@link #warmUpPreference}).
 */
public final class WarmUpOperationSelector {

    private static final List<String> PREFERRED_VERBS = Arrays.asList("List", "Describe", "Get");

    private static final Comparator<OperationModel> BY_HAS_OUTPUT_FIRST =
        Comparator.comparing(op -> hasOutput(op) ? 0 : 1);

    private static final Comparator<OperationModel> BY_AUTHENTICATED_FIRST =
        Comparator.comparing(op -> op.isAuthenticated() ? 0 : 1);

    private static final Comparator<OperationModel> BY_EMPTY_REQUEST_FIRST =
        Comparator.comparing(op -> acceptsEmptyRequest(op) ? 0 : 1);

    private static final Comparator<OperationModel> BY_FEWEST_REQUIRED_INPUTS =
        Comparator.comparingInt(WarmUpOperationSelector::requiredInputMemberCount);

    private static final Comparator<OperationModel> BY_PREFERRED_VERB =
        Comparator.comparingInt(WarmUpOperationSelector::verbRank);

    private static final Comparator<OperationModel> BY_NAME_ALPHABETICAL =
        Comparator.comparing(OperationModel::getOperationName);

    private WarmUpOperationSelector() {
    }

    /**
     * Selects the warm-up operation for the given service, or {@link Optional#empty()} if no operation is safe to
     * call as a warm-up.
     */
    public static Optional<OperationModel> selectWarmUpOperation(IntermediateModel model) {
        List<String> verifiedSimpleMethods = model.getCustomizationConfig().getVerifiedSimpleMethods();
        Comparator<OperationModel> preference = warmUpPreference(verifiedSimpleMethods);

        return model.getOperations().values().stream()
                    .filter(WarmUpOperationSelector::passesHardGates)
                    .min(preference);
    }

    /**
     * Required members the warm-up call must populate: those bound to the URI path or an endpoint context param,
     * which reject null. Other members stay null.
     */
    static List<MemberModel> membersRequiringDummyValue(OperationModel operation) {
        return inputMembers(operation).stream()
                                      .filter(MemberModel::isRequired)
                                      .filter(WarmUpOperationSelector::isUriOrEndpointBound)
                                      .collect(Collectors.toList());
    }

    /**
     * Preference order: returns output (so the unmarshaller is primed too), is authenticated (so signing is primed
     * too; {@code noAuth} operations skip signing entirely), verified simple method, accepts an empty request,
     * fewest required input members, read-only verb, then operation name as the deterministic tie-break.
     */
    private static Comparator<OperationModel> warmUpPreference(List<String> verifiedSimpleMethods) {
        return BY_HAS_OUTPUT_FIRST
            .thenComparing(BY_AUTHENTICATED_FIRST)
            .thenComparing(byVerifiedSimpleFirst(verifiedSimpleMethods))
            .thenComparing(BY_EMPTY_REQUEST_FIRST)
            .thenComparing(BY_FEWEST_REQUIRED_INPUTS)
            .thenComparing(BY_PREFERRED_VERB)
            .thenComparing(BY_NAME_ALPHABETICAL);
    }

    /**
     * Unlike the other tiers, this one depends on the service's customization config, so it cannot be a static
     * comparator constant.
     */
    private static Comparator<OperationModel> byVerifiedSimpleFirst(List<String> verifiedSimpleMethods) {
        return Comparator.comparing(op -> verifiedSimpleMethods.contains(op.getMethodName()) ? 0 : 1);
    }

    private static boolean passesHardGates(OperationModel operation) {
        return !isStreamingOrEventStream(operation)
               && !operation.isDeprecated()
               && allDummyMembersAreFillable(operation);
    }

    /**
     * A dummy member is fillable if it is a string. The warm-up call emits {@code "warmup"} for a plain member and an
     * ARN-shaped value for an ARN member; see {@link #isArnMember}.
     */
    private static boolean allDummyMembersAreFillable(OperationModel operation) {
        return membersRequiringDummyValue(operation).stream()
                                                    .allMatch(WarmUpOperationSelector::isDummyFillable);
    }

    private static boolean isDummyFillable(MemberModel member) {
        return "String".equals(member.getVariable().getSimpleType());
    }

    /**
     * An ARN endpoint context param needs an ARN-shaped dummy, since the endpoint rules parse it as an ARN. Identified
     * by the conventional capitalized {@code Arn}/{@code ARN} name suffix (e.g. {@code resourceArn}), so a name that
     * merely contains those letters (e.g. {@code learn}) does not match.
     */
    static boolean isArnMember(MemberModel member) {
        String name = member.getName();
        return member.getContextParam() != null
               && (name.endsWith("Arn") || name.endsWith("ARN"));
    }

    private static boolean isUriOrEndpointBound(MemberModel member) {
        return (member.getHttp() != null && member.getHttp().isUri())
               || member.getContextParam() != null;
    }

    private static boolean isStreamingOrEventStream(OperationModel operation) {
        return operation.isStreaming() || operation.hasEventStreamInput() || operation.hasEventStreamOutput();
    }

    private static boolean acceptsEmptyRequest(OperationModel operation) {
        return requiredInputMemberCount(operation) == 0;
    }

    private static boolean hasOutput(OperationModel operation) {
        return operation.getOutputShape() != null;
    }

    private static int requiredInputMemberCount(OperationModel operation) {
        return NumericUtils.saturatedCast(inputMembers(operation).stream().filter(MemberModel::isRequired).count());
    }

    private static List<MemberModel> inputMembers(OperationModel operation) {
        ShapeModel inputShape = operation.getInputShape();
        if (inputShape == null || CollectionUtils.isNullOrEmpty(inputShape.getMembers())) {
            return Collections.emptyList();
        }
        return inputShape.getMembers();
    }

    private static int verbRank(OperationModel operation) {
        String operationName = operation.getOperationName();
        for (int i = 0; i < PREFERRED_VERBS.size(); i++) {
            if (operationName.startsWith(PREFERRED_VERBS.get(i))) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }
}

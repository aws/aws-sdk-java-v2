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

package software.amazon.awssdk.policybuilder.iam;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.policybuilder.iam.internal.DefaultIamPrincipal;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The {@code Principal} element of a {@link IamStatement}, specifying who the statement should apply to.
 *
 * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_principal.html">Principal
 * user guide</a>
 */
@SdkPublicApi
@ThreadSafe
public interface IamPrincipal extends ToCopyableBuilder<IamPrincipal.Builder, IamPrincipal> {
    /**
     * An {@link IamPrincipal} representing ALL principals. When used on a statement, it means the policy should apply to
     * everyone.
     */
    IamPrincipal ALL = create("*", "*");

    /**
     * Create an {@link IamPrincipal} of the supplied type and ID (see {@link Builder#type(IamPrincipalType)} and
     * {@link Builder#id(String)}).
     * <p>
     * Both type and ID are required. This is equivalent to {@code IamPrincipal.builder().type(principalType).id(principalId)
     * .build()}.
     */
    static IamPrincipal create(IamPrincipalType principalType, String principalId) {
        return builder().type(principalType).id(principalId).build();
    }

    /**
     * Create an {@link IamPrincipal} of the supplied type and ID (see {@link Builder#type(String)} and
     * {@link Builder#id(String)}).
     * <p>
     * Both type and ID are required. This is equivalent to {@link #create(IamPrincipalType, String)}, except you do not need
     * to call {@code IamPrincipalType.create()}.
     */
    static IamPrincipal create(String principalType, String principalId) {
        return builder().type(principalType).id(principalId).build();
    }

    /**
     * Create multiple {@link IamPrincipal}s with the same {@link IamPrincipalType} and different IDs (see
     * {@link Builder#type(IamPrincipalType)} and {@link Builder#id(String)}).
     * <p>
     * Type is required, and the IDs in the IDs list must not be null. This is equivalent to calling
     * {@link #create(IamPrincipalType, String)} multiple times and collecting the results into a list.
     */
    static List<IamPrincipal> createAll(IamPrincipalType principalType, Collection<String> principalIds) {
        Validate.paramNotNull(principalType, "principalType");
        if (principalIds == null) {
            return emptyList();
        }
        return principalIds.stream()
                           .map(principalId -> create(principalType, principalId))
                           .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Create multiple {@link IamPrincipal}s with the same {@link IamPrincipalType} and different IDs (see
     * {@link Builder#type(String)} and {@link Builder#id(String)}).
     * <p>
     * Type is required, and the IDs in the IDs list must not be null. This is equivalent to calling
     * {@link #create(String, String)} multiple times and collecting the results into a list.
     */
    static List<IamPrincipal> createAll(String principalType, Collection<String> principalIds) {
        Validate.paramNotNull(principalType, "principalType");
        if (principalIds == null) {
            return emptyList();
        }
        return principalIds.stream()
                           .map(principalId -> create(principalType, principalId))
                           .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Create a {@link IamStatement.Builder} for an {@code IamPrincipal}.
     */
    static Builder builder() {
        return DefaultIamPrincipal.builder();
    }

    /**
     * Retrieve the value set by {@link Builder#type(IamPrincipalType)}.
     */
    IamPrincipalType type();

    /**
     * Retrieve the value set by {@link Builder#id(String)}.
     */
    String id();

    /**
     * @see #builder()
     */
    interface Builder extends CopyableBuilder<Builder, IamPrincipal> {
        /**
         * Set the {@link IamPrincipalType} associated with this principal.
         * <p>
         * This value is required.
         *
         * @see IamPrincipalType
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_principal.html">Principal
         * user guide</a>
         */
        Builder type(IamPrincipalType type);

        /**
         * Set the {@link IamPrincipalType} associated with this principal.
         * <p>
         * This is the same as {@link #type(IamPrincipalType)}, except you do not need to call {@code IamPrincipalType.create()}.
         * This value is required.
         *
         * @see IamPrincipalType
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_principal.html">Principal
         * user guide</a>
         */
        Builder type(String type);

        /**
         * Set the identifier of the principal.
         * <p>
         * The identifiers that can be used depend on the {@link #type(IamPrincipalType)} of the principal.
         *
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_principal.html">Principal
         * user guide</a>
         */
        Builder id(String id);
    }
}

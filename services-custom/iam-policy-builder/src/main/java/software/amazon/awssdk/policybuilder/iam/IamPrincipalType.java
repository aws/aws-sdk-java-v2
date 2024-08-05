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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.policybuilder.iam.internal.DefaultIamPrincipalType;

/**
 * The {@code IamPrincipalType} identifies what type of entity that the {@link IamPrincipal} refers to.
 *
 * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_principal.html">Principal
 * user guide</a>
 */
@SdkPublicApi
@ThreadSafe
public interface IamPrincipalType extends IamValue {
    /**
     * An {@code AWS} principal.
     * <p>
     * For example, this includes AWS accounts, IAM users, IAM roles, IAM role sessions or STS federated users.
     *
     * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_principal.html">Principal
     * user guide</a>
     */
    IamPrincipalType AWS = create("AWS");

    /**
     * A {@code Federated} principal.
     * <p>
     * This grants an external web identity, SAML identity provider, etc. permission to perform actions on your resources. For
     * example, cognito-identity.amazonaws.com or www.amazon.com.
     *
     * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_principal.html">Principal
     * user guide</a>
     */
    IamPrincipalType FEDERATED = create("Federated");

    /**
     * A {@code Service} principal.
     * <p>
     * This grants other AWS services permissions to perform actions on your resources. Identifiers are usually in the format
     * service-name.amazonaws.com. For example, ecs.amazonaws.com or lambda.amazonaws.com.
     *
     * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_principal.html">Principal
     * user guide</a>
     */
    IamPrincipalType SERVICE = create("Service");

    /**
     * A {@code CanonicalUser} principal.
     * <p>
     * Some services support a canonical user ID to identify your account without requiring your account ID to be shared. Such
     * identifiers are often a 64-digit alphanumeric value.
     *
     * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_principal.html">Principal
     * user guide</a>
     */
    IamPrincipalType CANONICAL_USER = create("CanonicalUser");

    /**
     * Create a new {@code IamPrincipalType} element with the provided {@link #value()}.
     */
    static IamPrincipalType create(String value) {
        return new DefaultIamPrincipalType(value);
    }
}
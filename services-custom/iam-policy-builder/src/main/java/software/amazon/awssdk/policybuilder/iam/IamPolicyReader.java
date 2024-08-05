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

import java.io.InputStream;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.policybuilder.iam.internal.DefaultIamPolicyReader;

/**
 * The {@link IamPolicyReader} converts a JSON policy into an {@link IamPolicy}.
 *
 * <h2>Usage Examples</h2>
 * <b>Log the number of statements in a policy downloaded from IAM.</b>
 * {@snippet :
 * // IamClient requires a dependency on software.amazon.awssdk:iam
 * try (IamClient iam = IamClient.builder().region(Region.AWS_GLOBAL).build()) {
 *     String policyArn = "arn:aws:iam::123456789012:policy/AllowWriteBookMetadata";
 *     GetPolicyResponse getPolicyResponse = iam.getPolicy(r -> r.policyArn(policyArn));
 *
 *     String policyVersion = getPolicyResponse.defaultVersionId();
 *     GetPolicyVersionResponse getPolicyVersionResponse =
 *         iam.getPolicyVersion(r -> r.policyArn(policyArn).versionId(policyVersion));
 *
 *     IamPolicy policy = IamPolicyReader.create().read(getPolicyVersionResponse.policyVersion().document());
 *
 *     System.out.println("Number of statements in the " + policyArn + ": " + policy.statements().size());
 * }
 * }
 *
 * @see IamPolicy#fromJson(String)
 */
@SdkPublicApi
@ThreadSafe
public interface IamPolicyReader {
    /**
     * Create a new {@link IamPolicyReader}.
     * <p>
     * This method is inexpensive, allowing the creation of readers wherever they are needed.
     */
    static IamPolicyReader create() {
        return new DefaultIamPolicyReader();
    }

    /**
     * Read a policy from a {@link String}.
     * <p>
     * This only performs minimal validation on the provided policy.
     *
     * @throws RuntimeException If the provided policy is not valid JSON or is missing a minimal set of required fields.
     */
    IamPolicy read(String policy);

    /**
     * Read a policy from an {@link InputStream}.
     * <p>
     * The stream must provide a UTF-8 encoded string representing the policy. This only performs minimal validation on the
     * provided policy.
     *
     * @throws RuntimeException If the provided policy is not valid JSON or is missing a minimal set of required fields.
     */
    IamPolicy read(InputStream policy);

    /**
     * Read a policy from a {@code byte} array.
     * <p>
     * The stream must provide a UTF-8 encoded string representing the policy. This only performs minimal validation on the
     * provided policy.
     *
     * @throws RuntimeException If the provided policy is not valid JSON or is missing a minimal set of required fields.
     */
    IamPolicy read(byte[] policy);
}

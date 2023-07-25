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
import software.amazon.awssdk.policybuilder.iam.internal.DefaultIamPolicyWriter;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The {@link IamPolicyReader} converts an {@link IamPolicy} into JSON.
 *
 * <h2>Usage Examples</h2>
 * <b>Create a new IAM identity policy that allows a role to write items to an Amazon DynamoDB table.</b>
 * {@snippet :
 * // IamClient requires a dependency on software.amazon.awssdk:iam
 * try (IamClient iam = IamClient.builder().region(Region.AWS_GLOBAL).build()) {
 *     IamPolicy policy =
 *         IamPolicy.builder()
 *                  .addStatement(IamStatement.builder()
 *                                            .effect(IamEffect.ALLOW)
 *                                            .addAction("dynamodb:PutItem")
 *                                            .addResource("arn:aws:dynamodb:us-east-2:123456789012:table/books")
 *                                            .build())
 *                  .build();
 *
 *     IamPolicyWriter writer = IamPolicyWriter.create();
 *     iam.createPolicy(r -> r.policyName("AllowWriteBookMetadata")
 *                            .policyDocument(writer.writeToString(policy)));
 * }
 * }
 *
 * <b>Create and use a writer that pretty-prints the IAM policy JSON:</b>
 * {@snippet :
 * IamPolicyWriter prettyWriter =
 *     IamPolicyWriter.builder()
 *                    .prettyPrint(true)
 *                    .build();
 * IamPolicy policy =
 *     IamPolicy.builder()
 *              .addStatement(IamStatement.builder()
 *                                        .effect(IamEffect.ALLOW)
 *                                        .addAction("dynamodb:PutItem")
 *                                        .addResource("arn:aws:dynamodb:us-east-2:123456789012:table/books")
 *                                        .build())
 *              .build();
 * System.out.println("Policy:\n" + policy.toJson(prettyWriter));
 * }
 *
 * @see IamPolicy#toJson()
 * @see IamPolicy#toJson(IamPolicyWriter)
 */
@SdkPublicApi
@ThreadSafe
public interface IamPolicyWriter extends ToCopyableBuilder<IamPolicyWriter.Builder, IamPolicyWriter> {
    /**
     * Create a new {@link IamPolicyReader}.
     * <p>
     * This method is inexpensive, allowing the creation of writers wherever they are needed.
     */
    static IamPolicyWriter create() {
        return DefaultIamPolicyWriter.create();
    }

    /**
     * Create a {@link Builder} for an {@code IamPolicyWriter}.
     */
    static Builder builder() {
        return DefaultIamPolicyWriter.builder();
    }

    /**
     * Write a policy to a {@link String}.
     * <p>
     * This does not validate that the provided policy is correct or valid.
     */
    String writeToString(IamPolicy policy);

    /**
     * Write a policy to a {@code byte} array.
     * <p>
     * This does not validate that the provided policy is correct or valid.
     */
    byte[] writeToBytes(IamPolicy policy);

    /**
     * @see #builder()
     */
    interface Builder extends CopyableBuilder<Builder, IamPolicyWriter> {
        /**
         * Configure whether the writer should "pretty-print" the output.
         * <p>
         * When set to true, this will add new lines and indentation to the output to make it easier for a human to read, at
         * the expense of extra data (white space) being output.
         * <p>
         * By default, this is {@code false}.
         */
        Builder prettyPrint(Boolean prettyPrint);
    }
}

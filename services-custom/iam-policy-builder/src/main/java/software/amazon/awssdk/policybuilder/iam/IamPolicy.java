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

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.policybuilder.iam.internal.DefaultIamPolicy;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An AWS access control policy is a object that acts as a container for one or
 * more statements, which specify fine grained rules for allowing or denying
 * various types of actions from being performed on your AWS resources.
 * <p>
 * By default, all requests to use your resource coming from anyone but you are
 * denied. Access control polices can override that by allowing different types
 * of access to your resources, or by explicitly denying different types of
 * access.
 * <p>
 * Each statement in an AWS access control policy takes the form:
 * "A has permission to do B to C where D applies".
 * <ul>
 *   <li>A is the <b>principal</b> - the AWS account that is making a request to
 *       access or modify one of your AWS resources.
 *   <li>B is the <b>action</b> - the way in which your AWS resource is being accessed or modified, such
 *       as sending a message to an Amazon SQS queue, or storing an object in an Amazon S3 bucket.
 *   <li>C is the <b>resource</b> - your AWS entity that the principal wants to access, such
 *       as an Amazon SQS queue, or an object stored in Amazon S3.
 *   <li>D is the set of <b>conditions</b> - optional constraints that specify when to allow or deny
 *       access for the principal to access your resource.  Many expressive conditions are available,
 *       some specific to each service.  For example you can use date conditions to allow access to
 *       your resources only after or before a specific time.
 * </ul>
 * <p>
 * For more information, see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/">The IAM User Guide</a>
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
 *     iam.createPolicy(r -> r.policyName("AllowWriteBookMetadata")
 *                            .policyDocument(policy.toJson()));
 * }
 * }
 *
 * <p>
 * <b>Download the policy uploaded in the previous example and create a new policy with "read" access added to it.</b>
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
 *     String decodedPolicy = URLDecoder.decode(getPolicyVersionResponse.policyVersion().document(), StandardCharsets.UTF_8);
 *     IamPolicy policy = IamPolicy.fromJson(decodedPolicy);
 *
 *     IamStatement newStatement = policy.statements().get(0).copy(s -> s.addAction("dynamodb:GetItem"));
 *     IamPolicy newPolicy = policy.copy(p -> p.statements(Arrays.asList(newStatement)));
 *
 *     iam.createPolicy(r -> r.policyName("AllowReadWriteBookMetadata")
 *                            .policyDocument(newPolicy.toJson()));
 * }
 * }
 *
 * @see IamPolicyReader
 * @see IamPolicyWriter
 * @see IamStatement
 * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/">IAM User Guide</a>
 */
@SdkPublicApi
@ThreadSafe
public interface IamPolicy extends ToCopyableBuilder<IamPolicy.Builder, IamPolicy> {
    /**
     * Create an {@code IamPolicy} from an IAM policy in JSON form.
     * <p>
     * This will raise an exception if the provided JSON is invalid or does not appear to represent a valid policy document.
     * <p>
     * This is equivalent to {@code IamPolicyReader.create().read(json)}.
     */
    static IamPolicy fromJson(String json) {
        return IamPolicyReader.create().read(json);
    }

    /**
     * Create an {@code IamPolicy} containing the provided statements.
     * <p>
     * At least one statement is required.
     * <p>
     * This is equivalent to {@code IamPolicy.builder().statements(statements).build()}
     */
    static IamPolicy create(Collection<IamStatement> statements) {
        return builder().statements(statements).build();
    }

    /**
     * Create a {@link Builder} for an {@code IamPolicy}.
     */
    static Builder builder() {
        return DefaultIamPolicy.builder();
    }

    /**
     * Retrieve the value set by {@link Builder#id(String)}.
     */
    String id();

    /**
     * Retrieve the value set by {@link Builder#version(String)}.
     */
    String version();

    /**
     * Retrieve the value set by {@link Builder#statements(Collection)}.
     */
    List<IamStatement> statements();

    /**
     * Convert this policy to the JSON format that is accepted by AWS services.
     * <p>
     * This is equivalent to {@code IamPolicyWriter.create().writeToString(policy)}
     * <p>
     * {@snippet :
     * IamPolicy policy =
     *     IamPolicy.builder()
     *              .addStatement(IamStatement.builder()
     *                                        .effect(IamEffect.ALLOW)
     *                                        .addAction("dynamodb:PutItem")
     *                                        .addResource("arn:aws:dynamodb:us-east-2:123456789012:table/books")
     *                                        .build())
     *              .build();
     * System.out.println("Policy:\n" + policy.toJson());
     * }
     */
    String toJson();

    /**
     * Convert this policy to the JSON format that is accepted by AWS services, using the provided writer.
     * <p>
     * This is equivalent to {@code writer.writeToString(policy)}
     * <p>
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
     */
    String toJson(IamPolicyWriter writer);

    /**
     * @see #builder()
     */
    interface Builder extends CopyableBuilder<Builder, IamPolicy> {
        /**
         * Configure the <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_id.html">{@code
         * Id}</a> element of the policy, specifying an optional identifier for the policy.
         * <p>
         * The ID is used differently in different services. ID is allowed in resource-based policies, but not in
         * identity-based policies.
         * <p>
         * For services that let you set an ID element, we recommend you use a UUID (GUID) for the value, or incorporate a UUID
         * as part of the ID to ensure uniqueness.
         * <p>
         * This value is optional.
         * <p>
         * {@snippet :
         * IamPolicy policy =
         *     IamPolicy.builder()
         *              .id("cd3ad3d9-2776-4ef1-a904-4c229d1642ee") // An identifier for the policy
         *              .addStatement(IamStatement.builder()
         *                                        .effect(IamEffect.DENY)
         *                                        .addAction(IamAction.ALL)
         *                                        .build())
         *              .build();
         *}
         *
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_id.html">ID user guide</a>
         */
        Builder id(String id);

        /**
         * Configure the
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_version.html">{@code Version}
         * </a> element of the policy, specifying the language syntax rules that are to be used to
         * process the policy.
         * <p>
         * By default, this value is {@code 2012-10-17}.
         * <p>
         * {@snippet :
         * IamPolicy policy =
         *     IamPolicy.builder()
         *              .version("2012-10-17") // The IAM policy language syntax version to use
         *              .addStatement(IamStatement.builder()
         *                                        .effect(IamEffect.DENY)
         *                                        .addAction(IamAction.ALL)
         *                                        .build())
         *              .build();
         * }
         *
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_version.html">Version
         * user guide</a>
         */
        Builder version(String version);

        /**
         * Configure the
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_statement.html">{@code
         * Statement}</a> element of the policy, specifying the access rules for this policy.
         * <p>
         * This will replace any other statements already added to the policy. At least one statement is required to
         * create a policy.
         * <p>
         * {@snippet :
         * IamPolicy policy =
         *     IamPolicy.builder()
         *              // Add a statement to this policy that denies all actions:
         *              .statements(Arrays.asList(IamStatement.builder()
         *                                                    .effect(IamEffect.DENY)
         *                                                    .addAction(IamAction.ALL)
         *                                                    .build()))
         *              .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_statement.html">
         * Statement user guide</a>
         */
        Builder statements(Collection<IamStatement> statements);

        /**
         * Append a
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_statement.html">{@code
         * Statement}</a> element to this policy to specify additional access rules.
         * <p>
         * At least one statement is required to create a policy.
         * <p>
         * {@snippet :
         * IamPolicy policy =
         *     IamPolicy.builder()
         *              // Add a statement to this policy that denies all actions:
         *              .addStatement(IamStatement.builder()
         *                                        .effect(IamEffect.DENY)
         *                                        .addAction(IamAction.ALL)
         *                                        .build())
         *              .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_statement.html">
         * Statement user guide</a>
         */
        Builder addStatement(IamStatement statement);

        /**
         * Append a
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_statement.html">{@code
         * Statement}</a> element to this policy to specify additional access rules.
         * <p>
         * This works the same as {@link #addStatement(IamStatement)}, except you do not need to specify {@code IamStatement
         * .builder()} or {@code build()}. At least one statement is required to create a policy.
         * <p>
         * {@snippet :
         * IamPolicy policy =
         *     IamPolicy.builder()
         *              // Add a statement to this policy that denies all actions:
         *              .addStatement(s -> s.effect(IamEffect.DENY)
         *                                  .addAction(IamAction.ALL))
         *              .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_statement.html">
         * Statement user guide</a>
         */
        Builder addStatement(Consumer<IamStatement.Builder> statement);
    }
}

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
import software.amazon.awssdk.policybuilder.iam.internal.DefaultIamStatement;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A statement is the formal description of a single permission, and is always
 * contained within a policy object.
 * <p>
 * A statement describes a rule for allowing or denying access to a specific AWS
 * resource based on how the resource is being accessed, and who is attempting
 * to access the resource. Statements can also optionally contain a list of
 * conditions that specify when a statement is to be honored.
 * <p>
 * For example, consider a statement that:
 * <ul>
 * <li>allows access (the effect)
 * <li>for a list of specific AWS account IDs (the principals)
 * <li>when accessing an SQS queue (the resource)
 * <li>using the SendMessage operation (the action)
 * <li>and the request occurs before a specific date (a condition)
 * </ul>
 *
 * <p>
 * Statements takes the form:  "A has permission to do B to C where D applies".
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
 *
 * <p>
 * There are many resources and conditions available for use in statements, and
 * you can combine them to form fine grained custom access control polices.
 *
 * <p>
 * Statements are typically attached to a {@link IamPolicy}.
 *
 * <p>
 * For more information, see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/">The IAM User guide</a>
 *
 * <h2>Usage Examples</h2>
 * <b>Create an
 * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/access_policies.html#policies_id-based">identity-based policy
 * statement</a> that allows a role to write items to an Amazon DynamoDB table.</b>
 * {@snippet :
 * IamStatement statement =
 *     IamStatement.builder()
 *                 .sid("GrantWriteBookMetadata")
 *                 .effect(IamEffect.ALLOW)
 *                 .addAction("dynamodb:PutItem")
 *                 .addResource("arn:aws:dynamodb:us-east-2:123456789012:table/books")
 *                 .build();
 * }
 *
 * <p>
 * <b>Create a
 * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/access_policies.html#policies_resource-based">resource-based policy
 * </a> statement that denies access to all users.</b>
 * {@snippet :
 * IamStatement statement =
 *     IamStatement.builder()
 *                 .effect(IamEffect.DENY)
 *                 .addPrincipal(IamPrincipal.ALL)
 *                 .build();
 * }
 *
 * @see IamPolicy
 * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_statement.html">Statement user
 * guide</a>
 */
@SdkPublicApi
@ThreadSafe
public interface IamStatement extends ToCopyableBuilder<IamStatement.Builder, IamStatement> {
    /**
     * Create a {@link Builder} for an {@code IamStatement}.
     */
    static Builder builder() {
        return DefaultIamStatement.builder();
    }

    /**
     * Retrieve the value set by {@link Builder#sid(String)}.
     */
    String sid();

    /**
     * Retrieve the value set by {@link Builder#effect(IamEffect)}.
     */
    IamEffect effect();

    /**
     * Retrieve the value set by {@link Builder#principals(Collection)}.
     */
    List<IamPrincipal> principals();

    /**
     * Retrieve the value set by {@link Builder#notPrincipals(Collection)}.
     */
    List<IamPrincipal> notPrincipals();

    /**
     * Retrieve the value set by {@link Builder#actions(Collection)}.
     */
    List<IamAction> actions();

    /**
     * Retrieve the value set by {@link Builder#notActions(Collection)}.
     */
    List<IamAction> notActions();

    /**
     * Retrieve the value set by {@link Builder#resources(Collection)}.
     */
    List<IamResource> resources();

    /**
     * Retrieve the value set by {@link Builder#notResources(Collection)}.
     */
    List<IamResource> notResources();

    /**
     * Retrieve the value set by {@link Builder#conditions(Collection)}.
     */
    List<IamCondition> conditions();

    /**
     * @see #builder()
     */
    interface Builder extends CopyableBuilder<Builder, IamStatement> {
        /**
         * Configure the <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_sid.html">{@code
         * Sid}</a> element of the policy, specifying an identifier for the statement.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBookMetadata") // An identifier for the statement
         *                 .effect(IamEffect.ALLOW)
         *                 .addAction("dynamodb:GetItem")
         *                 .addResource("arn:aws:dynamodb:us-east-2:123456789012:table/books")
         *                 .build();
         * }
         *
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_sid.html">Sid user
         * guide</a>
         */
        Builder sid(String sid);

        /**
         * Configure the
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_effect.html">{@code Effect}</a>
         * element of the policy, specifying whether the statement results in an allow or deny.
         * <p>
         * This value is required.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBookMetadata")
         *                 .effect(IamEffect.ALLOW) // The statement ALLOWS access
         *                 .addAction("dynamodb:GetItem")
         *                 .addResource("arn:aws:dynamodb:us-east-2:123456789012:table/books")
         *                 .build();
         * }
         *
         * @see IamEffect
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_effect.html">Effect user
         * guide</a>
         */
        Builder effect(IamEffect effect);

        /**
         * Configure the
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_effect.html">{@code Effect}</a>
         * element of the policy, specifying whether the statement results in an allow or deny.
         * <p>
         * This works the same as {@link #effect(IamEffect)}, except you do not need to {@link IamEffect}. This value is required.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBookMetadata")
         *                 .effect("Allow") // The statement ALLOWs access
         *                 .addAction("dynamodb:GetItem")
         *                 .addResource("arn:aws:dynamodb:us-east-2:123456789012:table/books")
         *                 .build();
         * }
         *
         * @see IamEffect
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_effect.html">Effect user
         * guide</a>
         */
        Builder effect(String effect);

        /**
         * Configure the
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_principal.html">{@code
         * Principal}</a> element of the statement, specifying the principals that are allowed or denied
         * access to a resource.
         * <p>
         * This will replace any other principals already added to the statement.
         * <p>
         * {@snippet :
         * List<IamPrincipal> bookReaderRoles =
         *     IamPrincipal.createAll("AWS",
         *                            Arrays.asList("arn:aws:iam::123456789012:role/books-service",
         *                                          "arn:aws:iam::123456789012:role/books-operator"));
         *
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBookContent")
         *                 .effect(IamEffect.ALLOW)
         *                 .principals(bookReaderRoles) // This statement allows access to the books service and operators
         *                 .addAction("s3:GetObject")
         *                 .addResource("arn:aws:s3:us-west-2:123456789012:accesspoint/book-content/object/*")
         *                 .build();
         * }
         *
         * @see IamPrincipal
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_principal.html">Principal
         * user guide</a>
         */
        Builder principals(Collection<IamPrincipal> principals);

        /**
         * Append a
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_principal.html">{@code
         * Principal}</a> to this statement, specifying a principal that is allowed or denied access to
         * a resource.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBookContent")
         *                 .effect(IamEffect.ALLOW)
         *                  // This statement allows access to the books service:
         *                 .addPrincipal(IamPrincipal.create("AWS", "arn:aws:iam::123456789012:role/books-service"))
         *                 .addAction("s3:GetObject")
         *                 .addResource("arn:aws:s3:us-west-2:123456789012:accesspoint/book-content/object/*")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_principal.html">Principal
         * user guide</a>
         */
        Builder addPrincipal(IamPrincipal principal);

        /**
         * Append a
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_principal.html">{@code
         * Principal}</a> to this statement, specifying a principal that is allowed or denied access to
         * a resource.
         * <p>
         * This works the same as {@link #addPrincipal(IamPrincipal)}, except you do not need to specify {@code IamPrincipal
         * .builder()} or {@code build()}.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBookContent")
         *                 .effect(IamEffect.ALLOW)
         *                 // This statement allows access to the books service:
         *                 .addPrincipal(p -> p.type("AWS").id("arn:aws:iam::123456789012:role/books-service"))
         *                 .addAction("s3:GetObject")
         *                 .addResource("arn:aws:s3:us-west-2:123456789012:accesspoint/book-content/object/*")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_principal.html">Principal
         * user guide</a>
         */
        Builder addPrincipal(Consumer<IamPrincipal.Builder> principal);

        /**
         * Append a
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_principal.html">{@code
         * Principal}</a> to this statement, specifying a principal that is allowed or denied access to
         * a resource.
         * <p>
         * This works the same as {@link #addPrincipal(IamPrincipal)}, except you do not need to specify {@code IamPrincipal
         * .create()}.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBookContent")
         *                 .effect(IamEffect.ALLOW)
         *                 // This statement allows access to the books service:
         *                 .addPrincipal(IamPrincipalType.AWS, "arn:aws:iam::123456789012:role/books-service")
         *                 .addAction("s3:GetObject")
         *                 .addResource("arn:aws:s3:us-west-2:123456789012:accesspoint/book-content/object/*")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_principal.html">Principal
         * user guide</a>
         */
        Builder addPrincipal(IamPrincipalType iamPrincipalType, String principal);

        /**
         * Append a
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_principal.html">{@code
         * Principal}</a> to this statement, specifying a principal that is allowed or denied access to
         * a resource.
         * <p>
         * This works the same as {@link #addPrincipal(IamPrincipalType, String)}, except you do not need to specify {@code
         * IamPrincipalType.create()}.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBookContent")
         *                 .effect(IamEffect.ALLOW)
         *                 // This statement allows access to the books service:
         *                 .addPrincipal("AWS", "arn:aws:iam::123456789012:role/books-service")
         *                 .addAction("s3:GetObject")
         *                 .addResource("arn:aws:s3:us-west-2:123456789012:accesspoint/book-content/object/*")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_principal.html">Principal
         * user guide</a>
         */
        Builder addPrincipal(String iamPrincipalType, String principal);

        /**
         * Append multiple
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_principal.html">{@code
         * Principal}s</a> to this statement, specifying principals that are allowed or denied access to
         * a resource.
         * <p>
         * This works the same as calling {@link #addPrincipal(IamPrincipalType, String)} multiple times with the same
         * {@link IamPrincipalType}.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBookContent")
         *                 .effect(IamEffect.ALLOW)
         *                  // This statement allows access to the books service and operators:
         *                 .addPrincipals(IamPrincipalType.AWS,
         *                                Arrays.asList("arn:aws:iam::123456789012:role/books-service",
         *                                             "arn:aws:iam::123456789012:role/books-operator"))
         *                 .addAction("s3:GetObject")
         *                 .addResource("arn:aws:s3:us-west-2:123456789012:accesspoint/book-content/object/*")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_principal.html">Principal
         * user guide</a>
         */
        Builder addPrincipals(IamPrincipalType iamPrincipalType, Collection<String> principals);

        /**
         * Append multiple
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_principal.html">{@code
         * Principal}s</a> to this statement, specifying principals that are allowed or denied access to
         * a resource.
         * <p>
         * This works the same as calling {@link #addPrincipal(String, String)} multiple times with the same
         * {@link IamPrincipalType}.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBookContent")
         *                 .effect(IamEffect.ALLOW)
         *                  // This statement allows access to the books service and operators:
         *                 .addPrincipals("AWS", Arrays.asList("arn:aws:iam::123456789012:role/books-service",
         *                                                     "arn:aws:iam::123456789012:role/books-operator"))
         *                 .addAction("s3:GetObject")
         *                 .addResource("arn:aws:s3:us-west-2:123456789012:accesspoint/book-content/object/*")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_principal.html">Principal
         * user guide</a>
         */
        Builder addPrincipals(String iamPrincipalType, Collection<String> principals);

        /**
         * Configure the
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notprincipal.html">{@code
         * NotPrincipal}</a> element of the statement, specifying that all principals are affected by the policy except the
         * ones listed.
         * <p>
         * Very few scenarios require the use of {@code NotPrincipal}. We recommend that you explore other authorization options
         * before you decide to use {@code NotPrincipal}. {@code NotPrincipal} can only be used with {@link IamEffect#DENY}
         * statements.
         * <p>
         * This will replace any other not-principals already added to the statement.
         * <p>
         * {@snippet :
         * List<IamPrincipal> bookReaderRoles =
         *     IamPrincipal.createAll("AWS",
         *                            Arrays.asList("arn:aws:iam::123456789012:role/books-service",
         *                                          "arn:aws:iam::123456789012:role/books-operator"));
         *
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBookContent")
         *                 .effect(IamEffect.DENY)
         *                  // This statement denies access to everyone except the books service and operators:
         *                 .notPrincipals(bookReaderRoles)
         *                 .addAction("s3:GetObject")
         *                 .addResource("arn:aws:s3:us-west-2:123456789012:accesspoint/book-content/object/*")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notprincipal.html">
         * NotPrincipal user guide</a>
         */
        Builder notPrincipals(Collection<IamPrincipal> notPrincipals);
        
        /**
         * Append a
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notprincipal.html">{@code
         * NotPrincipal}</a> to this statement, specifying that all principals are affected by the policy except the
         * ones listed.
         * <p>
         * Very few scenarios require the use of {@code NotPrincipal}. We recommend that you explore other authorization options
         * before you decide to use {@code NotPrincipal}. {@code NotPrincipal} can only be used with {@link IamEffect#DENY}
         * statements.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBookContent")
         *                 .effect(IamEffect.DENY)
         *                  // This statement denies access to everyone except the books service:
         *                 .addNotPrincipal(IamPrincipal.create("AWS", "arn:aws:iam::123456789012:role/books-service"))
         *                 .addAction("s3:GetObject")
         *                 .addResource("arn:aws:s3:us-west-2:123456789012:accesspoint/book-content/object/*")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notprincipal.html">
         * NotPrincipal user guide</a>
         */
        Builder addNotPrincipal(IamPrincipal notPrincipal);

        /**
         * Append a
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notprincipal.html">{@code
         * NotPrincipal}</a> to this statement, specifying that all principals are affected by the policy except the
         * ones listed.
         * <p>
         * Very few scenarios require the use of {@code NotPrincipal}. We recommend that you explore other authorization options
         * before you decide to use {@code NotPrincipal}. {@code NotPrincipal} can only be used with {@link IamEffect#DENY}
         * statements.
         * <p>
         * This works the same as {@link #addNotPrincipal(IamPrincipal)}, except you do not need to specify {@code IamPrincipal
         * .builder()} or {@code build()}.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBookContent")
         *                 .effect(IamEffect.DENY)
         *                  // This statement denies access to everyone except the books service:
         *                 .addNotPrincipal(p -> p.type("AWS").id("arn:aws:iam::123456789012:role/books-service"))
         *                 .addAction("s3:GetObject")
         *                 .addResource("arn:aws:s3:us-west-2:123456789012:accesspoint/book-content/object/*")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notprincipal.html">
         * NotPrincipal user guide</a>
         */
        Builder addNotPrincipal(Consumer<IamPrincipal.Builder> notPrincipal);

        /**
         * Append a
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notprincipal.html">{@code
         * NotPrincipal}</a> to this statement, specifying that all principals are affected by the policy except the
         * ones listed.
         * <p>
         * Very few scenarios require the use of {@code NotPrincipal}. We recommend that you explore other authorization options
         * before you decide to use {@code NotPrincipal}. {@code NotPrincipal} can only be used with {@link IamEffect#DENY}
         * statements.
         * <p>
         * This works the same as {@link #addNotPrincipal(IamPrincipal)}, except you do not need to specify {@code IamPrincipal
         * .create()}.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBookContent")
         *                 .effect(IamEffect.DENY)
         *                  // This statement denies access to everyone except the books service:
         *                 .addNotPrincipal(IamPrincipalType.AWS, "arn:aws:iam::123456789012:role/books-service")
         *                 .addAction("s3:GetObject")
         *                 .addResource("arn:aws:s3:us-west-2:123456789012:accesspoint/book-content/object/*")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notprincipal.html">
         * NotPrincipal user guide</a>
         */
        Builder addNotPrincipal(IamPrincipalType iamPrincipalType, String notPrincipal);

        /**
         * Append a
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notprincipal.html">{@code
         * NotPrincipal}</a> to this statement, specifying that all principals are affected by the policy except the
         * ones listed.
         * <p>
         * Very few scenarios require the use of {@code NotPrincipal}. We recommend that you explore other authorization options
         * before you decide to use {@code NotPrincipal}. {@code NotPrincipal} can only be used with {@link IamEffect#DENY}
         * statements.
         * <p>
         * This works the same as {@link #addNotPrincipal(IamPrincipalType, String)}, except you do not need to specify {@code
         * IamPrincipalType.create()}.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBookContent")
         *                 .effect(IamEffect.DENY)
         *                  // This statement denies access to everyone except the books service:
         *                 .addNotPrincipal("AWS", "arn:aws:iam::123456789012:role/books-service")
         *                 .addAction("s3:GetObject")
         *                 .addResource("arn:aws:s3:us-west-2:123456789012:accesspoint/book-content/object/*")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notprincipal.html">
         * NotPrincipal user guide</a>
         */
        Builder addNotPrincipal(String iamPrincipalType, String notPrincipal);

        /**
         * Append multiple 
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notprincipal.html">{@code
         * NotPrincipal}s</a> to this statement, specifying that all principals are affected by the policy except the
         * ones listed.
         * <p>
         * Very few scenarios require the use of {@code NotPrincipal}. We recommend that you explore other authorization options
         * before you decide to use {@code NotPrincipal}. {@code NotPrincipal} can only be used with {@link IamEffect#DENY}
         * statements.
         * <p>
         * This works the same as calling {@link #addNotPrincipal(IamPrincipalType, String)} multiple times with the same
         * {@link IamPrincipalType}.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBookContent")
         *                 .effect(IamEffect.DENY)
         *                  // This statement denies access to everyone except the books service and operators:
         *                 .addNotPrincipals(IamPrincipalType.AWS,
         *                                   Arrays.asList("arn:aws:iam::123456789012:role/books-service",
         *                                                "arn:aws:iam::123456789012:role/books-operator"))
         *                 .addAction("s3:GetObject")
         *                 .addResource("arn:aws:s3:us-west-2:123456789012:accesspoint/book-content/object/*")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notprincipal.html">
         * NotPrincipal user guide</a>
         */
        Builder addNotPrincipals(IamPrincipalType iamPrincipalType, Collection<String> notPrincipals);

        /**
         * Append multiple 
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notprincipal.html">{@code
         * NotPrincipal}s</a> to this statement, specifying that all principals are affected by the policy except the
         * ones listed.
         * <p>
         * Very few scenarios require the use of {@code NotPrincipal}. We recommend that you explore other authorization options
         * before you decide to use {@code NotPrincipal}. {@code NotPrincipal} can only be used with {@link IamEffect#DENY}
         * statements.
         * <p>
         * This works the same as calling {@link #addNotPrincipal(String, String)} multiple times with the same
         * {@link IamPrincipalType}.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBookContent")
         *                 .effect(IamEffect.DENY)
         *                  // This statement denies access to everyone except the books service and operators:
         *                 .addNotPrincipals("AWS", Arrays.asList("arn:aws:iam::123456789012:role/books-service",
         *                                                        "arn:aws:iam::123456789012:role/books-operator"))
         *                 .addAction("s3:GetObject")
         *                 .addResource("arn:aws:s3:us-west-2:123456789012:accesspoint/book-content/object/*")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notprincipal.html">
         * NotPrincipal user guide</a>
         */
        Builder addNotPrincipals(String iamPrincipalType, Collection<String> notPrincipals);

        /**
         * Configure the
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_action.html">{@code Action}</a>
         * element of the statement, specifying the actions that are allowed or denied.
         * <p>
         * This will replace any other actions already added to the statement.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadWriteBookMetadata")
         *                 .effect(IamEffect.ALLOW)
         *                 // This statement grants access to read and write items in Amazon DynamoDB:
         *                 .actions(Arrays.asList(IamAction.create("dynamodb:PutItem"),
         *                                        IamAction.create("dynamodb:GetItem")))
         *                 .addResource("arn:aws:dynamodb:us-east-2:123456789012:table/books")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_action.html">Action user
         * guide</a>
         */
        Builder actions(Collection<IamAction> actions);

        /**
         * Configure the
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_action.html">{@code Action}</a>
         * element of the statement, specifying the actions that are allowed or denied.
         * <p>
         * This works the same as {@link #actions(Collection)}, except you do not need to call {@code IamAction.create()
         * } on each action. This will replace any other actions already added to the statement.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadWriteBookMetadata")
         *                 .effect(IamEffect.ALLOW)
         *                 // This statement grants access to read and write items in Amazon DynamoDB:
         *                 .actionIds(Arrays.asList("dynamodb:PutItem", "dynamodb:GetItem"))
         *                 .addResource("arn:aws:dynamodb:us-east-2:123456789012:table/books")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_action.html">Action user
         * guide</a>
         */
        Builder actionIds(Collection<String> actions);

        /**
         * Append an <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_action.html">{@code
         * Action}</a> element to this statement, specifying an action that is allowed or denied.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBookMetadata")
         *                 .effect(IamEffect.ALLOW)
         *                 // This statement grants access to read items in Amazon DynamoDB:
         *                 .addAction(IamAction.create("dynamodb:GetItem"))
         *                 .addResource("arn:aws:dynamodb:us-east-2:123456789012:table/books")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_action.html">Action user
         * guide</a>
         */
        Builder addAction(IamAction action);

        /**
         * Append an <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_action.html">{@code
         * Action}</a> element to this statement, specifying an action that is allowed or denied.
         * <p>
         * This works the same as {@link #addAction(IamAction)}, except you do not need to call {@code IamAction.create()}.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBookMetadata")
         *                 .effect(IamEffect.ALLOW)
         *                 // This statement grants access to read items in Amazon DynamoDB:
         *                 .addAction("dynamodb:GetItem")
         *                 .addResource("arn:aws:dynamodb:us-east-2:123456789012:table/books")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_action.html">Action user
         * guide</a>
         */
        Builder addAction(String action);

        /**
         * Configure the
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notaction.html">{@code
         * NotAction}</a> element of the statement, specifying actions that are denied or allowed.
         * <p>
         * This will replace any other not-actions already added to the statement.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantAllButDeleteBookMetadataTable")
         *                 .effect(IamEffect.ALLOW)
         *                 // This statement grants access to do ALL CURRENT AND FUTURE actions against the books table, except
         *                 // dynamodb:DeleteTable
         *                 .notActions(Arrays.asList(IamAction.create("dynamodb:DeleteTable")))
         *                 .addResource("arn:aws:dynamodb:us-east-2:123456789012:table/books")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notaction.html">NotAction
         * user guide</a>
         */
        Builder notActions(Collection<IamAction> actions);

        /**
         * Configure the
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notaction.html">{@code
         * NotAction}</a> element of the statement, specifying actions that are denied or allowed.
         * <p>
         * This works the same as {@link #notActions(Collection)}, except you do not need to call {@code IamAction.create()}
         * on each action. This will replace any other not-actions already added to the statement.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantAllButDeleteBookMetadataTable")
         *                 .effect(IamEffect.ALLOW)
         *                 // This statement grants access to do ALL CURRENT AND FUTURE actions against the books table, except
         *                 // dynamodb:DeleteTable
         *                 .notActionIds(Arrays.asList("dynamodb:DeleteTable"))
         *                 .addResource("arn:aws:dynamodb:us-east-2:123456789012:table/books")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notaction.html">NotAction
         * user guide</a>
         */
        Builder notActionIds(Collection<String> actions);

        /**
         * Append a
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notaction.html">{@code
         * NotAction}</a> element to this statement, specifying an action that is denied or allowed.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantAllButDeleteBookMetadataTable")
         *                 .effect(IamEffect.ALLOW)
         *                 // This statement grants access to do ALL CURRENT AND FUTURE actions against the books table, except
         *                 // dynamodb:DeleteTable
         *                 .addNotAction(IamAction.create("dynamodb:DeleteTable"))
         *                 .addResource("arn:aws:dynamodb:us-east-2:123456789012:table/books")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notaction.html">NotAction
         * user guide</a>
         */
        Builder addNotAction(IamAction action);

        /**
         * Append a
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notaction.html">{@code
         * NotAction}</a> element to this statement, specifying an action that is denied or allowed.
         * <p>
         * This works the same as {@link #addNotAction(IamAction)}, except you do not need to call {@code IamAction.create()}.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantAllButDeleteBookMetadataTable")
         *                 .effect(IamEffect.ALLOW)
         *                 // This statement grants access to do ALL CURRENT AND FUTURE actions against the books table, except
         *                 // dynamodb:DeleteTable
         *                 .addNotAction("dynamodb:DeleteTable")
         *                 .addResource("arn:aws:dynamodb:us-east-2:123456789012:table/books")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notaction.html">NotAction
         * user guide</a>
         */
        Builder addNotAction(String action);

        /**
         * Configure the
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_resource.html">{@code Resource}
         * </a> element of the statement, specifying the resource(s) that the statement covers.
         * <p>
         * This will replace any other resources already added to the statement.
         * <p>
         * {@snippet :
         * List<IamResource> resources =
         *     Arrays.asList(IamResource.create("arn:aws:dynamodb:us-east-2:123456789012:table/books"),
         *                   IamResource.create("arn:aws:dynamodb:us-east-2:123456789012:table/customers"));
         *
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBookAndCustomersMetadata")
         *                 .effect(IamEffect.ALLOW)
         *                 .addAction("dynamodb:GetItem")
         *                 // This statement grants access to the books and customers tables:
         *                 .resources(resources)
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_resource.html">Resource
         * user guide</a>
         */
        Builder resources(Collection<IamResource> resources);

        /**
         * Configure the
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_resource.html">{@code Resource}
         * </a> element of the statement, specifying the resource(s) that the statement covers.
         * <p>
         * This works the same as {@link #resources(Collection)}, except you do not need to call {@code IamResource.create()}
         * on each resource. This will replace any other resources already added to the statement.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBookAndCustomersMetadata")
         *                 .effect(IamEffect.ALLOW)
         *                 .addAction("dynamodb:GetItem")
         *                 // This statement grants access to the books and customers tables:
         *                 .resourceIds(Arrays.asList("arn:aws:dynamodb:us-east-2:123456789012:table/books",
         *                                            "arn:aws:dynamodb:us-east-2:123456789012:table/customers"))
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_resource.html">Resource
         * user guide</a>
         */
        Builder resourceIds(Collection<String> resources);

        /**
         * Append a
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_resource.html">{@code Resource}
         * </a> element to the statement, specifying a resource that the statement covers.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBookMetadata")
         *                 .effect(IamEffect.ALLOW)
         *                 .addAction("dynamodb:GetItem")
         *                 // This statement grants access to the books table:
         *                 .addResource(IamResource.create("arn:aws:dynamodb:us-east-2:123456789012:table/books"))
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_resource.html">Resource
         * user guide</a>
         */
        Builder addResource(IamResource resource);

        /**
         * Append a
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_resource.html">{@code Resource}
         * </a> element to the statement, specifying a resource that the statement covers.
         * <p>
         * This works the same as {@link #addResource(IamResource)}, except you do not need to call {@code IamResource.create()}.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBookMetadata")
         *                 .effect(IamEffect.ALLOW)
         *                 .addAction("dynamodb:GetItem")
         *                 // This statement grants access to the books table:
         *                 .addResource("arn:aws:dynamodb:us-east-2:123456789012:table/books")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_resource.html">Resource
         * user guide</a>
         */
        Builder addResource(String resource);

        /**
         * Configure the
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notresource.html">{@code
         * NotResource}</a> element of the statement, specifying that the statement should apply to every resource except the
         * ones listed.
         * <p>
         * This will replace any other not-resources already added to the statement.
         * <p>
         * {@snippet :
         * List<IamResource> notResources =
         *     Arrays.asList(IamResource.create("arn:aws:dynamodb:us-east-2:123456789012:table/customers"));
         *
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadNotCustomers")
         *                 .effect(IamEffect.ALLOW)
         *                 .addAction("dynamodb:GetItem")
         *                 // This statement grants access to EVERY CURRENT AND FUTURE RESOURCE except the customers table:
         *                 .notResources(notResources)
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notresource.html">
         * NotResource user guide</a>
         */
        Builder notResources(Collection<IamResource> resources);

        /**
         * Configure the
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notresource.html">{@code
         * NotResource}</a> element of the statement, specifying that the statement should apply to every resource except the
         * ones listed.
         * <p>
         * This works the same as {@link #notResources(Collection)}, except you do not need to call {@code IamResource.create()}
         * on each resource. This will replace any other not-resources already added to the statement.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadNotCustomers")
         *                 .effect(IamEffect.ALLOW)
         *                 .addAction("dynamodb:GetItem")
         *                 // This statement grants access to EVERY CURRENT AND FUTURE RESOURCE except the customers table:
         *                 .notResourceIds(Arrays.asList("arn:aws:dynamodb:us-east-2:123456789012:table/customers"))
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notresource.html">
         * NotResource user guide</a>
         */
        Builder notResourceIds(Collection<String> resources);

        /**
         * Append a
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notresource.html">{@code
         * NotResource} </a> element to the statement, specifying that the statement should apply to every resource except the
         * ones listed.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadNotCustomers")
         *                 .effect(IamEffect.ALLOW)
         *                 .addAction("dynamodb:GetItem")
         *                 // This statement grants access to EVERY CURRENT AND FUTURE RESOURCE except the customers table:
         *                 .addNotResource(IamResource.create("arn:aws:dynamodb:us-east-2:123456789012:table/customers"))
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notresource.html">
         * NotResource user guide</a>
         */
        Builder addNotResource(IamResource resource);

        /**
         * Append a
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notresource.html">{@code
         * NotResource} </a> element to the statement, specifying that the statement should apply to every resource except the
         * ones listed.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadNotCustomers")
         *                 .effect(IamEffect.ALLOW)
         *                 .addAction("dynamodb:GetItem")
         *                 // This statement grants access to EVERY CURRENT AND FUTURE RESOURCE except the customers table:
         *                 .addNotResource("arn:aws:dynamodb:us-east-2:123456789012:table/customers")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_notresource.html">
         * NotResource user guide</a>
         */
        Builder addNotResource(String resource);

        /**
         * Configure the
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">{@code
         * Condition}</a> element of the statement, specifying the conditions in which the statement is in effect.
         * <p>
         * This will replace any other conditions already added to the statement.
         * <p>
         * {@snippet :
         * IamCondition startTime = IamCondition.create(IamConditionOperator.DATE_GREATER_THAN,
         *                                              "aws:CurrentTime",
         *                                              "1988-05-21T00:00:00Z");
         * IamCondition endTime = IamCondition.create(IamConditionOperator.DATE_LESS_THAN,
         *                                            "aws:CurrentTime",
         *                                            "2065-09-01T00:00:00Z");
         *
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBooks")
         *                 .effect(IamEffect.ALLOW)
         *                 .addAction("dynamodb:GetItem")
         *                 .addResource("arn:aws:dynamodb:us-east-2:123456789012:table/books")
         *                 // This statement grants access between the specified start and end times:
         *                 .conditions(Arrays.asList(startTime, endTime))
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">Condition
         * user guide</a>
         */
        Builder conditions(Collection<IamCondition> conditions);

        /**
         * Append a
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">{@code
         * Condition}</a> to the statement, specifying a condition in which the statement is in effect.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBooks")
         *                 .effect(IamEffect.ALLOW)
         *                 .addAction("dynamodb:GetItem")
         *                 .addResource("arn:aws:dynamodb:us-east-2:123456789012:table/books")
         *                 // This statement grants access after a specified start time:
         *                 .addCondition(IamCondition.create(IamConditionOperator.DATE_GREATER_THAN,
         *                                                   "aws:CurrentTime",
         *                                                   "1988-05-21T00:00:00Z"))
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">Condition
         * user guide</a>
         */
        Builder addCondition(IamCondition condition);

        /**
         * Append a
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">{@code
         * Condition}</a> to the statement, specifying a condition in which the statement is in effect.
         * <p>
         * This works the same as {@link #addCondition(IamCondition)}, except you do not need to specify {@code IamCondition
         * .builder()} or {@code build()}.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBooks")
         *                 .effect(IamEffect.ALLOW)
         *                 .addAction("dynamodb:GetItem")
         *                 .addResource("arn:aws:dynamodb:us-east-2:123456789012:table/books")
         *                 // This statement grants access after a specified start time:
         *                 .addCondition(c -> c.operator(IamConditionOperator.DATE_GREATER_THAN)
         *                                     .key("aws:CurrentTime")
         *                                     .value("1988-05-21T00:00:00Z"))
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">Condition
         * user guide</a>
         */
        Builder addCondition(Consumer<IamCondition.Builder> condition);

        /**
         * Append a
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">{@code
         * Condition}</a> to the statement, specifying a condition in which the statement is in effect.
         * <p>
         * This works the same as {@link #addCondition(IamCondition)}, except you do not need to specify {@code IamCondition
         * .create()}.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBooks")
         *                 .effect(IamEffect.ALLOW)
         *                 .addAction("dynamodb:GetItem")
         *                 .addResource("arn:aws:dynamodb:us-east-2:123456789012:table/books")
         *                 // This statement grants access after a specified start time:
         *                 .addCondition(IamConditionOperator.DATE_GREATER_THAN,
         *                               IamConditionKey.create("aws:CurrentTime"),
         *                               "1988-05-21T00:00:00Z")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">Condition
         * user guide</a>
         */
        Builder addCondition(IamConditionOperator operator, IamConditionKey key, String value);

        /**
         * Append a
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">{@code
         * Condition}</a> to the statement, specifying a condition in which the statement is in effect.
         * <p>
         * This works the same as {@link #addCondition(IamCondition)}, except you do not need to specify {@code IamCondition
         * .create()}.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBooks")
         *                 .effect(IamEffect.ALLOW)
         *                 .addAction("dynamodb:GetItem")
         *                 .addResource("arn:aws:dynamodb:us-east-2:123456789012:table/books")
         *                 // This statement grants access after a specified start time:
         *                 .addCondition(IamConditionOperator.DATE_GREATER_THAN, "aws:CurrentTime", "1988-05-21T00:00:00Z")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">Condition
         * user guide</a>
         */
        Builder addCondition(IamConditionOperator operator, String key, String value);

        /**
         * Append a
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">{@code
         * Condition}</a> to the statement, specifying a condition in which the statement is in effect.
         * <p>
         * This works the same as {@link #addCondition(IamCondition)}, except you do not need to specify {@code IamCondition
         * .create()}.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBooks")
         *                 .effect(IamEffect.ALLOW)
         *                 .addAction("dynamodb:GetItem")
         *                 .addResource("arn:aws:dynamodb:us-east-2:123456789012:table/books")
         *                 // This statement grants access after a specified start time:
         *                 .addCondition("DateGreaterThan", "aws:CurrentTime", "1988-05-21T00:00:00Z")
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">Condition
         * user guide</a>
         */
        Builder addCondition(String operator, String key, String values);

        /**
         * Append multiple
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">{@code
         * Condition}s</a> to the statement, specifying conditions in which the statement is in effect.
         * <p>
         * This works the same as {@link #addCondition(IamConditionOperator, IamConditionKey, String)} multiple times with the
         * same operator and key, but different values.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBooks")
         *                 .effect(IamEffect.ALLOW)
         *                 .addAction("dynamodb:GetItem")
         *                 .addResource("arn:aws:dynamodb:us-east-2:123456789012:table/books")
         *                 // This statement grants access only in the us-east-1 and us-west-2 regions:
         *                 .addConditions(IamConditionOperator.STRING_EQUALS,
         *                                IamConditionKey.create("aws:RequestedRegion"),
         *                                Arrays.asList("us-east-1", "us-west-2"))
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">Condition
         * user guide</a>
         */
        Builder addConditions(IamConditionOperator operator, IamConditionKey key, Collection<String> values);

        /**
         * Append multiple
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">{@code
         * Condition}s</a> to the statement, specifying conditions in which the statement is in effect.
         * <p>
         * This works the same as {@link #addCondition(IamConditionOperator, String, String)} multiple times with the
         * same operator and key, but different values.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBooks")
         *                 .effect(IamEffect.ALLOW)
         *                 .addAction("dynamodb:GetItem")
         *                 .addResource("arn:aws:dynamodb:us-east-2:123456789012:table/books")
         *                 // This statement grants access only in the us-east-1 and us-west-2 regions:
         *                 .addConditions(IamConditionOperator.STRING_EQUALS,
         *                                "aws:RequestedRegion",
         *                                Arrays.asList("us-east-1", "us-west-2"))
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">Condition
         * user guide</a>
         */
        Builder addConditions(IamConditionOperator operator, String key, Collection<String> values);

        /**
         * Append multiple
         * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">{@code
         * Condition}s</a> to the statement, specifying conditions in which the statement is in effect.
         * <p>
         * This works the same as {@link #addCondition(String, String, String)} multiple times with the
         * same operator and key, but different values.
         * <p>
         * {@snippet :
         * IamStatement statement =
         *     IamStatement.builder()
         *                 .sid("GrantReadBooks")
         *                 .effect(IamEffect.ALLOW)
         *                 .addAction("dynamodb:GetItem")
         *                 .addResource("arn:aws:dynamodb:us-east-2:123456789012:table/books")
         *                 // This statement grants access only in the us-east-1 and us-west-2 regions:
         *                 .addConditions("StringEquals", "aws:RequestedRegion", Arrays.asList("us-east-1", "us-west-2"))
         *                 .build();
         * }
         * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition.html">Condition
         * user guide</a>
         */
        Builder addConditions(String operator, String key, Collection<String> values);
    }
}

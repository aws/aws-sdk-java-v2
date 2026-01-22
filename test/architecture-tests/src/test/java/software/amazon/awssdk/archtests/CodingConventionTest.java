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

package software.amazon.awssdk.archtests;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableFrom;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_RAW_TYPE;
import static com.tngtech.archunit.lang.conditions.ArchConditions.setFieldWhere;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JODATIME;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;

import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkPublicApi;

@AnalyzeClasses(packages = "software.amazon.awssdk..")
@ArchIgnore(reason = "CI keeps crashing when running the tests. Ignoring them for now")
public class CodingConventionTest {

    @ArchTest
    static final ArchRule publicApisShouldBeFinal =
        freeze(classes().that().areAnnotatedWith(SdkPublicApi.class)
                        .and().areNotInterfaces()
                        .should().haveModifier(JavaModifier.FINAL))
            .because("public APIs SHOULD be final");

    @ArchTest
    static final ArchRule mustNotUseJavaLogging =
        NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

    @ArchTest
    static final ArchRule mustNotUseSlfLoggerDirectly =
        freeze(noClasses().should(setFieldWhere(assignableFrom(org.slf4j.Logger.class)
                                                    .onResultOf(JavaAccess.Functions.Get.<JavaFieldAccess,
                                                        AccessTarget.FieldAccessTarget>target().then(GET_RAW_TYPE)))
                                      .as("use org.slf4j.Logger")).because("use software.amazon.awssdk.utils.Logger instead"));

    @ArchTest
    static final ArchRule mustNotUseJodaTime =
        NO_CLASSES_SHOULD_USE_JODATIME;

    /**
     * {@link Future} SHOULD NOT be used unless there's a very good reason
     */
    @ArchTest
    static final ArchRule shouldNotUseFuture =
        freeze(noClasses().should().dependOnClassesThat().areAssignableFrom(Future.class)
                          .as("use java.util.concurrent.Future")
                          .because("Future SHOULD NOT be used, use CompletableFuture instead"));

    @ArchTest
    static final ArchRule shouldNotUseOptionalForFields =
        freeze(noFields().should().haveRawType(Optional.class)
                         .as("use Optional for fields")
                         .because("Optional SHOULD NOT be used for method parameters. See "
                                  + "https://github.com/aws/aws-sdk-java-v2/blob/master/docs"
                                  + "/design/UseOfOptional.md"));

    @ArchTest
    static final ArchRule mustNotUseOptionalForMethodParam =
        freeze(noMethods().should().haveRawParameterTypes(Optional.class)
                          .as("use Optional for method parameters")
                          .because("Optional MUST NOT be used for method parameters. See "
                                   + "https://github.com/aws/aws-sdk-java-v2/blob/master/docs/design/UseOfOptional.md"));

    @ArchTest
    static final ArchRule publicApisMustNotDeclareThrowableOfCheckedException =
        freeze(noMethods().that()
                          .areDeclaredInClassesThat().areAnnotatedWith(SdkPublicApi.class)
                          .should()
                          .declareThrowableOfType(Exception.class).orShould().declareThrowableOfType(IOException.class)
                          .because("public APIs MUST NOT throw checked exception"));

    @ArchTest
    static final ArchRule shouldNotHaveMoreThanFourParams =
        freeze(noClasses().that().areAnnotatedWith(SdkProtectedApi.class).or().areAnnotatedWith(SdkPublicApi.class)
                   .should(new HasMoreThanFourParams())
                          .because("the number of method parameters must be less than 4, consider creating a wrapper class"));

    private static final class HasMoreThanFourParams extends ArchCondition<JavaClass> {
        public HasMoreThanFourParams() {
            super("the number of method parameters must be less than 4");
        }

        @Override
        public void check(JavaClass item, ConditionEvents events) {
            Set<JavaMethod> methods = item.getMethods();
            for (JavaMethod method : methods) {
                List<JavaParameter> parameters = method.getParameters();
                if (parameters.size() > 4) {
                    String message = String.format("Method %s has 4+ parameters", method.getFullName());
                    events.add(SimpleConditionEvent.satisfied(method, message));
                }
            }
        }
    }
}

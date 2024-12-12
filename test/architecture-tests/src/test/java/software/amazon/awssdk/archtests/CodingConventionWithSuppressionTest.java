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

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Logger;

/**
 * This test class diffs from {@link CodingConventionTest}; it doesn't use archunit annotations such as {@link ArchTest}
 *
 * <p>
 * This is useful when we want to add manual suppressions for generated code.
 */
public class CodingConventionWithSuppressionTest {

    /**
     * Suppressions for APIs used in generated code to avoid having to update archunit_store for new services. Unfortunately, we
     * can't change the following because it may break people :(
     * <p>
     * DO NOT ADD NEW EXCEPTIONS
     */
    private static final Set<Pattern> ALLOWED_WARN_LOG_SUPPRESSION = new HashSet<>();

    /**
     * Suppressions for APIs used in generated code to avoid having to update archunit_store for new services. Unfortunately, we
     * can't change the following because it may break people :(
     * <p>
     * DO NOT ADD NEW EXCEPTIONS
     */
    private static final Set<Pattern> ALLOWED_ERROR_LOG_SUPPRESSION = new HashSet<>();

    // @Test
    // void publicApisShouldBeFinal() {
    //     JavaClasses classes = new ClassFileImporter()
    //         .withImportOptions(Arrays.asList(new ImportOption.Predefined.DoNotIncludeTests()))
    //         .importPackages("software.amazon.awssdk");
    //     freeze(classes().that().areAnnotatedWith(SdkPublicApi.class)
    //                     .and().areNotInterfaces()
    //                     .should().haveModifier(JavaModifier.FINAL))
    //         .because("public APIs SHOULD be final")
    //         .check(classes);
    // }

    @Test
    void shouldNotUseFuture() {
        JavaClasses classes = new ClassFileImporter()
            .withImportOptions(Arrays.asList(new ImportOption.Predefined.DoNotIncludeTests()))
            .importPackages("software.amazon.awssdk");
        freeze(noClasses().should().dependOnClassesThat().areAssignableFrom(Future.class)
                          .as("use java.util.concurrent.Future")
                          .because("Future SHOULD NOT be used, use CompletableFuture instead"))
            .check(classes);
    }

    // @Test
    // void shouldNotUseOptionalForFields() {
    //     JavaClasses classes = new ClassFileImporter()
    //         .withImportOptions(Arrays.asList(new ImportOption.Predefined.DoNotIncludeTests()))
    //         .importPackages("software.amazon.awssdk");
    //     freeze(noFields().should().haveRawType(Optional.class)
    //                      .as("use Optional for fields")
    //                      .because("Optional SHOULD NOT be used for method parameters. See "
    //                               + "https://github.com/aws/aws-sdk-java-v2/blob/master/docs"
    //                               + "/design/UseOfOptional.md"))
    //         .check(classes);
    // }
    //
    // @Test
    // void mustNotUseOptionalForMethodParam() {
    //     JavaClasses classes = new ClassFileImporter()
    //         .withImportOptions(Arrays.asList(new ImportOption.Predefined.DoNotIncludeTests()))
    //         .importPackages("software.amazon.awssdk");
    //     freeze(noMethods().should().haveRawParameterTypes(Optional.class)
    //                       .as("use Optional for method parameters")
    //                       .because("Optional MUST NOT be used for method parameters. See "
    //                                + "https://github.com/aws/aws-sdk-java-v2/blob/master/docs/design/UseOfOptional.md"))
    //         .check(classes);
    // }
    //
    // @Test
    // void publicApisMustNotDeclareThrowableOfCheckedException() {
    //     JavaClasses classes = new ClassFileImporter()
    //         .withImportOptions(Arrays.asList(new ImportOption.Predefined.DoNotIncludeTests()))
    //         .importPackages("software.amazon.awssdk");
    //     freeze(noMethods().that()
    //                       .areDeclaredInClassesThat().areAnnotatedWith(SdkPublicApi.class)
    //                       .should()
    //                       .declareThrowableOfType(Exception.class).orShould().declareThrowableOfType(IOException.class)
    //                       .because("public APIs MUST NOT throw checked exception"))
    //         .check(classes);
    // }
    //
    // @Test
    // void shouldNotAbuseWarnLog() {
    //     JavaClasses classes = new ClassFileImporter()
    //         .withImportOptions(Arrays.asList(
    //             location -> ALLOWED_WARN_LOG_SUPPRESSION.stream().noneMatch(location::matches),
    //             new ImportOption.Predefined.DoNotIncludeTests()))
    //         .importPackages("software.amazon.awssdk..");
    //
    //     ArchRule rule =
    //         freeze(methods().that().areDeclaredIn(Logger.class).and()
    //                         .haveName("warn").should(new MethodBeingUsedByOthers(
    //                 "log.warn is detected")))
    //             .as("log.warn is detected. Review it with the team. If this is a valid case, add it"
    //                 + " to ALLOWED_WARN_LOG_SUPPRESSION allowlist");
    //
    //     rule.check(classes);
    // }
    //
    // @Test
    // void shouldNotAbuseErrorLog() {
    //     JavaClasses classes = new ClassFileImporter()
    //         .withImportOptions(Arrays.asList(
    //             location -> ALLOWED_ERROR_LOG_SUPPRESSION.stream().noneMatch(location::matches),
    //             new ImportOption.Predefined.DoNotIncludeTests()))
    //         .importPackages("software.amazon.awssdk..");
    //
    //     ArchRule rule =
    //         freeze(methods().that().areDeclaredIn(Logger.class).and()
    //                         .haveName("error").should(new MethodBeingUsedByOthers("log.error is detected")))
    //             .as("log.error is detected. Review it with the team. If this is a valid case, add it to "
    //                 + "ALLOWED_ERROR_LOG_SUPPRESSION allowlist");
    //
    //     rule.check(classes);
    // }

    private static final class MethodBeingUsedByOthers extends ArchCondition<JavaMethod> {
        public MethodBeingUsedByOthers(String description) {
            super(description);
        }

        @Override
        public void check(JavaMethod method, ConditionEvents events) {
            method.getCallsOfSelf().stream()
                  .forEach(call -> events.add(SimpleConditionEvent.violated(method, call.getDescription())));
        }
    }
}

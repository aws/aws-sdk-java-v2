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

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Ensure classes annotated with SdkPublicApi, SdkProtectedApi, and SdkInternalApis are in the right package.
 */
public class PackageContainmentTest {

    /**
     * Suppressions for APIs used in generated code to avoid having to update archunit_store for new services.
     * Unfortunately, we can't change the following because it may break people :(
     *
     * DO NOT ADD NEW EXCEPTIONS
     */
    private static final Pattern TRANSFORM_PACKAGE = Pattern.compile(".*/transform/.*");
    private static final Pattern MODEL_PACKAGE = Pattern.compile(".*/model/.*");
    private static final Pattern ENDPOINTS_CONTEXT = Pattern.compile(".*/endpoints/.*ClientContextParams.class");

    /**
     * Suppressions for APIs used in generated code to avoid having to update archunit_store for new services.
     * Unfortunately, we can't change the following because it may break people :(
     *
     * DO NOT ADD NEW EXCEPTIONS
     */
    private static final Set<Pattern> ALLOWED_INTERNAL_APIS_OUTSIDE_OF_INTERNAL_PACKAGE = new HashSet<>(
        Arrays.asList(TRANSFORM_PACKAGE, MODEL_PACKAGE, ENDPOINTS_CONTEXT));

    @Test
    public void internalAPIs_shouldResideInInternalPackage() {
        JavaClasses importedClasses = new ClassFileImporter()
            .withImportOptions(Arrays.asList(
                location -> ALLOWED_INTERNAL_APIS_OUTSIDE_OF_INTERNAL_PACKAGE.stream().noneMatch(location::matches),
                new ImportOption.Predefined.DoNotIncludeTests()))
            .importPackages("software.amazon.awssdk");

        ArchRule rule = FreezingArchRule.freeze(
            classes().that().areAnnotatedWith(SdkInternalApi.class)
                     .and().areNotNestedClasses().and()
                     .areNotPackagePrivate()
                     .should()
                     .resideInAnyPackage("..internal..")
                     .because("Internal APIs MUST reside in internal subpackage"));

        rule.check(importedClasses);
    }

    @Test
    public void publicAndProtectedAPIs_mustNotResideInInternalPackage() {
        JavaClasses importedClasses = new ClassFileImporter()
            .withImportOptions(Arrays.asList(new ImportOption.Predefined.DoNotIncludeTests()))
            .importPackages("software.amazon.awssdk");

        ArchRule rule =
            FreezingArchRule.freeze(
                classes().that().areAnnotatedWith(SdkPublicApi.class).or()
                         .areAnnotatedWith(SdkProtectedApi.class)
                         .should()
                         .resideOutsideOfPackage("..internal..")
                         .because("Public and protected APIs MUST not reside in internal subpackage"));

        rule.check(importedClasses);

    }

}

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
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.endpoints.internal.S3EndpointAuthSchemeStrategyFactory;

/**
 * This test class diffs from {@link NamingConventionTest}; it doesn't use archunit annotations such as {@link ArchTest}
 *
 * <p>
 * This is useful when we want to add manual suppressions for generated code.
 */
public class NamingConventionWithSuppressionTest {

    /**
     * Suppressions for APIs used in generated code to avoid having to update archunit_store for new services. Unfortunately, we
     * can't change the following because it may break people :(
     * <p>
     * DO NOT ADD NEW EXCEPTIONS
     */
    private static final Set<Pattern> ALLOWED_SUPPLIER_SUPPRESSION = new HashSet<>(
        Arrays.asList(Pattern.compile(".*/DefaultEndpointAuthSchemeStrategyFactory.class"),
                      ArchUtils.classNameToPattern(S3EndpointAuthSchemeStrategyFactory.class)));

    @Test
    void supplierImpl_shouldHaveSupplierSuffix() {
        JavaClasses classes = new ClassFileImporter()
            .withImportOptions(Arrays.asList(
                location -> ALLOWED_SUPPLIER_SUPPRESSION.stream().noneMatch(location::matches),
                new ImportOption.Predefined.DoNotIncludeTests()))
            .importPackages("software.amazon.awssdk..");

        ArchRule rule =
            classes().that().implement(Supplier.class).and().areNotPackagePrivate().should().haveSimpleNameEndingWith(
                "Supplier");
        rule.check(classes);
    }
}

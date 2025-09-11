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
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/**
 * Architecture tests for the utils-lite package to ensure it only contains allowed classes.
 */
public class UtilsLitePackageTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
        .importPackages("software.amazon.awssdk.utilslite");

    @Test
    public void utilsLitePackage_shouldOnlyContainAllowedClasses() {
        ArchRule rule = classes()
            .that().resideInAPackage("software.amazon.awssdk.utilslite")
            .should().haveNameMatching(".*\\.(SdkInternalThreadLocal|SdkInternalThreadLocalTest)")
            .allowEmptyShould(true)
            .because("utils-lite package should only contain SdkInternalThreadLocal and its test");

        rule.check(CLASSES);
    }
}

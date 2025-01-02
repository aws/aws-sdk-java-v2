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
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import software.amazon.awssdk.awscore.presigner.SdkPresigner;

@AnalyzeClasses(packages = "software.amazon.awssdk")
public class NamingConventionTest {

    @ArchTest
    static final ArchRule noImplementation_shouldHaveImplSuffix =
        freeze(noClasses().that().areNotInterfaces().and().haveSimpleNameNotContaining("Builder").should().haveNameMatching(
            ".*Implementation|.*Impl"));

    @ArchTest
    static final ArchRule signerImpl_shouldHaveSignerSuffix =
        classes().that().implement(SdkPresigner.class).and().areNotPackagePrivate().should().haveSimpleNameEndingWith(
            "Presigner");
}

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

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.internal.AwsProtocolMetadata;
import software.amazon.awssdk.awscore.internal.AwsServiceProtocol;
import software.amazon.awssdk.core.internal.interceptor.trait.RequestCompression;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.core.internal.waiters.WaiterAttribute;
import software.amazon.awssdk.utils.internal.EnumUtils;

/**
 * Ensure classes annotated with SdkInternalApis are not accessible outside the module.
 */
public class InternalApiBoundaryTest {

    /**
     * Suppressions for internal APIs used in generated code to avoid having to update archunit_store for new services.
     * Unfortunately, we can't change the following because it may break people :(
     * <p>
     * DO NOT ADD NEW EXCEPTIONS suppression
     */
    private static final Set<Class<?>> ALLOWED_INTERNAL_API_ACROSS_MODULE_SUPPRESSION = new HashSet<>(
        Arrays.asList(WaiterAttribute.class, RequestCompression.class, RequestCompression.Builder.class, EnumUtils.class,
                      AwsServiceProtocol.class, AwsProtocolMetadata.class, MetricUtils.class, SystemSettingUtils.class,
                      ChecksumUtil.class));

    @Test
    void internalApi_shouldNotUsedAcrossModule() {
        JavaClasses importedClasses = new ClassFileImporter()
            .withImportOptions(Arrays.asList(
                new ImportOption.Predefined.DoNotIncludeTests()))
            .importPackages("software.amazon.awssdk");

        ArchRule rule =
            FreezingArchRule.freeze(noClasses().should(new UseInternalApisFromDifferentModule()))
                            .as("Use internal APIs from a different module");
        rule.check(importedClasses);
    }

    private static final class UseInternalApisFromDifferentModule extends ArchCondition<JavaClass> {
        public UseInternalApisFromDifferentModule() {
            super("Internal APIs are used across modules");
        }

        @Override
        public void check(JavaClass item, ConditionEvents events) {
            Set<JavaClass> directDependenciesFromSelf =
                item.getDirectDependenciesFromSelf().stream().map(dependency -> dependency.getTargetClass()).collect(Collectors.toSet());
            String packageName = item.getPackageName();
            for (JavaClass dependencyTargetClass : directDependenciesFromSelf) {
                String dependencyPackageName = dependencyTargetClass.getPackageName();

                boolean allowListed =
                    ALLOWED_INTERNAL_API_ACROSS_MODULE_SUPPRESSION.stream()
                                                                  .anyMatch(clazz -> dependencyTargetClass.getSimpleName().equals(clazz.getSimpleName()));

                if (allowListed || dependencyPackageName.equals(packageName)) {
                    continue;
                }

                if (JavaClass.Predicates.resideInAPackage("software.amazon.awssdk..internal..").test(dependencyTargetClass)) {
                    if (!ArchUtils.resideInSameRootPackage(packageName, dependencyPackageName)) {
                        String errorMessage = String.format("%s depends on an internal API from a different module (%s)",
                                                            item.getDescription(),
                                                            dependencyTargetClass.getDescription());
                        events.add(SimpleConditionEvent.satisfied(item, errorMessage));
                    }
                }
            }
        }
    }
}

/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.poet.model;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

@RunWith(Parameterized.class)
public class ModelCopierSpecTest {
    private static File serviceModelFile;
    private static IntermediateModel intermediateModel;
    private static TestMemberModels testMemberModels;

    private final ClassSpec spec;
    private final String specName;

    private static void setUp() throws URISyntaxException, IOException {
        serviceModelFile = new File(AwsModelSpecTest.class
                .getResource("service-2.json")
                .getFile());

        File customizationConfigFile = new File(AwsModelSpecTest.class
                .getResource("customization.config")
                .getFile());

        intermediateModel = new IntermediateModelBuilder(
                C2jModels.builder()
                        .serviceModel(ModelLoaderUtils.loadModel(ServiceModel.class, serviceModelFile))
                        .customizationConfig(ModelLoaderUtils.loadModel(CustomizationConfig.class, customizationConfigFile))
                        .build())
                .build();

        testMemberModels = new TestMemberModels(intermediateModel);
    }

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> data() {
        invokeSafely(ModelCopierSpecTest::setUp);
        return new ServiceModelCopiers(intermediateModel).copierSpecs().stream()
                                                         .map(spec -> new Object[] { spec, spec.className().simpleName().toLowerCase(Locale.ENGLISH) })
                                                         .collect(toList());
    }

    public ModelCopierSpecTest(ClassSpec spec, String specName) {
        this.spec = spec;
        this.specName = specName;
    }

    @Test
    public void basicGeneration() {
        assertThat(spec, generatesTo(specName + ".java"));
    }

    @Test
    public void doesNotReturnACopierForImmutableTypes() {
        ServiceModelCopiers copiers = new ServiceModelCopiers(intermediateModel);

        // Find members that are not Maps, Lists, Dates, or SdkBytes
        List<MemberModel> immutableMembers = testMemberModels.shapeToMemberMap().values().stream()
                .filter(m -> !(m.isList() || m.isMap()))
                .filter(m -> !(m.getVariable().getSimpleType().equals("Date")))
                .filter(m -> !(m.getVariable().getSimpleType().equals("SdkBytes")))
                .collect(Collectors.toList());

        // Quick sanity check to ensure we're actually testing something
        assertThat(immutableMembers.size(), is(greaterThan(0)));

        immutableMembers.forEach(m -> assertThat(copiers.copierClassFor(m), is(equalTo(Optional.empty()))));
    }
}

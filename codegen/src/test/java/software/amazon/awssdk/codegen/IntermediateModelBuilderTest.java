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

package software.amazon.awssdk.codegen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.model.service.EndpointBddModel;
import software.amazon.awssdk.codegen.model.service.EndpointRuleSetModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

public class IntermediateModelBuilderTest {

    @Test
    public void testServiceAndShapeNameCollisions() throws Exception {
        final File modelFile = new File(IntermediateModelBuilderTest.class
                                            .getResource("poet/client/c2j/collision/service-2.json").getFile());
        IntermediateModel testModel = new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(ModelLoaderUtils.loadModel(ServiceModel.class, modelFile))
                     .customizationConfig(CustomizationConfig.create())
                     .build())
            .build();

        assertThat(testModel.getShapes().values())
            .extracting(ShapeModel::getShapeName)
            .containsExactlyInAnyOrder("DefaultCollisionException", "DefaultCollisionRequest", "DefaultCollisionResponse");
    }

    @Test
    public void sharedOutputShapesLinkCorrectlyToOperationOutputs() {
        final File modelFile = new File(IntermediateModelBuilderTest.class
                                            .getResource("poet/client/c2j/shared-output/service-2.json").getFile());
        IntermediateModel testModel = new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(ModelLoaderUtils.loadModel(ServiceModel.class, modelFile))
                     .customizationConfig(CustomizationConfig.create())
                     .build())
            .build();

        assertEquals("PingResponse", testModel.getOperation("Ping").getOutputShape().getShapeName());
        assertEquals("SecurePingResponse", testModel.getOperation("SecurePing").getOutputShape().getShapeName());
    }

    @Test
    public void defaultEndpointDiscovery_true() {
        final File modelFile = new File(IntermediateModelBuilderTest.class
                                            .getResource("poet/client/c2j/endpointdiscovery/service-2.json").getFile());
        IntermediateModel testModel = new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(ModelLoaderUtils.loadModel(ServiceModel.class, modelFile))
                     .customizationConfig(CustomizationConfig.create())
                     .build())
            .build();

        assertTrue(testModel.getEndpointOperation().get().isEndpointCacheRequired());
    }

    @Test
    public void defaultEndpointDiscovery_false() {
        final File modelFile = new File(IntermediateModelBuilderTest.class
                                            .getResource("poet/client/c2j/endpointdiscoveryoptional/service-2.json").getFile());
        IntermediateModel testModel = new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(ModelLoaderUtils.loadModel(ServiceModel.class, modelFile))
                     .customizationConfig(CustomizationConfig.create())
                     .build())
            .build();

        assertFalse(testModel.getEndpointOperation().get().isEndpointCacheRequired());
    }

    @Test
    public void assertAwsQueryCompatibleTrait_notNull() {
        final File modelFile = new File(IntermediateModelBuilderTest.class
                                            .getResource("poet/client/c2j/query-to-json-errorcode/service-2.json").getFile());
        IntermediateModel testModel = new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(ModelLoaderUtils.loadModel(ServiceModel.class, modelFile))
                     .customizationConfig(CustomizationConfig.create())
                     .build())
            .build();

        assertNotNull(testModel.getMetadata().getAwsQueryCompatible());
    }

    /**
     * A service ships either endpoint model, and both declare the same parameters, so codegen must read them from
     * whichever one is present. Reading them off the rule set specifically left a BDD-only service with the two
     * parameters of the default rule set, which surfaces as a compile error in the generated provider.
     */
    @Test
    public void endpointParameters_bddModelOnly_comeFromTheBddModel() {
        IntermediateModel model = modelWithEndpoints(null, "query/endpoint-bdd-s3.json");

        assertThat(model.getEndpointParameters()).hasSize(17)
                                                 .containsKeys("Bucket", "ForcePathStyle", "UseFIPS", "UseDualStack");
    }

    @Test
    public void endpointParameters_ruleSetOnly_comeFromTheRuleSet() {
        IntermediateModel model = modelWithEndpoints("query/endpoint-rule-set.json", null);

        assertThat(model.getEndpointParameters()).hasSize(14)
                                                 .containsKeys("region", "endpointId", "AccountIdEndpointMode");
    }

    /**
     * The BDD model wins when a service ships both, so that deleting a service's rule set cannot change the parameters
     * codegen sees. The two fixtures declare different parameters only so that the winner is observable.
     */
    @Test
    public void endpointParameters_bothModels_comeFromTheBddModel() {
        IntermediateModel model = modelWithEndpoints("query/endpoint-rule-set-default-regional.json",
                                                     "query/endpoint-bdd-s3.json");

        assertThat(model.getEndpointParameters()).hasSize(17).containsKey("Bucket");
        assertThat(model.getEndpointParameters()).doesNotContainKey("unusedParam");
    }

    /**
     * Only codegen test fixtures ship neither model; every service ships at least one.
     */
    @Test
    public void endpointParameters_neitherModel_fallBackToTheDefaultRuleSet() {
        IntermediateModel model = modelWithEndpoints(null, null);

        assertThat(model.getEndpointParameters()).containsOnlyKeys("Endpoint", "Region");
    }

    /**
     * Parameters are assembled once, by this builder, so that the customization merge is the only writer.
     */
    @Test
    public void endpointParameters_areUnmodifiable() {
        IntermediateModel model = modelWithEndpoints("query/endpoint-rule-set.json", null);

        assertThatThrownBy(() -> model.getEndpointParameters().put("Extra", new ParameterModel()))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void endpointParameters_includeParametersDeclaredByTheCustomizationConfig() {
        IntermediateModel model = new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(serviceModel("query/service-2.json"))
                     .customizationConfig(customizationConfig("query/customization.config"))
                     .endpointRuleSetModel(endpointRuleSetModel("query/endpoint-rule-set.json"))
                     .build())
            .build();

        assertThat(model.getEndpointParameters()).containsKeys("CustomEndpointArray", "ArnList");
    }

    /**
     * The customization merge also has to reach a BDD-only service, which is the case that used to NPE: it read the
     * rule set field directly rather than the parameters codegen actually uses.
     */
    @Test
    public void endpointParameters_customizationParametersReachABddOnlyService() {
        IntermediateModel model = new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(serviceModel("query/service-2.json"))
                     .customizationConfig(customizationConfig("query/customization.config"))
                     .endpointBddModel(endpointBddModel("query/endpoint-bdd-s3.json"))
                     .build())
            .build();

        assertThat(model.getEndpointParameters()).hasSize(19)
                                                 .containsKeys("Bucket", "CustomEndpointArray", "ArnList");
    }

    @Test
    public void endpointParameters_customizationParameterAlreadyDeclared_throws() {
        assertThatIllegalStateException()
            .isThrownBy(() -> new IntermediateModelBuilder(
                C2jModels.builder()
                         .serviceModel(serviceModel("query/service-2.json"))
                         .customizationConfig(
                             customizationConfig("query/customization-with-duplicate-endpointparameter.config"))
                         .endpointRuleSetModel(endpointRuleSetModel("query/endpoint-rule-set.json"))
                         .build())
                .build())
            .withMessageContaining("Duplicate parameters found in customizationConfig");
    }

    private static IntermediateModel modelWithEndpoints(String ruleSetResource, String bddResource) {
        C2jModels.Builder models = C2jModels.builder()
                                            .serviceModel(serviceModel("query/service-2.json"))
                                            .customizationConfig(CustomizationConfig.create());
        if (ruleSetResource != null) {
            models.endpointRuleSetModel(endpointRuleSetModel(ruleSetResource));
        }
        if (bddResource != null) {
            models.endpointBddModel(endpointBddModel(bddResource));
        }
        return new IntermediateModelBuilder(models.build()).build();
    }

    private static ServiceModel serviceModel(String resource) {
        return ModelLoaderUtils.loadModel(ServiceModel.class, resource(resource));
    }

    private static CustomizationConfig customizationConfig(String resource) {
        return ModelLoaderUtils.loadModel(CustomizationConfig.class, resource(resource));
    }

    private static EndpointRuleSetModel endpointRuleSetModel(String resource) {
        return ModelLoaderUtils.loadModel(EndpointRuleSetModel.class, resource(resource));
    }

    private static EndpointBddModel endpointBddModel(String resource) {
        return ModelLoaderUtils.loadModel(EndpointBddModel.class, resource(resource));
    }

    private static File resource(String resource) {
        return new File(IntermediateModelBuilderTest.class.getResource("poet/client/c2j/" + resource).getFile());
    }
}

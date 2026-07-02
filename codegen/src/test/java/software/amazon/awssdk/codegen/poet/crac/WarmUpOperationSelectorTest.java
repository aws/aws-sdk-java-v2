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

package software.amazon.awssdk.codegen.poet.crac;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.poet.ClientTestModels;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

public class WarmUpOperationSelectorTest {

    @Test
    public void whenServiceHasVerifiedSimpleMethods_prefersOne() {
        // The rest-json test model lists a verifiedSimpleMethod. Because a simple method is hand-vetted as callable
        // with an empty request, selection must prefer it over an unvetted operation.
        IntermediateModel model = ClientTestModels.restJsonServiceModels();

        Optional<OperationModel> selected = WarmUpOperationSelector.selectWarmUpOperation(model);

        assertThat(selected).isPresent();
        assertThat(selected.get().getInputShape().isSimpleMethod()).isTrue();
    }

    @Test
    public void queryModel_selectsCanonicalReadOperation() {
        IntermediateModel model = ClientTestModels.queryServiceModels();

        Optional<OperationModel> selected = WarmUpOperationSelector.selectWarmUpOperation(model);

        assertThat(selected).isPresent();
        assertThat(selected.get().getOperationName()).isEqualTo("GetOperationWithChecksum");
    }

    @Test
    public void override_pinsExistingOperation() {
        IntermediateModel model = ClientTestModels.queryServiceModels();
        model.getCustomizationConfig().setWarmUpOperation("BearerAuthOperation");

        Optional<OperationModel> selected = WarmUpOperationSelector.selectWarmUpOperation(model);

        assertThat(selected).isPresent();
        assertThat(selected.get().getOperationName()).isEqualTo("BearerAuthOperation");
    }

    @Test
    public void override_none_disablesWarmUp() {
        IntermediateModel model = ClientTestModels.queryServiceModels();
        model.getCustomizationConfig().setWarmUpOperation("NONE");

        Optional<OperationModel> selected = WarmUpOperationSelector.selectWarmUpOperation(model);

        assertThat(selected).isEmpty();
    }

    @Test
    public void override_unknownOperation_fallsBackToAlgorithm() {
        IntermediateModel model = ClientTestModels.queryServiceModels();
        model.getCustomizationConfig().setWarmUpOperation("ThisOperationDoesNotExist");

        Optional<OperationModel> selected = WarmUpOperationSelector.selectWarmUpOperation(model);

        assertThat(selected).isPresent();
        assertThat(selected.get().getOperationName()).isEqualTo("GetOperationWithChecksum");
    }

    @Test
    public void selection_isDeterministicAcrossInvocations() {
        IntermediateModel model = ClientTestModels.queryServiceModels();

        Optional<OperationModel> first = WarmUpOperationSelector.selectWarmUpOperation(model);
        Optional<OperationModel> second = WarmUpOperationSelector.selectWarmUpOperation(model);

        assertThat(first).isPresent();
        assertThat(second).isPresent();
        assertThat(first.get().getOperationName()).isEqualTo(second.get().getOperationName());
    }

    @Test
    public void endpointDiscoveryOperation_isWarmUpSafe() {
        // Endpoint discovery is not a disqualifier. The two services that use it (timestreamquery, timestreamwrite)
        // set allowEndpointOverrideForEndpointDiscoveryRequiredOperations, so the warm-up call's endpointOverride
        // skips discovery and the call succeeds. The chosen operation is selectable as long as it clears the real
        // hazards (URI-path / host-prefix / streaming).
        IntermediateModel model = ClientTestModels.endpointDiscoveryModels();

        Optional<OperationModel> selected = WarmUpOperationSelector.selectWarmUpOperation(model);

        assertThat(selected).isPresent();
        assertThat(WarmUpOperationSelector.isWarmUpSafe(selected.get())).isTrue();
    }

    @Test
    public void deprecatedOperation_isNotWarmUpSafe() {
        // A deprecated operation must never be warmed up, even if it is otherwise callable with an empty request.
        // A service whose operations are all deprecated therefore generates a no-op provider.
        ShapeModel input = new ShapeModel("DeprecatedRequest");
        OperationModel deprecated = new OperationModel();
        deprecated.setOperationName("Deprecated");
        deprecated.setDeprecated(true);
        deprecated.setInputShape(input);

        assertThat(WarmUpOperationSelector.isWarmUpSafe(deprecated)).isFalse();
    }

    @Test
    public void eventStreamOperation_isNotWarmUpSafe() {
        // A service whose only operations carry event streams: the chosen operation must be rejected by the safety
        // check, since the generated event-stream method has a different signature and cannot be invoked with an
        // empty request. This guards against generating a body that does not compile.
        IntermediateModel model = eventStreamServiceModels();

        Optional<OperationModel> selected = WarmUpOperationSelector.selectWarmUpOperation(model);

        assertThat(selected).isPresent();
        assertThat(WarmUpOperationSelector.isWarmUpSafe(selected.get())).isFalse();
    }

    private static IntermediateModel eventStreamServiceModels() {
        ServiceModel serviceModel = ModelLoaderUtils.loadModel(
            ServiceModel.class,
            new File(WarmUpOperationSelectorTest.class.getResource(
                "/software/amazon/awssdk/codegen/poet/eventstream/service-2.json").getFile()));
        CustomizationConfig customizationConfig = ModelLoaderUtils.loadModel(
            CustomizationConfig.class,
            new File(WarmUpOperationSelectorTest.class.getResource(
                "/software/amazon/awssdk/codegen/poet/eventstream/customization.config").getFile()));
        return new IntermediateModelBuilder(C2jModels.builder()
                                                     .serviceModel(serviceModel)
                                                     .customizationConfig(customizationConfig)
                                                     .build())
            .build();
    }
}

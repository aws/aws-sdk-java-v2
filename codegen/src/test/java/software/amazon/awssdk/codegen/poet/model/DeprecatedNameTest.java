package software.amazon.awssdk.codegen.poet.model;

import java.io.File;
import org.junit.Test;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

public class DeprecatedNameTest {
    @Test(expected = IllegalStateException.class)
    public void throwsOnListDeprecation() {
        runTest("listdeprecationfailure");
    }

    @Test(expected = IllegalStateException.class)
    public void throwsOnMapDeprecation() {
        runTest("mapdeprecationfailure");
    }

    @Test(expected = IllegalStateException.class)
    public void throwsOnEnumDeprecation() {
        runTest("enumdeprecationfailure");
    }

    private void runTest(String testName) {
        File serviceModelFile = new File(getClass().getResource("./deprecatedname/" + testName + ".json").getFile());
        File customizationConfigFile = new File(getClass()
                                                    .getResource("./deprecatedname/" + testName + ".customization")
                                                    .getFile());
        ServiceModel serviceModel = ModelLoaderUtils.loadModel(ServiceModel.class, serviceModelFile);
        CustomizationConfig basicConfig = ModelLoaderUtils.loadModel(CustomizationConfig.class, customizationConfigFile);

        new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(serviceModel)
                     .customizationConfig(basicConfig)
                     .build())
            .build();
    }
}

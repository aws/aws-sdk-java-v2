package software.amazon.awssdk.codegen.poet.transform;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

@RunWith(Parameterized.class)
public class EventMarshallerGenerationTest {
    private static IntermediateModel intermediateModel;
    private final ShapeModel shapeModel;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        invokeSafely(EventMarshallerGenerationTest::setUp);
        return intermediateModel.getShapes().values().stream()
                                .filter(shape -> shape.isEvent())
                                .map(shape -> new Object[] {shape}).collect(toList());
    }

    public EventMarshallerGenerationTest(ShapeModel shapeModel) {
        this.shapeModel = shapeModel;
    }

    @Test
    public void basicGeneration() {
        assertThat(new JsonModelMarshallerSpec(intermediateModel, shapeModel, "Marshaller"), generatesTo(referenceFileForShape()));
    }

    private String referenceFileForShape() {
        return shapeModel.getShapeName().toLowerCase(Locale.ENGLISH) + "marshaller.java";
    }

    private static void setUp() throws IOException {
        File serviceModelFile = new File(JsonModelMarshallerSpecTest.class.getResource("service-2.json").getFile());
        File customizationConfigFile = new File(JsonModelMarshallerSpecTest.class
                                                    .getResource("customization.config")
                                                    .getFile());

        intermediateModel = new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(ModelLoaderUtils.loadModel(ServiceModel.class, serviceModelFile))
                     .customizationConfig(ModelLoaderUtils.loadModel(CustomizationConfig.class, customizationConfigFile))
                     .build())
            .build();
    }
}

package software.amazon.awssdk.codegen.poet.eventstream;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.customization.processors.UseLegacyEventGenerationSchemeProcessorTest;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.poet.model.AwsModelSpecTest;
import software.amazon.awssdk.codegen.poet.model.EventModelSpec;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

@RunWith(Parameterized.class)
public class EventModelSpecTest {
    private static IntermediateModel intermediateModel;

    private final MemberModel event;
    private final ShapeModel eventStream;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        invokeSafely(EventModelSpecTest::setUp);
        return intermediateModel.getShapes().values().stream()
                .filter(ShapeModel::isEventStream)
                .flatMap(eventStream -> eventStream.getMembers().stream()
                        .filter(m -> m.getShape().isEvent())
                        .map(e -> new Object[]{e, eventStream}))
                .collect(toList());
    }

    public EventModelSpecTest(MemberModel event, ShapeModel eventStream) {
        this.event = event;
        this.eventStream = eventStream;
    }

    @Test
    public void basicGeneration() {
        assertThat(new EventModelSpec(event, eventStream, intermediateModel), generatesTo(referenceFileForShape()));
    }

    private String referenceFileForShape() {
        String className = "Default" + intermediateModel.getNamingStrategy().getShapeClassName(event.getName());
        String fileName = className.toLowerCase(Locale.ENGLISH) + ".java";
        return "/software/amazon/awssdk/codegen/poet/eventstream/" + fileName;
    }

    private static void setUp() {
        File serviceModelFile = new File(EventModelSpecTest.class.getResource("service-2.json").getFile());
        ServiceModel serviceModel = ModelLoaderUtils.loadModel(ServiceModel.class, serviceModelFile);

        intermediateModel = new IntermediateModelBuilder(
                C2jModels.builder()
                        .serviceModel(serviceModel)
                        .customizationConfig(loadCustomizationConfig("customization.config"))
                        .build())
                .build();
    }

    private static CustomizationConfig loadCustomizationConfig(String configName) {
        String c2jFilePath = EventModelSpecTest.class.getResource(configName).getFile();
        File file = new File(c2jFilePath);
        return ModelLoaderUtils.loadModel(CustomizationConfig.class, file);
    }
}

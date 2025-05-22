package software.amazon.awssdk.codegen.poet.model;

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
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

@RunWith(Parameterized.class)
public class SharedStreamEventModelSpecTest {
    private static IntermediateModel intermediateModel;

    private final MemberModel event;
    private final ShapeModel eventStream;

    @Parameterized.Parameters(name = "{1}-{0}")
    public static Collection<Object[]> data() {
        invokeSafely(SharedStreamEventModelSpecTest::setUp);
        return intermediateModel.getShapes().values().stream()
                                .filter(ShapeModel::isEventStream)
                                .flatMap(eventStream -> eventStream.getMembers().stream()
                                                                   .filter(m -> m.getShape().isEvent())
                                                                   .map(e -> new Object[]{e, eventStream}))
                                .collect(toList());
    }

    public SharedStreamEventModelSpecTest(MemberModel event, ShapeModel eventStream) {
        this.event = event;
        this.eventStream = eventStream;
    }

    @Test
    public void basicGeneration() {
        assertThat(new EventModelSpec(event, eventStream, intermediateModel), generatesTo(referenceFileForShape()));
    }

    private String referenceFileForShape() {
        String namespacedEventImpl = eventStream.getShapeName() + "/default" + event.getName();
        return "sharedstream/" + namespacedEventImpl.toLowerCase(Locale.ENGLISH) + ".java";
    }

    private static void setUp() throws IOException {
        File serviceModelFile = new File(SharedStreamEventModelSpecTest.class.getResource("sharedstream/service-2.json").getFile());
        ServiceModel serviceModel = ModelLoaderUtils.loadModel(ServiceModel.class, serviceModelFile);

        intermediateModel = new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(serviceModel)
                     .customizationConfig(CustomizationConfig.create())
                     .build())
            .build();
    }
}

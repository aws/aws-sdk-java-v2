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

package software.amazon.awssdk.codegen.customization.processors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Shape;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;
import software.amazon.awssdk.utils.ImmutableMap;

public class EventStreamSharedEventProcessorTest {
    private static final String RESOURCE_ROOT = "/software/amazon/awssdk/codegen/customization/processors"
                                                + "/eventstreamsharedeventprocessor/";

    private ServiceModel serviceModel;

    @Before
    public void setUp() {
        File serviceModelFile =
            new File(EventStreamSharedEventProcessorTest.class.getResource(RESOURCE_ROOT + "service-2.json").getFile());
        serviceModel = ModelLoaderUtils.loadModel(ServiceModel.class, serviceModelFile);
    }

    @Test
    public void duplicatesAndRenamesSharedEvent() {
       File customizationConfigFile =
            new File(EventStreamSharedEventProcessorTest.class.getResource(RESOURCE_ROOT + "customization.config").getFile());
        CustomizationConfig config = ModelLoaderUtils.loadModel(CustomizationConfig.class, customizationConfigFile);

        EventStreamSharedEventProcessor processor =
            new EventStreamSharedEventProcessor(config.getDuplicateAndRenameSharedEvents());
        processor.preprocess(serviceModel);

        Shape newEventShape = serviceModel.getShape("PayloadB");
        assertNotNull(newEventShape);
        assertEquals(serviceModel.getShape("Payload"), newEventShape);

        Shape streamB = serviceModel.getShape("StreamB");
        assertEquals("PayloadB", streamB.getMembers().get("Payload").getShape());
    }

    @Test
    public void modelWithSharedEvents_raises() {
        CustomizationConfig emptyConfig = CustomizationConfig.create();

        assertThatThrownBy(() -> new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(serviceModel)
                     .customizationConfig(emptyConfig)
                     .build()).build())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Event shape `Payload` is shared between multiple EventStreams");
    }

    @Test
    public void invalidCustomization_missingShape() {
        Map<String, Map<String, String>> duplicateAndRenameSharedEvents = ImmutableMap.of("MissingShape", null);

        EventStreamSharedEventProcessor processor =
            new EventStreamSharedEventProcessor(duplicateAndRenameSharedEvents);
        assertThatThrownBy(() -> processor.preprocess(serviceModel))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot find eventstream shape [MissingShape]");
    }

    @Test
    public void invalidCustomization_notEventStream() {
        Map<String, Map<String, String>> duplicateAndRenameSharedEvents = ImmutableMap.of("Payload", null);

        EventStreamSharedEventProcessor processor =
            new EventStreamSharedEventProcessor(duplicateAndRenameSharedEvents);
        assertThatThrownBy(() -> processor.preprocess(serviceModel))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Error: [Payload] must be an EventStream");
    }

    @Test
    public void invalidCustomization_invalidMember() {
        Map<String, Map<String, String>> duplicateAndRenameSharedEvents = ImmutableMap.of(
            "StreamB", ImmutableMap.of("InvalidMember", "Payload"));

        EventStreamSharedEventProcessor processor =
            new EventStreamSharedEventProcessor(duplicateAndRenameSharedEvents);
        assertThatThrownBy(() -> processor.preprocess(serviceModel))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot find event member [InvalidMember] in the eventstream [StreamB]");
    }

    @Test
    public void invalidCustomization_shapeAlreadyExists() {
        Map<String, Map<String, String>> duplicateAndRenameSharedEvents = ImmutableMap.of(
            "StreamB", ImmutableMap.of("Payload", "Payload"));

        EventStreamSharedEventProcessor processor =
            new EventStreamSharedEventProcessor(duplicateAndRenameSharedEvents);
        assertThatThrownBy(() -> processor.preprocess(serviceModel))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Error: [Payload] is already in the model");
    }
}

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

package software.amazon.awssdk.codegen.poet.eventstream;

import static org.hamcrest.MatcherAssert.assertThat;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import org.junit.Test;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.ClientTestModels;

public class EventStreamFunctionalTests {

    @Test
    public void visitorBuilder() throws Exception {
        IntermediateModel model = ClientTestModels.jsonServiceModels();
        GeneratorTaskParams dependencies = GeneratorTaskParams.create(model, "sources/", "tests/");
        ClassSpec classSpec = new EventStreamVisitorBuilder(dependencies,
                                                            model.getOperation("EventStreamOperation"));
        assertThat(classSpec, generatesTo("test-visitor-builder.java"));
    }

    @Test
    public void responseHandler() throws Exception {
        IntermediateModel model = ClientTestModels.jsonServiceModels();
        GeneratorTaskParams dependencies = GeneratorTaskParams.create(model, "sources/", "tests/");
        ClassSpec classSpec = new EventStreamResponseHandlerSpec(dependencies,
                                                                 model.getOperation("EventStreamOperation"));
        assertThat(classSpec, generatesTo("test-response-handler.java"));
    }

    @Test
    public void responseHandlerBuilder() throws Exception {
        IntermediateModel model = ClientTestModels.jsonServiceModels();
        GeneratorTaskParams dependencies = GeneratorTaskParams.create(model, "sources/", "tests/");
        ClassSpec classSpec = new EventStreamResponseHandlerBuilderSpec(dependencies,
                                                                        model.getOperation("EventStreamOperation"));
        assertThat(classSpec, generatesTo("test-response-handler-builder.java"));
    }

}

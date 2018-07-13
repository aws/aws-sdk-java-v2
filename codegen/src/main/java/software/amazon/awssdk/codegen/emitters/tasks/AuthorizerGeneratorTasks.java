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

package software.amazon.awssdk.codegen.emitters.tasks;

import static software.amazon.awssdk.utils.FunctionalUtils.safeFunction;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.codegen.emitters.FreemarkerGeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.AuthorizerModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.utils.ImmutableMap;

public class AuthorizerGeneratorTasks extends BaseGeneratorTasks {

    private final String customRequestSignerDir;

    public AuthorizerGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
        this.customRequestSignerDir = dependencies.getPathProvider().getAuthorizerDirectory();
    }

    @Override
    protected boolean hasTasks() {
        return !model.getCustomAuthorizers().isEmpty();
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        info("Emitting Authorizer interfaces");
        return model.getCustomAuthorizers().values()
                    .stream()
                    .map(safeFunction(this::createTask))
                    .collect(Collectors.toList());
    }

    private GeneratorTask createTask(AuthorizerModel customAuthorizer) throws Exception {
        Metadata metadata = model.getMetadata();
        Map<String, Object> dataModel = ImmutableMap.of(
                "fileHeader", model.getFileHeader(),
                "className", customAuthorizer.getInterfaceName(),
                "authorizer", customAuthorizer,
                "metadata", metadata);
        return new FreemarkerGeneratorTask(customRequestSignerDir,
                                           customAuthorizer.getInterfaceName(),
                                           freemarker.getCustomAuthorizerTemplate(),
                                           dataModel);
    }
}

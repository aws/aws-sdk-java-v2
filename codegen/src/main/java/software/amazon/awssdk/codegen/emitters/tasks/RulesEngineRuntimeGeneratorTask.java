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

package software.amazon.awssdk.codegen.emitters.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.SimpleGeneratorTask;
import software.amazon.awssdk.codegen.poet.rules.EndpointRulesSpecUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

public final class RulesEngineRuntimeGeneratorTask extends BaseGeneratorTasks {
    public static final String RUNTIME_CLASS_NAME = "WaitersRuntime";

    private final String engineInternalClassDir;
    private final String engineInternalResourcesDir;
    private final String engineInternalPackageName;
    private final String fileHeader;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;

    public RulesEngineRuntimeGeneratorTask(GeneratorTaskParams generatorTaskParams) {
        super(generatorTaskParams);
        this.engineInternalClassDir = generatorTaskParams.getPathProvider().getEndpointRulesInternalDirectory();
        this.engineInternalResourcesDir = generatorTaskParams.getPathProvider().getEndpointRulesInternalResourcesDirectory();
        this.engineInternalPackageName = generatorTaskParams.getModel().getMetadata().getFullInternalEndpointRulesPackageName();
        this.fileHeader = generatorTaskParams.getModel().getFileHeader();
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(generatorTaskParams.getModel());
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        List<GeneratorTask> copyTasks = new ArrayList<>();

        List<String> rulesEngineFiles = endpointRulesSpecUtils.rulesEngineResourceFiles();

        for (String path : rulesEngineJavaFilePaths(rulesEngineFiles)) {
            String newFileName = computeNewName(path);
            copyTasks.add(new SimpleGeneratorTask(engineInternalClassDir,
                                                  newFileName,
                                                  fileHeader,
                                                  () -> rulesEngineFileContent("/" + path)));
        }

        return copyTasks;
    }

    private List<String> rulesEngineJavaFilePaths(Collection<String> runtimeEngineFiles) {
        return runtimeEngineFiles.stream()
                                 .filter(e -> e.endsWith(".java.resource"))
                                 .collect(Collectors.toList());
    }


    private String rulesEngineFileContent(String path) {
        return "package " + engineInternalPackageName + ";\n" +
               "\n"
               + loadResourceAsString(path);
    }

    private String loadResourceAsString(String path) {
        try {
            return IoUtils.toUtf8String(loadResource(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private InputStream loadResource(String name) {
        InputStream resourceAsStream = RulesEngineRuntimeGeneratorTask.class.getResourceAsStream(name);
        Validate.notNull(resourceAsStream, "Failed to load resource from %s", name);
        return resourceAsStream;
    }

    private String computeNewName(String path) {
        String[] pathComponents = path.split("/");
        return StringUtils.replace(pathComponents[pathComponents.length - 1], ".resource", "");
    }
}

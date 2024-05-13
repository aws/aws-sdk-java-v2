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
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.SimpleGeneratorTask;
import software.amazon.awssdk.utils.IoUtils;

public final class JmesPathRuntimeGeneratorTask extends BaseGeneratorTasks {
    public static final String RUNTIME_CLASS_NAME = "JmesPathRuntime";

    private final String runtimeClassDir;
    private final String runtimePackageName;
    private final String fileHeader;
    private final String runtimeClassCode;

    public JmesPathRuntimeGeneratorTask(GeneratorTaskParams generatorTaskParams) {
        super(generatorTaskParams);
        this.runtimeClassDir = generatorTaskParams.getPathProvider().getJmesPathInternalDirectory();
        this.runtimePackageName = generatorTaskParams.getModel().getMetadata().getFullInternalJmesPathPackageName();
        this.fileHeader = generatorTaskParams.getModel().getFileHeader();
        this.runtimeClassCode = loadRuntimeCode();
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        String codeContents =
            "package " + runtimePackageName + ";\n" +
            "\n"
            + runtimeClassCode;

        String fileName = RUNTIME_CLASS_NAME + ".java";
        return Collections.singletonList(new SimpleGeneratorTask(runtimeClassDir, fileName, fileHeader,
                                                                 () -> codeContents));
    }

    private static String loadRuntimeCode() {
        try {
            InputStream is = JmesPathRuntimeGeneratorTask.class.getResourceAsStream(
                String.format("/software/amazon/awssdk/codegen/jmespath/%s.java.resource", RUNTIME_CLASS_NAME));
            return IoUtils.toUtf8String(is);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }
}

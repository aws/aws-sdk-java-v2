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

public final class WaitersRuntimeGeneratorTask extends BaseGeneratorTasks {
    public static final String RUNTIME_CLASS_NAME = "WaitersRuntime";

    private final String waitersInternalClassDir;
    private final String waitersInternalPackageName;
    private final String fileHeader;
    private final String runtimeClassCode;

    public WaitersRuntimeGeneratorTask(GeneratorTaskParams generatorTaskParams) {
        super(generatorTaskParams);
        this.waitersInternalClassDir = generatorTaskParams.getPathProvider().getWaitersInternalDirectory();
        this.waitersInternalPackageName = generatorTaskParams.getModel().getMetadata().getFullWaitersInternalPackageName();
        this.fileHeader = generatorTaskParams.getModel().getFileHeader();
        this.runtimeClassCode = loadWaitersRuntimeCode();
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        String codeContents = "" +
                "package " + waitersInternalPackageName + ";\n" +
                "\n"
                + runtimeClassCode;

        String fileName = RUNTIME_CLASS_NAME + ".java";
        return Collections.singletonList(new SimpleGeneratorTask(waitersInternalClassDir, fileName, fileHeader,
                codeContents));
    }

    private static String loadWaitersRuntimeCode() {
        try {
            InputStream is = WaitersRuntimeGeneratorTask.class.getResourceAsStream(
                    "/software/amazon/awssdk/codegen/waiters/WaitersRuntime.java");
            return IoUtils.toUtf8String(is);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }
}

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

package software.amazon.awssdk.codegen.poet.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.SimpleGeneratorTask;
import software.amazon.awssdk.codegen.emitters.tasks.BaseGeneratorTasks;
import software.amazon.awssdk.utils.IoUtils;

public final class SdkClientConfigurationUtilGeneratorTask extends BaseGeneratorTasks {
    public static final String RUNTIME_CLASS_NAME = "SdkClientConfigurationUtil";

    private final String internalPackage;
    private final String internalClassDir;
    private final String fileHeader;
    private final String runtimeClassCode;

    public SdkClientConfigurationUtilGeneratorTask(GeneratorTaskParams generatorTaskParams) {
        super(generatorTaskParams);
        this.internalPackage = model.getMetadata().getFullClientInternalPackageName();
        this.internalClassDir = generatorTaskParams.getPathProvider().getClientInternalDirectory();
        this.fileHeader = generatorTaskParams.getModel().getFileHeader();
        this.runtimeClassCode = loadConfigUtilCode();
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        String codeContents =
            "package " + internalPackage + ";\n" +
            "\n"
            + runtimeClassCode;

        String fileName = RUNTIME_CLASS_NAME + ".java";
        return Collections.singletonList(new SimpleGeneratorTask(internalClassDir, fileName, fileHeader,
                                                                 () -> codeContents));
    }

    private static String loadConfigUtilCode() {
        try {
            InputStream is = SdkClientConfigurationUtilGeneratorTask.class.getResourceAsStream(
                "/software/amazon/awssdk/codegen/poet/model/SdkClientConfigurationUtil.java.resource");
            return IoUtils.toUtf8String(is);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }
}

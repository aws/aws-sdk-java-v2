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

package software.amazon.awssdk.codegen.poet.rules2.bdd;

import static software.amazon.awssdk.codegen.internal.Utils.closeQuietly;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;

public class WriteBinaryBddResourceTask extends GeneratorTask {
    private final String base64Nodes;
    private final String resourcesDirectory;
    private final String fileName;


    public WriteBinaryBddResourceTask(IntermediateModel model, GeneratorTaskParams generatorTaskParams) {
        this.resourcesDirectory = generatorTaskParams.getPathProvider().getResourcesDirectory();
        this.base64Nodes = model.getEndpointBddModel().getNodes();
        this.fileName = "endpoints_bdd_" + Integer.toHexString(base64Nodes.hashCode()) + ".bin";
    }

    @Override
    protected void compute() {
        Path outputFile = Paths.get(resourcesDirectory, fileName);
        try {
            Files.createDirectories(outputFile.getParent());
            Files.write(outputFile, Base64.getDecoder().decode(base64Nodes));
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error creating file %s", fileName), e);
        }
    }
}

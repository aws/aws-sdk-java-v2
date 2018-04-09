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

package software.amazon.awssdk.codegen.emitters;

import static software.amazon.awssdk.codegen.internal.Utils.closeQuietly;
import static software.amazon.awssdk.codegen.poet.PoetUtils.buildJavaFile;

import java.io.IOException;
import java.io.Writer;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public final class PoetGeneratorTask implements GeneratorTask {

    private final Writer writer;
    private final ClassSpec classSpec;
    private final String fileHeader;

    public PoetGeneratorTask(String outputDirectory, String fileHeader, ClassSpec classSpec) {
        this.fileHeader = fileHeader;
        this.writer = new CodeWriter(outputDirectory, classSpec.className().simpleName());
        this.classSpec = classSpec;
    }

    @Override
    public void execute() {
        try {
            writer.write(fileHeader + "\n");
            buildJavaFile(classSpec).writeTo(writer);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error creating class %s", classSpec.className().simpleName()), e);
        } finally {
            closeQuietly(writer);
        }
    }
}

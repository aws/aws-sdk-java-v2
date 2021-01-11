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

package software.amazon.awssdk.codegen.lite;

import static software.amazon.awssdk.codegen.lite.Utils.closeQuietly;

import com.squareup.javapoet.JavaFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.time.ZonedDateTime;
import software.amazon.awssdk.codegen.lite.emitters.CodeWriter;
import software.amazon.awssdk.utils.IoUtils;

public final class CodeGenerator {

    private final Writer writer;
    private final PoetClass poetClass;

    public CodeGenerator(String outputDirectory, PoetClass poetClass) {
        this.writer = new CodeWriter(outputDirectory, poetClass.className().simpleName());
        this.poetClass = poetClass;
    }

    public void generate() {
        try {
            writer.write(loadDefaultFileHeader() + "\n");
            JavaFile.builder(poetClass.className().packageName(), poetClass.poetClass())
                    .skipJavaLangImports(true)
                    .build()
                    .writeTo(writer);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error creating class %s", poetClass.className().simpleName()), e);
        } finally {
            closeQuietly(writer);
        }
    }

    private String loadDefaultFileHeader() throws IOException {
        try (InputStream inputStream = getClass()
            .getResourceAsStream("/software/amazon/awssdk/codegen/lite/DefaultFileHeader.txt")) {
            return IoUtils.toUtf8String(inputStream)
                          .replaceFirst("%COPYRIGHT_DATE_RANGE%", getCopyrightDateRange());
        }
    }

    private String getCopyrightDateRange() {
        int currentYear = ZonedDateTime.now().getYear();
        int copyrightStartYear = currentYear - 5;
        return String.format("%d-%d", copyrightStartYear, currentYear);
    }
}

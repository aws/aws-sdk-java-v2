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

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.CombinableMatcher.both;
import static software.amazon.awssdk.utils.FunctionalUtils.safeConsumer;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public final class PoetGeneratorTaskIntegrationTest {

    private static String PACKAGE = "com.blah.foo.bar";
    private final List<String> tempDirectories = new ArrayList<>();

    @Test
    public void canGenerateJavaClass() throws Exception {
        String randomBaseDirectory = Files.createTempDirectory(getClass().getSimpleName()).toString();
        tempDirectories.add(randomBaseDirectory);
        String fileHeader = "/*\n * this is the file header\n */";
        ClassSpec classSpec = dummyClass();

        PoetGeneratorTask sut = new PoetGeneratorTask(randomBaseDirectory, fileHeader, classSpec);
        sut.execute();

        String contents = new String(Files.readAllBytes(determineOutputFile(classSpec, randomBaseDirectory)));
        assertThat(contents, both(containsString(PACKAGE)).and(startsWith(fileHeader)));
    }

    @After
    public void cleanUp() {
        tempDirectories.forEach(safeConsumer(tempDir -> {
            List<Path> files = Files.walk(Paths.get(tempDir)).collect(toList());
            Collections.reverse(files);
            files.forEach(safeConsumer(Files::delete));
        }));
    }

    private Path determineOutputFile(ClassSpec classSpec, String randomBaseDirectory) {
        return Paths.get(randomBaseDirectory).resolve(classSpec.className().simpleName() + ".java");
    }

    private ClassSpec dummyClass() {
        return new ClassSpec() {
            @Override
            public TypeSpec poetSpec() {
                return TypeSpec.enumBuilder("Blah").addEnumConstant("FOO").addEnumConstant("BAR").build();
            }

            @Override
            public ClassName className() {
                return ClassName.get(PACKAGE, "Blah");
            }
        };
    }
}

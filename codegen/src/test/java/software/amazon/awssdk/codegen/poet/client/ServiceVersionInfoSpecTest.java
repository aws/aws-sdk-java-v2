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

package software.amazon.awssdk.codegen.poet.client;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import java.io.InputStream;
import java.util.Scanner;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.ClientTestModels;
import software.amazon.awssdk.codegen.poet.client.specs.ServiceVersionInfoSpec;
import software.amazon.awssdk.core.util.VersionInfo;

public class ServiceVersionInfoSpecTest {

    // a fixture test that dynamically updates the generated fixture with the current version
    // this is needed because every time codegen runs, the version will change.
    // we need a way to generate the fixture, and then edit it in place with the current version and only then make the assertion.
    @Test
    void testServiceVersionInfoClass() {
        String currVersion = VersionInfo.SDK_VERSION;
        ClassSpec serviceVersionInfoSpec = new ServiceVersionInfoSpec(ClientTestModels.restJsonServiceModels());

        String expectedContent = loadFixtureFile("test-service-version-info-class.java");
        String[] parts = expectedContent.split("public static final String VERSION = \"");
        if (parts.length == 2) {
            String privateConstructor = parts[1].substring(parts[1].indexOf("\""));
            expectedContent = parts[0] + "public static final String VERSION = \"" + currVersion
                              + privateConstructor;
        }

        String actualContent = generateContent(serviceVersionInfoSpec);

        assertThat(actualContent).isEqualTo(expectedContent);
    }

    private String loadFixtureFile(String filename) {
        InputStream is = getClass().getResourceAsStream("specs/" + filename);
        return new Scanner(is).useDelimiter("\\A").next();
    }

    private String generateContent(ClassSpec spec) {
        TypeSpec typeSpec = spec.poetSpec();
        JavaFile javaFile = JavaFile.builder(spec.className().packageName(), typeSpec).build();
        return javaFile.toString();
    }
}

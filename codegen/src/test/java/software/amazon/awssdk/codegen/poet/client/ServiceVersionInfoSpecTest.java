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
import static software.amazon.awssdk.core.util.VersionInfo.SDK_VERSION;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import java.io.InputStream;
import java.util.Scanner;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.ClientTestModels;
import software.amazon.awssdk.codegen.poet.client.specs.ServiceVersionInfoSpec;
import software.amazon.awssdk.codegen.utils.VersionUtils;

public class ServiceVersionInfoSpecTest {

    // Fixture test that compares generated ServiceVersionInfo class against expected output.
    // The fixture file uses {{VERSION}} as a placeholder for the SDK version. The placeholder get
    // replaced with actual value at test time, since the generated code injects the actual
    // version at build time.
    @Test
    void testServiceVersionInfoClass() {
        String currVersion = VersionUtils.convertToMajorMinorX(SDK_VERSION);
        ClassSpec serviceVersionInfoSpec = new ServiceVersionInfoSpec(ClientTestModels.restJsonServiceModels());

        String expectedContent = loadFixtureFile("test-service-version-info-class.java");
        expectedContent = expectedContent
            .replace("{{VERSION}}", currVersion);

        String actualContent = generateContent(serviceVersionInfoSpec);

        assertThat(actualContent).isEqualToIgnoringWhitespace(expectedContent);
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

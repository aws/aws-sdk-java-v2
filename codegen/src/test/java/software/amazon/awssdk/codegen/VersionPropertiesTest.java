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

package software.amazon.awssdk.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.util.VersionInfo;
import software.amazon.awssdk.utils.IoUtils;

public class VersionPropertiesTest {

    @Test
    public void versionProperties_content_isExactlySdkMinorVersion() throws IOException {
        String resource = "META-INF/software.amazon.awssdk/codegen/version.properties";
        String content;
        try (InputStream propertiesFile = VersionPropertiesTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertThat(propertiesFile).as(resource + " on classpath").isNotNull();
            content = IoUtils.toUtf8String(propertiesFile);
        }

        String[] versionParts = VersionInfo.SDK_VERSION.split("\\.");
        String minorVersion = versionParts[0] + "." + versionParts[1] + ".x";
        assertThat(content.replaceFirst("\\r?\\n$", "")).isEqualTo("version=" + minorVersion);
    }
}

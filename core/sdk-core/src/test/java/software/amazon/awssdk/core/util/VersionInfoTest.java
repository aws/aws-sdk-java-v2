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

package software.amazon.awssdk.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

public final class VersionInfoTest {

    @Test
    public void versionIsTheSameAsMavenProject() throws Exception {
        assertThat(VersionInfo.SDK_VERSION).isEqualTo(getSdkVersionFromPom());
    }

    private String getSdkVersionFromPom() throws URISyntaxException, IOException {
        Path pomPath = Paths.get(VersionInfo.class.getResource(".").toURI()).resolve("../../../../../../../pom.xml");
        String pom = new String(Files.readAllBytes(pomPath));

        Matcher match = Pattern.compile("<version>(.*)</version>").matcher(pom);

        if (match.find()) {
            return match.group(1);
        }
        throw new RuntimeException("Version not found in " + pomPath);
    }
}

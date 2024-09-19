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

package software.amazon.awssdk.testutils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import software.amazon.awssdk.utils.IoUtils;

public final class SdkVersionUtils {

    private SdkVersionUtils() {

    }

    public static String getSdkPreviousReleaseVersion(Path pomFile) throws IOException {
        Optional<String> versionString =
            Files.readAllLines(pomFile)
                 .stream().filter(l -> l.contains("<awsjavasdk.previous.version>")).findFirst();

        if (!versionString.isPresent()) {
            throw new AssertionError("No version is found");
        }

        String string = versionString.get().trim();
        String substring = string.substring(29, string.indexOf('/') - 1);
        return substring;
    }

    /**
     * Check if the provided v2 artifacts are available on Maven
     */
    public static boolean checkVersionAvailability(String version, String... artifactIds) throws IOException {
        for (String artifactId : artifactIds) {
            HttpURLConnection connection = null;
            try {
                URI uri = URI.create(String.format("https://repo.maven.apache.org/maven2/software/amazon/awssdk/%s/%s",
                                                   artifactId,
                                                   version));
                connection = (HttpURLConnection) uri.toURL().openConnection(Proxy.NO_PROXY);
                connection.connect();
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    return false;
                }
            } finally {
                IoUtils.closeQuietly(connection.getInputStream(), null);
                IoUtils.closeQuietly(connection.getErrorStream(), null);
            }
        }
        return true;
    }
}

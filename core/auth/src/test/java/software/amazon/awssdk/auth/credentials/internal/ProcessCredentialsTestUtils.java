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

package software.amazon.awssdk.auth.credentials.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Platform;

public final class ProcessCredentialsTestUtils {

    private static final String PROCESS_RESOURCE_PATH = "/resources/process/";

    private ProcessCredentialsTestUtils() {
    }

    public static String copyErrorCaseProcessCredentialsScript() {
        String scriptClasspathFilename = Platform.isWindows() ? "windows-credentials-error-script.bat"
                                                              : "linux-credentials-error-script.sh";

        return copyProcessCredentialsScript(scriptClasspathFilename);
    }

    public static String copyHappyCaseProcessCredentialsScript() {
        String scriptClasspathFilename = Platform.isWindows() ? "windows-credentials-script.bat"
                                                              : "linux-credentials-script.sh";

        return copyProcessCredentialsScript(scriptClasspathFilename);
    }

    public static String copyProcessCredentialsScript(String scriptClasspathFilename) {
        InputStream scriptInputStream = null;
        OutputStream scriptOutputStream = null;

        try {
            scriptInputStream = ProcessCredentialsTestUtils.class
                .getResourceAsStream(PROCESS_RESOURCE_PATH + scriptClasspathFilename);

            File scriptFileOnDisk = File.createTempFile("ProcessCredentialsProviderTest", scriptClasspathFilename);
            scriptFileOnDisk.deleteOnExit();

            if (!scriptFileOnDisk.setExecutable(true)) {
                throw new IllegalStateException("Could not make " + scriptFileOnDisk + " executable.");
            }

            scriptOutputStream = Files.newOutputStream(scriptFileOnDisk.toPath());

            IoUtils.copy(scriptInputStream, scriptOutputStream);

            return scriptFileOnDisk.getAbsolutePath();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IoUtils.closeQuietly(scriptInputStream, null);
            IoUtils.closeQuietly(scriptOutputStream, null);
        }
    }
}

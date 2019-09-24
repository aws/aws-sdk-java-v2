/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.auth.credentials;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.utils.DateUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Platform;

public class ProcessCredentialsProviderTest {
    private static String scriptLocation;
 
    @BeforeClass
    public static void setup()  {
        scriptLocation = copyProcessCredentialsScript();
    }
 
    @AfterClass
    public static void teardown() {
        if (scriptLocation != null && !new File(scriptLocation).delete()) {
            throw new IllegalStateException("Failed to delete file: " + scriptLocation);
        }
    }
 
    @Test
    public void staticCredentialsCanBeLoaded() {
        AwsCredentials credentials =
                ProcessCredentialsProvider.builder()
                                          .command(scriptLocation + " accessKeyId secretAccessKey")
                                          .build()
                                          .resolveCredentials();
 
        Assert.assertFalse(credentials instanceof AwsSessionCredentials);
        Assert.assertEquals("accessKeyId", credentials.accessKeyId());
        Assert.assertEquals("secretAccessKey", credentials.secretAccessKey());
    }
 
    @Test
    public void sessionCredentialsCanBeLoaded() {
        ProcessCredentialsProvider credentialsProvider =
                ProcessCredentialsProvider.builder()
                                          .command(scriptLocation + " accessKeyId secretAccessKey sessionToken " +
                                                   DateUtils.formatIso8601Date(Instant.now()))
                                          .credentialRefreshThreshold(Duration.ofSeconds(1))
                                          .build();

        AwsCredentials credentials = credentialsProvider.resolveCredentials();

        Assert.assertTrue(credentials instanceof AwsSessionCredentials);

        AwsSessionCredentials sessionCredentials = (AwsSessionCredentials) credentials;

        Assert.assertEquals("accessKeyId", sessionCredentials.accessKeyId());
        Assert.assertEquals("secretAccessKey", sessionCredentials.secretAccessKey());
        assertNotNull(sessionCredentials.sessionToken());
    }

    @Test
    public void resultsAreCached() {
        ProcessCredentialsProvider credentialsProvider =
            ProcessCredentialsProvider.builder()
                                      .command(scriptLocation + " accessKeyId secretAccessKey sessionToken " +
                                               DateUtils.formatIso8601Date(Instant.now().plusSeconds(20)))
                                      .build();

        AwsCredentials request1 = credentialsProvider.resolveCredentials();
        AwsCredentials request2 = credentialsProvider.resolveCredentials();

        Assert.assertEquals(request1, request2);
    }

    @Test
    public void expirationBufferOverrideIsApplied() {
        ProcessCredentialsProvider credentialsProvider =
                ProcessCredentialsProvider.builder()
                                          .command(scriptLocation + " accessKeyId secretAccessKey sessionToken " +
                                                   DateUtils.formatIso8601Date(Instant.now().plusSeconds(20)))
                                          .credentialRefreshThreshold(Duration.ofSeconds(20))
                                          .build();

        AwsCredentials request1 = credentialsProvider.resolveCredentials();
        AwsCredentials request2 = credentialsProvider.resolveCredentials();

        Assert.assertNotEquals(request1, request2);
    }

    @Test
    public void lackOfExpirationIsCachedForever() {
        ProcessCredentialsProvider credentialsProvider =
            ProcessCredentialsProvider.builder()
                                      .command(scriptLocation + " accessKeyId secretAccessKey sessionToken")
                                      .credentialRefreshThreshold(Duration.ofSeconds(20))
                                      .build();

        AwsCredentials request1 = credentialsProvider.resolveCredentials();
        AwsCredentials request2 = credentialsProvider.resolveCredentials();

        Assert.assertEquals(request1, request2);
    }
 
    @Test(expected = IllegalStateException.class)
    public void processOutputLimitIsEnforced() {
        ProcessCredentialsProvider.builder()
                                  .command(scriptLocation + " accessKeyId secretAccessKey")
                                  .processOutputLimit(1)
                                  .build()
                                  .resolveCredentials();
    }

    public static String copyProcessCredentialsScript() {
        String scriptClasspathFilename = Platform.isWindows() ? "windows-credentials-script.bat"
                                                              : "linux-credentials-script.sh";
        String scriptClasspathLocation = "/resources/process/" + scriptClasspathFilename;

        InputStream scriptInputStream = null;
        OutputStream scriptOutputStream = null;

        try {
            scriptInputStream = ProcessCredentialsProviderTest.class.getResourceAsStream(scriptClasspathLocation);

            File scriptFileOnDisk = File.createTempFile("ProcessCredentialsProviderTest", scriptClasspathFilename);
            scriptFileOnDisk.deleteOnExit();

            if (!scriptFileOnDisk.setExecutable(true)) {
                throw new IllegalStateException("Could not make " + scriptFileOnDisk + " executable.");
            }

            scriptOutputStream = new FileOutputStream(scriptFileOnDisk);

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
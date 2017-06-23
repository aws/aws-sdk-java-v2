/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.glacier.transfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.services.glacier.GlacierIntegrationTestBase;
import software.amazon.awssdk.test.util.RandomTempFile;

public class LargeFileWithListenerIntegrationTest extends GlacierIntegrationTestBase {
    private static final long contentLength = 1024L * 1024L * 100 + 123;
    private static final boolean cleanup = true;
    private File randomTempFile;
    private File downloadFile;
    private TestGlacierProgressListener progressListener = new TestGlacierProgressListener();

    @Before
    public void setup() throws IOException {
        randomTempFile = new RandomTempFile("LargeFileWithListenerIntegrationTest", contentLength, true);
        assertEquals(contentLength, randomTempFile.length());
        downloadFile = new File(randomTempFile.getParentFile(),
                                randomTempFile.getName() + ".download");
    }

    @After
    public void teanDown() {
        if (cleanup) {
            randomTempFile.delete();
            downloadFile.delete();
        }
    }

    // Create an unreliable glacier client, you can denote whether it is recoverable or not.
    private void initializeUnreliableClient(boolean recoverable) throws FileNotFoundException, IOException {
        setUpCredentials();
        glacier = new UnreliableGlaicerClient(credentials, recoverable);
    }
}

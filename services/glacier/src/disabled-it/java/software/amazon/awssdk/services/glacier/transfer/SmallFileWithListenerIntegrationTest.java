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
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.services.glacier.GlacierIntegrationTestBase;
import software.amazon.awssdk.test.util.RandomTempFile;

public class SmallFileWithListenerIntegrationTest extends GlacierIntegrationTestBase {
    private static final long contentLength = 1024 * 1024 - 123;
    private static final boolean DEBUG = false;
    private static final boolean cleanup = true;
    private File randomTempFile;
    private File downloadFile;
    private TestGlacierProgressListener progressListener = new TestGlacierProgressListener();

    @Before
    public void setup() throws IOException {
        randomTempFile = new RandomTempFile("SmallFileWithListenerIntegrationTest-", contentLength, true);
        if (DEBUG) {
            System.out.println("randomTempFile.length(): " + randomTempFile.length());
        }
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

    private ArchiveTransferManager newArchiveTransferManager() {
        return new ArchiveTransferManager(glacier, new StaticCredentialsProvider(credentials), new LegacyClientConfiguration());
    }

    // hchar: This test took about 4.2 hours to finish last time 

    /**
     * Tests that the progress listener can work correctly when doing single part upload and download.
     */
    @Test
    public void testSmallBinaryFileWithProgressListener() throws Exception {
        progressListener.reset();
        initializeClient();
        ArchiveTransferManager manager = newArchiveTransferManager();
        // Upload
        UploadResult uploadResult = manager.upload(accountId, vaultName,
                                                   "archive-description", randomTempFile, progressListener);
        String archiveId = uploadResult.getArchiveId();
        assertNotNull(archiveId);
        assertEquals(contentLength, progressListener.getTotalRequestBytesTransferred());
        assertTrue(progressListener.transferStarted);
        assertTrue(progressListener.transferCompleted);
        progressListener.reset();
        // Download
        downloadFile.createNewFile();
        manager.download(accountId, vaultName, archiveId, downloadFile, progressListener);

        assertTrue(progressListener.transferPreparing);
        assertEquals(contentLength, progressListener.getTotalResponseBytesTransferred());
        assertTrue(progressListener.transferStarted);
        assertTrue(progressListener.transferCompleted);

        assertFileEqualsFile(randomTempFile, downloadFile);
    }
}

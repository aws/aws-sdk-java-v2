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

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.services.glacier.GlacierIntegrationTestBase;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.test.util.RandomTempFile;

// hchar: last took ~14 hours to run this test
public class CustomizedDownloadChunkSizeIntegrationTest extends GlacierIntegrationTestBase {
    private static final long CONTENT_LENGTH = 1024 * 1024 * 3 - 123;
    private static final boolean DEBUG = false;
    private static final boolean CLEANUP = true;
    private File randomTempFile;
    private File downloadFile;

    @Before
    public void setup() throws IOException {
        randomTempFile = new RandomTempFile("CustomizedDownloadChunkSizeIntegrationTest", CONTENT_LENGTH);
        downloadFile = new File(randomTempFile.getParentFile(),
                                randomTempFile.getName() + ".download");

    }

    @After
    public void teanDown() {
        System.getProperties().remove("software.amazon.awssdk.services.glacier.transfer.downloadChunkSizeInMB");
        if (CLEANUP) {
            randomTempFile.delete();
            downloadFile.delete();
        }
    }

    @Test
    public void testGlacierUtilsWithCustomizedDownloadChunkSize() throws Exception {

        initializeClient();
        ArchiveTransferManager glacierUtils = newArchiveTransferManager();

        // Upload
        UploadResult uploadResult = glacierUtils.upload(vaultName, "archiveDescription", randomTempFile);
        String archiveId = uploadResult.getArchiveId();
        assertNotNull(archiveId);

        // Download
        if (DEBUG) {
            System.out.println("1) downloadFile=" + downloadFile + ", downloadFile.length()=" + downloadFile.length());
        }

        // Bad chunk size, not power of 2
        System.setProperty("software.amazon.awssdk.services.glacier.transfer.downloadChunkSizeInMB", "13");
        try {
            glacierUtils.download(vaultName, archiveId, downloadFile);
        } catch (AmazonClientException e) {
            assertNotNull(e.getMessage());
        }
        if (DEBUG) {
            System.out.println("2) downloadFile=" + downloadFile + ", downloadFile.length()=" + downloadFile.length());
        }

        // Customized chunk size 1 MB
        System.setProperty("software.amazon.awssdk.services.glacier.transfer.downloadChunkSizeInMB", "1");

        int retry = 0;
        for (; ; ) {
            try {
                glacierUtils.download(vaultName, archiveId, downloadFile);
                if (DEBUG) {
                    System.out.println("4) downloadFile=" + downloadFile + ", downloadFile.length()=" + downloadFile.length());
                }
                break;
            } catch (QueueDoesNotExistException ex) {
                if (retry++ >= 3) {
                    throw ex;
                }
                Thread.sleep(1000);
                System.out.println("Retrying download: " + retry + "\n"
                                   + " downloadFile=" + downloadFile
                                   + ", downloadFile.length()=" + downloadFile.length());
            }
        }
        assertFileEqualsFile(randomTempFile, downloadFile);
    }

    private ArchiveTransferManager newArchiveTransferManager() {
        return new ArchiveTransferManager(glacier, new StaticCredentialsProvider(credentials), new LegacyClientConfiguration());
    }
}

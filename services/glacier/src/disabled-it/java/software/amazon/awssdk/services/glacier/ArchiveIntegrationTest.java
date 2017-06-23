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

package software.amazon.awssdk.services.glacier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.glacier.model.CreateVaultRequest;
import software.amazon.awssdk.services.glacier.model.CreateVaultResult;
import software.amazon.awssdk.services.glacier.model.DeleteArchiveRequest;
import software.amazon.awssdk.services.glacier.model.DescribeJobRequest;
import software.amazon.awssdk.services.glacier.model.DescribeJobResult;
import software.amazon.awssdk.services.glacier.model.DescribeVaultOutput;
import software.amazon.awssdk.services.glacier.model.DescribeVaultRequest;
import software.amazon.awssdk.services.glacier.model.DescribeVaultResult;
import software.amazon.awssdk.services.glacier.model.GetJobOutputRequest;
import software.amazon.awssdk.services.glacier.model.GetJobOutputResult;
import software.amazon.awssdk.services.glacier.model.InitiateJobRequest;
import software.amazon.awssdk.services.glacier.model.InitiateJobResult;
import software.amazon.awssdk.services.glacier.model.JobParameters;
import software.amazon.awssdk.services.glacier.model.ListJobsRequest;
import software.amazon.awssdk.services.glacier.model.ListJobsResult;
import software.amazon.awssdk.services.glacier.model.ListVaultsRequest;
import software.amazon.awssdk.services.glacier.model.ListVaultsResult;
import software.amazon.awssdk.services.glacier.model.UploadArchiveRequest;
import software.amazon.awssdk.services.glacier.model.UploadArchiveResult;
import software.amazon.awssdk.test.util.RandomTempFile;

// hchar: last took ~7.5 hours to run
public class ArchiveIntegrationTest extends GlacierIntegrationTestBase {

    private static final String START_POSITION = "1048576";
    private static final String END_POSITION = "2097151";

    private static final boolean CLEANUP = true;
    private File randomTempFile;

    @Before
    public void setup() throws IOException {
        randomTempFile = new RandomTempFile("ArchiveIntegrationTest", CONTENT_LENGTH);

    }

    @After
    public void teanDown() {
        if (CLEANUP) {
            randomTempFile.delete();
        }
    }


    /**
     * Tests the basic archive operations for Glacier, including uploading to a
     * vault, listing archives, and downloading an archive.
     */
    @Test
    public void testArchiveOperations() throws Exception {
        initializeClient();

        // CreateVault
        CreateVaultResult createVaultResult = glacier.createVault(new CreateVaultRequest()
                                                                          .withAccountId(accountId)
                                                                          .withVaultName(vaultName));
        System.out.println("Request ID: " + createVaultResult.getLocation());


        // ListVaults
        ListVaultsResult listVaultsResult = glacier.listVaults(new ListVaultsRequest().withAccountId(accountId));
        assertNotNull(listVaultsResult.getVaultList());
        DescribeVaultOutput vault = listVaultsResult.getVaultList().get(0);
        // TODO: CreateDate and LastInventoryDate should probably be dates in the service model, not Strings
        assertNotNull(vault.getCreationDate());
        assertNotNull(vault.getSizeInBytes());
        assertNotNull(vault.getVaultARN());
        assertNotNull(vault.getVaultName());


        // ListVaults (without an accountId specified)
        listVaultsResult = glacier.listVaults(new ListVaultsRequest());
        assertNotNull(listVaultsResult.getVaultList());
        vault = listVaultsResult.getVaultList().get(0);
        // TODO: CreateDate and LastInventoryDate should probably be dates in the service model, not Strings
        assertNotNull(vault.getCreationDate());
        assertNotNull(vault.getSizeInBytes());
        assertNotNull(vault.getVaultARN());
        assertNotNull(vault.getVaultName());


        // DescribeVault
        DescribeVaultResult describeVaultResult = glacier.describeVault(new DescribeVaultRequest()
                                                                                .withAccountId(accountId)
                                                                                .withVaultName(vaultName));
        assertNotNull(describeVaultResult.getCreationDate());
        assertNotNull(describeVaultResult.getNumberOfArchives());
        assertNotNull(describeVaultResult.getSizeInBytes());
        assertNotNull(describeVaultResult.getVaultARN());
        assertNotNull(describeVaultResult.getVaultName());


        // DescribeVault (without an accountId specified)
        describeVaultResult = glacier.describeVault(new DescribeVaultRequest().withVaultName(vaultName));
        assertNotNull(describeVaultResult.getCreationDate());
        assertNotNull(describeVaultResult.getNumberOfArchives());
        assertNotNull(describeVaultResult.getSizeInBytes());
        assertNotNull(describeVaultResult.getVaultARN());
        assertNotNull(describeVaultResult.getVaultName());


        // ListJobs
        ListJobsResult listJobsResult = glacier.listJobs(new ListJobsRequest()
                                                                 .withAccountId(accountId).withVaultName(vaultName));
        listJobsResult.getJobList();


        InputStream is = new FileInputStream(randomTempFile);
        //        ResettableInputStream is = new ResettableInputStream(randomTempFile)
        //                .disableClose();
        // UploadArchive
        UploadArchiveRequest request = new UploadArchiveRequest()
                .withAccountId(accountId)
                .withArchiveDescription("archiveDescription")
                .withVaultName(vaultName)
                .withChecksum(TreeHashGenerator.calculateTreeHash(randomTempFile))
                .withContentLength(CONTENT_LENGTH)
                .withBody(is);

        UploadArchiveResult uploadArchiveResult = glacier.uploadArchive(request);
        is.close();
        System.out.println("Uploaded: " + uploadArchiveResult);
        String archiveId = parseResourceId(uploadArchiveResult.getLocation());
        assertNotNull(uploadArchiveResult.getChecksum());
        assertNotNull(uploadArchiveResult.getLocation());


        //        // ListUploads
        //        ListUploadsResult listUploadsResult = glacier.listUploads(new ListUploadsRequest()
        //                .withAccountId(accountId).withVaultName(vaultName));
        // TODO: no uploads ever show up?  is this only returning multipart uploads?
        //        assertTrue(listUploadsResult.getUploadsList().size() > 0);
        // TODO: UploadListElement is a bad type name
        //        UploadListElement uploadListElement = listUploadsResult.getUploadsList().get(0);
        //        assertNotNull(uploadListElement.getPartSizeInBytes());
        //        assertNotNull(uploadListElement.getVaultARN());


        // InitiateArchiveRetrieval
        // TODO: An enum in the model for types values (ex: "archive-retrieval") would help users
        InitiateJobResult initiateArchiveRetrievalResult =
                glacier.initiateJob(new InitiateJobRequest()
                                            .withAccountId(accountId)
                                            .withVaultName(vaultName)
                                            .withJobParameters(new JobParameters()
                                                                       .withArchiveId(archiveId)
                                                                       .withType("archive-retrieval")));
        System.out.println("Initiated: " + initiateArchiveRetrievalResult);
        String jobId = parseResourceId(initiateArchiveRetrievalResult.getLocation());
        assertNotNull(initiateArchiveRetrievalResult.getJobId());

        waitForJobToComplete(jobId);

        // GetJobOutput
        GetJobOutputResult getJobOutputResult = glacier.getJobOutput(new GetJobOutputRequest()
                                                                             .withAccountId(accountId)
                                                                             .withVaultName(vaultName)
                                                                             .withJobId(jobId));
        assertFileEqualsStream(randomTempFile, getJobOutputResult.getBody());

        // GetJobOutput (with range)
        getJobOutputResult = glacier.getJobOutput(new GetJobOutputRequest()
                                                          .withAccountId(accountId)
                                                          .withVaultName(vaultName)
                                                          .withRange("bytes=1023-2048")
                                                          .withJobId(jobId));
        drainInputStream(getJobOutputResult.getBody());

        // InitiateArchiveRangeRetrieval
        InitiateJobRequest initiateJobRequest = new InitiateJobRequest()
                .withAccountId(accountId)
                .withVaultName(vaultName)
                .withJobParameters(new JobParameters()
                                           .withRetrievalByteRange(START_POSITION + "-" + END_POSITION)
                                           .withArchiveId(archiveId)
                                           .withType("archive-retrieval"));
        initiateArchiveRetrievalResult = glacier.initiateJob(initiateJobRequest);
        System.out.println("Initiated: " + initiateArchiveRetrievalResult);
        jobId = parseResourceId(initiateArchiveRetrievalResult.getLocation());
        assertNotNull(initiateArchiveRetrievalResult.getJobId());

        waitForJobToComplete(jobId);

        // GetJobOutput (range retrieval)
        getJobOutputResult = glacier.getJobOutput(new GetJobOutputRequest()
                                                          .withAccountId(accountId)
                                                          .withVaultName(vaultName)
                                                          .withJobId(jobId));

        DescribeJobResult describeJobResult = glacier.describeJob(new DescribeJobRequest().withAccountId(accountId)
                                                                                          .withVaultName(vaultName)
                                                                                          .withJobId(jobId));
        assertEquals(START_POSITION + "-" + END_POSITION, describeJobResult.getRetrievalByteRange());
        drainInputStream(getJobOutputResult.getBody());

        // Delete the archive.
        glacier.deleteArchive(new DeleteArchiveRequest().withAccountId(accountId)
                                                        .withArchiveId(archiveId)
                                                        .withVaultName(vaultName));


    }

    private String parseResourceId(String artifactUrlResourcePath) {
        if (artifactUrlResourcePath == null) {
            return null;
        }

        int index = artifactUrlResourcePath.lastIndexOf('/');
        if (index < 0) {
            return null;
        }

        return artifactUrlResourcePath.substring(index + 1);
    }

    /**
     * Poll for the retrieval job to complete
     */
    private void waitForJobToComplete(String jobId) throws InterruptedException {
        // hchar: This test took almost 8 hours to finish last time I ran it 
        long endTime = Long.MAX_VALUE;
        DescribeJobResult describeJobResult = null;
        while (System.currentTimeMillis() < endTime) {
            Thread.sleep(1000 * 30);
            describeJobResult = glacier.describeJob(new DescribeJobRequest()
                                                            .withAccountId(accountId).withVaultName(vaultName)
                                                            .withJobId(jobId));
            System.out.println(String.valueOf(describeJobResult));
            if (describeJobResult.getCompleted()) {
                break;
            }
        }
        if (describeJobResult.getCompleted() == false) {
            fail("Retreive job for archive never completed accoutnId="
                 + accountId + ", vaultName=" + vaultName);
        }
    }

}

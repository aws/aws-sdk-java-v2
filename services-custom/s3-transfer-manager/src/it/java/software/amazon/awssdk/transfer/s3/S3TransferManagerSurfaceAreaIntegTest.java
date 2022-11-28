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

package software.amazon.awssdk.transfer.s3;

import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;
import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.model.CompletedCopy;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryUpload;
import software.amazon.awssdk.transfer.s3.model.Copy;
import software.amazon.awssdk.transfer.s3.model.DirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.DirectoryUpload;
import software.amazon.awssdk.transfer.s3.model.Download;
import software.amazon.awssdk.transfer.s3.model.DownloadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileUpload;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest;

public class S3TransferManagerSurfaceAreaIntegTest extends S3IntegrationTestBase {
    private static final String BUCKET = temporaryBucketName(S3TransferManagerCopyIntegrationTest.class);

    private static final String KEY = "key";

    private static final long OBJ_SIZE = 24 * MB;

    private static File sourceFile;


    @BeforeAll
    public static void setUp() throws Exception {
        createBucket(BUCKET);
        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(KEY)
                                     .build(), sourceFile.toPath());
        sourceFile = new RandomTempFile(OBJ_SIZE);
    }

    @AfterAll
    public static void teardown() throws Exception {
        deleteBucketAndAllContents(BUCKET);
        sourceFile.delete();
    }

    public void defaultClient() {
        S3TransferManager transferManager = S3TransferManager.create();

    }
    
    public void customClient() {
        S3AsyncClient s3AsyncClient = S3AsyncClient.crtBuilder()
                                                   .credentialsProvider(DefaultCredentialsProvider.create())
                                                   .region(Region.US_WEST_2)
                                                   .targetThroughputInGbps(20.0)
                                                   .minimumPartSizeInBytes(8 * MB)
                                                   .build();

        S3TransferManager transferManager =
            S3TransferManager.builder()
                             .s3AsyncClient(s3AsyncClient)
                             .build();
    }

    @Test
    public void downloadFile() {
        S3TransferManager transferManager = S3TransferManager.create();

        FileDownload download =
            transferManager.downloadFile(d -> d.getObjectRequest(g -> g.bucket(BUCKET).key(KEY)).destination(Paths.get(
                "myFile.txt")));

        // Wait for the transfer to complete
        download.completionFuture().join();
    }

    @Test
    public void download() {
        S3TransferManager transferManager = S3TransferManager.create();

        // Initiate the transfer
        Download<ResponseBytes<GetObjectResponse>> download =
            transferManager.download(d -> d.getObjectRequest(g -> g.bucket(BUCKET)
                                                                   .key(KEY))
                                           .responseTransformer(AsyncResponseTransformer.toBytes()));
        // Wait for the transfer to complete
        download.completionFuture().join();
    }

    @Test
    public void resumeDownloadFile() {
        S3TransferManager transferManager = S3TransferManager.create();

        // Initiate the transfer
        FileDownload download =
            transferManager.downloadFile(d -> d.getObjectRequest(g -> g.bucket(BUCKET).key(KEY)).destination(Paths.get(
                "myFile.txt")));

        // Pause the download
        ResumableFileDownload resumableFileDownload = download.pause(); // @link substring="pause" target="software.amazon.awssdk.transfer.s3.model.FileDownload#pause()"

        // Optionally, persist the download object
        Path path = Paths.get("resumableFileDownload.json");
        resumableFileDownload.serializeToFile(path);

        // Retrieve the resumableFileDownload from the file
        resumableFileDownload = ResumableFileDownload.fromFile(path);

        // Resume the download
        FileDownload resumedDownload = transferManager.resumeDownloadFile(resumableFileDownload);

        // Wait for the transfer to complete
        resumedDownload.completionFuture().join();
    }

    @Test
    public void resumeUploadFile() {
        S3TransferManager transferManager = S3TransferManager.create();

        // Initiate the transfer
        FileUpload upload =
            transferManager.uploadFile(d -> d.putObjectRequest(g -> g.bucket(BUCKET).key(KEY)).source(Paths.get("myFile"
                                                                                                                    + ".txt")));
        // Pause the upload
        ResumableFileUpload resumableFileUpload = upload.pause();

        // Optionally, persist the resumableFileUpload
        Path path = Paths.get("resumableFileUpload.json");
        resumableFileUpload.serializeToFile(path);

        // Retrieve the resumableFileUpload from the file
        ResumableFileUpload persistedResumableFileUpload = ResumableFileUpload.fromFile(path);

        // Resume the upload
        FileUpload resumedUpload = transferManager.resumeUploadFile(persistedResumableFileUpload);

        // Wait for the transfer to complete
        resumedUpload.completionFuture().join();
    }

    @Test
    public void uploadFile() {
        // @start region=uploadFile
        S3TransferManager transferManager = S3TransferManager.create();

        FileUpload upload = transferManager.uploadFile(u -> u.source(Paths.get("myFile.txt"))
                                                             .putObjectRequest(p -> p.bucket(BUCKET).key(
                                                                 KEY)));
        upload.completionFuture().join();
        // @end region=uploadFile
    }

    public void upload() {
        S3TransferManager transferManager = S3TransferManager.create();

        Upload upload = transferManager.upload(u -> u.requestBody(AsyncRequestBody.fromString("Hello world"))
                                                     .putObjectRequest(p -> p.bucket(BUCKET).key(KEY)));
        // Wait for the transfer to complete
        upload.completionFuture().join();
    }

    @Test
    public void uploadDirectory() {
        S3TransferManager transferManager = S3TransferManager.create();
        DirectoryUpload directoryUpload =
            transferManager.uploadDirectory(UploadDirectoryRequest.builder()
                                                                  .sourceDirectory(Paths.get("source/directory"))
                                                                  .bucket(BUCKET)
                                                                  .s3Prefix("prefix")
                                                                  .build());

        // Wait for the transfer to complete
        CompletedDirectoryUpload completedDirectoryUpload = directoryUpload.completionFuture().join();

        // Print out the failed uploads
        completedDirectoryUpload.failedTransfers().forEach(System.out::println);
    }

    @Test
    public void downloadDirectory() {
        S3TransferManager transferManager = S3TransferManager.create();
        DirectoryDownload directoryDownload =
            transferManager.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                      .destination(Paths.get("destination/directory"))
                                                                      .bucket(BUCKET)
                                                                      .listObjectsV2RequestTransformer(l -> l.prefix("prefix"))
                                                                      .build());
        // Wait for the transfer to complete
        CompletedDirectoryDownload completedDirectoryDownload = directoryDownload.completionFuture().join();

        // Print out the failed downloads
        completedDirectoryDownload.failedTransfers().forEach(System.out::println);
    }

    @Test
    public void copy() {
        S3TransferManager transferManager = S3TransferManager.create();
        Copy copy =
            transferManager.copy(c -> c.copyObjectRequest(r -> r.sourceBucket(BUCKET)
                                                                .sourceKey("source_key")
                                                                .destinationBucket(BUCKET)
                                                                .destinationKey("dest_key")));
        // Wait for the transfer to complete
        CompletedCopy completedCopy = copy.completionFuture().join();
    }
}

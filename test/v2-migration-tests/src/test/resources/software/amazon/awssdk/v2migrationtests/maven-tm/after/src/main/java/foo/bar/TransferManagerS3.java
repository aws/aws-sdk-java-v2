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

package foo.bar;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.Copy;
import software.amazon.awssdk.transfer.s3.model.CopyRequest;
import software.amazon.awssdk.transfer.s3.model.DirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.DirectoryUpload;
import software.amazon.awssdk.transfer.s3.model.DownloadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileUpload;
import software.amazon.awssdk.transfer.s3.model.ResumableTransfer;
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferProgress;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;

public class TransferManagerS3 {

    File file = new File("path/to/file.txt");

    void tmConstructor(AwsCredentials credentials, AwsCredentialsProvider credentialsProvider) {
        S3TransferManager tm = S3TransferManager.builder()
                .build();
        S3TransferManager tmBuilderDefault = S3TransferManager.create();
        S3TransferManager tmBuilderWithS3 = S3TransferManager.builder().build();
        S3TransferManager tmConstructorWithCred = S3TransferManager.builder().s3Client(S3AsyncClient.builder().credentialsProvider(StaticCredentialsProvider.create(credentials)).build()).build();
        S3TransferManager tmConstructorWithCredProvider = S3TransferManager.builder().s3Client(S3AsyncClient.builder().credentialsProvider(credentialsProvider).build()).build();
    }

    void download(S3TransferManager tm, String bucket, String key) {
        FileDownload download = tm.downloadFile(DownloadFileRequest.builder().getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build()).destination(file).build());

        long timeout = 89;
        FileDownload download2 = tm.downloadFile(DownloadFileRequest.builder().getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).overrideConfiguration(AwsRequestOverrideConfiguration.builder().apiCallTimeout(Duration.ofMillis(timeout)).build()).build()).destination(file).build());

        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucket).key(key)
                .build();

        FileDownload download3 = tm.downloadFile(DownloadFileRequest.builder().getObjectRequest(getObjectRequest).destination(file).build());

        FileDownload download4 = tm.downloadFile(DownloadFileRequest.builder().getObjectRequest(getObjectRequest.toBuilder().overrideConfiguration(getObjectRequest.overrideConfiguration().get().toBuilder().apiCallTimeout(Duration.ofMillis(timeout)).build()).build()).destination(file).build());
    }

    void upload(S3TransferManager tm, String bucket, String key) {
        tm.uploadFile(UploadFileRequest.builder().putObjectRequest(PutObjectRequest.builder().bucket(bucket).key(key).build()).source(file).build());

        File file = new File("file1.txt");
        PutObjectRequest requestWithFile = PutObjectRequest.builder().bucket(bucket).key(key)
                .build();
        tm.uploadFile(UploadFileRequest.builder().putObjectRequest(requestWithFile).source(file).build());

        PutObjectRequest requestWithoutPayload = PutObjectRequest.builder().bucket(bucket).key(key).websiteRedirectLocation("location")
                .build();
        tm.upload(UploadRequest.builder().putObjectRequest(requestWithoutPayload).requestBody(AsyncRequestBody.empty()).build());
    }

    void copy(S3TransferManager tm, String sourceBucket, String sourceKey, String destinationBucket, String destinationKey) {
        Copy copy = tm.copy(CopyRequest.builder().copyObjectRequest(CopyObjectRequest.builder().sourceBucket(sourceBucket).sourceKey(sourceKey).destinationBucket(destinationBucket).destinationKey(destinationKey).build()).build());

        CopyObjectRequest copyRequest = CopyObjectRequest.builder().sourceBucket(sourceBucket).sourceKey(sourceKey).destinationBucket(destinationBucket).destinationKey(destinationKey)
                .build();
        Copy copy2 = tm.copy(CopyRequest.builder().copyObjectRequest(copyRequest).build());
    }

    void downloadDirectory(S3TransferManager tm, File destination) {
        DirectoryDownload fileDownload = tm.downloadDirectory(DownloadDirectoryRequest.builder().bucket("bucket").listObjectsV2RequestTransformer(builder -> builder.prefix("key")).destination(destination.toPath()).build());
        tm.close();
    }

    void uploadDirectory(S3TransferManager tm) {
        DirectoryUpload fileUpload1 = tm.uploadDirectory(UploadDirectoryRequest.builder().bucket("bucket").s3Prefix("prefix").source(file.toPath()).maxDepth(true ? Integer.MAX_VALUE : 1).build());
    }

    void resume(S3TransferManager tm, ResumableFileDownload persistableDownload, ResumableFileUpload persistableUpload) {
        FileDownload download = tm.resumeDownloadFile(persistableDownload);
        FileUpload upload = tm.resumeUploadFile(persistableUpload);
    }

    void POJO_methods(ResumableTransfer transfer, OutputStream outputStream, TransferProgress progress) throws IOException {
        String s = transfer.serializeToString();
        transfer.serializeToOutputStream(outputStream);

        long bytesTransferred = progress.snapshot().transferredBytes();
    }
}
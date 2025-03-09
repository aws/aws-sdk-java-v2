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

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.Copy;
import software.amazon.awssdk.transfer.s3.model.CopyRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

import java.io.File;
import java.time.Duration;

public class TransferManagerS3 {

    File file = new File("path/to/file.txt");

    void tmConstructor() {
        S3TransferManager tm = S3TransferManager.builder()
                .build();
        S3TransferManager tmBuilderDefault = S3TransferManager.create();
        S3TransferManager tmBuilderWithS3 = S3TransferManager.builder().build();
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
    }

    void copy(S3TransferManager tm, String sourceBucket, String sourceKey, String destinationBucket, String destinationKey) {
        Copy copy = tm.copy(CopyRequest.builder().copyObjectRequest(CopyObjectRequest.builder().sourceBucket(sourceBucket).sourceKey(sourceKey).destinationBucket(destinationBucket).destinationKey(destinationKey).build()).build());

        CopyObjectRequest copyRequest = CopyObjectRequest.builder().sourceBucket(sourceBucket).sourceKey(sourceKey).destinationBucket(destinationBucket).destinationKey(destinationKey)
                .build();
        Copy copy2 = tm.copy(CopyRequest.builder().copyObjectRequest(copyRequest).build());
    }
}

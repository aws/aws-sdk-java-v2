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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.Copy;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.MultipleFileDownload;
import com.amazonaws.services.s3.transfer.PersistableDownload;
import com.amazonaws.services.s3.transfer.PersistableUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import java.io.File;

public class TransferManagerS3 {

    File file = new File("path/to/file.txt");

    void tmConstructor(AWSCredentials credentials, AWSCredentialsProvider credentialsProvider) {
        TransferManager tm = new TransferManager();
        TransferManager tmBuilderDefault = TransferManagerBuilder.defaultTransferManager();
        TransferManager tmBuilderWithS3 = TransferManagerBuilder.standard().build();
        TransferManager tmConstructorWithCred = new TransferManager(credentials);
        TransferManager tmConstructorWithCredProvider = new TransferManager(credentialsProvider);
    }

    void download(TransferManager tm, String bucket, String key) {
        Download download = tm.download(bucket, key, file);

        long timeout = 89;
        Download download2 = tm.download(bucket, key, file, timeout);

        GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, key);

        Download download3 = tm.download(getObjectRequest, file);

        Download download4 = tm.download(getObjectRequest, file, timeout);
    }

    void upload(TransferManager tm, String bucket, String key) {
        tm.upload(bucket, key, file);

        File file = new File("file1.txt");
        PutObjectRequest requestWithFile = new PutObjectRequest(bucket, key, file);
        tm.upload(requestWithFile);

        PutObjectRequest requestWithoutPayload = new PutObjectRequest(bucket, key, "location");
        tm.upload(requestWithoutPayload);
    }

    void copy(TransferManager tm, String sourceBucket, String sourceKey, String destinationBucket, String destinationKey) {
        Copy copy = tm.copy(sourceBucket, sourceKey, destinationBucket, destinationKey);

        CopyObjectRequest copyRequest = new CopyObjectRequest(sourceBucket, sourceKey, destinationBucket, destinationKey);
        Copy copy2 = tm.copy(copyRequest);
    }

    void downloadDirectory(TransferManager tm, File destination) {
        MultipleFileDownload fileDownload = tm.downloadDirectory("bucket", "key", destination);
        tm.shutdownNow();
    }

    void resume(TransferManager tm, PersistableDownload persistableDownload, PersistableUpload persistableUpload) {
        Download download = tm.resumeDownload(persistableDownload);
        Upload upload = tm.resumeUpload(persistableUpload);
    }
}

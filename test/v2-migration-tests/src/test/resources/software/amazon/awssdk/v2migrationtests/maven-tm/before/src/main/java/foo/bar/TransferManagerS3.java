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

import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.transfer.Copy;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import java.io.File;

public class TransferManagerS3 {

    File file = new File("path/to/file.txt");

    void tmConstructor() {
        TransferManager tm = new TransferManager();
        TransferManager tmBuilderDefault = TransferManagerBuilder.defaultTransferManager();
        TransferManager tmBuilderWithS3 = TransferManagerBuilder.standard().build();
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
    }

    void copy(TransferManager tm, String sourceBucket, String sourceKey, String destinationBucket, String destinationKey) {
        Copy copy = tm.copy(sourceBucket, sourceKey, destinationBucket, destinationKey);

        CopyObjectRequest copyRequest = new CopyObjectRequest(sourceBucket, sourceKey, destinationBucket, destinationKey);
        Copy copy2 = tm.copy(copyRequest);
    }
}

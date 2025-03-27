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

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;

public class S3Transforms {

    void upload(TransferManager tm, String bucket, String key) {
        InputStream inputStream = new ByteArrayInputStream(("HelloWorld").getBytes());
        PutObjectRequest requestWithInputStream = new PutObjectRequest(bucket, key, "location");
        requestWithInputStream.setInputStream(inputStream);
        tm.upload(requestWithInputStream);
    }

    private void generatePresignedUrl(AmazonS3 s3, String bucket, String key, Date expiration) {
        URL urlHead = s3.generatePresignedUrl(bucket, key, expiration, HttpMethod.HEAD);

        URL urlPatch = s3.generatePresignedUrl(bucket, key, expiration, HttpMethod.PATCH);

        URL urlPost = s3.generatePresignedUrl(bucket, key, expiration, HttpMethod.POST);


        HttpMethod httpMethod = HttpMethod.PUT;
        URL urlWithHttpMethodVariable = s3.generatePresignedUrl(bucket, key, expiration, httpMethod);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key);
        s3.generatePresignedUrl(request);
    }
}

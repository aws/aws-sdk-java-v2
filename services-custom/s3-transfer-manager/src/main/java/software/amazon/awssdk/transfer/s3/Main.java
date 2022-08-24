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

import java.net.URI;
import java.nio.file.Paths;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;

// Reproduce Github customer's issue
public class Main {
    public static void main(String[] args) {

        S3TransferManager transferManager =
            S3TransferManager.builder()
                             .s3ClientConfiguration(cfg -> cfg.credentialsProvider(AnonymousCredentialsProvider.create())
                                .region(Region.EU_CENTRAL_1)
            .targetThroughputInGbps(20.0)
            .minimumPartSizeInBytes(10L * 1000)
            .maxConcurrency(8)
            .endpointOverride(URI.create("http://localhost:8080"))
                        )
                        .build();

        transferManager.uploadFile(b -> b.source(Paths.get("file_path")));


    }
}
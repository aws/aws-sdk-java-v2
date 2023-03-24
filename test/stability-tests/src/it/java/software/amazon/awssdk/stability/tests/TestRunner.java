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

package software.amazon.awssdk.stability.tests;


import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload;

/**
 * The main method will be invoked when you execute the test jar generated from "mvn package -P test-jar"
 * <p>
 * You can add the tests in the main method. eg: try { S3AsyncStabilityTest s3AsyncStabilityTest = new S3AsyncStabilityTest();
 * S3AsyncStabilityTest.setup(); s3AsyncStabilityTest.putObject_getObject(); } finally { S3AsyncStabilityTest.cleanup(); }
 */
public class TestRunner {

    public static void main(String... args) {
        String bucket = "bucket";
        S3TransferManager transferManager = S3TransferManager.builder()
                                                             .s3Client(S3AsyncClient.crtBuilder().region(Region.EU_WEST_1).build()).build();


        CompletableFuture<CompletedDirectoryDownload> future = transferManager.downloadDirectory(b -> b.bucket(bucket)
                                                                                                       .destination(Paths.get(
                                                                                                           "/tmp/test/1"))
                                                                                                       .listObjectsV2RequestTransformer(l -> l.prefix("16M_dir/")))
                                                                              .completionFuture();


        CompletableFuture<CompletedDirectoryDownload> future2 = transferManager.downloadDirectory(b -> b.bucket(bucket)
                                                                                                       .destination(Paths.get(
                                                                                                           "/tmp/test/2"))
                                                                                                       .listObjectsV2RequestTransformer(l -> l.prefix("16M_dir/")
                                                                                                           ))
                                                                              .completionFuture();

        future.join().failedTransfers().forEach(i -> System.out.println("failed " + i));
        future2.join().failedTransfers().forEach(i -> System.out.println("failed " + i));
    }
}


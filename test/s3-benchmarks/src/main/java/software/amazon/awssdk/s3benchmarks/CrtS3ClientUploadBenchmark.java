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

package software.amazon.awssdk.s3benchmarks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.http.HttpRequestBodyStream;
import software.amazon.awssdk.crt.s3.S3MetaRequest;
import software.amazon.awssdk.crt.s3.S3MetaRequestOptions;
import software.amazon.awssdk.crt.s3.S3MetaRequestResponseHandler;

public class CrtS3ClientUploadBenchmark extends BaseCrtClientBenchmark {

    private final String filepath;

    public CrtS3ClientUploadBenchmark(TransferManagerBenchmarkConfig config ) {
        super(config);
        this.filepath = config.filePath();
    }

    @Override
    public void sendOneRequest(List<Double> latencies) throws IOException  {
        CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        S3MetaRequestResponseHandler responseHandler = new TestS3MetaRequestResponseHandler(resultFuture);

        String endpoint = bucket + ".s3." + region + ".amazonaws.com";

        // ByteBuffer payload = ByteBuffer.wrap(Files.readAllBytes(Paths.get(filepath)));
        File uploadFile = new File(filepath);
        HttpRequestBodyStream payloadStream = new HttpRequestBodyStream() {
            @Override
            public boolean sendRequestBody(ByteBuffer outBuffer) {
                try (FileInputStream fis = new FileInputStream(uploadFile)) {
                    int b;
                    while ((b = fis.read()) > -1) {
                        outBuffer.put((byte) b);
                    }
                } catch (IOException ioe) {
                    throw new UncheckedIOException(ioe);
                }
                return true;
            }

            @Override
            public boolean resetPosition() {
                return true;
            }

            @Override
            public long getLength() {
                return uploadFile.length();
            }
        };

        HttpHeader[] headers = { new HttpHeader("Host", endpoint),
                                 new HttpHeader("Content-Length", String.valueOf(uploadFile.length())) };
        HttpRequest httpRequest = new HttpRequest("PUT", "/put_object_test_1MB.txt", headers, payloadStream);

        S3MetaRequestOptions metaRequestOptions = new S3MetaRequestOptions()
            .withMetaRequestType(S3MetaRequestOptions.MetaRequestType.PUT_OBJECT)
            .withHttpRequest(httpRequest)
            .withResponseHandler(responseHandler);

        long start = System.currentTimeMillis();
        try (S3MetaRequest metaRequest = crtS3Client.makeMetaRequest(metaRequestOptions)) {
            resultFuture.get(10, TimeUnit.MINUTES);
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        long end = System.currentTimeMillis();
        latencies.add((end - start) / 1000.0);
    }

}

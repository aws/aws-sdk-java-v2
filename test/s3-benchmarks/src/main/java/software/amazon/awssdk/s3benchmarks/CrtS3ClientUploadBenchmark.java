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

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.http.HttpRequestBodyStream;
import software.amazon.awssdk.crt.s3.S3MetaRequest;
import software.amazon.awssdk.crt.s3.S3MetaRequestOptions;
import software.amazon.awssdk.crt.s3.S3MetaRequestResponseHandler;
import software.amazon.awssdk.crt.utils.ByteBufferUtils;

public class CrtS3ClientUploadBenchmark extends BaseCrtClientBenchmark {

    private final String filepath;

    public CrtS3ClientUploadBenchmark(TransferManagerBenchmarkConfig config) {
        super(config);
        this.filepath = config.filePath();
    }

    @Override
    public void sendOneRequest(List<Double> latencies) throws Exception {
        CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        S3MetaRequestResponseHandler responseHandler = new TestS3MetaRequestResponseHandler(resultFuture);

        String endpoint = bucket + ".s3." + region + ".amazonaws.com";

        ByteBuffer payload = ByteBuffer.wrap(Files.readAllBytes(Paths.get(filepath)));
        HttpRequestBodyStream payloadStream = new PayloadStream(payload);

        HttpHeader[] headers = {new HttpHeader("Host", endpoint)};
        HttpRequest httpRequest = new HttpRequest(
            "PUT",
            "/" + key,
            headers,
            payloadStream);

        S3MetaRequestOptions metaRequestOptions = new S3MetaRequestOptions()
            .withEndpoint(URI.create("https://" + endpoint))
            .withMetaRequestType(S3MetaRequestOptions.MetaRequestType.PUT_OBJECT)
            .withHttpRequest(httpRequest)
            .withResponseHandler(responseHandler);

        long start = System.currentTimeMillis();
        try (S3MetaRequest metaRequest = crtS3Client.makeMetaRequest(metaRequestOptions)) {
            resultFuture.get(10, TimeUnit.MINUTES);
        }
        long end = System.currentTimeMillis();
        latencies.add((end - start) / 1000.0);
    }

    private static class PayloadStream implements HttpRequestBodyStream {
        private ByteBuffer payload;

        private PayloadStream(ByteBuffer payload) {
            this.payload = payload;
        }

        @Override
        public boolean sendRequestBody(ByteBuffer outBuffer) {
            ByteBufferUtils.transferData(payload, outBuffer);
            return payload.remaining() == 0;
        }

        @Override
        public boolean resetPosition() {
            return true;
        }

        @Override
        public long getLength() {
            return payload.capacity();
        }

    }
}

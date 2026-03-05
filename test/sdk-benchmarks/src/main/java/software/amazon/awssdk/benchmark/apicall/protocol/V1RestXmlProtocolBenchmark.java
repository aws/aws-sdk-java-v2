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

package software.amazon.awssdk.benchmark.apicall.protocol;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;


@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class V1RestXmlProtocolBenchmark {
    private ProtocolBenchmarkServer server;
    private AmazonS3 s3;
    private byte[] testData;

    @Setup
    public void setup() throws Exception {
        server = new ProtocolBenchmarkServer();
        server.start();

        ClientConfiguration config = new ClientConfiguration();
        config.setDisableSocketProxy(true);
        System.setProperty("com.amazonaws.services.s3.disableGetObjectMD5Validation", "true");
        System.setProperty("com.amazonaws.services.s3.disablePutObjectMD5Validation", "true");
        
        s3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials("test", "test")))
                .withClientConfiguration(config)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                        server.getHttpUri().toString(), "us-east-1"))
                .withPathStyleAccessEnabled(true)
                .build();

        testData = "test-data".getBytes();
    }

    @TearDown
    public void tearDown() throws Exception {
        s3.shutdown();
        server.stop();
    }

    @Benchmark
    public void putObject() {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("application/octet-stream");
        metadata.setContentEncoding("gzip");
        metadata.setCacheControl("max-age=3600");
        metadata.setSSEAlgorithm("AES256");
        metadata.addUserMetadata("key1", "value1");
        metadata.addUserMetadata("key2", "value2");
        metadata.setContentLength(testData.length);

        PutObjectRequest request = new PutObjectRequest(
                "test-bucket",
                "test-key",
                new ByteArrayInputStream(testData),
                metadata)
                .withCannedAcl(CannedAccessControlList.Private);

        s3.putObject(request);
    }
}

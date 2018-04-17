/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.machinelearning;

import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.machinelearning.model.CreateDataSourceFromS3Request;
import software.amazon.awssdk.services.machinelearning.model.CreateDataSourceFromS3Response;
import software.amazon.awssdk.services.machinelearning.model.CreateMLModelRequest;
import software.amazon.awssdk.services.machinelearning.model.CreateMLModelResponse;
import software.amazon.awssdk.services.machinelearning.model.CreateRealtimeEndpointRequest;
import software.amazon.awssdk.services.machinelearning.model.DeleteDataSourceRequest;
import software.amazon.awssdk.services.machinelearning.model.DeleteMLModelRequest;
import software.amazon.awssdk.services.machinelearning.model.DeleteRealtimeEndpointRequest;
import software.amazon.awssdk.services.machinelearning.model.EntityStatus;
import software.amazon.awssdk.services.machinelearning.model.MLModelType;
import software.amazon.awssdk.services.machinelearning.model.Prediction;
import software.amazon.awssdk.services.machinelearning.model.PredictorNotMountedException;
import software.amazon.awssdk.services.machinelearning.model.RealtimeEndpointStatus;
import software.amazon.awssdk.services.machinelearning.model.S3DataSpec;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.testutils.Waiter;
import software.amazon.awssdk.testutils.service.AwsTestBase;

public class AmazonMachineLearningIntegrationTest extends AwsTestBase {

    private static final String BUCKET_NAME = temporaryBucketName("aws-java-sdk-eml-test");

    private static final String KEY = "data.csv";

    private static final String DATA_LOCATION_S3 =
            "s3://" + BUCKET_NAME + "/" + KEY;


    private static final String DATA = "0, 42, foo, bar\n1, 38, alice, bob";

    private static final String DATA_SCHEMA = "{"
                                              + "\"version\":\"1.0\","
                                              + "\"recordAnnotationFieldName\":null,"
                                              + "\"recordWeightFieldName\":null,"
                                              + "\"targetFieldName\":\"a\","
                                              + "\"dataFormat\":\"CSV\","
                                              + "\"dataFileContainsHeader\":false,"
                                              + "\"variables\": ["
                                              + "{"
                                              + "\"fieldName\":\"a\","
                                              + "\"fieldType\":\"BINARY\""
                                              + "},{"
                                              + "\"fieldName\":\"b\","
                                              + "\"fieldType\":\"NUMERIC\""
                                              + "},{"
                                              + "\"fieldName\":\"c\","
                                              + "\"fieldType\":\"CATEGORICAL\""
                                              + "},{"
                                              + "\"fieldName\":\"d\","
                                              + "\"fieldType\":\"TEXT\""
                                              + "}"
                                              + "]"
                                              + "}";

    private static S3Client s3;
    private static MachineLearningClient client;

    private static String dataSourceId;
    private static String mlModelId;

    @BeforeClass
    public static void setUp() throws IOException {
        setUpCredentials();
        setUpS3();

        System.setProperty("software.amazon.awssdk.sdk.disableCertChecking", "true");

        client = MachineLearningClient.builder()
                                      .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                      .region(Region.US_EAST_1)
                                      .build();
    }

    private static void setUpS3() {
        s3 = S3Client.builder()
                     .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                     .region(Region.US_EAST_1)
                     .build();

        s3.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());

        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET_NAME)
                                     .key(KEY)
                                     .acl(ObjectCannedACL.PUBLIC_READ)
                                     .build(),
                     RequestBody.fromBytes(DATA.getBytes()));
    }

    @AfterClass
    public static void cleanUp() {
        if (client != null) {
            if (mlModelId != null) {
                try {
                    client.deleteRealtimeEndpoint(DeleteRealtimeEndpointRequest.builder()
                                                          .mlModelId(mlModelId).build());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    client.deleteMLModel(DeleteMLModelRequest.builder()
                                                 .mlModelId(mlModelId).build());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (dataSourceId != null) {
                try {
                    client.deleteDataSource(DeleteDataSourceRequest.builder()
                                                    .dataSourceId(dataSourceId).build());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (s3 != null) {
            try {
                s3.deleteObject(DeleteObjectRequest.builder().bucket(BUCKET_NAME).key(KEY).build());
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                s3.deleteBucket(DeleteBucketRequest.builder().bucket(BUCKET_NAME).build());
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    @Test
    public void testBinary() throws Exception {
        CreateDataSourceFromS3Response result =
                client.createDataSourceFromS3(CreateDataSourceFromS3Request.builder()
                        .dataSpec(S3DataSpec.builder()
                                .dataLocationS3(DATA_LOCATION_S3)
                                .dataSchema(DATA_SCHEMA).build())
                        .computeStatistics(true)
                        .dataSourceId("data_" + System.currentTimeMillis())
                        .build());

        dataSourceId = result.dataSourceId();

        waitForDataSource();

        CreateMLModelResponse result2 =
                client.createMLModel(CreateMLModelRequest.builder()
                        .trainingDataSourceId(dataSourceId)
                        .mlModelType(MLModelType.BINARY)
                        .mlModelId("mlid_" + System.currentTimeMillis())
                        .build());

        mlModelId = result2.mlModelId();

        waitForMlModel();


        client.createRealtimeEndpoint(CreateRealtimeEndpointRequest.builder()
                .mlModelId(mlModelId)
                .build());

        String uri = waitUntilMounted();

        HashMap<String, String> record = new HashMap<String, String>() {
            {
                put("b", "123");
                put("c", "oop");
                put("d", "goop");
            }
        };

        Prediction prediction = Waiter.run(() -> client.predict(r -> r.predictEndpoint(uri).mlModelId(mlModelId).record(record)))
                                      .ignoringException(PredictorNotMountedException.class)
                                      .orFailAfter(Duration.ofMinutes(5))
                                      .prediction();

        System.out.println(prediction.predictedLabel());
        System.out.println(prediction.predictedValue());
        System.out.println(prediction.predictedScores());
        System.out.println(prediction.details());
    }

    private void waitForDataSource() throws InterruptedException {
        Waiter.run(() -> client.getDataSource(r -> r.dataSourceId(dataSourceId)))
              .until(r -> r.status() == EntityStatus.COMPLETED)
              .orFailAfter(Duration.ofMinutes(10));
    }

    private void waitForMlModel() throws InterruptedException {
        Waiter.run(() -> client.getMLModel(r -> r.mlModelId(mlModelId)))
              .until(r -> r.status() == EntityStatus.COMPLETED)
              .orFailAfter(Duration.ofMinutes(10));
    }

    private String waitUntilMounted() throws InterruptedException {
        return Waiter.run(() -> client.getMLModel(r -> r.mlModelId(mlModelId)))
                     .until(r -> r.endpointInfo() != null &&
                                 r.endpointInfo().endpointStatus() == RealtimeEndpointStatus.READY &&
                                 r.status() == EntityStatus.COMPLETED)
                     .orFailAfter(Duration.ofMinutes(10))
                     .endpointInfo()
                     .endpointUrl();
    }
}
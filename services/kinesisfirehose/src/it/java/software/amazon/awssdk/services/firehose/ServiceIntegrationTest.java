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

package software.amazon.awssdk.services.firehose;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.testutils.SdkAsserts.assertNotEmpty;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.firehose.model.CreateDeliveryStreamRequest;
import software.amazon.awssdk.services.firehose.model.ListDeliveryStreamsRequest;
import software.amazon.awssdk.services.firehose.model.ListDeliveryStreamsResponse;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchRequest;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchResponseEntry;
import software.amazon.awssdk.services.firehose.model.PutRecordRequest;
import software.amazon.awssdk.services.firehose.model.Record;
import software.amazon.awssdk.services.firehose.model.S3DestinationConfiguration;
import software.amazon.awssdk.testutils.service.AwsTestBase;


public class ServiceIntegrationTest extends AwsTestBase {

    private static final String DEVLIVERY_STREAM_NAME = "java-sdk-delivery-stream-"
                                                        + System.currentTimeMillis();
    private static final String FAKE_S3_BUCKET_ARN = "arn:aws:s3:::fake-s3-bucket-arn";
    private static final String FAKE_IAM_ROLE_ARN = "arn:aws:iam:::fake-iam-role-arn";

    private static FirehoseClient firehose;


    @BeforeClass
    public static void setup() throws FileNotFoundException, IOException {
        //        setUpCredentials();
        //        firehose = new AmazonKinesisFirehoseClient(credentials);
        //        s3 = new AmazonS3Client(credentials);

        // TODO: firehose can't whitelist our shared account at this point, so
        // for now we are using the test account provided by the firehose team
        ProfileCredentialsProvider firehostTestCreds = ProfileCredentialsProvider.builder().profileName("firehose-test").build();
        firehose = FirehoseClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
    }

    @AfterClass
    public static void tearDown() {
        //        firehose.deleteDeliveryStream(new DeleteDeliveryStreamRequest()
        //                .withDeliveryStreamName(DEVLIVERY_STREAM_NAME));
    }

    //    @Test
    // Nope, can't make it work without full access to S3 and IAM
    public void testOperations() {

        // create delivery stream
        CreateDeliveryStreamRequest request =
                CreateDeliveryStreamRequest.builder()
                                           .deliveryStreamName(DEVLIVERY_STREAM_NAME)
                                           .s3DestinationConfiguration(S3DestinationConfiguration.builder()
                                                                                                 .bucketARN(FAKE_S3_BUCKET_ARN)
                                                                                                 .roleARN(FAKE_IAM_ROLE_ARN)
                                                                                                 .build())
                                           .build();
        firehose.createDeliveryStream(request);

        // put record
        String recordId = firehose.putRecord(PutRecordRequest.builder()
                                                             .deliveryStreamName(DEVLIVERY_STREAM_NAME)
                                                             .record(Record.builder()
                                                                           .data(SdkBytes.fromByteArray(new byte[] {0, 1, 2}))
                                                                           .build())
                                                             .build()
                                            ).recordId();
        assertNotEmpty(recordId);

        // put record batch
        List<PutRecordBatchResponseEntry> entries = firehose.putRecordBatch(
                PutRecordBatchRequest.builder()
                                     .deliveryStreamName(DEVLIVERY_STREAM_NAME)
                                     .records(Record.builder().data(SdkBytes.fromByteArray(new byte[] {0})).build(),
                                              Record.builder().data(SdkBytes.fromByteArray(new byte[] {1})).build())
                                     .build()
                                                                           ).requestResponses();
        assertEquals(2, entries.size());
        for (PutRecordBatchResponseEntry entry : entries) {
            if (entry.errorCode() == null) {
                assertNotEmpty(entry.recordId());
            } else {
                assertNotEmpty(entry.errorMessage());
            }
        }
    }

    @Test
    public void testListDeliveryStreams() {
        ListDeliveryStreamsResponse result = firehose
                .listDeliveryStreams(ListDeliveryStreamsRequest.builder().build());
        assertNotNull(result.deliveryStreamNames());
        assertNotNull(result.hasMoreDeliveryStreams());
    }

    @Test
    public void testCreateDeliveryStream_InvalidParameter() {
        try {
            firehose.createDeliveryStream(CreateDeliveryStreamRequest.builder().build());
            fail("ValidationException is expected.");
        } catch (AwsServiceException exception) {
            assertEquals("ValidationException", exception.awsErrorDetails().errorCode());
            assertNotEmpty(exception.awsErrorDetails().errorMessage());
        }
    }

}

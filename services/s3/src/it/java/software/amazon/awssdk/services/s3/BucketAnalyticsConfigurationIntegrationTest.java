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

package software.amazon.awssdk.services.s3;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.AnalyticsConfiguration;
import software.amazon.awssdk.services.s3.model.AnalyticsExportDestination;
import software.amazon.awssdk.services.s3.model.AnalyticsFilter;
import software.amazon.awssdk.services.s3.model.AnalyticsS3BucketDestination;
import software.amazon.awssdk.services.s3.model.AnalyticsS3ExportFileFormat;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketAnalyticsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketAnalyticsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.ListBucketAnalyticsConfigurationsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketAnalyticsConfigurationsResponse;
import software.amazon.awssdk.services.s3.model.PutBucketAnalyticsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.StorageClassAnalysis;
import software.amazon.awssdk.services.s3.model.StorageClassAnalysisDataExport;
import software.amazon.awssdk.services.s3.model.StorageClassAnalysisSchemaVersion;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.testutils.RandomTempFile;

public class BucketAnalyticsConfigurationIntegrationTest extends S3IntegrationTestBase {

    /**
     * The bucket created and used by these tests.
     */
    private static final String BUCKET_NAME = temporaryBucketName("java-bucket-analytics-integ-test");

    private static final String BUCKET_ARN = "arn:aws:s3:::" + BUCKET_NAME;

    /**
     * The key used in these tests.
     */
    private static final String KEY = "key";

    @BeforeClass
    public static void setUpFixture() throws Exception {
        S3IntegrationTestBase.setUp();
        s3.createBucket(CreateBucketRequest.builder()
                                           .bucket(BUCKET_NAME)
                                           .createBucketConfiguration(CreateBucketConfiguration.builder()
                                                                                               .locationConstraint("us-west-2")
                                                                                               .build())
                                           .build());

        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET_NAME)
                                     .key(KEY)
                                     .build(), RequestBody.of(new RandomTempFile("foo", 1024)));
    }

    @AfterClass
    public static void tearDownFixture() {
        deleteBucketAndAllContents(BUCKET_NAME);
    }

    @Test
    public void testAnalyticsConfiguration_works_properly_with_setting_only_required_fields() throws Exception {
        String configId = "id";
        AnalyticsConfiguration config = AnalyticsConfiguration.builder()
                                                              .id(configId)
                                                              .storageClassAnalysis(getStorageClassAnalysis())
                                                              .build();

        s3.putBucketAnalyticsConfiguration(PutBucketAnalyticsConfigurationRequest.builder()
                                                                                 .bucket(BUCKET_NAME)
                                                                                 .analyticsConfiguration(config)
                                                                                 .id(configId)
                                                                                 .build());

        AnalyticsConfiguration returnedConfig = s3
                .getBucketAnalyticsConfiguration(GetBucketAnalyticsConfigurationRequest.builder()
                                                                                       .bucket(BUCKET_NAME)
                                                                                       .id(configId)
                                                                                       .build())
                .analyticsConfiguration();

        assertEquals(configId, returnedConfig.id());
        assertNull(returnedConfig.filter());
        assertEquals(StorageClassAnalysisSchemaVersion.V_1,
                     returnedConfig.storageClassAnalysis().dataExport().outputSchemaVersion());

        AnalyticsS3BucketDestination s3BucketDestination =
                returnedConfig.storageClassAnalysis().dataExport().destination().s3BucketDestination();
        assertEquals(BUCKET_ARN, s3BucketDestination.bucket());
        assertEquals(AnalyticsS3ExportFileFormat.CSV, s3BucketDestination.format());
        assertNull(s3BucketDestination.bucketAccountId());
        assertNull(s3BucketDestination.prefix());
    }

    @Test
    public void testDeleteBucketAnalyticsConfiguration() throws Exception {
        String configId = "id";
        AnalyticsConfiguration config = AnalyticsConfiguration.builder()
                                                              .id(configId)
                                                              .storageClassAnalysis(getStorageClassAnalysis())
                                                              .build();

        s3.putBucketAnalyticsConfiguration(PutBucketAnalyticsConfigurationRequest.builder()
                                                                                 .bucket(BUCKET_NAME)
                                                                                 .analyticsConfiguration(config)
                                                                                 .id(configId)
                                                                                 .build());

        assertNotNull(s3.getBucketAnalyticsConfiguration(GetBucketAnalyticsConfigurationRequest.builder()
                                                                                               .bucket(BUCKET_NAME)
                                                                                               .id(configId)
                                                                                               .build())
                        .analyticsConfiguration());

        s3.deleteBucketAnalyticsConfiguration(DeleteBucketAnalyticsConfigurationRequest.builder()
                                                                                       .bucket(BUCKET_NAME)
                                                                                       .id(configId)
                                                                                       .build());

        ListBucketAnalyticsConfigurationsResponse result =
                s3.listBucketAnalyticsConfigurations(ListBucketAnalyticsConfigurationsRequest.builder()
                                                                                             .bucket(BUCKET_NAME)
                                                                                             .build());
        assertNull(result.analyticsConfigurationList());
    }

    @Test
    public void testListBucketAnalyticsConfiguration() throws Exception {
        String configId = "id";
        String configId2 = "id2";
        AnalyticsConfiguration config = AnalyticsConfiguration.builder()
                                                              .id(configId)
                                                              .storageClassAnalysis(getStorageClassAnalysis())
                                                              .build();

        AnalyticsConfiguration config2 = AnalyticsConfiguration.builder()
                                                               .id(configId2)
                                                               .storageClassAnalysis(getStorageClassAnalysis())
                                                               .build();

        s3.putBucketAnalyticsConfiguration(PutBucketAnalyticsConfigurationRequest.builder()
                                                                                 .bucket(BUCKET_NAME)
                                                                                 .analyticsConfiguration(config)
                                                                                 .id(configId)
                                                                                 .build());

        s3.putBucketAnalyticsConfiguration(PutBucketAnalyticsConfigurationRequest.builder()
                                                                                 .bucket(BUCKET_NAME)
                                                                                 .analyticsConfiguration(config2)
                                                                                 .id(configId2)
                                                                                 .build());

        ListBucketAnalyticsConfigurationsResponse result = s3.listBucketAnalyticsConfigurations(
                ListBucketAnalyticsConfigurationsRequest.builder().bucket(BUCKET_NAME).build());

        List<AnalyticsConfiguration> analyticsConfigurationList = result.analyticsConfigurationList();
        assertNull(result.continuationToken());
        assertNull(result.nextContinuationToken());
        assertFalse(result.isTruncated());
        assertEquals(2, analyticsConfigurationList.size());
        assertEquals(configId, analyticsConfigurationList.get(0).id());
        assertEquals(configId2, analyticsConfigurationList.get(1).id());

        s3.deleteBucketAnalyticsConfiguration(DeleteBucketAnalyticsConfigurationRequest.builder()
                                                                                       .bucket(BUCKET_NAME)
                                                                                       .id(configId)
                                                                                       .build());

        s3.deleteBucketAnalyticsConfiguration(DeleteBucketAnalyticsConfigurationRequest.builder()
                                                                                       .bucket(BUCKET_NAME)
                                                                                       .id(configId2)
                                                                                       .build());
    }

    @Test(expected = S3Exception.class)
    public void testAnalyticsConfiguration_works_properly_with_emptyFilter() throws Exception {
        String configId = "id";
        AnalyticsConfiguration config = AnalyticsConfiguration.builder()
                                                              .id(configId)
                                                              .filter(AnalyticsFilter.builder()
                                                                                     .build())
                                                              .storageClassAnalysis(getStorageClassAnalysis())
                                                              .build();

        s3.putBucketAnalyticsConfiguration(PutBucketAnalyticsConfigurationRequest.builder()
                                                                                 .bucket(BUCKET_NAME)
                                                                                 .analyticsConfiguration(config)
                                                                                 .id(configId)
                                                                                 .build());
    }

    @Test(expected = S3Exception.class)
    public void testAnalyticsConfiguration_works_properly_with_emptyPrefix() throws Exception {

        String configId = "id";
        AnalyticsConfiguration config = AnalyticsConfiguration.builder()
                                                              .id(configId)
                                                              .filter(AnalyticsFilter.builder()
                                                                                     .prefix("")
                                                                                     .build())
                                                              .storageClassAnalysis(getStorageClassAnalysis())
                                                              .build();

        s3.putBucketAnalyticsConfiguration(PutBucketAnalyticsConfigurationRequest.builder()
                                                                                 .bucket(BUCKET_NAME)
                                                                                 .analyticsConfiguration(config)
                                                                                 .id(configId)
                                                                                 .build());
    }

    @Test
    public void testAnalyticsConfiguration_works_properly_with_onlyTag() throws Exception {

        String configId = "id";
        AnalyticsConfiguration config = AnalyticsConfiguration.builder()
                                                              .id(configId)
                                                              .filter(AnalyticsFilter.builder()
                                                                                     .tag(Tag.builder()
                                                                                             .key("key")
                                                                                             .value("value")
                                                                                             .build())
                                                                                     .build())
                                                              .storageClassAnalysis(getStorageClassAnalysis())
                                                              .build();

        s3.putBucketAnalyticsConfiguration(PutBucketAnalyticsConfigurationRequest.builder()
                                                                                 .bucket(BUCKET_NAME)
                                                                                 .analyticsConfiguration(config)
                                                                                 .id(configId)
                                                                                 .build());

        config = s3.getBucketAnalyticsConfiguration(GetBucketAnalyticsConfigurationRequest.builder()
                                                                                          .bucket(BUCKET_NAME)
                                                                                          .id(configId)
                                                                                          .build())
                   .analyticsConfiguration();

        assertEquals(configId, config.id());
        assertEquals("key", config.filter().tag().key());
        assertEquals("value", config.filter().tag().value());
        assertEquals(StorageClassAnalysisSchemaVersion.V_1,
                     config.storageClassAnalysis().dataExport().outputSchemaVersion());

        AnalyticsS3BucketDestination s3BucketDestination = config.storageClassAnalysis().dataExport().destination()
                                                                 .s3BucketDestination();
        assertEquals(BUCKET_ARN, s3BucketDestination.bucket());
        assertEquals(AnalyticsS3ExportFileFormat.CSV, s3BucketDestination.format());
        assertNull(s3BucketDestination.bucketAccountId());
        assertNull(s3BucketDestination.prefix());
    }

    @Test(expected = S3Exception.class)
    public void setBucketAnalyticsConfiguration_fails_when_requiredfield_is_missing() throws Exception {
        String configId = "id";
        StorageClassAnalysisDataExport dataExport = StorageClassAnalysisDataExport.builder()
                                                                                  .outputSchemaVersion(
                                                                                          StorageClassAnalysisSchemaVersion.V_1)
                                                                                  .destination(
                                                                                          AnalyticsExportDestination.builder()
                                                                                                                    .build())
                                                                                  .build();

        AnalyticsConfiguration config = AnalyticsConfiguration.builder()
                                                              .id(configId)
                                                              .storageClassAnalysis(StorageClassAnalysis.builder()
                                                                                                        .dataExport(dataExport)
                                                                                                        .build())
                                                              .build();

        s3.putBucketAnalyticsConfiguration(PutBucketAnalyticsConfigurationRequest.builder()
                                                                                 .bucket(BUCKET_NAME)
                                                                                 .analyticsConfiguration(config)
                                                                                 .id(configId)
                                                                                 .build());
    }

    private StorageClassAnalysis getStorageClassAnalysis() {
        AnalyticsS3BucketDestination s3BucketDestination = AnalyticsS3BucketDestination.builder()
                                                                                       .bucket(BUCKET_ARN)
                                                                                       .format(AnalyticsS3ExportFileFormat.CSV)
                                                                                       .build();

        return StorageClassAnalysis.builder()
                                   .dataExport(StorageClassAnalysisDataExport.builder()
                                                                             .outputSchemaVersion(
                                                                                     StorageClassAnalysisSchemaVersion.V_1)
                                                                             .destination(AnalyticsExportDestination.builder()
                                                                                                                    .s3BucketDestination(
                                                                                                                            s3BucketDestination)
                                                                                                                    .build())
                                                                             .build())
                                   .build();
    }
}
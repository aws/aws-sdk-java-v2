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

package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.DeleteBucketInventoryConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketInventoryConfigurationRequest;
import software.amazon.awssdk.services.s3.model.InventoryConfiguration;
import software.amazon.awssdk.services.s3.model.InventoryDestination;
import software.amazon.awssdk.services.s3.model.InventoryFilter;
import software.amazon.awssdk.services.s3.model.InventoryFormat;
import software.amazon.awssdk.services.s3.model.InventoryFrequency;
import software.amazon.awssdk.services.s3.model.InventoryIncludedObjectVersions;
import software.amazon.awssdk.services.s3.model.InventoryOptionalField;
import software.amazon.awssdk.services.s3.model.InventoryS3BucketDestination;
import software.amazon.awssdk.services.s3.model.InventorySchedule;
import software.amazon.awssdk.services.s3.model.ListBucketInventoryConfigurationsRequest;
import software.amazon.awssdk.services.s3.model.PutBucketInventoryConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.testutils.RandomTempFile;

public class BucketInventoryConfigurationIntegrationTest extends S3IntegrationTestBase {

    /**
     * The bucket created and used by these tests.
     */
    private static final String BUCKET_NAME = temporaryBucketName("java-bucket-inventory-integ-test");
    private static final String BUCKET_ARN = "arn:aws:s3:::" + BUCKET_NAME;

    /**
     * The key used in these tests.
     */
    private static final String KEY = "key";

    @BeforeClass
    public static void setUpFixture() throws Exception {
        S3IntegrationTestBase.setUp();
        createBucket(BUCKET_NAME);
        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET_NAME)
                                     .key(KEY)
                                     .build(),
                     RequestBody.fromFile(new RandomTempFile("foo", 1024)));
    }

    @AfterClass
    public static void tearDownFixture() {
        deleteBucketAndAllContents(BUCKET_NAME);
    }

    @Test
    public void testInventoryConfiguration_works_properly_with_setting_only_required_fields() throws Exception {
        String configId = "id";
        InventoryS3BucketDestination s3BucketDestination = InventoryS3BucketDestination.builder()
                                                                                       .bucket(BUCKET_ARN)
                                                                                       .format(InventoryFormat.CSV)
                                                                                       .build();

        InventoryDestination destination = InventoryDestination.builder().s3BucketDestination(s3BucketDestination).build();

        InventoryConfiguration config = InventoryConfiguration.builder()
                                                              .isEnabled(true)
                                                              .id(configId)
                                                              .destination(destination)
                                                              .includedObjectVersions(InventoryIncludedObjectVersions.ALL)
                                                              .schedule(InventorySchedule.builder()
                                                                                         .frequency(InventoryFrequency.DAILY)
                                                                                         .build())
                                                              .build();


        s3.putBucketInventoryConfiguration(PutBucketInventoryConfigurationRequest.builder()
                                                                                 .bucket(BUCKET_NAME)
                                                                                 .inventoryConfiguration(config)
                                                                                 .id(configId)
                                                                                 .build());

        config = s3.getBucketInventoryConfiguration(GetBucketInventoryConfigurationRequest.builder()
                                                                                          .bucket(BUCKET_NAME)
                                                                                          .id(configId)
                                                                                          .build())
                   .inventoryConfiguration();

        assertEquals(configId, config.id());
        assertTrue(config.isEnabled());
        assertEquals(InventoryIncludedObjectVersions.ALL, config.includedObjectVersions());
        assertEquals(InventoryFrequency.DAILY, config.schedule().frequency());
        s3BucketDestination = config.destination().s3BucketDestination();
        assertEquals(BUCKET_ARN, s3BucketDestination.bucket());
        assertEquals(InventoryFormat.CSV, s3BucketDestination.format());
        assertNull(s3BucketDestination.accountId());
        assertNull(s3BucketDestination.prefix());


        s3.deleteBucketInventoryConfiguration(DeleteBucketInventoryConfigurationRequest.builder()
                                                                                       .bucket(BUCKET_NAME)
                                                                                       .id(configId)
                                                                                       .build());

        List<InventoryConfiguration> configurations = s3.listBucketInventoryConfigurations(
                ListBucketInventoryConfigurationsRequest.builder()
                                                        .bucket(BUCKET_NAME)
                                                        .build())
                                                        .inventoryConfigurationList();
        assertTrue(configurations.isEmpty());
    }

    @Test
    public void testInventoryConfiguration_with_filter() throws Exception {
        String configId = "id";
        String prefix = "prefix";
        String accountId = "test-account";
        List<String> optionalFields = new ArrayList<String>() {
            {
                add(InventoryOptionalField.E_TAG.toString());
                add(InventoryOptionalField.SIZE.toString());
            }
        };

        InventoryS3BucketDestination s3BucketDestination = InventoryS3BucketDestination.builder()
                                                                                       .bucket(BUCKET_ARN)
                                                                                       .format(InventoryFormat.CSV)
                                                                                       .accountId(accountId)
                                                                                       .prefix(prefix)
                                                                                       .build();

        InventoryDestination destination = InventoryDestination.builder().s3BucketDestination(s3BucketDestination).build();

        InventoryConfiguration config = InventoryConfiguration.builder()
                                                              .isEnabled(true)
                                                              .id(configId)
                                                              .destination(destination)
                                                              .includedObjectVersions(InventoryIncludedObjectVersions.ALL)
                                                              .schedule(InventorySchedule.builder()
                                                                                         .frequency(InventoryFrequency.DAILY)
                                                                                         .build())
                                                              .filter(InventoryFilter.builder().prefix(prefix).build())
                                                              .optionalFieldsWithStrings(optionalFields)
                                                              .build();


        s3.putBucketInventoryConfiguration(PutBucketInventoryConfigurationRequest.builder()
                                                                                 .bucket(BUCKET_NAME)
                                                                                 .inventoryConfiguration(config)
                                                                                 .id(configId)
                                                                                 .build());

        config = s3.getBucketInventoryConfiguration(GetBucketInventoryConfigurationRequest.builder().bucket(BUCKET_NAME)
                                                                                          .id(configId)
                                                                                          .build())
                   .inventoryConfiguration();

        assertEquals(configId, config.id());
        assertTrue(config.isEnabled());
        assertEquals(InventoryIncludedObjectVersions.ALL, config.includedObjectVersions());
        assertEquals(InventoryFrequency.DAILY, config.schedule().frequency());
        s3BucketDestination = config.destination().s3BucketDestination();
        assertEquals(BUCKET_ARN, s3BucketDestination.bucket());
        assertEquals(InventoryFormat.CSV, s3BucketDestination.format());
        assertEquals(accountId, s3BucketDestination.accountId());
        assertEquals(prefix, s3BucketDestination.prefix());
        assertEquals(prefix, config.filter().prefix());
        assertTrue(config.optionalFieldsAsStrings().containsAll(optionalFields));

        s3.deleteBucketInventoryConfiguration(DeleteBucketInventoryConfigurationRequest.builder()
                                                                                       .bucket(BUCKET_NAME)
                                                                                       .id(configId)
                                                                                       .build());
    }
}

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

package software.amazon.awssdk.v2migration.internal.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class NamingConversionUtilsTest {
    @Test
    void v1Pojos_shouldConvertToV2() {
        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.model.DescribeJobResult"))
            .isEqualTo("software.amazon.awssdk.services.iot.model.DescribeJobResponse");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.model.DescribeJobRequest"))
            .isEqualTo("software.amazon.awssdk.services.iot.model.DescribeJobRequest");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.model.AuditFinding"))
            .isEqualTo("software.amazon.awssdk.services.iot.model.AuditFinding");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.ResultConfiguration"))
            .isEqualTo("software.amazon.awssdk.services.iot.ResultConfiguration");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.ResultConfigurationResult"))
            .isEqualTo("software.amazon.awssdk.services.iot.ResultConfigurationResponse");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.ListCACertificatesRequest"))
            .isEqualTo("software.amazon.awssdk.services.iot.ListCaCertificatesRequest");
    }

    @Test
    void v1PojoSpecialCase_shouldConvertToV2() {
        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.stepfunctions.model.DeleteActivityRequest"))
            .isEqualTo("software.amazon.awssdk.services.sfn.model.DeleteActivityRequest");
        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.stepfunctions.model.AWSStepFunctionsException"))
            .isEqualTo("software.amazon.awssdk.services.sfn.model.SfnException");
    }

    @Test
    void v1ClientSpecialCase_shouldConvertToV2() {
        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.dynamodbv2.AmazonDynamoDB"))
            .isEqualTo("software.amazon.awssdk.services.dynamodb.DynamoDbClient");
        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient"))
            .isEqualTo("software.amazon.awssdk.services.swf.SwfClient");
        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.cloudsearchv2.AmazonCloudSearch"))
            .isEqualTo("software.amazon.awssdk.services.cloudsearch.CloudSearchClient");
    }


    @Test
    void v1SyncClients_shouldConvertToV2() {
        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.AWSIot"))
            .isEqualTo("software.amazon.awssdk.services.iot.IotClient");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.AWSIotClient"))
            .isEqualTo("software.amazon.awssdk.services.iot.IotClient");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.AbstractAWSIot"))
            .isEqualTo("software.amazon.awssdk.services.iot.IotClient");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.AWSIotClientBuilder"))
            .isEqualTo("software.amazon.awssdk.services.iot.IotClientBuilder");
    }

    @Test
    void v1AsyncClients_shouldConvertToV2() {
        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.AWSIotAsync"))
            .isEqualTo("software.amazon.awssdk.services.iot.IotAsyncClient");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.AWSIotAsyncClient"))
            .isEqualTo("software.amazon.awssdk.services.iot.IotAsyncClient");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.AbstractAWSIotAsync"))
            .isEqualTo("software.amazon.awssdk.services.iot.IotAsyncClient");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.AWSIotAsyncClientBuilder"))
            .isEqualTo("software.amazon.awssdk.services.iot.IotAsyncClientBuilder");
    }

    @Test
    void v1Exception_shouldConvertToV2() {

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.AmazonIOTException"))
            .isEqualTo("software.amazon.awssdk.services.iot.IotException");
    }

    @Test
    void v2WildCardImport_shouldConvertToV2() {
        assertThat(NamingConversionUtils.getV2ModelPackageWildCardEquivalent("com.amazonaws.services.iot.model.*"))
            .isEqualTo("software.amazon.awssdk.services.iot.model.*");
        assertThat(NamingConversionUtils.getV2ModelPackageWildCardEquivalent("com.amazonaws.services.iot.*"))
            .isEqualTo("software.amazon.awssdk.services.iot.*");
        assertThat(NamingConversionUtils.getV2ModelPackageWildCardEquivalent("com.amazonaws.services.cloudsearchv2.*"))
            .isEqualTo("software.amazon.awssdk.services.cloudsearch.*");
    }

    @Test
    void packageNameV2Suffix_shouldBeRemoved() {
        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.cloudsearchv2.AmazonCloudSearchClient"))
                       .isEqualTo("software.amazon.awssdk.services.cloudsearch.CloudSearchClient");
        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.dynamodbv2.AmazonDynamoDB"))
            .isEqualTo("software.amazon.awssdk.services.dynamodb.DynamoDbClient");
    }

    @Test
    void v1S3PojoSpecialCase_shouldConvertToV2() {
        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.s3.model.GetObjectMetadataRequest"))
            .isEqualTo("software.amazon.awssdk.services.s3.model.HeadObjectRequest");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.s3.model.InitiateMultipartUploadRequest"))
            .isEqualTo("software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.s3.model.InitiateMultipartUploadResponse"))
            .isEqualTo("software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.s3.model.ListVersionsRequest"))
            .isEqualTo("software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.s3.model.ObjectMetadata"))
            .isEqualTo("software.amazon.awssdk.services.s3.model.HeadObjectResponse");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.s3.model.ObjectListing"))
            .isEqualTo("software.amazon.awssdk.services.s3.model.ListObjectsResponse");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.s3.model.CorsRule"))
            .isEqualTo("software.amazon.awssdk.services.s3.model.CORSRule");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.s3.model.BucketCrossOriginConfiguration"))
            .isEqualTo("software.amazon.awssdk.services.s3.model.CORSConfiguration");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.s3.model.GetBucketCrossOriginConfigurationRequest"))
            .isEqualTo("software.amazon.awssdk.services.s3.model.GetBucketCorsRequest");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.s3.model.DeleteBucketCrossOriginConfigurationRequest"))
            .isEqualTo("software.amazon.awssdk.services.s3.model.DeleteBucketCorsRequest");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.s3.model.SetBucketCrossOriginConfigurationRequest"))
            .isEqualTo("software.amazon.awssdk.services.s3.model.PutBucketCorsRequest");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.s3.model.GetBucketVersioningConfigurationRequest"))
            .isEqualTo("software.amazon.awssdk.services.s3.model.GetBucketVersioningRequest");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.s3.model.DeleteBucketLifecycleConfigurationRequest"))
            .isEqualTo("software.amazon.awssdk.services.s3.model.DeleteBucketLifecycleRequest");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.s3.model.DeleteBucketReplicationConfigurationRequest"))
            .isEqualTo("software.amazon.awssdk.services.s3.model.DeleteBucketReplicationRequest");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.s3.model.DeleteBucketTaggingConfigurationRequest"))
            .isEqualTo("software.amazon.awssdk.services.s3.model.DeleteBucketTaggingRequest");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.s3.model.DeleteBucketWebsiteConfigurationRequest"))
            .isEqualTo("software.amazon.awssdk.services.s3.model.DeleteBucketWebsiteRequest");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.s3.model.GetBucketLoggingConfigurationRequest"))
            .isEqualTo("software.amazon.awssdk.services.s3.model.GetBucketLoggingRequest");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.s3.model.GetBucketReplicationConfigurationRequest"))
            .isEqualTo("software.amazon.awssdk.services.s3.model.GetBucketReplicationRequest");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.s3.model.GetBucketTaggingConfigurationRequest"))
            .isEqualTo("software.amazon.awssdk.services.s3.model.GetBucketTaggingRequest");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.s3.model.GetBucketWebsiteConfigurationRequest"))
            .isEqualTo("software.amazon.awssdk.services.s3.model.GetBucketWebsiteRequest");
    }
}

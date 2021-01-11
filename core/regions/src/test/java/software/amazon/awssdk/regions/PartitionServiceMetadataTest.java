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
package software.amazon.awssdk.regions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Test;

public class PartitionServiceMetadataTest {

    private static final List<String> AWS_PARTITION_GLOBAL_SERVICES = Arrays.asList(
            "budgets", "cloudfront", "iam", "route53", "shield", "waf");

    private static final List<String> AWS_PARTITION_REGIONALIZED_SERVICES = Arrays.asList(
            "acm", "apigateway", "application-autoscaling", "appstream2", "autoscaling", "batch",
            "cloudformation", "cloudhsm", "cloudsearch", "cloudtrail", "codebuild", "codecommit", "codedeploy",
            "codepipeline", "cognito-identity", "cognito-idp", "cognito-sync", "config", "cur", "data.iot",
            "datapipeline", "directconnect", "dms", "ds", "dynamodb", "ec2", "ecs", "elasticache",
            "elasticbeanstalk", "elasticfilesystem", "elasticloadbalancing", "elasticmapreduce", "elastictranscoder",
            "email", "es", "events", "firehose", "gamelift", "glacier", "health", "inspector",
            "iot", "kinesis", "kinesisanalytics", "kms", "lambda", "lightsail", "logs", "machinelearning",
            "marketplacecommerceanalytics", "metering.marketplace", "mobileanalytics", "monitoring", "opsworks",
            "opsworks-cm", "pinpoint", "polly", "rds", "redshift", "rekognition", "route53domains", "s3",
            "sdb", "servicecatalog", "sms", "snowball", "sns", "sqs", "ssm", "states", "storagegateway",
            "streams.dynamodb", "sts", "support", "swf", "waf-regional", "workspaces", "xray");

    private static final List<String> AWS_CN_PARTITION_GLOBAL_SERVICES = Arrays.asList("iam");

    private static final List<String> AWS_CN_PARTITION_REGIONALIZED_SERVICES = Arrays.asList(
            "autoscaling", "cloudformation", "cloudtrail", "config", "directconnect", "dynamodb",
            "ec2", "elasticache", "elasticbeanstalk", "elasticloadbalancing", "elasticmapreduce",
            "events", "glacier", "kinesis", "logs", "monitoring", "rds", "redshift", "s3",
            "sns", "sqs", "storagegateway", "streams.dynamodb", "sts", "swf");

    private static final List<String> AWS_US_GOV_PARTITION_REGIONALIZED_SERVICES = Arrays.asList(
            "autoscaling", "cloudformation", "cloudhsm", "cloudtrail", "config", "directconnect",
            "dynamodb", "ec2", "elasticache", "elasticloadbalancing", "elasticmapreduce", "glacier",
            "kms", "logs", "monitoring", "rds", "redshift", "s3", "snowball", "sns", "sqs", "streams.dynamodb",
            "sts", "swf");

    private static final List<String> AWS_US_GOV_PARTITION_GLOBAL_SERVICES = Arrays.asList("iam");

    @Test
    public void endpointFor_ReturnsEndpoint_ForAllRegionalizedServices_When_AwsPartition() {
        AWS_PARTITION_REGIONALIZED_SERVICES.forEach(
            s -> assertThat(ServiceMetadata.of(s).endpointFor(Region.US_EAST_1)).isNotNull());
    }

    @Test
    public void endpointFor_ReturnsEndpoint_ForAllGlobalServices_When_AwsGlobalRegion() {
        AWS_PARTITION_GLOBAL_SERVICES.forEach(
            s -> assertThat(ServiceMetadata.of(s).endpointFor(Region.AWS_GLOBAL)).isNotNull());
    }

    @Test
    public void endpointFor_ReturnsEndpoint_ForAllRegionalizedServices_When_AwsCnPartition() {
        AWS_CN_PARTITION_REGIONALIZED_SERVICES.forEach(
            s -> assertThat(ServiceMetadata.of(s).endpointFor(Region.CN_NORTH_1)).isNotNull());
    }

    @Test
    public void endpointFor_ReturnsEndpoint_ForAllGlobalServices_When_AwsCnGlobalRegion() {
        AWS_CN_PARTITION_GLOBAL_SERVICES.forEach(
            s -> assertThat(ServiceMetadata.of(s).endpointFor(Region.AWS_CN_GLOBAL)).isNotNull());
    }

    @Test
    public void endpointFor_ReturnsEndpoint_ForAllRegionalizedServices_When_AwsUsGovPartition() {
        AWS_US_GOV_PARTITION_REGIONALIZED_SERVICES.forEach(
            s -> assertThat(ServiceMetadata.of(s).endpointFor(Region.US_GOV_WEST_1)).isNotNull());
    }

    @Test
    public void endpointFor_ReturnsEndpoint_ForAllGlobalServices_When_AwsUsGovGlobalRegion() {
        AWS_US_GOV_PARTITION_GLOBAL_SERVICES.forEach(
            s -> assertThat(ServiceMetadata.of(s).endpointFor(Region.AWS_US_GOV_GLOBAL)).isNotNull());
    }

    @Test
    public void regions_ReturnsGreaterThan15Regions_ForS3() {
        assertThat(ServiceMetadata.of("s3").regions().size()).isGreaterThan(15);
    }

    @Test
    public void servicePartitions_ReturnsAllValidPartitions() {
        validateServicesAreInPartition(AWS_PARTITION_GLOBAL_SERVICES, "aws");
        validateServicesAreInPartition(AWS_PARTITION_REGIONALIZED_SERVICES, "aws");
        validateServicesAreInPartition(AWS_CN_PARTITION_GLOBAL_SERVICES, "aws-cn");
        validateServicesAreInPartition(AWS_CN_PARTITION_REGIONALIZED_SERVICES, "aws-cn");
        validateServicesAreInPartition(AWS_US_GOV_PARTITION_GLOBAL_SERVICES, "aws-us-gov");
        validateServicesAreInPartition(AWS_US_GOV_PARTITION_REGIONALIZED_SERVICES, "aws-us-gov");
    }

    @Test
    public void servicePartitions_HasGlobalEndpoint_ForGlobalServices() {
        validateHasGlobalEndpointInPartition(AWS_PARTITION_GLOBAL_SERVICES, "aws", true);
        validateHasGlobalEndpointInPartition(AWS_CN_PARTITION_GLOBAL_SERVICES, "aws-cn", true);
        validateHasGlobalEndpointInPartition(AWS_US_GOV_PARTITION_GLOBAL_SERVICES, "aws-us-gov", true);
    }

    @Test
    public void servicePartitions_HasNoGlobalEndpoint_ForRegionalServices() {
        validateHasGlobalEndpointInPartition(AWS_PARTITION_REGIONALIZED_SERVICES, "aws", false);
        validateHasGlobalEndpointInPartition(AWS_CN_PARTITION_REGIONALIZED_SERVICES, "aws-cn", false);
        validateHasGlobalEndpointInPartition(AWS_US_GOV_PARTITION_REGIONALIZED_SERVICES, "aws-us-gov", false);
    }

    private void validateHasGlobalEndpointInPartition(List<String> services, String partition, boolean hasGlobalEndpoint) {
        services.forEach(s -> assertThat(ServiceMetadata.of(s)
                                                        .servicePartitions()
                                                        .stream()
                                                        .filter(sp -> sp.partition().id().equals(partition))
                                                        .anyMatch(sp -> sp.globalRegion().isPresent()))
            .as(s + " is global in " + partition)
            .isEqualTo(hasGlobalEndpoint));
    }

    private void validateServicesAreInPartition(List<String> services, String partition) {
        services.forEach(s -> assertThat(ServiceMetadata.of(s)
                                                        .servicePartitions()
                                                        .stream()
                                                        .map(p -> p.partition().id())
                                                        .collect(Collectors.toList()))
            .as(s + " is in " + partition)
            .contains(partition));
    }
}

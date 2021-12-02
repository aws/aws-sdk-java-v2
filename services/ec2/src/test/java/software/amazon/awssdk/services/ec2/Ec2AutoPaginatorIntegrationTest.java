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

package software.amazon.awssdk.services.ec2;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.stream.Stream;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.model.SpotPrice;
import software.amazon.awssdk.services.ec2.paginators.DescribeSpotPriceHistoryPublisher;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class Ec2AutoPaginatorIntegrationTest extends AwsIntegrationTestBase {

    @Test
    public void testSpotPriceHistorySyncPaginator() {
        Ec2Client ec2Client = Ec2Client.builder()
                                       .region(Region.US_EAST_1)
                                       .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                       .build();

        Stream<SpotPrice> spotPrices = ec2Client.describeSpotPriceHistoryPaginator(builder -> {
            builder.availabilityZone("us-east-1a")
                   .productDescriptions("Linux/UNIX (Amazon VPC)")
                   .instanceTypesWithStrings("t1.micro")
                   .startTime(Instant.now().minusMillis(1));
        }).spotPriceHistory().stream();

        assertThat(spotPrices.count()).isEqualTo(1);
    }

    @Test
    public void testSpotPriceHistoryAsyncPaginator() {
        Ec2AsyncClient ec2Client = Ec2AsyncClient.builder()
                                                 .region(Region.US_EAST_1)
                                                 .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                                 .build();

        DescribeSpotPriceHistoryPublisher publisher = ec2Client.describeSpotPriceHistoryPaginator(builder -> {
            builder.availabilityZone("us-east-1a")
                   .productDescriptions("Linux/UNIX (Amazon VPC)")
                   .instanceTypesWithStrings("t1.micro")
                   .startTime(Instant.now().minusMillis(1));
        });

        publisher.subscribe(r -> assertThat(r.spotPriceHistory().size()).isEqualTo(1))
                 .join();
    }
}

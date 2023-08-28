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

import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Ec2Response;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

class Ec2DryRunSupportIntegrationTest extends AwsIntegrationTestBase {

    Ec2Client ec2Client = Ec2Client.builder()
                                   .region(Region.US_EAST_1)
                                   .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                   .build();


    @Test
    void dryRunSuccessTest() {
        assertThat(isDryRunComplete(
            () -> ec2Client.describeRegions(
                r -> r.regionNames(Region.US_EAST_1.id()).dryRun(true))))
            .isTrue();

    }

    private boolean isDryRunComplete(Supplier<Ec2Response> dryRunOp) {
        Ec2Response ec2Response = null;
        try {
            ec2Response = dryRunOp.get();
        } catch (Ec2Exception exception) {
            AwsErrorDetails awsErrorDetails = exception.awsErrorDetails();
            if ("AuthFailure".equals(awsErrorDetails.errorCode())) {
                return false;
            }
            if ("DryRunOperation".equals(awsErrorDetails.errorCode())) {
                return true;
            }
            throw exception;
        }
        throw new IllegalStateException("Unable to get dryRun status for ec2Response:  " + ec2Response);
    }

}

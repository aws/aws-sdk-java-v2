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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

public class InvalidRegionTest {
    @Test
    public void invalidS3UtilitiesRegionAtClientGivesHelpfulMessage() {
        S3Utilities utilities = S3Utilities.builder().region(Region.of("US_EAST_1")).build();

        assertThatThrownBy(() -> utilities.getUrl(r -> r.bucket("foo").key("bar")))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("US_EAST_1")
            .hasMessageContaining("region")
            .hasMessageContaining("us-east-1");
    }

    @Test
    public void invalidS3UtilitiesRegionAtRequestGivesHelpfulMessage() {
        S3Utilities utilities = S3Utilities.builder().region(Region.of("us-east-1")).build();

        assertThatThrownBy(() -> utilities.getUrl(r -> r.bucket("foo").key("bar").region(Region.of("US_WEST_2"))))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("US_WEST_2")
            .hasMessageContaining("region")
            .hasMessageContaining("us-west-2");
    }

    @Test
    public void invalidS3ArnRegionAtRequestGivesHelpfulMessage() {
        S3Client client = S3Client.builder()
                                  .region(Region.of("us-east-1"))
                                  .credentialsProvider(AnonymousCredentialsProvider.create())
                                  .serviceConfiguration(c -> c.useArnRegionEnabled(true))
                                  .build();

        assertThatThrownBy(() -> client.getObject(r -> r.bucket("arn:aws:s3:US_EAST_1:123456789012:accesspoint/test")
                                                        .key("test")))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("US_EAST_1")
            .hasMessageContaining("region");
    }

    @Test
    public void invalidS3PresignerRegionAtClientGivesHelpfulMessage() {
        assertThatThrownBy(() -> S3Presigner.builder().region(Region.of("US_EAST_1")).build())
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("US_EAST_1")
            .hasMessageContaining("region")
            .hasMessageContaining("us-east-1");
    }

    @Test
    public void invalidS3PresignerArnRegionAtRequestGivesHelpfulMessage() {
        S3Presigner presigner = S3Presigner.builder()
                                           .region(Region.of("us-east-1"))
                                           .credentialsProvider(AnonymousCredentialsProvider.create())
                                           .serviceConfiguration(S3Configuration.builder().useArnRegionEnabled(true).build())
                                           .build();

        String arn = "arn:aws:s3:US_EAST_1:123456789012:accesspoint/test";
        assertThatThrownBy(() -> presigner.presignGetObject(r -> r.getObjectRequest(g -> g.bucket(arn).key("test"))
                                                                  .signatureDuration(Duration.ofMinutes(15))))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("US_EAST_1")
            .hasMessageContaining("region");
    }
}

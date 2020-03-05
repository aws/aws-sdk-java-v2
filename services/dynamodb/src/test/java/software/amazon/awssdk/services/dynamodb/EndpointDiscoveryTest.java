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

package software.amazon.awssdk.services.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.StringInputStream;

public class EndpointDiscoveryTest {
    @Test(timeout = 10_000)
    public void canBeEnabledViaProfileOnOverrideConfiguration() throws InterruptedException {
        ExecutionInterceptor interceptor = Mockito.spy(AbstractExecutionInterceptor.class);

        String profileFileContent =
            "[default]\n" +
            "aws_endpoint_discovery_enabled = true";

        ProfileFile profileFile = ProfileFile.builder()
                                             .type(ProfileFile.Type.CONFIGURATION)
                                             .content(new StringInputStream(profileFileContent))
                                             .build();

        DynamoDbClient dynamoDb = DynamoDbClient.builder()
                                                .region(Region.US_WEST_2)
                                                .credentialsProvider(AnonymousCredentialsProvider.create())
                                                .overrideConfiguration(c -> c.defaultProfileFile(profileFile)
                                                                             .defaultProfileName("default")
                                                                             .addExecutionInterceptor(interceptor)
                                                                             .retryPolicy(r -> r.numRetries(0)))
                                                .build();

        assertThatThrownBy(dynamoDb::listTables).isInstanceOf(SdkException.class);

        ArgumentCaptor<Context.BeforeTransmission> context;

        do {
            Thread.sleep(1);
            context = ArgumentCaptor.forClass(Context.BeforeTransmission.class);
            Mockito.verify(interceptor, atLeastOnce()).beforeTransmission(context.capture(), any());
        } while (context.getAllValues().size() < 2);

        assertThat(context.getAllValues()
                          .stream()
                          .anyMatch(v -> v.httpRequest()
                                          .firstMatchingHeader("X-Amz-Target")
                                          .map(h -> h.equals("DynamoDB_20120810.DescribeEndpoints"))
                                          .orElse(false)))
            .isTrue();
    }

    public static abstract class AbstractExecutionInterceptor implements ExecutionInterceptor {}
}

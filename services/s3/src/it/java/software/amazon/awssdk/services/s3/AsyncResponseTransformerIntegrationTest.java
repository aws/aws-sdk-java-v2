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

import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;

@RunWith(MockitoJUnitRunner.class)
public class AsyncResponseTransformerIntegrationTest {

    @Mock
    private AsyncResponseTransformer asyncResponseTransformer;

    @Test
    public void AsyncResponseTransformerPrepareCalled_BeforeCredentailsResolution() {
        S3AsyncClient client = S3AsyncClient.builder()
                                            .credentialsProvider(AwsCredentialsProviderChain.of(
                                                ProfileCredentialsProvider.create("dummyprofile")))
                                            .build();

        try {
            client.getObject(b -> b.bucket("dummy").key("key"), asyncResponseTransformer).join();
            fail("Expected an exception during credential resolution");
        } catch (Throwable t) {

        }

        verify(asyncResponseTransformer, times(1)).prepare();
        verify(asyncResponseTransformer, times(1)).exceptionOccurred(any());
    }
}

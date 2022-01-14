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

package software.amazon.awssdk.transfer.s3.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@RunWith(MockitoJUnitRunner.class)
public class DefaultS3CrtAsyncClientTest {
    @Mock
    private SdkAsyncHttpClient mockHttpClient;

    @Mock
    private S3AsyncClient mockS3AsyncClient;

    private DefaultS3CrtAsyncClient s3CrtAsyncClient;

    @Before
    public void methodSetup() {
        s3CrtAsyncClient = new DefaultS3CrtAsyncClient(mockHttpClient,
                                                       mockS3AsyncClient);
    }

    @Test
    public void requestSignerOverrideProvided_shouldThrowException() {
        assertThatThrownBy(() -> s3CrtAsyncClient.getObject(b -> b.bucket("bucket").key("key").overrideConfiguration(o -> o.signer(AwsS3V4Signer.create())),
                                                            AsyncResponseTransformer.toBytes())).isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> s3CrtAsyncClient.putObject(b -> b.bucket("bucket").key("key").overrideConfiguration(o -> o.signer(AwsS3V4Signer.create())),
                                                            AsyncRequestBody.fromString("foobar"))).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void closeS3Client_shouldCloseUnderlyingResources() {
        s3CrtAsyncClient.close();
        verify(mockHttpClient).close();
        verify(mockS3AsyncClient).close();
    }
}

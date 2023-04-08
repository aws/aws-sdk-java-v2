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

package software.amazon.awssdk.auth.credentials;

import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.internal.WebIdentityTokenCredentialProperties;
import software.amazon.awssdk.core.exception.HttpImplementationException;

public class WebIdentityTokenFileCredentialsProviderTest {

    @Test(expected = HttpImplementationException.class)
    public void create_should_fail_fast_with_httpImplementationException() {
        WebIdentityTokenFileCredentialsProvider provider = new WebIdentityTokenFileCredentialsProvider.BuilderImpl()
            .setFactory(new MockMultipleHttpImplFailureFactory())
            .roleArn("arn:aws:iam::123456789012:role/testRole")
            .webIdentityTokenFile(Paths.get("/src/test/token.jwt"))
            .roleSessionName("test")
            .build();
    }

    @Test
    public void create_should_not_fail_for_other_exceptions() {
        WebIdentityTokenFileCredentialsProvider provider = new WebIdentityTokenFileCredentialsProvider.BuilderImpl()
            .setFactory(new MockOtherFailureFactory())
            .roleArn("arn:aws:iam::123456789012:role/testRole")
            .webIdentityTokenFile(Paths.get("/src/test/token.jwt"))
            .roleSessionName("test")
            .build();
        Assert.assertNotNull(provider);
    }

    private class MockMultipleHttpImplFailureFactory implements WebIdentityTokenCredentialsProviderFactory {

        @Override
        public AwsCredentialsProvider create(WebIdentityTokenCredentialProperties credentialProperties) {
            throw HttpImplementationException
                .builder()
                .message("mock multiple http implementations exception")
                .build();
        }

    }

    private class MockOtherFailureFactory implements WebIdentityTokenCredentialsProviderFactory {

        @Override
        public AwsCredentialsProvider create(WebIdentityTokenCredentialProperties credentialProperties) {
            throw new RuntimeException("mock some provider create failure");
        }

    }
}
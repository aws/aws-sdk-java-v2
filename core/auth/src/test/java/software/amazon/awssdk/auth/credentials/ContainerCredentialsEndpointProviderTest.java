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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static software.amazon.awssdk.core.SdkSystemSetting.AWS_CONTAINER_AUTHORIZATION_TOKEN;
import static software.amazon.awssdk.core.SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_FULL_URI;
import static software.amazon.awssdk.core.SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_RELATIVE_URI;

import java.io.IOException;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.util.SdkUserAgent;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

public class ContainerCredentialsEndpointProviderTest {

    private static final EnvironmentVariableHelper helper = new EnvironmentVariableHelper();
    private static final ContainerCredentialsProvider.ContainerCredentialsEndpointProvider sut =
        new ContainerCredentialsProvider.ContainerCredentialsEndpointProvider(null);

    @Before
    public void clearContainerVariablesIncaseWereRunningTestsOnEC2() {
        helper.remove(AWS_CONTAINER_CREDENTIALS_RELATIVE_URI);
    }

    @After
    public void restoreOriginal() {
        helper.reset();
    }

    @Test
    public void takesUriFromOverride() throws IOException {
        String hostname = "http://localhost:8080";
        String path = "/endpoint";
        helper.set(AWS_CONTAINER_CREDENTIALS_RELATIVE_URI, path);
        assertThat(new ContainerCredentialsProvider.ContainerCredentialsEndpointProvider(hostname).endpoint().toString(),
                   equalTo(hostname + path));
    }

    @Test
    public void takesUriFromTheEnvironmentVariable() throws IOException {
        String fullUri = "http://localhost:8080/endpoint";
        helper.set(AWS_CONTAINER_CREDENTIALS_FULL_URI.environmentVariable(), fullUri);
        assertThat(sut.endpoint().toString(), equalTo(fullUri));
    }

    @Test
    public void theLoopbackAddressIsAlsoAcceptable() throws IOException {
        String fullUri = "http://127.0.0.1:9851/endpoint";
        helper.set(AWS_CONTAINER_CREDENTIALS_FULL_URI.environmentVariable(), fullUri);

        assertThat(sut.endpoint().toString(), equalTo(fullUri));
    }

    @Test
    public void theLoopbackIpv6AddressIsAlsoAcceptable() throws IOException {
        String fullUri = "http://[::1]:9851/endpoint";
        helper.set(AWS_CONTAINER_CREDENTIALS_FULL_URI.environmentVariable(), fullUri);

        assertThat(sut.endpoint().toString(), equalTo(fullUri));
    }

    @Test
    public void anyHttpsAddressIsAlsoAcceptable() throws IOException {
        String fullUri = "https://192.168.10.120:9851/endpoint";
        helper.set(AWS_CONTAINER_CREDENTIALS_FULL_URI.environmentVariable(), fullUri);

        assertThat(sut.endpoint().toString(), equalTo(fullUri));
    }

    @Test
    public void anyHttpsIpv6AddressIsAlsoAcceptable() throws IOException {
        String fullUri = "https://[::FFFF:152.16.24.123]/endpoint";
        helper.set(AWS_CONTAINER_CREDENTIALS_FULL_URI.environmentVariable(), fullUri);

        assertThat(sut.endpoint().toString(), equalTo(fullUri));
    }

    @Test(expected = SdkClientException.class)
    public void nonLoopbackAddressIsNotAcceptable() throws IOException {
        String fullUri = "http://192.168.10.120:9851/endpoint";
        helper.set(AWS_CONTAINER_CREDENTIALS_FULL_URI.environmentVariable(), fullUri);

        assertThat(sut.endpoint().toString(), equalTo(fullUri));
    }

    @Test(expected = SdkClientException.class)
    public void nonLoopbackIpv6AddressIsNotAcceptable() throws IOException {
        String fullUri = "http://[::FFFF:152.16.24.123]/endpoint";
        helper.set(AWS_CONTAINER_CREDENTIALS_FULL_URI.environmentVariable(), fullUri);

        assertThat(sut.endpoint().toString(), equalTo(fullUri));
    }

    @Test(expected = SdkClientException.class)
    public void onlyLocalHostAddressesAreValid() throws IOException {
        helper.set(AWS_CONTAINER_CREDENTIALS_FULL_URI.environmentVariable(), "http://google.com/endpoint");
        sut.endpoint();
    }

    @Test
    public void authorizationHeaderIsPresentIfEnvironmentVariableSet() {
        helper.set(AWS_CONTAINER_AUTHORIZATION_TOKEN.environmentVariable(), "hello authorized world!");
        Map<String, String> headers = sut.headers();
        assertThat(headers.size(), equalTo(2));
        assertThat(headers, hasEntry("Authorization", "hello authorized world!"));
        assertThat(headers, hasEntry("User-Agent", SdkUserAgent.create().userAgent()));
    }
}

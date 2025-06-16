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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * Tests for {@link InstanceProfileCredentialsProvider} using the extended IMDS path.
 */
public class InstanceProfileCredentialsProviderExtendedIntegrationTest {
    private static final String EXTENDED_PATH = "/latest/meta-data/iam/security-credentials-extended/";
    private EC2MetadataServiceMock mockServer;

    @Before
    public void setUp() throws Exception {
        mockServer = new EC2MetadataServiceMock(EXTENDED_PATH);
        mockServer.start();
    }

    @After
    public void tearDown() {
        mockServer.stop();
    }

    @Test
    public void resolveCredentials_withExtendedPath_includesAccountId() {
        mockServer.setAvailableSecurityCredentials("test-role");
        mockServer.setResponseFileName("sessionResponseExtended");

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();
        AwsCredentials credentials = provider.resolveCredentials();

        assertThat(credentials.accountId()).isPresent();
        assertThat(credentials.accountId().get()).isEqualTo("123456789012");
    }

    @Test
    public void resolveCredentials_withExtendedPath_hasCorrectCredentials() {
        mockServer.setAvailableSecurityCredentials("test-role");
        mockServer.setResponseFileName("sessionResponseExtended");

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();
        AwsCredentials credentials = provider.resolveCredentials();

        assertThat(credentials).isInstanceOf(AwsSessionCredentials.class);
        assertThat(credentials.accessKeyId()).isEqualTo("ACCESS_KEY_ID");
        assertThat(credentials.secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");
        assertThat(((AwsSessionCredentials) credentials).sessionToken()).isEqualTo("TOKEN");
    }

    @Test
    public void testSessionCredentials_MultipleInstanceProfiles() {
        mockServer.setAvailableSecurityCredentials("test-credentials");
        mockServer.setResponseFileName("sessionResponseExtended");

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();
        AwsCredentials credentials = provider.resolveCredentials();

        assertThat(credentials).isInstanceOf(AwsSessionCredentials.class);
        assertThat(credentials.accessKeyId()).isEqualTo("ACCESS_KEY_ID");
        assertThat(credentials.secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");
        assertThat(((AwsSessionCredentials) credentials).sessionToken()).isEqualTo("TOKEN");
    }

    @Test
    public void testNoInstanceProfiles() throws Exception {
        mockServer.setResponseFileName("sessionResponseExtended");
        mockServer.setAvailableSecurityCredentials("");

        try (InstanceProfileCredentialsProvider credentialsProvider = InstanceProfileCredentialsProvider.create()) {

            try {
                credentialsProvider.resolveCredentials();
                fail("Expected an SdkClientException, but wasn't thrown");
            } catch (SdkClientException ace) {
                assertNotNull(ace.getMessage());
            }
        }
    }

    @Test
    public void resolveCredentials_withDisabledMetadata_throwsException() {
        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_DISABLED.property(), "true");
        try {
            InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();
            assertThatThrownBy(() -> provider.resolveCredentials())
                .isInstanceOf(SdkClientException.class)
                .hasMessageContaining("IMDS credentials have been disabled");
        } finally {
            System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_DISABLED.property());
        }
    }

    @Test
    public void resolveCredentials_withNoAvailableCredentials_throwsException() {
        mockServer.setAvailableSecurityCredentials("");
        mockServer.setResponseFileName("sessionResponseExtended");

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();
        assertThatThrownBy(() -> provider.resolveCredentials())
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Failed to load credentials from IMDS.");
    }
}

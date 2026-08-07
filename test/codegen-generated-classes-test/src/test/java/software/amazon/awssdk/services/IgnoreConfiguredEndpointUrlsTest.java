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

package software.amazon.awssdk.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClientBuilder;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

/**
 * Tests that the {@code ignore_configured_endpoint_urls} setting correctly suppresses endpoint URL resolution from
 * environment variables, system properties, and the shared configuration file, while preserving programmatic
 * endpoint overrides set on the client builder.
 */
class IgnoreConfiguredEndpointUrlsTest {
    private static final String SERVICE_ENV_VAR = "AWS_ENDPOINT_URL_AMAZONPROTOCOLRESTJSON";
    private static final String GLOBAL_ENV_VAR = "AWS_ENDPOINT_URL";
    private static final String SERVICE_SYS_PROP = "aws.endpointUrlProtocolRestJson";
    private static final String GLOBAL_SYS_PROP = "aws.endpointUrl";
    private static final String DEFAULT_ENDPOINT = "https://customresponsemetadata.us-west-2.amazonaws.com";
    private static final String ENV_ENDPOINT = "https://env-endpoint.example.com";
    private static final String PROFILE_ENDPOINT = "https://profile-endpoint.example.com";
    private static final String CLIENT_OVERRIDE_ENDPOINT = "https://client-override.example.com";

    private final EnvironmentVariableHelper helper = new EnvironmentVariableHelper();

    @BeforeEach
    void setup() {
        helper.reset();
        System.clearProperty(GLOBAL_SYS_PROP);
        System.clearProperty(SERVICE_SYS_PROP);
        System.clearProperty(SdkSystemSetting.AWS_IGNORE_CONFIGURED_ENDPOINT_URLS.property());
    }

    @AfterEach
    void teardown() {
        helper.reset();
        System.clearProperty(GLOBAL_SYS_PROP);
        System.clearProperty(SERVICE_SYS_PROP);
        System.clearProperty(SdkSystemSetting.AWS_IGNORE_CONFIGURED_ENDPOINT_URLS.property());
    }

    @Test
    void defaultBehavior_endpointFromEnvVarIsUsed() {
        helper.set(SERVICE_ENV_VAR, ENV_ENDPOINT);

        String resolved = resolveEndpoint(null, null);

        assertThat(resolved).startsWith(ENV_ENDPOINT);
    }

    @Test
    void ignoreViaSystemProperty_endpointFromEnvVarIsIgnored() {
        helper.set(SERVICE_ENV_VAR, ENV_ENDPOINT);
        System.setProperty(SdkSystemSetting.AWS_IGNORE_CONFIGURED_ENDPOINT_URLS.property(), "true");

        String resolved = resolveEndpoint(null, null);

        assertThat(resolved).startsWith(DEFAULT_ENDPOINT);
    }

    @Test
    void ignoreViaEnvVar_endpointFromEnvVarIsIgnored() {
        helper.set(SERVICE_ENV_VAR, ENV_ENDPOINT);
        helper.set(SdkSystemSetting.AWS_IGNORE_CONFIGURED_ENDPOINT_URLS, "true");

        String resolved = resolveEndpoint(null, null);

        assertThat(resolved).startsWith(DEFAULT_ENDPOINT);
    }

    @Test
    void ignoreViaProfile_endpointFromEnvVarIsIgnored() {
        helper.set(SERVICE_ENV_VAR, ENV_ENDPOINT);

        ProfileFile profileFile = profileWithIgnore("true", null);
        String resolved = resolveEndpoint(null, profileFile);

        assertThat(resolved).startsWith(DEFAULT_ENDPOINT);
    }

    @Test
    void ignoreViaProfile_profileEndpointInSameProfileIsAlsoIgnored() {
        ProfileFile profileFile = profileWithIgnore("true", PROFILE_ENDPOINT);
        String resolved = resolveEndpoint(null, profileFile);

        assertThat(resolved).startsWith(DEFAULT_ENDPOINT);
    }

    @Test
    void ignoreTrue_globalEnvVarIsIgnored() {
        helper.set(GLOBAL_ENV_VAR, ENV_ENDPOINT);
        System.setProperty(SdkSystemSetting.AWS_IGNORE_CONFIGURED_ENDPOINT_URLS.property(), "true");

        String resolved = resolveEndpoint(null, null);

        assertThat(resolved).startsWith(DEFAULT_ENDPOINT);
    }

    @Test
    void ignoreTrue_serviceSysPropIsIgnored() {
        System.setProperty(SERVICE_SYS_PROP, ENV_ENDPOINT);
        System.setProperty(SdkSystemSetting.AWS_IGNORE_CONFIGURED_ENDPOINT_URLS.property(), "true");

        String resolved = resolveEndpoint(null, null);

        assertThat(resolved).startsWith(DEFAULT_ENDPOINT);
    }

    @Test
    void ignoreTrue_globalSysPropIsIgnored() {
        System.setProperty(GLOBAL_SYS_PROP, ENV_ENDPOINT);
        System.setProperty(SdkSystemSetting.AWS_IGNORE_CONFIGURED_ENDPOINT_URLS.property(), "true");

        String resolved = resolveEndpoint(null, null);

        assertThat(resolved).startsWith(DEFAULT_ENDPOINT);
    }

    @Test
    void ignoreTrue_profileEndpointIsIgnored() {
        System.setProperty(SdkSystemSetting.AWS_IGNORE_CONFIGURED_ENDPOINT_URLS.property(), "true");

        ProfileFile profileFile = profileWithEndpoint(PROFILE_ENDPOINT);
        String resolved = resolveEndpoint(null, profileFile);

        assertThat(resolved).startsWith(DEFAULT_ENDPOINT);
    }

    @Test
    void ignoreTrue_servicesSectionEndpointIsIgnored() {
        System.setProperty(SdkSystemSetting.AWS_IGNORE_CONFIGURED_ENDPOINT_URLS.property(), "true");

        ProfileFile profileFile = profileWithServicesSection(PROFILE_ENDPOINT);
        String resolved = resolveEndpoint(null, profileFile);

        assertThat(resolved).startsWith(DEFAULT_ENDPOINT);
    }

    @Test
    void ignoreTrue_programmaticEndpointOverrideStillWorks() {
        helper.set(SERVICE_ENV_VAR, ENV_ENDPOINT);
        System.setProperty(SdkSystemSetting.AWS_IGNORE_CONFIGURED_ENDPOINT_URLS.property(), "true");

        String resolved = resolveEndpoint(URI.create(CLIENT_OVERRIDE_ENDPOINT), null);

        assertThat(resolved).startsWith(CLIENT_OVERRIDE_ENDPOINT);
    }

    @Test
    void ignoreTrue_programmaticEndpointOverrideWorksEvenWithAllSourcesConfigured() {
        helper.set(SERVICE_ENV_VAR, ENV_ENDPOINT);
        helper.set(GLOBAL_ENV_VAR, ENV_ENDPOINT);
        System.setProperty(SERVICE_SYS_PROP, ENV_ENDPOINT);
        System.setProperty(GLOBAL_SYS_PROP, ENV_ENDPOINT);
        System.setProperty(SdkSystemSetting.AWS_IGNORE_CONFIGURED_ENDPOINT_URLS.property(), "true");

        ProfileFile profileFile = profileWithEndpoint(PROFILE_ENDPOINT);
        String resolved = resolveEndpoint(URI.create(CLIENT_OVERRIDE_ENDPOINT), profileFile);

        assertThat(resolved).startsWith(CLIENT_OVERRIDE_ENDPOINT);
    }

    @Test
    void ignoreFalse_endpointFromEnvVarIsUsed() {
        helper.set(SERVICE_ENV_VAR, ENV_ENDPOINT);
        System.setProperty(SdkSystemSetting.AWS_IGNORE_CONFIGURED_ENDPOINT_URLS.property(), "false");

        String resolved = resolveEndpoint(null, null);

        assertThat(resolved).startsWith(ENV_ENDPOINT);
    }

    @Test
    void ignoreViaBuilder_endpointFromEnvVarIsIgnored() {
        helper.set(SERVICE_ENV_VAR, ENV_ENDPOINT);

        EndpointCapturingInterceptor interceptor = new EndpointCapturingInterceptor();
        ProtocolRestJsonClient client =
            ProtocolRestJsonClient.builder()
                                  .region(Region.US_WEST_2)
                                  .credentialsProvider(AnonymousCredentialsProvider.create())
                                  .ignoreConfiguredEndpointUrls(true)
                                  .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor))
                                  .build();

        try {
            client.allTypes();
        } catch (EndpointCapturingInterceptor.CaptureCompletedException e) {
            // expected
        }

        assertThat(interceptor.endpoints()).singleElement().asString().startsWith(DEFAULT_ENDPOINT);
    }

    @Test
    void ignoreViaBuilder_builderWinsOverEnvVarSetToFalse() {
        helper.set(SERVICE_ENV_VAR, ENV_ENDPOINT);
        helper.set(SdkSystemSetting.AWS_IGNORE_CONFIGURED_ENDPOINT_URLS, "false");

        EndpointCapturingInterceptor interceptor = new EndpointCapturingInterceptor();
        ProtocolRestJsonClient client =
            ProtocolRestJsonClient.builder()
                                  .region(Region.US_WEST_2)
                                  .credentialsProvider(AnonymousCredentialsProvider.create())
                                  .ignoreConfiguredEndpointUrls(true)
                                  .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor))
                                  .build();

        try {
            client.allTypes();
        } catch (EndpointCapturingInterceptor.CaptureCompletedException e) {
            // expected
        }

        assertThat(interceptor.endpoints()).singleElement().asString().startsWith(DEFAULT_ENDPOINT);
    }

    @Test
    void ignoreTrue_isEndpointOverriddenReturnsFalseWhenFallingToDefault() {
        System.setProperty(SERVICE_SYS_PROP, ENV_ENDPOINT);
        System.setProperty(SdkSystemSetting.AWS_IGNORE_CONFIGURED_ENDPOINT_URLS.property(), "true");

        EndpointCapturingInterceptor interceptor = new EndpointCapturingInterceptor();
        ProtocolRestJsonClient client = baseBuilder(null, null)
            .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor))
            .build();

        try {
            client.allTypes();
        } catch (EndpointCapturingInterceptor.CaptureCompletedException e) {
            // expected
        }

        assertThat(interceptor.endpoints()).singleElement().asString().startsWith(DEFAULT_ENDPOINT);
    }

    private String resolveEndpoint(URI endpointOverride, ProfileFile profileFile) {
        EndpointCapturingInterceptor interceptor = new EndpointCapturingInterceptor();

        ProtocolRestJsonClientBuilder builder = baseBuilder(endpointOverride, profileFile);
        builder.overrideConfiguration(c -> {
            if (profileFile != null) {
                c.defaultProfileFile(profileFile).defaultProfileName("default");
            }
            c.addExecutionInterceptor(interceptor);
        });

        ProtocolRestJsonClient client = builder.build();

        try {
            client.allTypes();
        } catch (EndpointCapturingInterceptor.CaptureCompletedException e) {
            // expected
        }

        assertThat(interceptor.endpoints()).hasSize(1);
        return interceptor.endpoints().get(0);
    }

    private ProtocolRestJsonClientBuilder baseBuilder(URI endpointOverride, ProfileFile profileFile) {
        ProtocolRestJsonClientBuilder builder =
            ProtocolRestJsonClient.builder()
                                  .region(Region.US_WEST_2)
                                  .credentialsProvider(AnonymousCredentialsProvider.create());

        if (endpointOverride != null) {
            builder.endpointOverride(endpointOverride);
        }

        return builder;
    }

    private static ProfileFile profileWithIgnore(String ignoreValue, String endpointUrl) {
        StringBuilder content = new StringBuilder();
        content.append("[default]\n");
        content.append("ignore_configured_endpoint_urls = ").append(ignoreValue).append("\n");
        if (endpointUrl != null) {
            content.append("endpoint_url = ").append(endpointUrl).append("\n");
        }
        return ProfileFile.builder()
                          .type(ProfileFile.Type.CONFIGURATION)
                          .content(content.toString())
                          .build();
    }

    private static ProfileFile profileWithEndpoint(String endpointUrl) {
        String content = "[default]\n"
                         + "endpoint_url = " + endpointUrl + "\n";
        return ProfileFile.builder()
                          .type(ProfileFile.Type.CONFIGURATION)
                          .content(content)
                          .build();
    }

    private static ProfileFile profileWithServicesSection(String endpointUrl) {
        String content = "[default]\n"
                         + "services = dev\n\n"
                         + "[services dev]\n"
                         + "amazonprotocolrestjson =\n"
                         + "  endpoint_url = " + endpointUrl + "\n";
        return ProfileFile.builder()
                          .type(ProfileFile.Type.CONFIGURATION)
                          .content(content)
                          .build();
    }
}

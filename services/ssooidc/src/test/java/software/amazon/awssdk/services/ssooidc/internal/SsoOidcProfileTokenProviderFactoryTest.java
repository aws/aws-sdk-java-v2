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

package software.amazon.awssdk.services.ssooidc.internal;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.token.SdkTokenProvider;
import software.amazon.awssdk.auth.token.SdkTokenProviderFactoryProperties;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.services.ssooidc.SsoOidcProfileTokenProviderFactory;
import software.amazon.awssdk.utils.StringInputStream;

public class SsoOidcProfileTokenProviderFactoryTest {

    @Test
    public void create_throwsExceptionIfRegionNotPassed() {
        String startUrl = "https://my-start-url.com";
        Assertions.assertThatExceptionOfType(NullPointerException.class).isThrownBy(
            () -> SdkTokenProviderFactoryProperties.builder().
                                                   startUrl(startUrl)
                                                   .build()
        ).withMessage("region must not be null.");
    }

    @Test
    public void create_throwsExceptionIfStartUrlNotPassed() {
        String region = "test-region";

        Assertions.assertThatExceptionOfType(NullPointerException.class).isThrownBy(
            () -> SdkTokenProviderFactoryProperties.builder().
                                                   region(region)
                                                   .build()
        ).withMessage("startUrl must not be null.");
    }

    @Test
    public void  create_SsooidcTokenProvider_from_SsooidcSpecificProfile(){
        String profileContent = "[profile ssooidc]\n" +
                                "sso_region=us-east-1\n" +
                                "sso_start_url= https://start-url\n";
        ProfileFile profiles = ProfileFile.builder()
                                          .content(new StringInputStream(profileContent))
                                          .type(ProfileFile.Type.CONFIGURATION)
                                          .build();
        SdkTokenProvider sdkTokenProvider = new SsoOidcProfileTokenProviderFactory().create(profiles.profile("ssooidc").get());
        Assertions.assertThat(sdkTokenProvider).isNotNull();

    }


    @Test
    public void create_SsooidcTokenProvider_with_ssoAccountIdInProfile(){
        String profileContent = "[profile sso]\n" +
                                "sso_region=us-east-1\n" +
                                "sso_account_id=1234567\n" +
                                "sso_start_url= https://start-url\n";
        ProfileFile profiles = ProfileFile.builder()
                                          .content(new StringInputStream(profileContent))
                                          .type(ProfileFile.Type.CONFIGURATION)
                                          .build();

        Assertions.assertThatExceptionOfType(IllegalStateException.class)
                  .isThrownBy(() -> new SsoOidcProfileTokenProviderFactory().create(profiles.profile("sso").get()));

    }

    @Test
    public void create_SsooidcTokenProvider_with_ssoRoleNameInProfile(){
        String profileContent = "[profile sso]\n" +
                                "sso_region=us-east-1\n" +
                                "sso_role_name=ssoSpecificRole\n" +
                                "sso_start_url= https://start-url\n";
        ProfileFile profiles = ProfileFile.builder()
                                          .content(new StringInputStream(profileContent))
                                          .type(ProfileFile.Type.CONFIGURATION)
                                          .build();

        Assertions.assertThatExceptionOfType(IllegalStateException.class)
                  .isThrownBy(() -> new SsoOidcProfileTokenProviderFactory().create(profiles.profile("sso").get()));

    }

}
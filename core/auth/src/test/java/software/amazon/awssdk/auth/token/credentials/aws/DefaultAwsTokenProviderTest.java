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

package software.amazon.awssdk.auth.token.credentials.aws;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.token.credentials.aws.DefaultAwsTokenProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.utils.StringInputStream;

public class DefaultAwsTokenProviderTest {

    @Test
    public void defaultCreate() {
        DefaultAwsTokenProvider tokenProvider = DefaultAwsTokenProvider.create();
        assertThatThrownBy(tokenProvider::resolveToken)
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Unable to load token");
    }

    @Test
    public void profileFile() {
        DefaultAwsTokenProvider tokenProvider = DefaultAwsTokenProvider.builder()
                                                                       .profileFile(() -> profileFile("[default]"))
                                                                       .profileName("default")
                                                                       .build();
        assertThatThrownBy(tokenProvider::resolveToken)
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Unable to load token");
    }

    private ProfileFile profileFile(String string) {
        return ProfileFile.builder()
                          .content(new StringInputStream(string))
                          .type(ProfileFile.Type.CONFIGURATION)
                          .build();
    }
}

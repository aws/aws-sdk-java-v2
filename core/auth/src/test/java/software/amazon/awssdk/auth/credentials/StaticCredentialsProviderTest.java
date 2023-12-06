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

import org.junit.jupiter.api.Test;

public class StaticCredentialsProviderTest {

    @Test
    void getAwsCredentials_ReturnsSameCredentials() {
        AwsCredentials credentials = AwsBasicCredentials.builder()
                                                        .accessKeyId("akid")
                                                        .secretAccessKey("skid")
                                                        .credentialScope("credentialScope")
                                                        .build();
        AwsCredentials actualCredentials = StaticCredentialsProvider.create(credentials).resolveCredentials();
        assertThat(actualCredentials).isEqualTo(credentials);
    }

    @Test
    public void getSessionAwsCredentials_ReturnsSameCredentials() {
        AwsSessionCredentials credentials = AwsSessionCredentials.builder()
                                                                 .accessKeyId("akid")
                                                                 .secretAccessKey("skid")
                                                                 .sessionToken("token")
                                                                 .credentialScope("credentialScope")
                                                                 .build();
        AwsCredentials actualCredentials = StaticCredentialsProvider.create(credentials).resolveCredentials();
        assertThat(actualCredentials).isEqualTo(credentials);
    }

    @Test
    void nullCredentials_ThrowsException() {
        assertThatThrownBy(() -> StaticCredentialsProvider.create(null)).isInstanceOf(NullPointerException.class);
    }
}

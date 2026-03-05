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

package software.amazon.awssdk.services.s3.internal.s3express;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeParams;
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeProvider;
import software.amazon.awssdk.services.s3.auth.scheme.internal.DefaultS3AuthSchemeProvider;

class S3ExpressAuthSchemeProviderTest {

    private static final String S3EXPRESS_BUCKET = "s3expressformat--use1-az1--x-s3";

    @Test
    public void s3express_defaultAuthEnabled_returnsS3ExpressAuthScheme() {
        S3AuthSchemeProvider provider = DefaultS3AuthSchemeProvider.create();
        S3AuthSchemeParams params = S3AuthSchemeParams.builder()
                                                      .operation("PutObject")
                                                      .region(Region.US_EAST_1)
                                                      .bucket(S3EXPRESS_BUCKET)
                                                      .build();

        List<AuthSchemeOption> authOptions = provider.resolveAuthScheme(params);

        assertThat(authOptions).isNotNull();
        assertThat(authOptions.isEmpty()).isFalse();
        assertThat(authOptions.get(0).schemeId()).isEqualTo("aws.auth#sigv4-s3express");
    }

    @Test
    public void s3express_authDisabled_returnsV4AuthScheme() {
        S3AuthSchemeProvider provider = DefaultS3AuthSchemeProvider.create();
        S3AuthSchemeParams params = S3AuthSchemeParams.builder()
                                                      .operation("PutObject")
                                                      .region(Region.US_EAST_1)
                                                      .bucket(S3EXPRESS_BUCKET)
                                                      .disableS3ExpressSessionAuth(true)
                                                      .build();

        List<AuthSchemeOption> authOptions = provider.resolveAuthScheme(params);

        assertThat(authOptions).isNotNull();
        assertThat(authOptions.isEmpty()).isFalse();
        assertThat(authOptions.get(0).schemeId()).isEqualTo("aws.auth#sigv4");
    }
}

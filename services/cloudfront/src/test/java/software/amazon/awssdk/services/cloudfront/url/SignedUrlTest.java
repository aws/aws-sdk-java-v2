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

package software.amazon.awssdk.services.cloudfront.url;

import static org.assertj.core.api.Assertions.assertThat;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cloudfront.internal.url.DefaultSignedUrl;

class SignedUrlTest {

    @Test
    void signedUrl_shouldWork() {
        String protocol = "https";
        String domain = "domain";
        String encodedPath = "encodedPath";
        String url = protocol + "://" + domain + "/" + encodedPath;
        SignedUrl signedUrl = DefaultSignedUrl.builder().protocol(protocol).domain(domain).encodedPath(encodedPath).build();

        assertThat(signedUrl.protocol()).isEqualTo(protocol);
        assertThat(signedUrl.domain()).isEqualTo(domain);
        assertThat(signedUrl.encodedPath()).isEqualTo(encodedPath);
        assertThat(signedUrl.url()).isEqualTo(url);
    }

    @Test
    void testEqualsAndHashCodeContract() {
        EqualsVerifier.forClass(DefaultSignedUrl.class).verify();
    }

}

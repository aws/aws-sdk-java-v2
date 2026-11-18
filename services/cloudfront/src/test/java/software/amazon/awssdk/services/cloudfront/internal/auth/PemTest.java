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

package software.amazon.awssdk.services.cloudfront.internal.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.utils.StringUtils;

class PemTest {

    @Test
    void readPemObjects_validObject_returnsDerBytes() throws IOException {
        String pem = "-----BEGIN PUBLIC KEY-----\nAQID\n-----END PUBLIC KEY-----\n";

        List<PemObject> objects = Pem.readPemObjects(stream(pem));

        assertThat(objects).hasSize(1);
        assertThat(objects.get(0).getPemObjectType()).isEqualTo(PemObjectType.PUBLIC_KEY_X509);
        assertThat(objects.get(0).getDerBytes()).containsExactly(1, 2, 3);
    }

    @Test
    void readPemObjects_overlongBeginMarker_isSkipped() throws IOException {
        String pem = StringUtils.repeat("A", 200) + "-----BEGIN PUBLIC KEY-----\nAQID\n"
                     + StringUtils.repeat("A", 200) + "-----END PUBLIC KEY-----\n";

        assertThat(Pem.readPemObjects(stream(pem))).isEmpty();
    }

    @Test
    void readPemObjects_overlongBeginMarkerFollowedByValidObject_returnsValidObject() throws IOException {
        String pem = StringUtils.repeat("A", 200) + "-----BEGIN X-----\n"
                     + "-----BEGIN PUBLIC KEY-----\nAQID\n-----END PUBLIC KEY-----\n";

        List<PemObject> objects = Pem.readPemObjects(stream(pem));

        assertThat(objects).hasSize(1);
        assertThat(objects.get(0).getDerBytes()).containsExactly(1, 2, 3);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void readPemObjects_overlongMarkerWithNearMatchingLines_completesQuickly() throws IOException {
        int prefixLength = 250_000;
        StringBuilder pem = new StringBuilder();
        pem.append(StringUtils.repeat("A", prefixLength)).append("-----BEGIN X-----\n");
        String line = StringUtils.repeat("A", 2 * prefixLength) + "\n";
        pem.append(line).append(line);

        assertThat(Pem.readPemObjects(stream(pem.toString()))).isEmpty();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void readPemObjects_maxLengthMarkerWithLongContentLines_completesQuickly() throws IOException {
        String beginMarker = "-----BEGIN X-----";
        String marker = StringUtils.repeat("A", 128 - beginMarker.length()) + beginMarker;
        assertThat(marker).hasSize(128);
        StringBuilder pem = new StringBuilder(marker).append('\n');
        String line = StringUtils.repeat("A", 1_000_000) + "\n";
        for (int i = 0; i < 4; i++) {
            pem.append(line);
        }

        assertThat(Pem.readPemObjects(stream(pem.toString()))).isEmpty();
    }

    @Test
    void readPemObjects_indentedEndMarker_returnsDerBytes() throws IOException {
        String pem = "-----BEGIN PUBLIC KEY-----\nAQID\n  -----END PUBLIC KEY-----  \n";

        List<PemObject> objects = Pem.readPemObjects(stream(pem));

        assertThat(objects).hasSize(1);
        assertThat(objects.get(0).getDerBytes()).containsExactly(1, 2, 3);
    }

    private static ByteArrayInputStream stream(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }
}

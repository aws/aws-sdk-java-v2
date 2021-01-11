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

package software.amazon.awssdk.services.s3.internal.resource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.net.URI;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class S3AccessPointBuilderTest {
    private static final String LONG_STRING_64 = "1234567890123456789012345678901234567890123456789012345678901234";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void toURI_noDualstack() {
        URI result = S3AccessPointBuilder.create()
                                         .accessPointName("access-point")
                                         .accountId("account-id")
                                         .region("region")
                                         .protocol("protocol")
                                         .domain("domain")
                                         .toUri();

        assertThat(result, is(URI.create("protocol://access-point-account-id.s3-accesspoint.region.domain")));
    }

    @Test
    public void toURI_dualstack() {
        URI result = S3AccessPointBuilder.create()
                                         .accessPointName("access-point")
                                         .accountId("account-id")
                                         .region("region")
                                         .protocol("protocol")
                                         .domain("domain")
                                         .dualstackEnabled(true)
                                         .toUri();

        assertThat(result,
                   is(URI.create("protocol://access-point-account-id.s3-accesspoint.dualstack.region.domain")));
    }

    @Test
    public void toURI_FipsEnabled() {
        URI result = S3AccessPointBuilder.create()
                                         .accessPointName("access-point")
                                         .accountId("account-id")
                                         .region("region")
                                         .protocol("protocol")
                                         .domain("domain")
                                         .fipsEnabled(true)
                                         .toUri();

        assertThat(result, is(URI.create("protocol://access-point-account-id.s3-accesspoint.fips-region.domain")));
    }

    @Test
    public void toURI_accessPointNameWithSlashes_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> S3AccessPointBuilder.create()
                                         .accessPointName("access/point")
                                         .accountId("account-id")
                                         .region("region")
                                         .protocol("protocol")
                                         .domain("domain")
                                         .toUri())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("accessPointName")
            .hasMessageContaining("alphanumeric");
    }

    @Test
    public void toURI_accountIdWithSlashes_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> S3AccessPointBuilder.create()
                                                     .accessPointName("accesspoint")
                                                     .accountId("account/id")
                                                     .region("region")
                                                     .protocol("protocol")
                                                     .domain("domain")
                                                     .toUri())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("accountId")
            .hasMessageContaining("alphanumeric");
    }

    @Test
    public void toURI_accessPointNameWithTooLongString_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> S3AccessPointBuilder.create()
                                                     .accessPointName(LONG_STRING_64)
                                                     .accountId("account-id")
                                                     .region("region")
                                                     .protocol("protocol")
                                                     .domain("domain")
                                                     .toUri())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("accessPointName")
            .hasMessageContaining("63");    // max length
    }

    @Test
    public void toURI_accountIdWithTooLongString_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> S3AccessPointBuilder.create()
                                                     .accessPointName("accesspoint")
                                                     .accountId(LONG_STRING_64)
                                                     .region("region")
                                                     .protocol("protocol")
                                                     .domain("domain")
                                                     .toUri())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("accountId")
            .hasMessageContaining("63");    // max length
    }

    @Test
    public void toURI_accessPointNameWithEmptyString_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> S3AccessPointBuilder.create()
                                                     .accessPointName("")
                                                     .accountId("account-id")
                                                     .region("region")
                                                     .protocol("protocol")
                                                     .domain("domain")
                                                     .toUri())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("accessPointName")
            .hasMessageContaining("missing");
    }

    @Test
    public void toURI_accountIdWithEmptyString_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> S3AccessPointBuilder.create()
                                                     .accessPointName("accesspoint")
                                                     .accountId("")
                                                     .region("region")
                                                     .protocol("protocol")
                                                     .domain("domain")
                                                     .toUri())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("accountId")
            .hasMessageContaining("missing");
    }

    @Test
    public void toURI_accessPointNameWithUrlEncodedCharacters_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> S3AccessPointBuilder.create()
                                                     .accessPointName("access%2fpoint")
                                                     .accountId("account-id")
                                                     .region("region")
                                                     .protocol("protocol")
                                                     .domain("domain")
                                                     .toUri())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("accessPointName")
            .hasMessageContaining("alphanumeric");
    }

    @Test
    public void toURI_accountIdWithUrlEncodedCharacters_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> S3AccessPointBuilder.create()
                                                     .accessPointName("accesspoint")
                                                     .accountId("account%2fid")
                                                     .region("region")
                                                     .protocol("protocol")
                                                     .domain("domain")
                                                     .toUri())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("accountId")
            .hasMessageContaining("alphanumeric");
    }
}
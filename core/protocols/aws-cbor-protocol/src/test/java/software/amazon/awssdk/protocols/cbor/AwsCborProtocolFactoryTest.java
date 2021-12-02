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

package software.amazon.awssdk.protocols.cbor;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static software.amazon.awssdk.core.SdkSystemSetting.CBOR_ENABLED;
import static software.amazon.awssdk.core.traits.TimestampFormatTrait.Format.RFC_822;
import static software.amazon.awssdk.core.traits.TimestampFormatTrait.Format.UNIX_TIMESTAMP;
import static software.amazon.awssdk.core.traits.TimestampFormatTrait.Format.UNIX_TIMESTAMP_MILLIS;

import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;

public class AwsCborProtocolFactoryTest {

    private static AwsCborProtocolFactory factory;

    @BeforeClass
    public static void setup() {
        factory = AwsCborProtocolFactory.builder().build();
    }

    @Test
    public void defaultTimestampFormats_cborEnabled() {
        Map<MarshallLocation, TimestampFormatTrait.Format> defaultTimestampFormats = factory.getDefaultTimestampFormats();
        assertThat(defaultTimestampFormats.get(MarshallLocation.HEADER)).isEqualTo(RFC_822);
        assertThat(defaultTimestampFormats.get(MarshallLocation.PAYLOAD)).isEqualTo(UNIX_TIMESTAMP_MILLIS);
    }

    @Test
    public void defaultTimestampFormats_cborDisabled() {
        System.setProperty(CBOR_ENABLED.property(), "false");
        try {
            Map<MarshallLocation, TimestampFormatTrait.Format> defaultTimestampFormats = factory.getDefaultTimestampFormats();
            assertThat(defaultTimestampFormats.get(MarshallLocation.HEADER)).isEqualTo(RFC_822);
            assertThat(defaultTimestampFormats.get(MarshallLocation.PAYLOAD)).isEqualTo(UNIX_TIMESTAMP);
        } finally {
            System.clearProperty(CBOR_ENABLED.property());
        }
    }
}

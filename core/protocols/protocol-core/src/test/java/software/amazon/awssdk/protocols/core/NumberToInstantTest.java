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

package software.amazon.awssdk.protocols.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;

class NumberToInstantTest {

    @Test
    public void unixTimestampWithoutRoundingNeeded() {
        NumberToInstant instance = instance(TimestampFormatTrait.Format.UNIX_TIMESTAMP);
        Instant timestamp = instance.convert(1099510880.773d, instantField());
        assertNotNull(timestamp);
        assertEquals(Instant.ofEpochMilli(1099510880773L), timestamp);
    }

    @Test
    public void parsingTimestampWithRoundingNeeded() {
        NumberToInstant instance = instance(TimestampFormatTrait.Format.UNIX_TIMESTAMP);
        // NOTE: 1099510880.771d * 1_000d == 1.0995108807709999E12
        Instant timestamp = instance.convert(1099510880.771d, instantField());
        assertNotNull(timestamp);
        assertEquals(Instant.ofEpochMilli(1099510880771L), timestamp);
    }

    static NumberToInstant instance(TimestampFormatTrait.Format format) {
        Map<MarshallLocation, TimestampFormatTrait.Format> formats = new EnumMap<>(MarshallLocation.class);
        formats.put(MarshallLocation.PAYLOAD, format);

        return NumberToInstant.create(formats);
    }

    static SdkField<Instant> instantField() {
        return SdkField.builder(MarshallingType.INSTANT)
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).build())
            .build();
    }
}

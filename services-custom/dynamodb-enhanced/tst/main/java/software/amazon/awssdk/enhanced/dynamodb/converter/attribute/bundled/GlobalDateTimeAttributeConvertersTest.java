/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ConverterTestUtils.assertFails;
import static software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ConverterTestUtils.fromAttributeValue;
import static software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ConverterTestUtils.toAttributeValue;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;

public class GlobalDateTimeAttributeConvertersTest {
    @Test
    public void instantAsIntegerAttributeConverterBehaves() {
        InstantAsIntegerAttributeConverter converter = InstantAsIntegerAttributeConverter.create();

        assertThat(toAttributeValue(converter, Instant.MIN).asNumber()).isEqualTo("-31557014167219200");
        assertThat(toAttributeValue(converter, Instant.EPOCH.minusMillis(1)).asNumber()).isEqualTo("-0.001");
        assertThat(toAttributeValue(converter, Instant.EPOCH).asNumber()).isEqualTo("0");
        assertThat(toAttributeValue(converter, Instant.EPOCH.plusMillis(1)).asNumber()).isEqualTo("0.001");
        assertThat(toAttributeValue(converter, Instant.MAX).asNumber()).isEqualTo("31556889864403199.999999999");

        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("-31557014167219201")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("0.0000000000")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromNumber("31556889864403200")));

        // InstantAsIntegerAttributeConverter format
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("-31557014167219200"))).isEqualTo(Instant.MIN);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("-0.001"))).isEqualTo(Instant.EPOCH.minusMillis(1));
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber(".0"))).isEqualTo(Instant.EPOCH);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("0."))).isEqualTo(Instant.EPOCH);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("0"))).isEqualTo(Instant.EPOCH);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("0.000"))).isEqualTo(Instant.EPOCH);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("0.001"))).isEqualTo(Instant.EPOCH.plusMillis(1));
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("31556889864403199.999999999"))).isEqualTo(Instant.MAX);

        // InstantAsStringAttributeConverter format
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("1970-01-01T00:00:00Z"))).isEqualTo(Instant.EPOCH);

        // OffsetDateTimeAsStringAttributeConverter format
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("1970-01-01T00:00:00+01:00")))
                .isEqualTo(Instant.EPOCH.minus(1, ChronoUnit.HOURS));

        // ZonedDateTimeAsStringAttributeConverter format
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("1970-01-01T01:00:00+01:00[Europe/Paris]")))
                .isEqualTo(Instant.EPOCH);
    }

    @Test
    public void instantAsStringAttributeConverterBehaves() {
        InstantAsStringAttributeConverter converter = InstantAsStringAttributeConverter.create();

        assertThat(toAttributeValue(converter, Instant.MIN).asString()).isEqualTo("-1000000000-01-01T00:00:00Z");
        assertThat(toAttributeValue(converter, Instant.EPOCH.minusMillis(1)).asString()).isEqualTo("1969-12-31T23:59:59.999Z");
        assertThat(toAttributeValue(converter, Instant.EPOCH).asString()).isEqualTo("1970-01-01T00:00:00Z");
        assertThat(toAttributeValue(converter, Instant.EPOCH.plusMillis(1)).asString()).isEqualTo("1970-01-01T00:00:00.001Z");
        assertThat(toAttributeValue(converter, Instant.MAX).asString()).isEqualTo("+1000000000-12-31T23:59:59.999999999Z");

        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("-1000000001-12-31T23:59:59.999999999Z")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("X")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("+1000000001-01-01T00:00:00Z")));

        // InstantAsIntegerAttributeConverter format
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("0"))).isEqualTo(Instant.EPOCH);

        // InstantAsStringAttributeConverter format
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("-1000000000-01-01T00:00:00Z"))).isEqualTo(Instant.MIN);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("1969-12-31T23:59:59.999Z"))).isEqualTo(Instant.EPOCH.minusMillis(1));
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("1970-01-01T00:00:00Z"))).isEqualTo(Instant.EPOCH);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("1970-01-01T00:00:00.001Z"))).isEqualTo(Instant.EPOCH.plusMillis(1));
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("+1000000000-12-31T23:59:59.999999999Z"))).isEqualTo(Instant.MAX);

        // OffsetDateTimeAsStringAttributeConverter format
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("1970-01-01T00:00:00+01:00")))
                .isEqualTo(Instant.EPOCH.minus(1, ChronoUnit.HOURS));

        // ZonedDateTimeAsStringAttributeConverter format
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("1970-01-01T01:00:00+01:00[Europe/Paris]")))
                .isEqualTo(Instant.EPOCH);
    }

    @Test
    public void offsetDateTimeAsStringAttributeConverterBehaves() {
        OffsetDateTimeAsStringAttributeConverter converter = OffsetDateTimeAsStringAttributeConverter.create();

        OffsetDateTime epochUtc = Instant.EPOCH.atOffset(ZoneOffset.UTC);

        assertThat(toAttributeValue(converter, OffsetDateTime.MIN).asString()).isEqualTo("-999999999-01-01T00:00:00+18:00");
        assertThat(toAttributeValue(converter, epochUtc.minusNanos(1)).asString()).isEqualTo("1969-12-31T23:59:59.999999999Z");
        assertThat(toAttributeValue(converter, epochUtc).asString()).isEqualTo("1970-01-01T00:00:00Z");
        assertThat(toAttributeValue(converter, epochUtc.plusNanos(1)).asString()).isEqualTo("1970-01-01T00:00:00.000000001Z");
        assertThat(toAttributeValue(converter, OffsetDateTime.MAX).asString()).isEqualTo("+999999999-12-31T23:59:59.999999999-18:00");

        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("-1000000001-12-31T23:59:59.999999999Z")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("X")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("+1000000001-01-01T00:00:00Z")));

        // InstantAsIntegerAttributeConverter format
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("0"))).isEqualTo(epochUtc);

        // InstantAsStringAttributeConverter format
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("1970-01-01T00:00:00Z"))).isEqualTo(epochUtc);

        // OffsetDateTimeAsStringAttributeConverter format
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("1970-01-01T00:00:00+01:00")))
                .isEqualTo(OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(1)));
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("-999999999-01-01T00:00:00+18:00")))
                .isEqualTo(OffsetDateTime.MIN);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("1969-12-31T23:59:59.999999999Z")))
                .isEqualTo(epochUtc.minusNanos(1));
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("1970-01-01T00:00:00Z")))
                .isEqualTo(epochUtc);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("1970-01-01T00:00:00.000000001Z")))
                .isEqualTo(epochUtc.plusNanos(1));
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("+999999999-12-31T23:59:59.999999999-18:00")))
                .isEqualTo(OffsetDateTime.MAX);

        // ZonedDateTimeAsStringAttributeConverter format
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("1970-01-01T01:00:00+01:00[Europe/Paris]")))
                .isEqualTo(Instant.EPOCH.atOffset(ZoneOffset.ofHours(1)));
    }

    @Test
    public void zonedDateTimeAsStringAttributeConverterBehaves() {
        ZonedDateTimeAsStringAttributeConverter converter = ZonedDateTimeAsStringAttributeConverter.create();

        ZonedDateTime epochUtc = Instant.EPOCH.atZone(ZoneOffset.UTC);
        ZonedDateTime min = OffsetDateTime.MIN.toZonedDateTime();
        ZonedDateTime max = OffsetDateTime.MAX.toZonedDateTime();

        assertThat(toAttributeValue(converter, min).asString()).isEqualTo("-999999999-01-01T00:00:00+18:00");
        assertThat(toAttributeValue(converter, epochUtc.minusNanos(1)).asString()).isEqualTo("1969-12-31T23:59:59.999999999Z");
        assertThat(toAttributeValue(converter, epochUtc).asString()).isEqualTo("1970-01-01T00:00:00Z");
        assertThat(toAttributeValue(converter, Instant.EPOCH.atZone(ZoneId.of("Europe/Paris"))).asString())
                .isEqualTo("1970-01-01T01:00:00+01:00[Europe/Paris]");
        assertThat(toAttributeValue(converter, epochUtc.plusNanos(1)).asString()).isEqualTo("1970-01-01T00:00:00.000000001Z");
        assertThat(toAttributeValue(converter, max).asString()).isEqualTo("+999999999-12-31T23:59:59.999999999-18:00");

        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("-1000000001-12-31T23:59:59.999999999Z")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("X")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("+1000000001-01-01T00:00:00Z")));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromString("1970-01-01T00:00:00+01:00[FakeZone]")));

        // InstantAsIntegerAttributeConverter format
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromNumber("0"))).isEqualTo(epochUtc);

        // InstantAsStringAttributeConverter format
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("1970-01-01T00:00:00Z"))).isEqualTo(epochUtc);

        // OffsetDateTimeAsStringAttributeConverter format
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("1970-01-01T00:00:00+01:00")))
                .isEqualTo(epochUtc.minus(1, ChronoUnit.HOURS));

        // ZonedDateTimeAsStringAttributeConverter format
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("-999999999-01-01T00:00:00+18:00")))
                .isEqualTo(min);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("1970-01-01T01:00:00+01:00[Europe/Paris]")))
                .isEqualTo(Instant.EPOCH.atZone(ZoneId.of("Europe/Paris")));
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromString("+999999999-12-31T23:59:59.999999999-18:00")))
                .isEqualTo(max);
    }
}

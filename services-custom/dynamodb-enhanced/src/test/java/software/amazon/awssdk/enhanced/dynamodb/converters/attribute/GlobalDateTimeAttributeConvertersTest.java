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

package software.amazon.awssdk.enhanced.dynamodb.converters.attribute;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.assertFails;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.transformFrom;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.transformTo;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.InstantAsIntegerAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.InstantAsStringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.OffsetDateTimeAsStringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ZonedDateTimeAsStringAttributeConverter;

public class GlobalDateTimeAttributeConvertersTest {
    @Test
    public void instantAsIntegerAttributeConverterBehaves() {
        InstantAsIntegerAttributeConverter converter = InstantAsIntegerAttributeConverter.create();

        assertThat(transformFrom(converter, Instant.MIN).n()).isEqualTo("-31557014167219200");
        assertThat(transformFrom(converter, Instant.EPOCH.minusMillis(1)).n()).isEqualTo("-0.001");
        assertThat(transformFrom(converter, Instant.EPOCH).n()).isEqualTo("0");
        assertThat(transformFrom(converter, Instant.EPOCH.plusMillis(1)).n()).isEqualTo("0.001");
        assertThat(transformFrom(converter, Instant.MAX).n()).isEqualTo("31556889864403199.999999999");

        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromNumber("-31557014167219201")));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromNumber("0.0000000000")));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromNumber("31556889864403200")));

        // InstantAsIntegerAttributeConverter format
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("-31557014167219200"))).isEqualTo(Instant.MIN);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("-0.001"))).isEqualTo(Instant.EPOCH.minusMillis(1));
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber(".0"))).isEqualTo(Instant.EPOCH);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("0."))).isEqualTo(Instant.EPOCH);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("0"))).isEqualTo(Instant.EPOCH);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("0.000"))).isEqualTo(Instant.EPOCH);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("0.001"))).isEqualTo(Instant.EPOCH.plusMillis(1));
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("31556889864403199.999999999"))).isEqualTo(Instant.MAX);

        // InstantAsStringAttributeConverter format
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("1970-01-01T00:00:00Z"))).isEqualTo(Instant.EPOCH);

        // OffsetDateTimeAsStringAttributeConverter format
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("1970-01-01T00:00:00+01:00")))
                .isEqualTo(Instant.EPOCH.minus(1, ChronoUnit.HOURS));

        // ZonedDateTimeAsStringAttributeConverter format
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("1970-01-01T01:00:00+01:00[Europe/Paris]")))
                .isEqualTo(Instant.EPOCH);
    }

    @Test
    public void instantAsStringAttributeConverterBehaves() {
        InstantAsStringAttributeConverter converter = InstantAsStringAttributeConverter.create();

        assertThat(transformFrom(converter, Instant.MIN).s()).isEqualTo("-1000000000-01-01T00:00:00Z");
        assertThat(transformFrom(converter, Instant.EPOCH.minusMillis(1)).s()).isEqualTo("1969-12-31T23:59:59.999Z");
        assertThat(transformFrom(converter, Instant.EPOCH).s()).isEqualTo("1970-01-01T00:00:00Z");
        assertThat(transformFrom(converter, Instant.EPOCH.plusMillis(1)).s()).isEqualTo("1970-01-01T00:00:00.001Z");
        assertThat(transformFrom(converter, Instant.MAX).s()).isEqualTo("+1000000000-12-31T23:59:59.999999999Z");

        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("-1000000001-12-31T23:59:59.999999999Z")));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("X")));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("+1000000001-01-01T00:00:00Z")));

        // InstantAsIntegerAttributeConverter format
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("0"))).isEqualTo(Instant.EPOCH);

        // InstantAsStringAttributeConverter format
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("-1000000000-01-01T00:00:00Z"))).isEqualTo(Instant.MIN);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("1969-12-31T23:59:59.999Z"))).isEqualTo(Instant.EPOCH.minusMillis(1));
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("1970-01-01T00:00:00Z"))).isEqualTo(Instant.EPOCH);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("1970-01-01T00:00:00.001Z"))).isEqualTo(Instant.EPOCH.plusMillis(1));
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("+1000000000-12-31T23:59:59.999999999Z"))).isEqualTo(Instant.MAX);

        // OffsetDateTimeAsStringAttributeConverter format
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("1970-01-01T00:00:00+01:00")))
                .isEqualTo(Instant.EPOCH.minus(1, ChronoUnit.HOURS));

        // ZonedDateTimeAsStringAttributeConverter format
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("1970-01-01T01:00:00+01:00[Europe/Paris]")))
                .isEqualTo(Instant.EPOCH);
    }

    @Test
    public void offsetDateTimeAsStringAttributeConverterBehaves() {
        OffsetDateTimeAsStringAttributeConverter converter = OffsetDateTimeAsStringAttributeConverter.create();

        OffsetDateTime epochUtc = Instant.EPOCH.atOffset(ZoneOffset.UTC);

        assertThat(transformFrom(converter, OffsetDateTime.MIN).s()).isEqualTo("-999999999-01-01T00:00:00+18:00");
        assertThat(transformFrom(converter, epochUtc.minusNanos(1)).s()).isEqualTo("1969-12-31T23:59:59.999999999Z");
        assertThat(transformFrom(converter, epochUtc).s()).isEqualTo("1970-01-01T00:00:00Z");
        assertThat(transformFrom(converter, epochUtc.plusNanos(1)).s()).isEqualTo("1970-01-01T00:00:00.000000001Z");
        assertThat(transformFrom(converter, OffsetDateTime.MAX).s()).isEqualTo("+999999999-12-31T23:59:59.999999999-18:00");

        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("-1000000001-12-31T23:59:59.999999999Z")));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("X")));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("+1000000001-01-01T00:00:00Z")));

        // InstantAsIntegerAttributeConverter format
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("0"))).isEqualTo(epochUtc);

        // InstantAsStringAttributeConverter format
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("1970-01-01T00:00:00Z"))).isEqualTo(epochUtc);

        // OffsetDateTimeAsStringAttributeConverter format
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("1970-01-01T00:00:00+01:00")))
                .isEqualTo(OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(1)));
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("-999999999-01-01T00:00:00+18:00")))
                .isEqualTo(OffsetDateTime.MIN);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("1969-12-31T23:59:59.999999999Z")))
                .isEqualTo(epochUtc.minusNanos(1));
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("1970-01-01T00:00:00Z")))
                .isEqualTo(epochUtc);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("1970-01-01T00:00:00.000000001Z")))
                .isEqualTo(epochUtc.plusNanos(1));
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("+999999999-12-31T23:59:59.999999999-18:00")))
                .isEqualTo(OffsetDateTime.MAX);

        // ZonedDateTimeAsStringAttributeConverter format
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("1970-01-01T01:00:00+01:00[Europe/Paris]")))
                .isEqualTo(Instant.EPOCH.atOffset(ZoneOffset.ofHours(1)));
    }

    @Test
    public void zonedDateTimeAsStringAttributeConverterBehaves() {
        ZonedDateTimeAsStringAttributeConverter converter = ZonedDateTimeAsStringAttributeConverter.create();

        ZonedDateTime epochUtc = Instant.EPOCH.atZone(ZoneOffset.UTC);
        ZonedDateTime min = OffsetDateTime.MIN.toZonedDateTime();
        ZonedDateTime max = OffsetDateTime.MAX.toZonedDateTime();

        assertThat(transformFrom(converter, min).s()).isEqualTo("-999999999-01-01T00:00:00+18:00");
        assertThat(transformFrom(converter, epochUtc.minusNanos(1)).s()).isEqualTo("1969-12-31T23:59:59.999999999Z");
        assertThat(transformFrom(converter, epochUtc).s()).isEqualTo("1970-01-01T00:00:00Z");
        assertThat(transformFrom(converter, Instant.EPOCH.atZone(ZoneId.of("Europe/Paris"))).s())
                .isEqualTo("1970-01-01T01:00:00+01:00[Europe/Paris]");
        assertThat(transformFrom(converter, epochUtc.plusNanos(1)).s()).isEqualTo("1970-01-01T00:00:00.000000001Z");
        assertThat(transformFrom(converter, max).s()).isEqualTo("+999999999-12-31T23:59:59.999999999-18:00");

        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("-1000000001-12-31T23:59:59.999999999Z")));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("X")));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("+1000000001-01-01T00:00:00Z")));
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1970-01-01T00:00:00+01:00[FakeZone]")));

        // InstantAsIntegerAttributeConverter format
        assertThat(transformTo(converter, EnhancedAttributeValue.fromNumber("0"))).isEqualTo(epochUtc);

        // InstantAsStringAttributeConverter format
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("1970-01-01T00:00:00Z"))).isEqualTo(epochUtc);

        // OffsetDateTimeAsStringAttributeConverter format
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("1970-01-01T00:00:00+01:00")))
                .isEqualTo(epochUtc.minus(1, ChronoUnit.HOURS));

        // ZonedDateTimeAsStringAttributeConverter format
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("-999999999-01-01T00:00:00+18:00")))
                .isEqualTo(min);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("1970-01-01T01:00:00+01:00[Europe/Paris]")))
                .isEqualTo(Instant.EPOCH.atZone(ZoneId.of("Europe/Paris")));
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("+999999999-12-31T23:59:59.999999999-18:00")))
                .isEqualTo(max);
    }
}

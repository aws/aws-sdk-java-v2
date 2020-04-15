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
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ZonedDateTimeAsStringAttributeConverter;

public class ZonedDateTimeAsStringAttributeConverterTest {

    private static ZonedDateTimeAsStringAttributeConverter converter = ZonedDateTimeAsStringAttributeConverter.create();

    private static ZonedDateTime epochUtc = Instant.EPOCH.atZone(ZoneOffset.UTC);
    private static ZonedDateTime min = OffsetDateTime.MIN.toZonedDateTime();
    private static ZonedDateTime max = OffsetDateTime.MAX.toZonedDateTime();

    @Test
    public void ZonedDateTimeAsStringAttributeConverterMinTest() {
        verifyTransform(min, "-999999999-01-01T00:00+18:00");
    }

    @Test
    public void ZonedDateTimeAsStringAttributeConverterEpochMinusOneMilliTest() {
        verifyTransform(epochUtc.minusNanos(1), "1969-12-31T23:59:59.999999999Z");
    }

    @Test
    public void ZonedDateTimeAsStringAttributeConverterEpochTest() {
        verifyTransform(epochUtc, "1970-01-01T00:00Z");
    }

    @Test
    public void ZonedDateTimeAsStringAttributeConverterEpochPlusOneMilliTest() {
        verifyTransform(epochUtc.plusNanos(1), "1970-01-01T00:00:00.000000001Z");
    }

    @Test
    public void ZonedDateTimeAsStringAttributeConverterMaxTest() {
        verifyTransform(max, "+999999999-12-31T23:59:59.999999999-18:00");
    }

    @Test
    public void ZonedDateTimeAsStringAttributeConverterTimeZoneAtParisTest() {
        verifyTransform(Instant.EPOCH.atZone(ZoneId.of("Europe/Paris")), "1970-01-01T01:00+01:00[Europe/Paris]");
    }

    @Test
    public void ZonedDateTimeAsStringAttributeConverterNormalOffsetTest() {
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("1970-01-01T00:00:00+01:00")))
            .isEqualTo(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(1)));
    }


    @Test
    public void ZonedDateTimeAsStringAttributeConverterExceedLowerBoundTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("-1000000001-12-31T23:59:59.999999999Z")
                                                                       .toAttributeValue()));
    }

    @Test
    public void ZonedDateTimeAsStringAttributeConverterInvalidFormatTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("X")
                                                                       .toAttributeValue()));
    }

    @Test
    public void ZonedDateTimeAsStringAttributeConverterExceedHigherBoundTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("+1000000001-01-01T00:00:00Z")
                                                                       .toAttributeValue()));
    }

    @Test
    public void ZonedDateTimeAsStringAttributeConverterFakeZoneTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1970-01-01T00:00:00+01:00[FakeZone]")
                                                                       .toAttributeValue()));
    }

    @Test
    public void ZonedDateTimeAsStringAttributeConverterNotAcceptLocalDateTimeTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1988-05-21T00:12:00.000000001")
                                                                       .toAttributeValue()));
    }

    @Test
    public void ZonedDateTimeAsStringAttributeConverterNotAcceptLocalDateTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1988-05-21")
                                                                       .toAttributeValue()));
    }

    @Test
    public void ZonedDateTimeAsStringAttributeConverterNotAcceptLocalTimeTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("00:12:00.000000001")
                                                                       .toAttributeValue()));
    }

    @Test
    public void ZonedDateTimeAsStringAttributeConverterNotAcceptMonthDayTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("05-21")
                                                                       .toAttributeValue()));
    }

    @Test
    public void ZonedDateTimeAsStringAttributeConverterAdditionallyAcceptInstantTest() {
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("1970-01-01T00:00:00Z"))).isEqualTo(epochUtc);
    }

    @Test
    public void ZonedDateTimeAsStringAttributeConverterAdditionallyAcceptOffsetDateTimeTest() {
        // To make sure the specific zone converter is selected, here a specific Zoned converter is used.
        ZonedDateTimeAsStringAttributeConverter converter = ZonedDateTimeAsStringAttributeConverter.create();

        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("1970-01-01T00:00:00+01:00")))
            .isEqualTo(epochUtc.minus(1, ChronoUnit.HOURS));
    }

    private void verifyTransform(ZonedDateTime objectToTransform, String attributeValueString) {
        assertThat(transformFrom(converter, objectToTransform))
            .isEqualTo(EnhancedAttributeValue.fromString(attributeValueString).toAttributeValue());
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString(attributeValueString).toAttributeValue()))
            .isEqualTo(objectToTransform);
    }

}

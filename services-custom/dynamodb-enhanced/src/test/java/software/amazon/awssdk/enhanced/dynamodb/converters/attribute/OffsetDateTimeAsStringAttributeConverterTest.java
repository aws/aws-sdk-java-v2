package software.amazon.awssdk.enhanced.dynamodb.converters.attribute;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.assertFails;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.transformFrom;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.transformTo;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.OffsetDateTimeAsStringAttributeConverter;

public class OffsetDateTimeAsStringAttributeConverterTest {

    private static OffsetDateTimeAsStringAttributeConverter converter = OffsetDateTimeAsStringAttributeConverter.create();

    private static OffsetDateTime epochUtc = Instant.EPOCH.atOffset(ZoneOffset.UTC);

    @Test
    public void OffsetDateTimeAsStringAttributeConverterMinTest() {
        verifyTransform(OffsetDateTime.MIN, "-999999999-01-01T00:00+18:00");
    }

    @Test
    public void OffsetDateTimeAsStringAttributeConverterEpochMinusOneMilliTest() {
        verifyTransform(epochUtc.minusNanos(1), "1969-12-31T23:59:59.999999999Z");
    }

    @Test
    public void OffsetDateTimeAsStringAttributeConverterEpochTest() {
        verifyTransform(epochUtc, "1970-01-01T00:00Z");
    }

    @Test
    public void OffsetDateTimeAsStringAttributeConverterEpochPlusOneMilliTest() {
        verifyTransform(epochUtc.plusNanos(1), "1970-01-01T00:00:00.000000001Z");
    }

    @Test
    public void OffsetDateTimeAsStringAttributeConverterMaxTest() {
        verifyTransform(OffsetDateTime.MAX, "+999999999-12-31T23:59:59.999999999-18:00");
    }

    @Test
    public void OffsetDateTimeAsStringAttributeConverterNormalOffsetTest() {
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("1970-01-01T00:00:00+01:00")))
            .isEqualTo(OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(1)));
    }


    @Test
    public void OffsetDateTimeAsStringAttributeConverterExceedLowerBoundTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("-1000000001-12-31T23:59:59.999999999Z")
                                                                       .toAttributeValue()));
    }

    @Test
    public void OffsetDateTimeAsStringAttributeConverterInvalidFormatTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("X")
                                                                       .toAttributeValue()));
    }

    @Test
    public void OffsetDateTimeAsStringAttributeConverterExceedHigherBoundTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("+1000000001-01-01T00:00:00Z")
                                                                       .toAttributeValue()));
    }

    @Test
    public void OffsetDateTimeAsStringAsStringAttributeConverterNotAcceptLocalDateTimeTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1988-05-21T00:12:00.000000001")
                                                                       .toAttributeValue()));
    }

    @Test
    public void OffsetDateTimeAsStringAttributeConverterNotAcceptTimeZoneNamedZonedTimeTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1988-05-21T00:12:00+01:00[Europe/Paris]")
                                                                       .toAttributeValue()));
    }

    @Test
    public void OffsetDateTimeAsStringAttributeConverterNotAcceptLocalDateTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1988-05-21")
                                                                       .toAttributeValue()));
    }

    @Test
    public void OffsetDateTimeAsStringAttributeConverterNotAcceptLocalTimeTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("00:12:00.000000001")
                                                                       .toAttributeValue()));
    }

    @Test
    public void OffsetDateTimeAsStringAttributeConverterNotAcceptMonthDayTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("05-21")
                                                                       .toAttributeValue()));
    }

    @Test
    public void OffsetDateTimeAsStringAttributeConverterAdditionallyAcceptInstantTest() {
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString("1970-01-01T00:00:00Z"))).isEqualTo(epochUtc);
    }

    private void verifyTransform(OffsetDateTime objectToTransform, String attributeValueString) {
        assertThat(transformFrom(converter, objectToTransform))
            .isEqualTo(EnhancedAttributeValue.fromString(attributeValueString).toAttributeValue());
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString(attributeValueString).toAttributeValue()))
            .isEqualTo(objectToTransform);
    }
}

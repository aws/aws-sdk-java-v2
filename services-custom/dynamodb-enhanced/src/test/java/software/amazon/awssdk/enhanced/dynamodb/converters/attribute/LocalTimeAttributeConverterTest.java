package software.amazon.awssdk.enhanced.dynamodb.converters.attribute;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.assertFails;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.transformFrom;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.transformTo;

import java.time.LocalTime;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.LocalTimeAttributeConverter;

public class LocalTimeAttributeConverterTest {

    private static LocalTimeAttributeConverter converter = LocalTimeAttributeConverter.create();

    @Test
    public void LocalTimeAttributeConverterMinTest() {
        verifyTransform(LocalTime.MIN, "00:00");
    }

    @Test
    public void LocalTimeAttributeConverterNormalTest() {
        verifyTransform(LocalTime.of(1, 2, 3, 4), "01:02:03.000000004");
    }

    @Test
    public void LocalTimeAttributeConverterMaxTest() {
        verifyTransform(LocalTime.MAX, "23:59:59.999999999");
    }


    @Test
    public void LocalTimeAttributeConverterInvalidFormatTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("-1")
                                                                       .toAttributeValue()));
    }

    @Test
    public void LocalTimeAttributeConverterExceedHigherBoundTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("24:00:00")
                                                                       .toAttributeValue()));
    }

    @Test
    public void LocalTimeAttributeConverterInvalidNanoSecondsTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("00:00:00.9999999999")
                                                                       .toAttributeValue()));
    }

    @Test
    public void LocalTimeAttributeConverterNotAcceptLocalDateTimeTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1988-05-21T00:12:00.000000001")
                                                                       .toAttributeValue()));
    }

    @Test
    public void LocalTimeAttributeConverterNotAcceptInstantTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1988-05-21T00:12:00.000000001Z")
                                                                       .toAttributeValue()));
    }

    @Test
    public void LocalTimeAttributeConverterNotAcceptOffsetTimeTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1988-05-21T00:12:00+01:00")
                                                                       .toAttributeValue()));
    }

    @Test
    public void LocalTimeAttributeConverterNotAcceptZonedTimeTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1988-05-21T00:12:00+01:00[Europe/Paris]")
                                                                       .toAttributeValue()));
    }

    @Test
    public void LocalTimeAttributeConverterNotAcceptLocalDateTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1988-05-21")
                                                                       .toAttributeValue()));
    }

    @Test
    public void LocalTimeAttributeConverterNotAcceptMonthDayTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("05-21")
                                                                       .toAttributeValue()));
    }

    private void verifyTransform(LocalTime objectToTransform, String attributeValueString) {
        assertThat(transformFrom(converter, objectToTransform))
            .isEqualTo(EnhancedAttributeValue.fromString(attributeValueString).toAttributeValue());
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString(attributeValueString).toAttributeValue()))
            .isEqualTo(objectToTransform);
    }
}

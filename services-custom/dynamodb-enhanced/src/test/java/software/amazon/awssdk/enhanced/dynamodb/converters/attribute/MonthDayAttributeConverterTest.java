package software.amazon.awssdk.enhanced.dynamodb.converters.attribute;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.assertFails;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.transformFrom;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.transformTo;

import java.time.MonthDay;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.MonthDayAttributeConverter;

public class MonthDayAttributeConverterTest {

    private static MonthDayAttributeConverter converter = MonthDayAttributeConverter.create();

    @Test
    public void MonthDayAttributeConverterMinTest() {
        verifyTransform(MonthDay.of(1, 1), "--01-01");
    }

    @Test
    public void MonthDayAttributeConverterNormalTest() {
        verifyTransform(MonthDay.of(5, 21), "--05-21");
    }

    @Test
    public void MonthDayAttributeConverterMaxTest() {
        verifyTransform(MonthDay.of(12, 31), "--12-31");
    }


    @Test
    public void MonthDayAttributeConverterInvalidFormatTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("X")
                                                                       .toAttributeValue()));
    }

    @Test
    public void MonthDayAttributeConverterInvalidDateTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("--02-30")
                                                                       .toAttributeValue()));
    }

    @Test
    public void MonthDayAttributeConverterNotAcceptLocalDateTimeTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1988-05-21T00:12:00.000000001")
                                                                       .toAttributeValue()));
    }

    @Test
    public void MonthDayAttributeConverterNotAcceptInstantTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1988-05-21T00:12:00.000000001Z")
                                                                       .toAttributeValue()));
    }

    @Test
    public void MonthDayAttributeConverterNotAcceptOffsetTimeTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1988-05-21T00:12:00+01:00")
                                                                       .toAttributeValue()));
    }

    @Test
    public void MonthDayAttributeConverterNotAcceptZonedTimeTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1988-05-21T00:12:00+01:00[Europe/Paris]")
                                                                       .toAttributeValue()));
    }

    @Test
    public void MonthDayAttributeConverterNotAcceptLocalDateTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1988-05-21")
                                                                       .toAttributeValue()));
    }

    @Test
    public void MonthDayAttributeConverterNotAcceptLocalTimeTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("00:12:00.000000001")
                                                                       .toAttributeValue()));
    }

    private void verifyTransform(MonthDay objectToTransform, String attributeValueString) {
        assertThat(transformFrom(converter, objectToTransform))
            .isEqualTo(EnhancedAttributeValue.fromString(attributeValueString).toAttributeValue());
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString(attributeValueString).toAttributeValue()))
            .isEqualTo(objectToTransform);
    }

}

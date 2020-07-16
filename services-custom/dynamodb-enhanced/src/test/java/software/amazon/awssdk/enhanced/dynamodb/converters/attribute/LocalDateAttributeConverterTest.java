package software.amazon.awssdk.enhanced.dynamodb.converters.attribute;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.assertFails;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.transformFrom;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.transformTo;

import java.time.LocalDate;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.LocalDateAttributeConverter;

public class LocalDateAttributeConverterTest {

    private static LocalDateAttributeConverter converter = LocalDateAttributeConverter.create();

    @Test
    public void LocalDateAttributeConverterMinTest() {
        verifyTransform(LocalDate.MIN, "-999999999-01-01");
    }

    @Test
    public void LocalDateAttributeConverterNormalTest() {
        verifyTransform(LocalDate.of(0, 1, 1), "0000-01-01");
    }

    @Test
    public void LocalDateAttributeConverterMaxTest() {
        verifyTransform(LocalDate.MAX, "+999999999-12-31");
    }


    @Test
    public void LocalDateAttributeConverterLowerBoundTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("-9999999999-01-01")
                                                                       .toAttributeValue()));
    }

    @Test
    public void LocalDateAttributeConverterHigherBoundTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("9999999999-12-31")
                                                                       .toAttributeValue()));
    }

    @Test
    public void LocalDateAttributeConverterExceedHigherBoundTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("9999999999-12-32")
                                                                       .toAttributeValue()));
    }

    @Test
    public void LocalDateAttributeConverterNotAcceptLocalDateTimeTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1988-05-21T00:12:00.000000001")
                                                                       .toAttributeValue()));
    }

    @Test
    public void LocalDateAttributeConverterNotAcceptInstantTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1988-05-21T00:12:00.000000001Z")
                                                                       .toAttributeValue()));
    }

    @Test
    public void LocalDateAttributeConverterNotAcceptOffsetTimeTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1988-05-21T00:12:00+01:00")
                                                                       .toAttributeValue()));
    }

    @Test
    public void LocalDateAttributeConverterNotAcceptZonedTimeTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("1988-05-21T00:12:00+01:00[Europe/Paris]")
                                                                       .toAttributeValue()));
    }

    @Test
    public void LocalDateAttributeConverterNotAcceptLocalTimeTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("00:12:00.000000001")
                                                                       .toAttributeValue()));
    }

    @Test
    public void LocalDateAttributeConverterNotAcceptMonthDayTest() {
        assertFails(() -> transformTo(converter, EnhancedAttributeValue.fromString("05-21")
                                                                       .toAttributeValue()));
    }

    private void verifyTransform(LocalDate objectToTransform, String attributeValueString) {
        assertThat(transformFrom(converter, objectToTransform))
            .isEqualTo(EnhancedAttributeValue.fromString(attributeValueString).toAttributeValue());
        assertThat(transformTo(converter, EnhancedAttributeValue.fromString(attributeValueString).toAttributeValue()))
            .isEqualTo(objectToTransform);
    }
}

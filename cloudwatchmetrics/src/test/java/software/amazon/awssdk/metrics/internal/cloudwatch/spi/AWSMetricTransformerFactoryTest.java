package software.amazon.awssdk.metrics.internal.cloudwatch.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.After;
import org.junit.Test;

public class AWSMetricTransformerFactoryTest {

    @After
    public void after() {
        AwsMetricTransformerFactory
            .setTransformerPackage(AwsMetricTransformerFactory.DEFAULT_METRIC_TRANSFORM_PROVIDER_PACKAGE);
    }

    @Test
    public void nonNullDefaultPackage() {
        assertNotNull(AwsMetricTransformerFactory.getTransformerPackage());
    }

    @Test
    public void defaultAdaptors() {
        for (AwsMetricTransformerFactory f : AwsMetricTransformerFactory.values()) {
            RequestMetricTransformer a = f.getRequestMetricTransformer();
            assertEquals(
                    AwsMetricTransformerFactory.getTransformerPackage(), 
                a.getClass().getPackage().getName());
        }
    }

    @Test
    public void customAdaptors() {
        String customProviderPackage = "testing.custom.transformer";
        AwsMetricTransformerFactory.setTransformerPackage(customProviderPackage);
        RequestMetricTransformer a = AwsMetricTransformerFactory.DynamoDb
            .getRequestMetricTransformer();
        assertEquals(customProviderPackage, 
            a.getClass().getPackage().getName());
    }

    @Test(expected=IllegalArgumentException.class)
    public void nullCustomPackage() {
        AwsMetricTransformerFactory.setTransformerPackage(null);
    }

    @Test
    public void customPackageNotExist() {
        AwsMetricTransformerFactory.setTransformerPackage("i_dont_exist");
        assertSame(RequestMetricTransformer.NONE, 
                AwsMetricTransformerFactory.DynamoDb.getRequestMetricTransformer());
    }
}

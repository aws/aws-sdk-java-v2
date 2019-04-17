package software.amazon.awssdk.services.applicationautoscaling;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.applicationautoscaling.model.DescribeScalingPoliciesRequest;
import software.amazon.awssdk.services.applicationautoscaling.model.DescribeScalingPoliciesResponse;
import software.amazon.awssdk.services.applicationautoscaling.model.ServiceNamespace;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class AsyncClientIntegrationTest extends AwsIntegrationTestBase {
    private static ApplicationAutoScalingAsyncClient autoscaling;

    @BeforeClass
    public static void setUp() {
        autoscaling = ApplicationAutoScalingAsyncClient.builder()
                                                  .credentialsProvider(getCredentialsProvider())
                                                  .build();
    }

    @Test
    public void testScalingPolicy() {
        DescribeScalingPoliciesResponse res = autoscaling.describeScalingPolicies(
            DescribeScalingPoliciesRequest.builder().serviceNamespace(ServiceNamespace.ECS).build())
                                                         .join();

        Assert.assertNotNull(res);
        Assert.assertNotNull(res.scalingPolicies());
    }
}

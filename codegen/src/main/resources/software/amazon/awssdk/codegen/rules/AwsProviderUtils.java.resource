import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.regions.Region;

@SdkInternalApi
public final class AwsProviderUtils {
    private AwsProviderUtils() {
    }

    public static Region regionBuiltIn(ExecutionAttributes executionAttributes) {
        return executionAttributes.getAttribute(AwsExecutionAttribute.AWS_REGION);
    }

    public static Boolean dualStackEnabledBuiltIn(ExecutionAttributes executionAttributes) {
        return executionAttributes.getAttribute(AwsExecutionAttribute.DUALSTACK_ENDPOINT_ENABLED);
    }

    public static Boolean fipsEnabledBuiltIn(ExecutionAttributes executionAttributes) {
        return executionAttributes.getAttribute(AwsExecutionAttribute.FIPS_ENDPOINT_ENABLED);
    }

    public static String endpointBuiltIn(ExecutionAttributes executionAttributes) {
        if (ProviderUtils.endpointIsOverridden(executionAttributes)) {
            return executionAttributes.getAttribute(SdkExecutionAttribute.CLIENT_ENDPOINT).toString();
        }
        return null;
    }
}


package software.amazonaws.test;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.waf.regional.WafRegionalClient;

/**
 * The module containing all dependencies required by the {@link MyWafRegionalFunction}.
 */
public class DependencyFactory {

    private DependencyFactory() {}

    /**
     * @return an instance of WafRegionalClient
     */
    public static WafRegionalClient wafRegionalClient() {
        return WafRegionalClient.builder()
                       .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                       .region(Region.AP_SOUTHEAST_1)
                       .httpClientBuilder(ApacheHttpClient.builder())
                       .build();
    }
}

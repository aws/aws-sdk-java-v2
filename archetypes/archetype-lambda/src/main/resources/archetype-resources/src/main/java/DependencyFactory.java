#parse ( "global.vm")

package ${package};

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.${httpClientPackageName};
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.${servicePackage}.${serviceClientClassName};

/**
 * The module containing all dependencies required by the {@link ${handlerClassName}}.
 */
public class DependencyFactory {

    private DependencyFactory() {}

    /**
     * @return an instance of ${serviceClientClassName}
     */
    public static ${serviceClientClassName} ${serviceClientVariable}Client() {
        return ${serviceClientClassName}.builder()
                       .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                       .region(Region.${regionEnum})
                       .httpClientBuilder(${httpClientClassName}.builder())
                       .build();
    }
}

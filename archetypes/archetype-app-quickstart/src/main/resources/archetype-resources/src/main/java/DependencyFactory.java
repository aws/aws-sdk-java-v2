#parse ( "global.vm")

package ${package};

import software.amazon.awssdk.http.${httpClientPackageName};
import software.amazon.awssdk.services.${servicePackage}.${serviceClientClassName};

/**
 * The module containing all dependencies required by the {@link Handler}.
 */
public class DependencyFactory {

    private DependencyFactory() {}

    /**
     * @return an instance of ${serviceClientClassName}
     */
    public static ${serviceClientClassName} ${serviceClientVariable}Client() {
        return ${serviceClientClassName}.builder()
                       .httpClientBuilder(${httpClientClassName}.builder())
                       .build();
    }
}

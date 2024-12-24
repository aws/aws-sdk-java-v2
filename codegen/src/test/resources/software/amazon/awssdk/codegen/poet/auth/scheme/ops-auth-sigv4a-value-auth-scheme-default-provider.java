package software.amazon.awssdk.services.database.auth.scheme.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.services.database.auth.scheme.DatabaseAuthSchemeParams;
import software.amazon.awssdk.services.database.auth.scheme.DatabaseAuthSchemeProvider;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultDatabaseAuthSchemeProvider implements DatabaseAuthSchemeProvider {
    private static final DefaultDatabaseAuthSchemeProvider DEFAULT = new DefaultDatabaseAuthSchemeProvider();

    private DefaultDatabaseAuthSchemeProvider() {
    }

    public static DefaultDatabaseAuthSchemeProvider create() {
        return DEFAULT;
    }

    @Override
    public List<AuthSchemeOption> resolveAuthScheme(DatabaseAuthSchemeParams params) {
        List<AuthSchemeOption> options = new ArrayList<>();
        switch (params.operation()) {
            case "DeleteRow":
            case "PutRow":
                options.add(AuthSchemeOption.builder().schemeId("aws.auth#sigv4")
                                            .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "database-service")
                                            .putSignerProperty(AwsV4HttpSigner.REGION_NAME, params.region().id()).build());
                break;
            default:
                options.add(AuthSchemeOption.builder().schemeId("aws.auth#sigv4a")
                                            .putSignerProperty(AwsV4aHttpSigner.SERVICE_SIGNING_NAME, "database-service")
                                            .putSignerProperty(AwsV4aHttpSigner.REGION_SET, params.regionSet()).build());
                options.add(AuthSchemeOption.builder().schemeId("aws.auth#sigv4")
                                            .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "database-service")
                                            .putSignerProperty(AwsV4HttpSigner.REGION_NAME, params.region().id()).build());
                break;
        }
        return Collections.unmodifiableList(options);
    }
}

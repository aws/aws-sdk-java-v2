package software.amazon.awssdk.services.query.authscheme.internal;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.spi.HttpAuthOption;
import software.amazon.awssdk.services.query.authscheme.QueryAuthSchemeParams;
import software.amazon.awssdk.services.query.authscheme.QueryAuthSchemeProvider;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultQueryAuthSchemeProvider implements QueryAuthSchemeProvider {

    private static final DefaultQueryAuthSchemeProvider DEFAULT = new DefaultQueryAuthSchemeProvider();

    private DefaultQueryAuthSchemeProvider() {
    }

    public static DefaultQueryAuthSchemeProvider create() {
        return DEFAULT;
    }

    @Override
    public List<HttpAuthOption> resolveAuthScheme(QueryAuthSchemeParams authSchemeParams) {
        return new ArrayList<>();
    }
}

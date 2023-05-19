package software.amazon.awssdk.services.query.authscheme;

import java.util.List;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.auth.spi.HttpAuthOption;
import software.amazon.awssdk.services.query.authscheme.internal.DefaultQueryAuthSchemeProvider;


/**
 * An auth scheme provider for Query. The auth scheme provider takes a set of parameters using
 * {@link QueryAuthSchemeParams}, and resolves a list of {@link HttpAuthOption} based on the given parameters.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public interface QueryAuthSchemeProvider {
    /**
     * Resolve the auth schemes based on the given set of parameters.
     */
    List<HttpAuthOption> resolveAuthScheme(QueryAuthSchemeParams authSchemeParams);

    /**
     * Resolve the auth schemes based on the given set of parameters.
     */
    default List<HttpAuthOption> resolveAuthScheme(Consumer<QueryAuthSchemeParams.Builder> consumer) {
        QueryAuthSchemeParams.Builder builder = QueryAuthSchemeParams.builder();
        consumer.accept(builder);
        return resolveAuthScheme(builder.build());
    }

    static QueryAuthSchemeProvider defaultProvider() {
        return DefaultQueryAuthSchemeProvider.create();
    }
}

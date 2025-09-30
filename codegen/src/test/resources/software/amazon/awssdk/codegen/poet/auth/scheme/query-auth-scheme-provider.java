package software.amazon.awssdk.services.query.auth.scheme;

import java.util.List;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeProvider;
import software.amazon.awssdk.services.query.auth.scheme.internal.DefaultQueryAuthSchemeProvider;
import software.amazon.awssdk.services.query.auth.scheme.internal.PreferredQueryAuthSchemeProvider;

/**
 * An auth scheme provider for Query service. The auth scheme provider takes a set of parameters using
 * {@link QueryAuthSchemeParams}, and resolves a list of {@link AuthSchemeOption} based on the given parameters.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public interface QueryAuthSchemeProvider extends AuthSchemeProvider {
    /**
     * Resolve the auth schemes based on the given set of parameters.
     */
    List<AuthSchemeOption> resolveAuthScheme(QueryAuthSchemeParams authSchemeParams);

    /**
     * Resolve the auth schemes based on the given set of parameters.
     */
    default List<AuthSchemeOption> resolveAuthScheme(Consumer<QueryAuthSchemeParams.Builder> consumer) {
        QueryAuthSchemeParams.Builder builder = QueryAuthSchemeParams.builder();
        consumer.accept(builder);
        return resolveAuthScheme(builder.build());
    }

    /**
     * Get the default auth scheme provider.
     */
    static QueryAuthSchemeProvider defaultProvider() {
        return DefaultQueryAuthSchemeProvider.create();
    }

    /**
     * Get the default auth scheme provider the preferred auth schemes in order of preference.
     */
    static QueryAuthSchemeProvider defaultProvider(List<String> authSchemePreference) {
        return new PreferredQueryAuthSchemeProvider(defaultProvider(), authSchemePreference);
    }
}

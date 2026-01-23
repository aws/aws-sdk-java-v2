package software.amazon.awssdk.services.query.auth.scheme.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.services.query.auth.scheme.QueryAuthSchemeParams;
import software.amazon.awssdk.services.query.auth.scheme.QueryAuthSchemeProvider;
import software.amazon.awssdk.utils.CollectionUtils;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class PreferredQueryAuthSchemeProvider implements QueryAuthSchemeProvider {
    private final QueryAuthSchemeProvider delegate;

    private final List<String> authSchemePreference;

    public PreferredQueryAuthSchemeProvider(QueryAuthSchemeProvider delegate, List<String> authSchemePreference) {
        this.delegate = delegate;
        this.authSchemePreference = authSchemePreference != null ? authSchemePreference : Collections.emptyList();
    }

    /**
     * Resolve the auth schemes based on the given set of parameters.
     */
    @Override
    public List<AuthSchemeOption> resolveAuthScheme(QueryAuthSchemeParams params) {
        List<AuthSchemeOption> candidateAuthSchemes = delegate.resolveAuthScheme(params);
        if (CollectionUtils.isNullOrEmpty(authSchemePreference)) {
            return candidateAuthSchemes;
        }
        List<AuthSchemeOption> authSchemes = new ArrayList<>();
        authSchemePreference.forEach(preferredSchemeId -> {
            candidateAuthSchemes
                .stream()
                .filter(candidate -> {
                    String candidateSchemeName = candidate.schemeId().contains("#") ? candidate.schemeId().split("#")[1]
                                                                                    : candidate.schemeId();
                    return candidateSchemeName.equals(preferredSchemeId);
                }).findFirst().ifPresent(authSchemes::add);
        });
        candidateAuthSchemes.forEach(candidate -> {
            if (!authSchemes.contains(candidate)) {
                authSchemes.add(candidate);
            }
        });
        return authSchemes;
    }
}

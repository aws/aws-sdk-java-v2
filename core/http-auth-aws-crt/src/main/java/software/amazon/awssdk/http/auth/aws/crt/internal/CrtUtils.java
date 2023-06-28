package software.amazon.awssdk.http.auth.aws.crt.internal;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.crt.auth.credentials.Credentials;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

public class CrtUtils {
    private static final String BODY_HASH_NAME = "x-amz-content-sha256";
    private static final String DATE_NAME = "X-Amz-Date";
    private static final String AUTHORIZATION_NAME = "Authorization";
    private static final String REGION_SET_NAME = "X-amz-region-set";

    private static final String SIGNATURE_NAME = "X-Amz-Signature";
    private static final String CREDENTIAL_NAME = "X-Amz-Credential";
    private static final String ALGORITHM_NAME = "X-Amz-Algorithm";
    private static final String SIGNED_HEADERS_NAME = "X-Amz-SignedHeaders";
    private static final String EXPIRES_NAME = "X-Amz-Expires";

    private static final String HOST_HEADER = "Host";

    private static final Set<String> FORBIDDEN_HEADERS =
        Stream.of(BODY_HASH_NAME, DATE_NAME, AUTHORIZATION_NAME, REGION_SET_NAME)
            .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
    private static final Set<String> FORBIDDEN_PARAMS =
        Stream.of(SIGNATURE_NAME, DATE_NAME, CREDENTIAL_NAME, ALGORITHM_NAME, SIGNED_HEADERS_NAME, REGION_SET_NAME, EXPIRES_NAME)
            .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));

    /**
     * Sanitize an {@link SdkHttpRequest}, in order to prepare it for converting to a CRT request
     * destined to be signed.
     * <p>
     * Sanitizing includes checking the path is not empty, filtering headers and query parameters
     * that are forbidden in CRT, and adding the host header (overriding if already presesnt).
     */
    public static SdkHttpRequest sanitizeRequest(SdkHttpRequest request) {

        SdkHttpRequest.Builder builder = request.toBuilder();

        // Ensure path is non-empty
        String path = builder.encodedPath();
        if (path == null || path.length() == 0) {
            builder.encodedPath("/");
        }

        builder.clearHeaders();

        // Filter headers that will cause signing to fail
        request.forEachHeader((name, value) -> {
            if (!FORBIDDEN_HEADERS.contains(name)) {
                builder.putHeader(name, value);
            }
        });

        // Add host, which must be signed. We ignore any pre-existing Host header to match the behavior of the SigV4 signer.
        String hostHeader = SdkHttpUtils.isUsingStandardPort(request.protocol(), request.port())
            ? request.host()
            : request.host() + ":" + request.port();
        builder.putHeader(HOST_HEADER, hostHeader);

        builder.clearQueryParameters();

        // Filter query parameters that will cause signing to fail
        request.forEachRawQueryParameter((key, value) -> {
            if (!FORBIDDEN_PARAMS.contains(key)) {
                builder.putRawQueryParameter(key, value);
            }
        });

        return builder.build();
    }

    /**
     * Convert an {@link AwsCredentialsIdentity} to the CRT equivalent of credentials ({@link Credentials}).
     */
    static Credentials toCredentials(AwsCredentialsIdentity credentialsIdentity) {
        byte[] sessionToken = null;

        if (credentialsIdentity == null) {
            return null;
        }

        // identity-spi defines 2 known types - AwsCredentialsIdentity and a sub-type AwsSessionCredentialsIdentity
        if (credentialsIdentity instanceof AwsSessionCredentialsIdentity) {
            sessionToken = ((AwsSessionCredentialsIdentity) credentialsIdentity)
                .sessionToken()
                .getBytes(StandardCharsets.UTF_8);
        }

        return new Credentials(
            credentialsIdentity.accessKeyId().getBytes(StandardCharsets.UTF_8),
            credentialsIdentity.secretAccessKey().getBytes(StandardCharsets.UTF_8),
            sessionToken
        );
    }

}

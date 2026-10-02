package software.amazon.awssdk.services.s3.auth.scheme.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeParams;
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeProvider;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class FallbackS3AuthSchemeProvider implements S3AuthSchemeProvider {
    private static final FallbackS3AuthSchemeProvider DEFAULT = new FallbackS3AuthSchemeProvider();

    private FallbackS3AuthSchemeProvider() {
    }

    public static FallbackS3AuthSchemeProvider create() {
        return DEFAULT;
    }

    @Override
    public List<AuthSchemeOption> resolveAuthScheme(S3AuthSchemeParams params) {
        List<AuthSchemeOption> options = new ArrayList<>();
        switch (params.operation()) {
            case "UploadPart":
            case "PutObject":
                options.add(AuthSchemeOption.builder().schemeId("aws.auth#sigv4")
                                            .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "s3")
                                            .putSignerProperty(AwsV4HttpSigner.REGION_NAME, params.region().id())
                                            .putSignerProperty(AwsV4HttpSigner.PAYLOAD_SIGNING_ENABLED, false)
                                            .putSignerProperty(AwsV4HttpSigner.DOUBLE_URL_ENCODE, false)
                                            .putSignerProperty(AwsV4HttpSigner.NORMALIZE_PATH, false)
                                            .putSignerProperty(AwsV4HttpSigner.CHUNK_ENCODING_ENABLED, true).build());
                break;
            default:
                options.add(AuthSchemeOption.builder().schemeId("aws.auth#sigv4")
                                            .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "s3")
                                            .putSignerProperty(AwsV4HttpSigner.REGION_NAME, params.region().id())
                                            .putSignerProperty(AwsV4HttpSigner.PAYLOAD_SIGNING_ENABLED, false)
                                            .putSignerProperty(AwsV4HttpSigner.DOUBLE_URL_ENCODE, false)
                                            .putSignerProperty(AwsV4HttpSigner.NORMALIZE_PATH, false).build());
                break;
        }
        return Collections.unmodifiableList(options);
    }
}

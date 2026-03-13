package software.amazon.awssdk.services.json.auth.scheme.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.services.json.auth.scheme.JsonAuthSchemeParams;
import software.amazon.awssdk.services.json.auth.scheme.JsonAuthSchemeProvider;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultJsonAuthSchemeProvider implements JsonAuthSchemeProvider {
    private static final DefaultJsonAuthSchemeProvider DEFAULT = new DefaultJsonAuthSchemeProvider();

    private DefaultJsonAuthSchemeProvider() {
    }

    public static DefaultJsonAuthSchemeProvider create() {
        return DEFAULT;
    }

    @Override
    public List<AuthSchemeOption> resolveAuthScheme(JsonAuthSchemeParams params) {
        List<AuthSchemeOption> options = new ArrayList<>();
        switch (params.operation()) {
            case "BearerAuthOperation":
                options.add(AuthSchemeOption.builder().schemeId("smithy.api#httpBearerAuth").build());
                break;
            case "StreamingInputOutputOperation":
                options.add(AuthSchemeOption.builder().schemeId("aws.auth#sigv4")
                                            .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "json-service")
                                            .putSignerProperty(AwsV4HttpSigner.REGION_NAME, params.region().id())
                                            .putSignerProperty(AwsV4HttpSigner.PAYLOAD_SIGNING_ENABLED, false).build());
                break;
            case "PutOperationWithChecksum":
                options.add(AuthSchemeOption.builder().schemeId("aws.auth#sigv4")
                                            .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "json-service")
                                            .putSignerProperty(AwsV4HttpSigner.REGION_NAME, params.region().id())
                                            .putSignerProperty(AwsV4HttpSigner.CHUNK_ENCODING_ENABLED, true).build());
                break;
            default:
                options.add(AuthSchemeOption.builder().schemeId("aws.auth#sigv4")
                                            .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "json-service")
                                            .putSignerProperty(AwsV4HttpSigner.REGION_NAME, params.region().id()).build());
                break;
        }
        return Collections.unmodifiableList(options);
    }
}

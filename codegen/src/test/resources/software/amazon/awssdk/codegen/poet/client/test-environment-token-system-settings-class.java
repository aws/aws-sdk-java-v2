package software.amazon.awssdk.services.json.internal;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.SystemSetting;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public class EnvironmentTokenSystemSettings implements SystemSetting {
    @Override
    public String property() {
        return "aws.bearerTokenJsonService";
    }

    @Override
    public String environmentVariable() {
        return "AWS_BEARER_TOKEN_JSON_SERVICE";
    }

    @Override
    public String defaultValue() {
        return null;
    }
}

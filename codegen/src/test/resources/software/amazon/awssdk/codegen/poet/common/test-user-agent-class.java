package software.amazon.awssdk.services.json.internal;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.util.VersionInfo;
import software.amazon.awssdk.services.json.model.JsonRequest;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public class UserAgentUtils {
    private UserAgentUtils() {
    }

    public static <T extends JsonRequest> T applyUserAgentInfo(T request,
                                                               Consumer<AwsRequestOverrideConfiguration.Builder> userAgentApplier) {
        AwsRequestOverrideConfiguration overrideConfiguration = request.overrideConfiguration()
                                                                       .map(c -> c.toBuilder().applyMutation(userAgentApplier).build())
                                                                       .orElse((AwsRequestOverrideConfiguration.builder().applyMutation(userAgentApplier).build()));
        return (T) request.toBuilder().overrideConfiguration(overrideConfiguration).build();
    }

    public static <T extends JsonRequest> T applyPaginatorUserAgent(T request) {
        return applyUserAgentInfo(request,
                                  b -> b.addApiName(ApiName.builder().version(VersionInfo.SDK_VERSION).name("PAGINATED").build()));
    }
}
package software.amazon.awssdk.services.json.internal;

import java.lang.String;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class ServiceVersionInfo {
  /**
   * Returns the current version for the AWS SDK in which this class is running.
   */
  public static final String VERSION = "{{VERSION}}";

  /**
   * Returns a user agent containing the service and version info
   */
  @SdkInternalApi
  public static final String USER_AGENT = "{{USER_AGENT}}";

  private ServiceVersionInfo() {
  }
}

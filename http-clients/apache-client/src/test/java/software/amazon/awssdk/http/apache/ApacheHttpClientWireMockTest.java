package software.amazon.awssdk.http.apache;

import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientTestSuite;

public class ApacheHttpClientWireMockTest extends SdkHttpClientTestSuite {
    @Override
    protected SdkHttpClient createSdkHttpClient(SdkHttpClientOptions options) {
        return ApacheSdkHttpClientFactory.builder().build().createHttpClient();
    }
}


package software.amazonaws.test;

import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * The module containing all dependencies required by the {@link Handler}.
 */
public class DependencyFactory {

    private DependencyFactory() {}

    /**
     * @return an instance of S3AsyncClient
     */
    public static S3AsyncClient s3Client() {
        return S3AsyncClient.builder()
                       .httpClientBuilder(NettyNioAsyncHttpClient.builder())
                       .build();
    }
}

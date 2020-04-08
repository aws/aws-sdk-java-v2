
package software.amazonaws.test;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;

/**
 * The module containing all dependencies required by the {@link MyNettyFunction}.
 */
public class DependencyFactory {

    private DependencyFactory() {}

    /**
     * @return an instance of KinesisAsyncClient
     */
    public static KinesisAsyncClient kinesisClient() {
        return KinesisAsyncClient.builder()
                       .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                       .region(Region.US_EAST_1)
                       .httpClientBuilder(NettyNioAsyncHttpClient.builder())
                       .build();
    }
}

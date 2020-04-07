
package software.amazonaws.test;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsClient;

/**
 * The module containing all dependencies required by the {@link MyDynamoDbStreamsFunction}.
 */
public class DependencyFactory {

    private DependencyFactory() {}

    /**
     * @return an instance of DynamoDbStreamsClient
     */
    public static DynamoDbStreamsClient dynamoDbStreamsClient() {
        return DynamoDbStreamsClient.builder()
                       .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                       .region(Region.AP_SOUTHEAST_1)
                       .httpClientBuilder(ApacheHttpClient.builder())
                       .build();
    }
}

package software.amazon.awssdk.services.json;

import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.regions.Region;

@Generated("software.amazon.awssdk:codegen")
public class SimpleMethodsIntegrationTest {
    private static JsonClient client;

    @BeforeClass
    public static void setup() {
        if (JsonClient.serviceMetadata().regions().isEmpty()) {
            client = JsonClient.builder().region(Region.US_EAST_1).build();
        } else if (JsonClient.serviceMetadata().regions().contains(Region.AWS_GLOBAL)) {
            client = JsonClient.builder().region(Region.AWS_GLOBAL).build();
        } else if (JsonClient.serviceMetadata().regions().contains(Region.US_EAST_1)) {
            client = JsonClient.builder().region(Region.US_EAST_1).build();
        } else {
            client = JsonClient.builder().region(JsonClient.serviceMetadata().regions().get(0)).build();
        }
    }

    @Test
    public void getWithoutRequiredMembers_SimpleMethod_Succeeds() throws Exception {
        client.getWithoutRequiredMembers();
    }
}

package software.amazon.awssdk.services.s3.internal.endpoints;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import java.net.URI;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class S3ObjectLambdaOperationEndpointBuilderTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void toUri_buildsCorrectEndpoint() {
        URI endpoint = S3ObjectLambdaOperationEndpointBuilder.create()
                .domain("my-domain.com")
                .protocol("https")
                .region("us-west-2")
                .toUri();

        assertThat(endpoint.toString(), is("https://s3-object-lambda.us-west-2.my-domain.com"));
    }

    @Test
    public void toUri_disallowsNullProtocol() {
        thrown.expect(NullPointerException.class);

        S3ObjectLambdaOperationEndpointBuilder.create()
                .domain("my-domain.com")
                .protocol(null)
                .region("us-west-2")
                .toUri();
    }

    @Test
    public void toUri_disallowsEmptyProtocol() {
        thrown.expect(IllegalArgumentException.class);

        S3ObjectLambdaOperationEndpointBuilder.create()
                .domain("my-domain.com")
                .protocol("")
                .region("us-west-2")
                .toUri();
    }

    @Test
    public void toUri_disallowsNullRegion() {
        thrown.expect(NullPointerException.class);

        S3ObjectLambdaOperationEndpointBuilder.create()
                .domain("my-domain.com")
                .protocol("https")
                .region(null)
                .toUri();
    }

    @Test
    public void toUri_disallowsEmptyRegion() {
        thrown.expect(IllegalArgumentException.class);

        S3ObjectLambdaOperationEndpointBuilder.create()
                .domain("my-domain.com")
                .protocol("https")
                .region("")
                .toUri();
    }

    @Test
    public void toUri_disallowsNullDomain() {
        thrown.expect(NullPointerException.class);

        S3ObjectLambdaOperationEndpointBuilder.create()
                .domain(null)
                .protocol("https")
                .region("region")
                .toUri();
    }

    @Test
    public void toUri_disallowsEmptyDomain() {
        thrown.expect(IllegalArgumentException.class);

        S3ObjectLambdaOperationEndpointBuilder.create()
                .domain("")
                .protocol("https")
                .region("region")
                .toUri();
    }
}

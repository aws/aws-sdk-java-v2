package software.amazon.awssdk.core.internal.http.async;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;

/**
 * TCK verification test for {@link SimpleRequestProvider}.
 */
public class SimpleRequestProviderTckTest extends PublisherVerification<ByteBuffer> {
    private static final byte[] CONTENT = new byte[4906];
    public SimpleRequestProviderTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long l) {
        return new SimpleRequestProvider(makeFullRequest(), new ExecutionAttributes());
    }

    @Override
    public long maxElementsFromPublisher() {
        // SimpleRequestProvider is a one shot publisher
        return 1;
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        return null;
    }

    private static SdkHttpFullRequest makeFullRequest() {
        return SdkHttpFullRequest.builder()
                .protocol("https")
                .host("aws.amazon.com")
                .method(SdkHttpMethod.PUT)
                .content(new ByteArrayInputStream(CONTENT))
                .build();
    }
}

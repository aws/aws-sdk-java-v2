package software.amazon.awssdk.benchmark.ec2;

import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.transform.RunInstancesRequestMarshaller;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

public class V1Ec2MarshallerBenchmark {

    @Benchmark
    public Object marshall(MarshallerState s) {
        return runInstancesRequestMarshaller().marshall(s.getReq());
    }

    @State(Scope.Benchmark)
    public static class MarshallerState {
        @Param( {"TINY", "SMALL", "HUGE"})
        private TestItem testItem;

        private RunInstancesRequest req;

        @Setup
        public void setup() {
            req = testItem.getValue();
        }

        public RunInstancesRequest getReq() {
            return req;
        }
    }

    public enum TestItem {
        TINY,
        SMALL,
        HUGE;

        private static final V1ItemFactory factory = new V1ItemFactory();

        private RunInstancesRequest request;

        static {
            TINY.request = factory.tiny();
            SMALL.request = factory.small();
            HUGE.request = factory.huge();
        }

        public RunInstancesRequest getValue() {
            return request;
        }
    }


    private static final RunInstancesRequestMarshaller RUN_INSTANCES_REQUEST_MARSHALLER
        = new RunInstancesRequestMarshaller();

    public static RunInstancesRequestMarshaller runInstancesRequestMarshaller() {
        return RUN_INSTANCES_REQUEST_MARSHALLER;
    }

}

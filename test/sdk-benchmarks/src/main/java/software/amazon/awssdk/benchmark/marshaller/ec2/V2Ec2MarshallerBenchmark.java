/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.benchmark.marshaller.ec2;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import software.amazon.awssdk.protocols.query.AwsEc2ProtocolFactory;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.transform.RunInstancesRequestMarshaller;

public class V2Ec2MarshallerBenchmark {

    private static final AwsEc2ProtocolFactory PROTOCOL_FACTORY = AwsEc2ProtocolFactory.builder().build();

    private static final RunInstancesRequestMarshaller RUN_INSTANCES_REQUEST_MARSHALLER
        = new RunInstancesRequestMarshaller(PROTOCOL_FACTORY);

    @Benchmark
    public Object marshall(MarshallerState s) {
        return runInstancesRequestMarshaller().marshall(s.getReq());
    }

    @State(Scope.Benchmark)
    public static class MarshallerState {
        @Param({"TINY", "SMALL", "HUGE"})
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

        private static final V2ItemFactory FACTORY = new V2ItemFactory();

        private RunInstancesRequest request;

        static {
            TINY.request = FACTORY.tiny();
            SMALL.request = FACTORY.small();
            HUGE.request = FACTORY.huge();
        }

        public RunInstancesRequest getValue() {
            return request;
        }
    }

    private static RunInstancesRequestMarshaller runInstancesRequestMarshaller() {
        return RUN_INSTANCES_REQUEST_MARSHALLER;
    }

}

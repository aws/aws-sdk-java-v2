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

package software.amazon.awssdk.benchmark.coldstart;

import org.openjdk.jmh.infra.Blackhole;

/**
 * Measured unit for the cold-start benchmarks: build a service client and complete its first operation.
 *
 * <p>Separate from {@link SdkClientCreationBenchmark}, whose unit is construction only. The first operation is where the
 * marshaller, unmarshaller, signer, endpoint rules and interceptor chain are first exercised, so construction alone would
 * not represent what an application pays on its first request.
 */
public interface SdkClientColdStartBenchmark {

    void coldFirstCall(Blackhole blackhole) throws Exception;
}

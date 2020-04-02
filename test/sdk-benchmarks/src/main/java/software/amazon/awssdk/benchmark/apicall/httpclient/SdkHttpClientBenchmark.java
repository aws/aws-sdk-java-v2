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

package software.amazon.awssdk.benchmark.apicall.httpclient;

import org.openjdk.jmh.infra.Blackhole;

/**
 * Interface to be used for sdk http client benchmark
 */
public interface SdkHttpClientBenchmark {

    /**
     * Benchmark for sequential api calls
     *
     * @param blackhole the blackhole
     */
    void sequentialApiCall(Blackhole blackhole);

    /**
     * Benchmark for concurrent api calls.
     *
     * <p>Not applies to all sdk http clients such as UrlConnectionHttpClient.
     * Running with UrlConnectionHttpClient has high error rate because it doesn't
     * support connection pooling.
     *
     * @param blackhole the blackhole
     */
    default void concurrentApiCall(Blackhole blackhole) {
    }
}

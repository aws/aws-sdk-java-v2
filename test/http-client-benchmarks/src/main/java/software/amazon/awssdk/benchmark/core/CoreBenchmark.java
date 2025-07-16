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

package software.amazon.awssdk.benchmark.core;


import org.openjdk.jmh.infra.Blackhole;

/**
 * Core benchmark interface defining the four essential operations
 * that each HTTP client implementation must benchmark.
 */
public interface CoreBenchmark {
    void simpleGet(Blackhole blackhole) throws Exception;
    
    void simplePut(Blackhole blackhole) throws Exception;
    
    void multiThreadedGet(Blackhole blackhole) throws Exception;
    
    void multiThreadedPut(Blackhole blackhole) throws Exception;
}

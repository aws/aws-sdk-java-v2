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

package software.amazon.awssdk.enhanced.dynamodb.query.engine;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.query.result.EnhancedQueryLatencyReport;
import software.amazon.awssdk.enhanced.dynamodb.query.result.EnhancedQueryResult;
import software.amazon.awssdk.enhanced.dynamodb.query.spec.QueryExpressionSpec;

/**
 * Engine that executes a {@link QueryExpressionSpec} against DynamoDB, producing an
 * {@link EnhancedQueryResult}.
 */
@SdkInternalApi
public interface QueryExpressionEngine {

    /**
     * Execute the given spec and return an iterable of result rows.
     */
    EnhancedQueryResult execute(QueryExpressionSpec spec);

    /**
     * Execute the given spec and optionally report latency.
     *
     * @param spec           the query specification
     * @param reportConsumer optional consumer for the latency report; may be null
     * @return iterable of result rows
     */
    default EnhancedQueryResult execute(QueryExpressionSpec spec,
                                        Consumer<EnhancedQueryLatencyReport> reportConsumer) {
        long start = System.nanoTime();
        EnhancedQueryResult result = execute(spec);
        if (reportConsumer != null) {
            long totalMs = (System.nanoTime() - start) / 1_000_000;
            reportConsumer.accept(new EnhancedQueryLatencyReport(0L, 0L, 0L, totalMs));
        }
        return result;
    }
}

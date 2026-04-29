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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.query.result.EnhancedQueryRow;
import software.amazon.awssdk.enhanced.dynamodb.query.spec.QueryExpressionSpec;

/**
 * Async engine that executes a {@link QueryExpressionSpec} and produces a stream of
 * {@link EnhancedQueryRow}s.
 */
@SdkInternalApi
public interface QueryExpressionAsyncEngine {

    /**
     * Execute the given spec and return a publisher of result rows.
     */
    SdkPublisher<EnhancedQueryRow> execute(QueryExpressionSpec spec);
}

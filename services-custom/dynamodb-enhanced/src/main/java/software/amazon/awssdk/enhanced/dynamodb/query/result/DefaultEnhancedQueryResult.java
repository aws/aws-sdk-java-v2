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

package software.amazon.awssdk.enhanced.dynamodb.query.result;

import java.util.Iterator;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;

/**
 * Default implementation of {@link EnhancedQueryResult} that wraps an iterable of rows.
 */
@SdkInternalApi
public class DefaultEnhancedQueryResult implements EnhancedQueryResult {

    private final Iterable<EnhancedQueryRow> iterable;

    public DefaultEnhancedQueryResult(SdkIterable<EnhancedQueryRow> iterable) {
        this.iterable = iterable;
    }

    public DefaultEnhancedQueryResult(List<EnhancedQueryRow> rows) {
        this.iterable = rows;
    }

    @Override
    public Iterator<EnhancedQueryRow> iterator() {
        return iterable.iterator();
    }
}

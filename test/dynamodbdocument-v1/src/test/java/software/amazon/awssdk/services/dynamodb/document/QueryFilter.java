/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.document;

import software.amazon.awssdk.services.dynamodb.document.internal.Filter;

/**
 * A <a href=
 * "http://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_Query.html#DDB-Query-request-QueryFilter"
 * >query filter</a>.
 *
 * Typical usages:
 * <blockquote>
 * <code>new QueryFilter("strAttr").eq("attrValue");</code>
 * <p>
 * <code>new QueryFilter("intAttr").gt(42);</code>
 * <p>
 * ...
 * </blockquote>
 */
public class QueryFilter extends Filter<QueryFilter> {

    /**
     * A <a href=
     * "http://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_Query.html#DDB-Query-request-QueryFilter"
     * >query filter</a>.
     *
     * Typical usages:
     * <blockquote>
     * <code>new QueryFilter("strAttr").eq("attrValue");</code>
     * <p>
     * <code>new QueryFilter("intAttr").gt(42);</code>
     * <p>
     * ...
     * </blockquote>
     */
    public QueryFilter(String attr) {
        super(attr);
    }
}

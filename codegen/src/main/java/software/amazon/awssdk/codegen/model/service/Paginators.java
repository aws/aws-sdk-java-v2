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

package software.amazon.awssdk.codegen.model.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * POJO class to represent the paginators.json file.
 */
public class Paginators {

    private static final Paginators NONE = new Paginators(Collections.emptyMap());

    private Map<String, PaginatorDefinition> pagination;

    private Paginators() {
        this(new HashMap<>());
    }

    private Paginators(Map<String, PaginatorDefinition> pagination) {
        this.pagination = pagination;
    }

    public static Paginators none() {
        return NONE;
    }

    /**
     * Returns a map of operation name to its {@link PaginatorDefinition}.
     */
    public Map<String, PaginatorDefinition> getPagination() {
        return pagination;
    }

    public void setPagination(Map<String, PaginatorDefinition> pagination) {
        this.pagination = pagination;
    }

    public PaginatorDefinition getPaginatorDefinition(String operationName) {
        return pagination.get(operationName);
    }
}

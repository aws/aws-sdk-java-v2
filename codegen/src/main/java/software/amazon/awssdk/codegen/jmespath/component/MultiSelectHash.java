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

package software.amazon.awssdk.codegen.jmespath.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A multi-select-hash expression is similar to a multi-select-list {@link MultiSelectList} expression, except that a hash is
 * created instead of a list. A multi-select-hash expression also requires key names to be provided, as specified in the
 * keyval-expr ({@link KeyValueExpression} rule.
 *
 * https://jmespath.org/specification.html#multiselect-hash
 */
public class MultiSelectHash {
    private final List<KeyValueExpression> expressions;

    public MultiSelectHash(KeyValueExpression... expressions) {
        this.expressions = Arrays.asList(expressions);
    }

    public MultiSelectHash(Collection<KeyValueExpression> expressions) {
        this.expressions = new ArrayList<>(expressions);
    }

    public List<KeyValueExpression> keyValueExpressions() {
        return Collections.unmodifiableList(expressions);
    }
}

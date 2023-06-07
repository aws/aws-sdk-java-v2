/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.acm.endpoints.internal;

import java.util.HashMap;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class FatScope<T> {
    private final HashMap<Identifier, T> types;
    private final HashMap<Expr, T> facts;

    public FatScope(HashMap<Identifier, T> types, HashMap<Expr, T> facts) {
        this.types = types;
        this.facts = facts;
    }

    public FatScope() {
        this(new HashMap<>(), new HashMap<>());
    }

    public HashMap<Identifier, T> types() {
        return types;
    }

    public HashMap<Expr, T> facts() {
        return facts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FatScope<?> fatScope = (FatScope<?>) o;

        if (types != null ? !types.equals(fatScope.types) : fatScope.types != null) {
            return false;
        }
        return facts != null ? facts.equals(fatScope.facts) : fatScope.facts == null;
    }

    @Override
    public int hashCode() {
        int result = types != null ? types.hashCode() : 0;
        result = 31 * result + (facts != null ? facts.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FatScope[" + "types=" + types + ", " + "facts=" + facts + ']';
    }

}

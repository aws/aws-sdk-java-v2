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

package software.amazon.awssdk.migration.internal.utils;

import java.util.List;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

public final class IdentifierUtils {
    private IdentifierUtils() {
    }

    public static String simpleName(JavaType.Parameterized parameterized) {
        StringBuffer sb = new StringBuffer(parameterized.getClassName())
            .append('<');

        List<JavaType> typeParams = parameterized.getTypeParameters();

        for (int i = 0; i < typeParams.size(); ++i) {
            JavaType.FullyQualified fqParamType = TypeUtils.asFullyQualified(typeParams.get(i));
            if (fqParamType == null) {
                throw new RuntimeException("Encountered non fully qualified type");
            }
            sb.append(fqParamType.getClassName());

            if (i + 1 != typeParams.size()) {
                sb.append(", ");
            }
        }

        return sb.append(">").toString();
    }
}

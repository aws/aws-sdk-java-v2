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

package software.amazon.awssdk.codegen.poet;

import com.squareup.javapoet.ClassName;
import java.util.Arrays;
import java.util.Collections;

/**
 * A Poet static import definition to enable inclusion of static imports at code generation time
 */
public interface StaticImport {

    /**
     * @return The Poet representation of the class to import (may or may not yet exist)
     */
    ClassName className();

    /**
     * The members to import from the class for example if memberNames() returned List("trim", "isBlank") for
     * StringUtils.class then the following static imports would be generated:
     *
     * import static software.amazon.awssdk.utils.StringUtils.trim;
     * import static software.amazon.awssdk.utils.StringUtils.isBlank;
     *
     * @return The members to import from the class representation
     */
    Iterable<String> memberNames();

    /**
     * A helper implementation to create a {@link StaticImport} from a {@link Class}
     * @param clz the class to import
     * @param members the members from that class to import, if none then * is assumed
     * @return an anonymous implementation of {@link StaticImport}
     */
    static StaticImport staticMethodImport(Class<?> clz, String...members) {
        return new StaticImport() {
            @Override
            public ClassName className() {
                return ClassName.get(clz);
            }

            @Override
            public Iterable<String> memberNames() {
                if (members.length > 0) {
                    return Arrays.asList(members);
                }
                return Collections.singletonList("*");
            }
        };
    }
}

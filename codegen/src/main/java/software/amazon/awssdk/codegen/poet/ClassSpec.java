/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.squareup.javapoet.TypeSpec;
import java.util.Collections;

/**
 * Represents the a Poet generated class
 */
public interface ClassSpec {

    /**
     * @return The actual class specification generated from a <code>PoetSpec.builder()...</code> implementation
     */
    TypeSpec poetSpec();

    /**
     * @return The Poet representation of the class being generated, this may be used by other classes
     */
    ClassName className();

    /**
     * An optional hook to allow inclusion of static imports for example converting:
     *
     * <pre><code>
     * import software.amazon.awssdk.utils.StringUtils;
     * //...
     *   if(StringUtils.isBlank(value))...
     * </code></pre>
     *
     * to
     *
     * <pre><code>
     * import software.amazon.awssdk.utils.StringUtils.isBlank;
     * //...
     *   if(isBlank(value))...
     * </code></pre>
     *
     * @return the static imports to include
     */
    default Iterable<StaticImport> staticImports() {
        return Collections.emptyList();
    }
}

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

package software.amazon.awssdk.codegen.emitters;

import java.util.function.Function;
import java.util.stream.Stream;

public interface CodeTransformer extends Function<String, String> {
    static CodeTransformer chain(CodeTransformer... processors) {
        return input -> Stream.of(processors).map(p -> (Function<String, String>) p)
                              .reduce(Function.identity(), Function::andThen).apply(input);
    }

    String apply(String input);
}

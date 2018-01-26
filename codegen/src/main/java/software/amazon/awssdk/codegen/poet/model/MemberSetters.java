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

package software.amazon.awssdk.codegen.poet.model;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import java.util.List;

/**
 * Creates the methods for fluent and bean style member setters for a
 * {@link software.amazon.awssdk.codegen.model.intermediate.MemberModel}.
 */
interface MemberSetters {
    List<MethodSpec> fluentDeclarations(TypeName returnType);

    List<MethodSpec> fluent(TypeName returnType);

    MethodSpec beanStyle();
}


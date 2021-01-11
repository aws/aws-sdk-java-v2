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

package software.amazon.awssdk.codegen.poet.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import java.util.Arrays;
import java.util.List;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;

public class ExceptionProperties {

    private ExceptionProperties() {
    }

    public static List<MethodSpec> builderInterfaceMethods(ClassName className) {
        return Arrays.asList(
                builderMethod(className, "awsErrorDetails", AwsErrorDetails.class),
                builderMethod(className, "message", String.class),
                builderMethod(className, "requestId", String.class),
                builderMethod(className, "statusCode", int.class),
                builderMethod(className, "cause", Throwable.class));
    }

    public static List<MethodSpec> builderImplMethods(ClassName className) {
        return Arrays.asList(
                builderImplMethods(className, "awsErrorDetails", AwsErrorDetails.class),
                builderImplMethods(className, "message", String.class),
                builderImplMethods(className, "requestId", String.class),
                builderImplMethods(className, "statusCode", int.class),
                builderImplMethods(className, "cause", Throwable.class));
    }

    private static MethodSpec builderMethod(ClassName className, String name, Class clazz) {
        return MethodSpec.methodBuilder(name)
                .addAnnotation(Override.class)
                .returns(className)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(clazz, name)
                .build();
    }

    private static MethodSpec builderImplMethods(ClassName className, String name, Class clazz) {
        return MethodSpec.methodBuilder(name)
                .addAnnotation(Override.class)
                .returns(className)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(clazz, name)
                .addStatement("this." + name + " = " + name)
                .addStatement("return this")
                .build();
    }
}

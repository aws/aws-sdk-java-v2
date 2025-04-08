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

package software.amazon.awssdk.codegen.poet.eventstream;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.awssdk.codegen.poet.common.AbstractEnumClass;
import software.amazon.awssdk.core.SdkEventType;

public class EventTypeEnumSpec extends AbstractEnumClass {
    private static final Object VALUE = "value";
    private final String enumPackageName;
    private final IntermediateModel intermediateModel;

    public EventTypeEnumSpec(String enumPackageName, IntermediateModel intermediateModel, ShapeModel eventStream) {
        super(eventStream);
        this.enumPackageName = enumPackageName;
        this.intermediateModel = intermediateModel;
    }

    @Override
    protected void addSuperInterface(TypeSpec.Builder enumBuilder) {
        enumBuilder.addSuperinterface(ClassName.get(SdkEventType.class));
    }

    @Override
    protected void addDeprecated(TypeSpec.Builder enumBuilder) {
        // no-op
    }

    @Override
    protected void addJavadoc(TypeSpec.Builder enumBuilder) {
        enumBuilder.addJavadoc("The known possible types of events for {@code $N}.", getShape().getShapeName());
    }

    @Override
    protected void addEnumConstants(TypeSpec.Builder enumBuilder) {
        NamingStrategy namingStrategy = intermediateModel.getNamingStrategy();
        getShape().getMembers().stream()
                .filter(m -> m.getShape().isEvent())
                .forEach(m -> {
                    String value = m.getC2jName();
                    String name = namingStrategy.getEnumValueName(value);
                    enumBuilder.addEnumConstant(name, TypeSpec.anonymousClassBuilder("$S", value).build());
                });
    }

    @Override
    protected void addAdditionalMethods(TypeSpec.Builder enumBuilder) {
        enumBuilder.addMethod(
            MethodSpec.methodBuilder("id")
                      .addAnnotation(Override.class)
                      .returns(String.class)
                      .addModifiers(Modifier.PUBLIC)
                      .addStatement("return $T.valueOf($N)", String.class, VALUE)
                      .build()
        );
    }

    @Override
    public ClassName className() {
        return ClassName.get(enumPackageName, "EventType");
    }
}

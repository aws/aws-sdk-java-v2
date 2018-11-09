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

package software.amazon.awssdk.codegen.poet.transform.protocols;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.xml.AwsXmlProtocolFactory;

/**
 * MarshallerSpec for Xml protocol
 */
public class XmlMarshallerSpec extends QueryMarshallerSpec {

    public XmlMarshallerSpec(IntermediateModel model, ShapeModel shapeModel) {
        super(model, shapeModel);
    }

    @Override
    public CodeBlock marshalCodeBlock(ClassName requestClassName) {
        String variableName = shapeModel.getVariable().getVariableName();
        return CodeBlock.builder()
                        .addStatement("$T<$T> protocolMarshaller = protocolFactory.createProtocolMarshaller"
                                      + "(SDK_OPERATION_BINDING, rootMarshallLocationName, xmlNameSpaceUri)",
                                      ProtocolMarshaller.class, SdkHttpFullRequest.class)
                        .addStatement("return protocolMarshaller.marshall($L)", variableName)
                        .build();
    }

    @Override
    public List<FieldSpec> additionalFields() {
        return Arrays.asList(rootMarshallLocationName(), xmlNameSpaceUriField());
    }

    @Override
    protected Class<?> protocolFactoryClass() {
        return AwsXmlProtocolFactory.class;
    }

    /**
     * Some rest-xml services like Route53 have a additional element tag which is the root element.
     * This value is in the "input" shape in c2j model
     */
    private FieldSpec rootMarshallLocationName() {
        return FieldSpec.builder(ClassName.get(String.class), "rootMarshallLocationName")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
                        .initializer("$S", shapeModel.getMarshaller() != null ?  shapeModel.getMarshaller().getLocationName()
                                                                              :  null)
                        .build();
    }

    /**
     * xmlNamespace key can be present in the operation shape under "input" key (Route53)
     * or in the individual member structure (cloudfront, s3).
     */
    private FieldSpec xmlNameSpaceUriField() {
        return FieldSpec.builder(ClassName.get(String.class), "xmlNameSpaceUri")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
                        .initializer("$S", xmlNameSpaceUri())
                        .build();
    }

    private String xmlNameSpaceUri() {
        if (shapeModel.getMarshaller() != null && shapeModel.getMarshaller().getXmlNameSpaceUri() != null) {
            return shapeModel.getMarshaller().getXmlNameSpaceUri();
        }

        Set<String> xmlUris = shapeModel.getMembers().stream()
                                        .filter(m -> m.getXmlNameSpaceUri() != null)
                                        .map(MemberModel::getXmlNameSpaceUri)
                                        .collect(Collectors.toSet());

        if (xmlUris.isEmpty()) {
            return null;
        } else if (xmlUris.size() == 1) {
            return xmlUris.iterator().next();
        } else {
            throw new RuntimeException("Request has more than 1 xmlNameSpace uri.");
        }
    }
}

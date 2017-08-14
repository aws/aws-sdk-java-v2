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

package software.amazon.awssdk.codegen.poet.client.specs;

import com.squareup.javapoet.MethodSpec;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.codegen.poet.PoetExtensions;

public class Ec2ProtocolSpec extends QueryXmlProtocolSpec {

    public Ec2ProtocolSpec(PoetExtensions poetExtensions) {
        super(poetExtensions);
    }

    @Override
    public List<MethodSpec> additionalMethods() {
        List<MethodSpec> additionalMethods = new ArrayList<>();

        // TODO: Implement support for dry run requests.
        // additionalMethods.add(dryRunMethod());

        return additionalMethods;
    }

    /*
    private MethodSpec dryRunMethod() {
        TypeVariableName typeVariableName = TypeVariableName.get("X", AmazonWebServiceRequest.class);
        ClassName dryRunResult = poetExtensions.getModelClass("DryRunResult");
        TypeName dryRunResultGeneric = ParameterizedTypeName.get(dryRunResult, typeVariableName);
        ClassName dryRunRequest = poetExtensions.getModelClass("DryRunSupportedRequest");
        TypeName dryRunRequestGeneric = ParameterizedTypeName.get(dryRunRequest, typeVariableName);
        return MethodSpec.methodBuilder("dryRun")
                .returns(dryRunResultGeneric)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(dryRunRequestGeneric, "request")
                .addTypeVariable(typeVariableName)
                .addStatement("$T<X> dryRunRequest = request.getDryRunRequest()",
                        Request.class)
                .beginControlFlow("try")
                .addStatement("$T<$T> responseHandler = new $T<$T>(new $T())",
                        StaxResponseHandler.class,
                        dryRunResult,
                        StaxResponseHandler.class,
                        dryRunResult,
                        VoidStaxUnmarshaller.class)
                .addStatement("\nclientHandler.execute(new $T<$T, $T>().withMarshaller($L).withResponseHandler($N)" +
                                ".withInput($L))",
                        ClientExecutionParams.class,
                        Request.class,
                        dryRunResult,
                        "null",
                        "responseHandler",
                        "dryRunRequest")
                .addStatement("throw new $T($S)", AmazonClientException.class,
                        "Unrecognized service response for the dry-run request.")
                .endControlFlow()
                .beginControlFlow("catch (AmazonServiceException ase)")
                .beginControlFlow("if (ase.getErrorCode().equals($S) && ase.getStatusCode() == 412)",
                        "DryRunOperation")
                .addStatement("return new $T(true, request, ase.getMessage(), ase)", dryRunResultGeneric)
                .endControlFlow()
                .beginControlFlow("else if (ase.getErrorCode().equals($S) && ase.getStatusCode() == 403)",
                        "UnauthorizedOperation")
                .addStatement("return new $T(false, request, ase.getMessage(), ase)", dryRunResultGeneric)
                .endControlFlow()
                .addStatement("throw new $T($S, ase)", AmazonClientException.class,
                        "Unrecognized service response for the dry-run request.")
                .endControlFlow()
                .build();

    }
    */
}

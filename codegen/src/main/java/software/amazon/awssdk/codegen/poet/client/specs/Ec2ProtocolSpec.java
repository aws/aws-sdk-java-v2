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

package software.amazon.awssdk.codegen.poet.client.specs;

import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.protocols.query.AwsEc2ProtocolFactory;

public class Ec2ProtocolSpec extends QueryProtocolSpec {

    public Ec2ProtocolSpec(IntermediateModel model, PoetExtensions poetExtensions) {
        super(model, poetExtensions);
    }

    @Override
    protected Class<?> protocolFactoryClass() {
        return AwsEc2ProtocolFactory.class;
    }
}

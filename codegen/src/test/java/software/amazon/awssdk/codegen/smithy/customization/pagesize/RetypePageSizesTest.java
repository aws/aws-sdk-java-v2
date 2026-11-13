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

package software.amazon.awssdk.codegen.smithy.customization.pagesize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;

class RetypePageSizesTest {

    @Test
    void retargetsListedMembersToANewStringShape() {
        Model model = retype(RetypePageSizes.route53(),
                             "namespace com.amazonaws.route53\n"
                             + "service AWSDnsV20130401 { version: \"2013-04-01\" }\n"
                             + "structure ListCidrBlocksRequest { MaxResults: Integer }\n"
                             + "structure ListHostedZonesRequest { MaxItems: Integer }\n"
                             + "structure GetHostedZoneLimitRequest { MaxItems: Integer }\n");

        assertTarget(model, "route53#ListCidrBlocksRequest$MaxResults", "route53#MaxResults");
        assertTarget(model, "route53#ListHostedZonesRequest$MaxItems", "route53#PageMaxItems");
        assertThat(model.expectShape(ShapeId.from("com.amazonaws.route53#GetHostedZoneLimitRequest$MaxItems"),
                                     MemberShape.class).getTarget()).isEqualTo(ShapeId.from("smithy.api#Integer"));
        assertThat(model.expectShape(ShapeId.from("com.amazonaws.route53#MaxResults")).isStringShape()).isTrue();
        assertThat(model.expectShape(ShapeId.from("com.amazonaws.route53#PageMaxItems")).isStringShape()).isTrue();
    }

    @Test
    void retargetsListedMembersToAnExistingStringShape() {
        Model model = retype(RetypePageSizes.cloudFront(),
                             "namespace com.amazonaws.cloudfront\n"
                             + "service Cloudfront2020_05_31 { version: \"2020-05-31\" }\n"
                             + "integer integer\n"
                             + "string string\n"
                             + "structure ListDistributionsRequest { MaxItems: integer }\n");

        assertTarget(model, "cloudfront#ListDistributionsRequest$MaxItems", "cloudfront#string");
    }

    @Test
    void listedMemberThatNoLongerTargetsAnInteger_fails() {
        assertThatThrownBy(() -> retype(RetypePageSizes.glacier(),
                                        "namespace com.amazonaws.glacier\n"
                                        + "service Glacier { version: \"2012-06-01\" }\n"
                                        + "string string\n"
                                        + "structure ListVaultsInput { limit: Long }\n"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ListVaultsInput$limit");
    }

    private static Model retype(RetypePageSizes customization, String smithy) {
        Model model = Model.assembler()
                           .addUnparsedModel("test.smithy", "$version: \"2.0\"\n" + smithy)
                           .assemble()
                           .unwrap();
        return customization.preprocess(model, CustomizationConfig.create());
    }

    private static void assertTarget(Model model, String member, String target) {
        assertThat(model.expectShape(ShapeId.from("com.amazonaws." + member), MemberShape.class).getTarget())
            .isEqualTo(ShapeId.from("com.amazonaws." + target));
    }
}

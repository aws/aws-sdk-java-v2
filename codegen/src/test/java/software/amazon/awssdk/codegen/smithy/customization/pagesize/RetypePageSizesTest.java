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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;

class RetypePageSizesTest {

    @Test
    void retargetsListedMembersToANewStringShape() {
        Map<ShapeId, List<ShapeId>> members = new LinkedHashMap<>();
        members.put(id("route53#MaxResults"), Arrays.asList(id("route53#ListCidrBlocksRequest$MaxResults")));
        members.put(id("route53#PageMaxItems"), Arrays.asList(id("route53#ListHostedZonesRequest$MaxItems")));

        Model model = retype(new RetypePageSizes("Route 53", members),
                             "namespace com.amazonaws.route53\n"
                             + "service AWSDnsV20130401 { version: \"2013-04-01\" }\n"
                             + "structure ListCidrBlocksRequest { MaxResults: Integer }\n"
                             + "structure ListHostedZonesRequest { MaxItems: Integer }\n"
                             + "structure GetHostedZoneLimitRequest { MaxItems: Integer }\n");

        assertThat(target(model, "route53#ListCidrBlocksRequest$MaxResults")).isEqualTo(id("route53#MaxResults"));
        assertThat(target(model, "route53#ListHostedZonesRequest$MaxItems")).isEqualTo(id("route53#PageMaxItems"));
        assertThat(target(model, "route53#GetHostedZoneLimitRequest$MaxItems"))
            .isEqualTo(ShapeId.from("smithy.api#Integer"));
        assertThat(model.expectShape(id("route53#MaxResults")).isStringShape()).isTrue();
        assertThat(model.expectShape(id("route53#PageMaxItems")).isStringShape()).isTrue();
    }

    @Test
    void retargetsListedMembersToAnExistingStringShape() {
        Model model = retype(toServiceString("CloudFront", "cloudfront", "ListDistributionsRequest$MaxItems"),
                             "namespace com.amazonaws.cloudfront\n"
                             + "service Cloudfront2020_05_31 { version: \"2020-05-31\" }\n"
                             + "integer integer\n"
                             + "string string\n"
                             + "structure ListDistributionsRequest { MaxItems: integer }\n");

        assertThat(target(model, "cloudfront#ListDistributionsRequest$MaxItems")).isEqualTo(id("cloudfront#string"));
    }

    @Test
    void listedMemberThatNoLongerTargetsAnInteger_fails() {
        assertThatThrownBy(() -> retype(toServiceString("Glacier", "glacier", "ListVaultsInput$limit"),
                                        "namespace com.amazonaws.glacier\n"
                                        + "service Glacier { version: \"2012-06-01\" }\n"
                                        + "string string\n"
                                        + "structure ListVaultsInput { limit: Long }\n"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ListVaultsInput$limit");
    }

    @Test
    void listedMemberMissingFromTheModel_fails() {
        assertThatThrownBy(() -> retype(toServiceString("Glacier", "glacier", "ListVaultsInput$limit"),
                                        "namespace com.amazonaws.glacier\n"
                                        + "service Glacier { version: \"2012-06-01\" }\n"
                                        + "string string\n"
                                        + "structure ListVaultsInput { maxResults: Integer }\n"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ListVaultsInput$limit is not in the Glacier model");
    }

    private static RetypePageSizes toServiceString(String sdkId, String service, String member) {
        Map<ShapeId, List<ShapeId>> members = new LinkedHashMap<>();
        members.put(id(service + "#string"), Arrays.asList(id(service + "#" + member)));
        return new RetypePageSizes(sdkId, members);
    }

    private static Model retype(RetypePageSizes customization, String smithy) {
        Model model = Model.assembler()
                           .addUnparsedModel("test.smithy", "$version: \"2.0\"\n" + smithy)
                           .assemble()
                           .unwrap();
        return customization.preprocess(model, CustomizationConfig.create());
    }

    private static ShapeId target(Model model, String member) {
        return model.expectShape(id(member), MemberShape.class).getTarget();
    }

    private static ShapeId id(String id) {
        return ShapeId.from("com.amazonaws." + id);
    }
}

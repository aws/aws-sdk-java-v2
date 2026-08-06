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

package software.amazon.awssdk.codegen.smithy;

import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.smithy.aws.traits.protocols.Ec2QueryNameTrait;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;

/**
 * Resolves a member's per-protocol wire names ({@code marshallLocationName} /
 * {@code unmarshallLocationName}). Smithy counterpart to C2J's {@code AddShapes} {@code derive*LocationName}
 * methods.
 */
final class SmithyWireNames {

    private SmithyWireNames() {
    }

    /**
     * Wire name used when writing a member. EC2 has its own rule (see {@link #deriveEc2MarshallName});
     * other protocols use the name-override trait, then the HTTP binding location name, then the
     * member name.
     */
    static String marshallLocationName(String protocol,
                                       MemberShape member,
                                       String memberName,
                                       String bindingLocationName,
                                       boolean containerElement) {
        if (Protocol.EC2.getValue().equalsIgnoreCase(protocol)) {
            return deriveEc2MarshallName(member, memberName, containerElement);
        }
        return traitOrBindingName(protocol, member, bindingLocationName, memberName);
    }

    /**
     * Wire name used when reading a member: name-override trait, then HTTP binding location name,
     * then the member name.
     */
    static String unmarshallLocationName(String protocol,
                                         MemberShape member,
                                         String memberName,
                                         String bindingLocationName) {
        return traitOrBindingName(protocol, member, bindingLocationName, memberName);
    }

    private static String traitOrBindingName(String protocol,
                                             MemberShape member,
                                             String bindingLocationName,
                                             String memberName) {
        String traitName = memberNameOverride(protocol, member);
        if (StringUtils.isNotBlank(traitName)) {
            return traitName;
        }
        if (StringUtils.isNotBlank(bindingLocationName)) {
            return bindingLocationName;
        }
        return memberName;
    }

    /**
     * Protocol-appropriate wire-name override, or {@code null} if none. XML protocols (rest-xml, ec2)
     * use {@code @xmlName}; JSON protocols use {@code @jsonName}. {@code query} is omitted for now: it
     * also needs C2J's {@code queryName} precedence, which has no Smithy trait wired yet.
     */
    private static String memberNameOverride(String protocol, MemberShape member) {
        if (Protocol.REST_XML.getValue().equalsIgnoreCase(protocol)
            || Protocol.EC2.getValue().equalsIgnoreCase(protocol)) {
            return member.getTrait(XmlNameTrait.class).map(XmlNameTrait::getValue).orElse(null);
        }
        if (Protocol.REST_JSON.getValue().equalsIgnoreCase(protocol)
            || Protocol.AWS_JSON.getValue().equalsIgnoreCase(protocol)) {
            return member.getTrait(JsonNameTrait.class).map(JsonNameTrait::getValue).orElse(null);
        }
        return null;
    }

    /**
     * EC2 marshall wire name: {@code @ec2QueryName} verbatim, else {@code @xmlName} or the member
     * name with its first character upper-cased (mirrors C2J's {@code deriveLocationNameForEc2}).
     * A synthetic list element / map key / value ({@code containerElement}) keeps its literal
     * lower-case name, matching C2J.
     */
    private static String deriveEc2MarshallName(MemberShape member, String memberName, boolean containerElement) {
        String ec2QueryName = member.getTrait(Ec2QueryNameTrait.class)
                                    .map(Ec2QueryNameTrait::getValue)
                                    .orElse(null);
        if (StringUtils.isNotBlank(ec2QueryName)) {
            return ec2QueryName;
        }
        String xmlName = member.getTrait(XmlNameTrait.class)
                               .map(XmlNameTrait::getValue)
                               .orElse(null);
        if (StringUtils.isNotBlank(xmlName)) {
            return upperCaseFirst(xmlName);
        }
        if (containerElement) {
            return memberName;
        }
        return upperCaseFirst(memberName);
    }

    private static String upperCaseFirst(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}

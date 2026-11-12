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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.smithy.customization.SmithyServiceCustomization;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Keeps the listed page-size members strings. Smithy requires a page size to be an integer, so this runs after
 * validation.
 */
@SdkInternalApi
public final class RetypePageSizes implements SmithyServiceCustomization {
    private static final Set<String> SDK_IDS =
        Collections.unmodifiableSet(new HashSet<>(Arrays.asList("CloudFront", "Route 53", "Glacier")));
    private static final Map<ShapeId, List<ShapeId>> MEMBERS_BY_TARGET = membersByTarget();

    @Override
    public Set<String> sdkIds() {
        return SDK_IDS;
    }

    @Override
    public Model preprocess(Model model, CustomizationConfig config) {
        String namespace = model.getServiceShapes().iterator().next().getId().getNamespace();
        List<Shape> replaced = new ArrayList<>();
        MEMBERS_BY_TARGET.forEach((target, members) -> {
            if (!target.getNamespace().equals(namespace)) {
                return;
            }
            List<Shape> retargeted = retarget(model, members, target);
            if (retargeted.isEmpty()) {
                return;
            }
            if (!model.getShape(target).isPresent()) {
                replaced.add(StringShape.builder().id(target).build());
            } else if (!model.expectShape(target).isStringShape()) {
                throw new IllegalStateException(target + " is not a string");
            }
            replaced.addAll(retargeted);
        });
        return ModelTransformer.create().replaceShapes(model, replaced);
    }

    private static List<Shape> retarget(Model model, List<ShapeId> members, ShapeId target) {
        List<Shape> retargeted = new ArrayList<>();
        for (ShapeId memberId : members) {
            model.getShape(memberId).flatMap(Shape::asMemberShape).ifPresent(member -> {
                if (!model.expectShape(member.getTarget()).isIntegerShape()) {
                    throw new IllegalStateException(memberId + " no longer targets an integer");
                }
                retargeted.add(member.toBuilder().target(target).build());
            });
        }
        return retargeted;
    }

    private static Map<ShapeId, List<ShapeId>> membersByTarget() {
        Map<ShapeId, List<ShapeId>> members = new LinkedHashMap<>();
        put(members, "com.amazonaws.cloudfront", "string",
            "ListCachePoliciesRequest$MaxItems",
            "ListCloudFrontOriginAccessIdentitiesRequest$MaxItems",
            "ListContinuousDeploymentPoliciesRequest$MaxItems",
            "ListDistributionsByAnycastIpListIdRequest$MaxItems",
            "ListDistributionsByCachePolicyIdRequest$MaxItems",
            "ListDistributionsByKeyGroupRequest$MaxItems",
            "ListDistributionsByOriginRequestPolicyIdRequest$MaxItems",
            "ListDistributionsByOwnedResourceRequest$MaxItems",
            "ListDistributionsByRealtimeLogConfigRequest$MaxItems",
            "ListDistributionsByResponseHeadersPolicyIdRequest$MaxItems",
            "ListDistributionsByTrustStoreRequest$MaxItems",
            "ListDistributionsByVpcOriginIdRequest$MaxItems",
            "ListDistributionsByWebACLIdRequest$MaxItems",
            "ListDistributionsRequest$MaxItems",
            "ListFieldLevelEncryptionConfigsRequest$MaxItems",
            "ListFieldLevelEncryptionProfilesRequest$MaxItems",
            "ListFunctionsRequest$MaxItems",
            "ListInvalidationsRequest$MaxItems",
            "ListKeyGroupsRequest$MaxItems",
            "ListKeyValueStoresRequest$MaxItems",
            "ListOriginAccessControlsRequest$MaxItems",
            "ListOriginRequestPoliciesRequest$MaxItems",
            "ListPublicKeysRequest$MaxItems",
            "ListRealtimeLogConfigsRequest$MaxItems",
            "ListResponseHeadersPoliciesRequest$MaxItems",
            "ListStreamingDistributionsRequest$MaxItems",
            "ListVpcOriginsRequest$MaxItems");
        put(members, "com.amazonaws.route53", "MaxResults",
            "ListCidrBlocksRequest$MaxResults",
            "ListCidrCollectionsRequest$MaxResults",
            "ListCidrLocationsRequest$MaxResults",
            "ListQueryLoggingConfigsRequest$MaxResults",
            "ListVPCAssociationAuthorizationsRequest$MaxResults");
        put(members, "com.amazonaws.route53", "PageMaxItems",
            "ListGeoLocationsRequest$MaxItems",
            "ListGeoLocationsResponse$MaxItems",
            "ListHealthChecksRequest$MaxItems",
            "ListHealthChecksResponse$MaxItems",
            "ListHostedZonesByNameRequest$MaxItems",
            "ListHostedZonesByNameResponse$MaxItems",
            "ListHostedZonesByVPCRequest$MaxItems",
            "ListHostedZonesByVPCResponse$MaxItems",
            "ListHostedZonesRequest$MaxItems",
            "ListHostedZonesResponse$MaxItems",
            "ListResourceRecordSetsRequest$MaxItems",
            "ListResourceRecordSetsResponse$MaxItems",
            "ListReusableDelegationSetsRequest$MaxItems",
            "ListReusableDelegationSetsResponse$MaxItems",
            "ListTrafficPoliciesRequest$MaxItems",
            "ListTrafficPoliciesResponse$MaxItems",
            "ListTrafficPolicyInstancesByHostedZoneRequest$MaxItems",
            "ListTrafficPolicyInstancesByHostedZoneResponse$MaxItems",
            "ListTrafficPolicyInstancesByPolicyRequest$MaxItems",
            "ListTrafficPolicyInstancesByPolicyResponse$MaxItems",
            "ListTrafficPolicyInstancesRequest$MaxItems",
            "ListTrafficPolicyInstancesResponse$MaxItems",
            "ListTrafficPolicyVersionsRequest$MaxItems",
            "ListTrafficPolicyVersionsResponse$MaxItems");
        put(members, "com.amazonaws.glacier", "string",
            "ListJobsInput$limit",
            "ListMultipartUploadsInput$limit",
            "ListPartsInput$limit",
            "ListVaultsInput$limit");
        return Collections.unmodifiableMap(members);
    }

    private static void put(Map<ShapeId, List<ShapeId>> members, String namespace, String target, String... ids) {
        members.put(ShapeId.fromParts(namespace, target),
                    Arrays.stream(ids).map(id -> ShapeId.from(namespace + "#" + id)).collect(Collectors.toList()));
    }
}

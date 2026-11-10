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

package software.amazon.awssdk.codegen.smithy.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Page-size members that are strings in C2J and integers in Smithy keep {@code String}. Smithy requires an
 * {@code @paginated} page size to be an integer, so the retarget runs after validation. The members are listed
 * explicitly: a rule by name or by {@code @paginated} selects the wrong set.
 */
@SdkInternalApi
final class PageSizeCustomization implements SmithyServiceCustomization {
    private static final Map<ShapeId, ShapeId> TARGETS = targets();

    @Override
    public Set<String> sdkIds() {
        return new HashSet<>(Arrays.asList("CloudFront", "Route 53", "Glacier"));
    }

    @Override
    public Model preprocess(Model model, CustomizationConfig config) {
        List<Shape> replaced = new ArrayList<>();
        Set<ShapeId> added = new HashSet<>();
        for (Map.Entry<ShapeId, ShapeId> entry : TARGETS.entrySet()) {
            if (!model.getShape(entry.getKey()).isPresent()) {
                continue;
            }
            MemberShape member = model.expectShape(entry.getKey(), MemberShape.class);
            if (!model.expectShape(member.getTarget()).isIntegerShape()) {
                throw new IllegalStateException(member.getId() + " no longer targets an integer");
            }
            ShapeId target = entry.getValue();
            if (!model.getShape(target).isPresent() && added.add(target)) {
                replaced.add(StringShape.builder().id(target).build());
            }
            replaced.add(member.toBuilder().target(target).build());
        }
        return ModelTransformer.create().replaceShapes(model, replaced);
    }

    private static Map<ShapeId, ShapeId> targets() {
        Map<ShapeId, ShapeId> targets = new LinkedHashMap<>();
        add(targets, "com.amazonaws.cloudfront#ListCachePoliciesRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListCloudFrontOriginAccessIdentitiesRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListContinuousDeploymentPoliciesRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListDistributionsByAnycastIpListIdRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListDistributionsByCachePolicyIdRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListDistributionsByKeyGroupRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListDistributionsByOriginRequestPolicyIdRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListDistributionsByOwnedResourceRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListDistributionsByRealtimeLogConfigRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListDistributionsByResponseHeadersPolicyIdRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListDistributionsByTrustStoreRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListDistributionsByVpcOriginIdRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListDistributionsByWebACLIdRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListDistributionsRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListFieldLevelEncryptionConfigsRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListFieldLevelEncryptionProfilesRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListFunctionsRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListInvalidationsRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListKeyGroupsRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListKeyValueStoresRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListOriginAccessControlsRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListOriginRequestPoliciesRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListPublicKeysRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListRealtimeLogConfigsRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListResponseHeadersPoliciesRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListStreamingDistributionsRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.cloudfront#ListVpcOriginsRequest$MaxItems",
            "com.amazonaws.cloudfront#string");
        add(targets, "com.amazonaws.route53#ListCidrBlocksRequest$MaxResults",
            "com.amazonaws.route53#MaxResults");
        add(targets, "com.amazonaws.route53#ListCidrCollectionsRequest$MaxResults",
            "com.amazonaws.route53#MaxResults");
        add(targets, "com.amazonaws.route53#ListCidrLocationsRequest$MaxResults",
            "com.amazonaws.route53#MaxResults");
        add(targets, "com.amazonaws.route53#ListGeoLocationsRequest$MaxItems",
            "com.amazonaws.route53#PageMaxItems");
        add(targets, "com.amazonaws.route53#ListGeoLocationsResponse$MaxItems",
            "com.amazonaws.route53#PageMaxItems");
        add(targets, "com.amazonaws.route53#ListHealthChecksRequest$MaxItems",
            "com.amazonaws.route53#PageMaxItems");
        add(targets, "com.amazonaws.route53#ListHealthChecksResponse$MaxItems",
            "com.amazonaws.route53#PageMaxItems");
        add(targets, "com.amazonaws.route53#ListHostedZonesByNameRequest$MaxItems",
            "com.amazonaws.route53#PageMaxItems");
        add(targets, "com.amazonaws.route53#ListHostedZonesByNameResponse$MaxItems",
            "com.amazonaws.route53#PageMaxItems");
        add(targets, "com.amazonaws.route53#ListHostedZonesByVPCRequest$MaxItems",
            "com.amazonaws.route53#PageMaxItems");
        add(targets, "com.amazonaws.route53#ListHostedZonesByVPCResponse$MaxItems",
            "com.amazonaws.route53#PageMaxItems");
        add(targets, "com.amazonaws.route53#ListHostedZonesRequest$MaxItems",
            "com.amazonaws.route53#PageMaxItems");
        add(targets, "com.amazonaws.route53#ListHostedZonesResponse$MaxItems",
            "com.amazonaws.route53#PageMaxItems");
        add(targets, "com.amazonaws.route53#ListQueryLoggingConfigsRequest$MaxResults",
            "com.amazonaws.route53#MaxResults");
        add(targets, "com.amazonaws.route53#ListResourceRecordSetsRequest$MaxItems",
            "com.amazonaws.route53#PageMaxItems");
        add(targets, "com.amazonaws.route53#ListResourceRecordSetsResponse$MaxItems",
            "com.amazonaws.route53#PageMaxItems");
        add(targets, "com.amazonaws.route53#ListReusableDelegationSetsRequest$MaxItems",
            "com.amazonaws.route53#PageMaxItems");
        add(targets, "com.amazonaws.route53#ListReusableDelegationSetsResponse$MaxItems",
            "com.amazonaws.route53#PageMaxItems");
        add(targets, "com.amazonaws.route53#ListTrafficPoliciesRequest$MaxItems",
            "com.amazonaws.route53#PageMaxItems");
        add(targets, "com.amazonaws.route53#ListTrafficPoliciesResponse$MaxItems",
            "com.amazonaws.route53#PageMaxItems");
        add(targets, "com.amazonaws.route53#ListTrafficPolicyInstancesByHostedZoneRequest$MaxItems",
            "com.amazonaws.route53#PageMaxItems");
        add(targets, "com.amazonaws.route53#ListTrafficPolicyInstancesByHostedZoneResponse$MaxItems",
            "com.amazonaws.route53#PageMaxItems");
        add(targets, "com.amazonaws.route53#ListTrafficPolicyInstancesByPolicyRequest$MaxItems",
            "com.amazonaws.route53#PageMaxItems");
        add(targets, "com.amazonaws.route53#ListTrafficPolicyInstancesByPolicyResponse$MaxItems",
            "com.amazonaws.route53#PageMaxItems");
        add(targets, "com.amazonaws.route53#ListTrafficPolicyInstancesRequest$MaxItems",
            "com.amazonaws.route53#PageMaxItems");
        add(targets, "com.amazonaws.route53#ListTrafficPolicyInstancesResponse$MaxItems",
            "com.amazonaws.route53#PageMaxItems");
        add(targets, "com.amazonaws.route53#ListTrafficPolicyVersionsRequest$MaxItems",
            "com.amazonaws.route53#PageMaxItems");
        add(targets, "com.amazonaws.route53#ListTrafficPolicyVersionsResponse$MaxItems",
            "com.amazonaws.route53#PageMaxItems");
        add(targets, "com.amazonaws.route53#ListVPCAssociationAuthorizationsRequest$MaxResults",
            "com.amazonaws.route53#MaxResults");
        add(targets, "com.amazonaws.glacier#ListJobsInput$limit",
            "com.amazonaws.glacier#string");
        add(targets, "com.amazonaws.glacier#ListMultipartUploadsInput$limit",
            "com.amazonaws.glacier#string");
        add(targets, "com.amazonaws.glacier#ListPartsInput$limit",
            "com.amazonaws.glacier#string");
        add(targets, "com.amazonaws.glacier#ListVaultsInput$limit",
            "com.amazonaws.glacier#string");
        return targets;
    }

    private static void add(Map<ShapeId, ShapeId> targets, String member, String target) {
        targets.put(ShapeId.from(member), ShapeId.from(target));
    }
}

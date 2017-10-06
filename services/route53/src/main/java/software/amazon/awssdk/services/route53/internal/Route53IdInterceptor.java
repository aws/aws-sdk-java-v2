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

package software.amazon.awssdk.services.route53.internal;

import java.util.stream.Collectors;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.services.route53.model.AliasTarget;
import software.amazon.awssdk.services.route53.model.ChangeInfo;
import software.amazon.awssdk.services.route53.model.ChangeResourceRecordSetsResponse;
import software.amazon.awssdk.services.route53.model.CreateHealthCheckResponse;
import software.amazon.awssdk.services.route53.model.CreateHostedZoneResponse;
import software.amazon.awssdk.services.route53.model.CreateReusableDelegationSetResponse;
import software.amazon.awssdk.services.route53.model.DelegationSet;
import software.amazon.awssdk.services.route53.model.DeleteHostedZoneResponse;
import software.amazon.awssdk.services.route53.model.GetChangeResponse;
import software.amazon.awssdk.services.route53.model.GetHealthCheckResponse;
import software.amazon.awssdk.services.route53.model.GetHostedZoneResponse;
import software.amazon.awssdk.services.route53.model.GetReusableDelegationSetResponse;
import software.amazon.awssdk.services.route53.model.HealthCheck;
import software.amazon.awssdk.services.route53.model.HostedZone;
import software.amazon.awssdk.services.route53.model.ListHealthChecksResponse;
import software.amazon.awssdk.services.route53.model.ListHostedZonesResponse;
import software.amazon.awssdk.services.route53.model.ListResourceRecordSetsResponse;
import software.amazon.awssdk.services.route53.model.ListReusableDelegationSetsResponse;
import software.amazon.awssdk.services.route53.model.ResourceRecordSet;

/**
 * Route 53 returns a portion of the URL resource path as the ID for a few
 * elements, but when the service accepts those IDs, the resource path portion
 * cannot be included, otherwise requests fail. This handler removes those
 * partial resource path elements from IDs returned by Route 53.
 */
public class Route53IdInterceptor implements ExecutionInterceptor {
    @Override
    public SdkResponse modifyResponse(Context.ModifyResponse context, ExecutionAttributes executionAttributes) {
        SdkResponse response = context.response();
        if (response instanceof ChangeResourceRecordSetsResponse) {
            ChangeResourceRecordSetsResponse result = (ChangeResourceRecordSetsResponse) response;
            return result.toBuilder()
                         .changeInfo(removePrefix(result.changeInfo()))
                         .build();

        } else if (response instanceof CreateHostedZoneResponse) {
            CreateHostedZoneResponse result = (CreateHostedZoneResponse) response;
            return result.toBuilder()
                         .changeInfo(removePrefix(result.changeInfo()))
                         .hostedZone(removePrefix(result.hostedZone()))
                         .delegationSet(removePrefix(result.delegationSet()))
                         .build();

        } else if (response instanceof DeleteHostedZoneResponse) {
            DeleteHostedZoneResponse result = (DeleteHostedZoneResponse) response;
            return result.toBuilder()
                         .changeInfo(removePrefix(result.changeInfo()))
                         .build();

        } else if (response instanceof GetChangeResponse) {
            GetChangeResponse result = (GetChangeResponse) response;
            return result.toBuilder()
                         .changeInfo(removePrefix(result.changeInfo()))
                         .build();

        } else if (response instanceof GetHostedZoneResponse) {
            GetHostedZoneResponse result = (GetHostedZoneResponse) response;
            return result.toBuilder()
                         .hostedZone(removePrefix(result.hostedZone()))
                         .delegationSet(removePrefix(result.delegationSet()))
                         .build();

        } else if (response instanceof ListHostedZonesResponse) {
            ListHostedZonesResponse result = (ListHostedZonesResponse) response;
            return result.toBuilder()
                         .hostedZones(result.hostedZones().stream()
                                            .map(this::removePrefix)
                                            .collect(Collectors.toList()))
                         .build();

        } else if (response instanceof ListResourceRecordSetsResponse) {
            ListResourceRecordSetsResponse result = (ListResourceRecordSetsResponse) response;
            return result.toBuilder()
                         .resourceRecordSets(result.resourceRecordSets().stream()
                                                   .map(this::removePrefix)
                                                   .collect(Collectors.toList()))
                         .build();

        } else if (response instanceof CreateHealthCheckResponse) {
            CreateHealthCheckResponse result = (CreateHealthCheckResponse) response;
            return result.toBuilder()
                         .healthCheck(removePrefix(result.healthCheck()))
                         .build();

        } else if (response instanceof GetHealthCheckResponse) {
            GetHealthCheckResponse result = (GetHealthCheckResponse) response;
            return result.toBuilder()
                         .healthCheck(removePrefix(result.healthCheck()))
                         .build();

        } else if (response instanceof ListHealthChecksResponse) {
            ListHealthChecksResponse result = (ListHealthChecksResponse) response;
            return result.toBuilder()
                         .healthChecks(result.healthChecks().stream()
                                             .map(this::removePrefix)
                                             .collect(Collectors.toList()))
                         .build();

        } else if (response instanceof CreateReusableDelegationSetResponse) {
            CreateReusableDelegationSetResponse result = (CreateReusableDelegationSetResponse) response;
            return result.toBuilder()
                         .delegationSet(removePrefix(result.delegationSet()))
                         .build();

        } else if (response instanceof GetReusableDelegationSetResponse) {
            GetReusableDelegationSetResponse result = (GetReusableDelegationSetResponse) response;
            return result.toBuilder()
                         .delegationSet(removePrefix(result.delegationSet()))
                         .build();

        } else if (response instanceof ListReusableDelegationSetsResponse) {
            ListReusableDelegationSetsResponse result = (ListReusableDelegationSetsResponse) response;
            return result.toBuilder()
                         .delegationSets(result.delegationSets().stream()
                                               .map(this::removePrefix)
                                               .collect(Collectors.toList()))
                         .build();
        }

        return response;
    }

    private ResourceRecordSet removePrefix(ResourceRecordSet rrset) {
        if (rrset == null) {
            return null;
        }

        return rrset.toBuilder()
                    .aliasTarget(removePrefix(rrset.aliasTarget()))
                    .healthCheckId(removePrefix(rrset.healthCheckId()))
                    .setIdentifier(removePrefix(rrset.setIdentifier()))
                    .build();
    }

    private AliasTarget removePrefix(AliasTarget aliasTarget) {
        if (aliasTarget == null) {
            return null;
        }

        return aliasTarget.toBuilder()
                          .hostedZoneId(removePrefix(aliasTarget.hostedZoneId()))
                          .build();
    }

    private ChangeInfo removePrefix(ChangeInfo changeInfo) {
        if (changeInfo == null) {
            return null;
        }

        return changeInfo.toBuilder()
                         .id(removePrefix(changeInfo.id()))
                         .build();
    }

    private HostedZone removePrefix(HostedZone hostedZone) {
        if (hostedZone == null) {
            return null;
        }

        return hostedZone.toBuilder()
                         .id(removePrefix(hostedZone.id()))
                         .build();
    }

    private HealthCheck removePrefix(HealthCheck healthCheck) {
        if (healthCheck == null) {
            return null;
        }

        return healthCheck.toBuilder()
                          .id(removePrefix(healthCheck.id()))
                          .build();
    }

    private DelegationSet removePrefix(DelegationSet delegationSet) {
        if (delegationSet == null) {
            return null;
        }

        return delegationSet.toBuilder()
                            .id(removePrefix(delegationSet.id()))
                            .build();
    }

    private String removePrefix(String s) {
        if (s == null) {
            return null;
        }

        int lastIndex = s.lastIndexOf("/");
        if (lastIndex > 0) {
            return s.substring(lastIndex + 1);
        }

        return s;
    }
}

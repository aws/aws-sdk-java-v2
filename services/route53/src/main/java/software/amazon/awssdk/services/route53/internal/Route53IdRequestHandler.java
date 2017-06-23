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

import software.amazon.awssdk.Response;
import software.amazon.awssdk.handlers.RequestHandler;
import software.amazon.awssdk.http.SdkHttpFullRequest;
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
import software.amazon.awssdk.util.ImmutableObjectUtils;

/**
 * Route 53 returns a portion of the URL resource path as the ID for a few
 * elements, but when the service accepts those IDs, the resource path portion
 * cannot be included, otherwise requests fail. This handler removes those
 * partial resource path elements from IDs returned by Route 53.
 */
public class Route53IdRequestHandler extends RequestHandler {


    @Override
    public void afterResponse(SdkHttpFullRequest request, Response<?> response) {
        Object obj = response.getAwsResponse();
        if (obj instanceof ChangeResourceRecordSetsResponse) {
            ChangeResourceRecordSetsResponse result = (ChangeResourceRecordSetsResponse) obj;
            removePrefix(result.changeInfo());
        } else if (obj instanceof CreateHostedZoneResponse) {
            CreateHostedZoneResponse result = (CreateHostedZoneResponse) obj;
            removePrefix(result.changeInfo());
            removePrefix(result.hostedZone());
            removePrefix(result.delegationSet());
        } else if (obj instanceof DeleteHostedZoneResponse) {
            DeleteHostedZoneResponse result = (DeleteHostedZoneResponse) obj;
            removePrefix(result.changeInfo());
        } else if (obj instanceof GetChangeResponse) {
            GetChangeResponse result = (GetChangeResponse) obj;
            removePrefix(result.changeInfo());
        } else if (obj instanceof GetHostedZoneResponse) {
            GetHostedZoneResponse result = (GetHostedZoneResponse) obj;
            removePrefix(result.hostedZone());
        } else if (obj instanceof ListHostedZonesResponse) {
            ListHostedZonesResponse result = (ListHostedZonesResponse) obj;
            for (HostedZone zone : result.hostedZones()) {
                removePrefix(zone);
            }
        } else if (obj instanceof ListResourceRecordSetsResponse) {
            ListResourceRecordSetsResponse result = (ListResourceRecordSetsResponse) obj;
            for (ResourceRecordSet rrset : result.resourceRecordSets()) {
                removePrefix(rrset);
            }
        } else if (obj instanceof CreateHealthCheckResponse) {
            CreateHealthCheckResponse result = (CreateHealthCheckResponse) obj;
            removePrefix(result.healthCheck());
        } else if (obj instanceof GetHealthCheckResponse) {
            GetHealthCheckResponse result = (GetHealthCheckResponse) obj;
            removePrefix(result.healthCheck());
        } else if (obj instanceof ListHealthChecksResponse) {
            ListHealthChecksResponse result = (ListHealthChecksResponse) obj;
            for (HealthCheck check : result.healthChecks()) {
                removePrefix(check);
            }
        } else if (obj instanceof CreateReusableDelegationSetResponse) {
            CreateReusableDelegationSetResponse result = (CreateReusableDelegationSetResponse) obj;
            removePrefix(result.delegationSet());
        } else if (obj instanceof GetHostedZoneResponse) {
            GetHostedZoneResponse result = (GetHostedZoneResponse) obj;
            removePrefix(result.delegationSet());
        } else if (obj instanceof GetReusableDelegationSetResponse) {
            GetReusableDelegationSetResponse result = (GetReusableDelegationSetResponse) obj;
            removePrefix(result.delegationSet());
        } else if (obj instanceof ListReusableDelegationSetsResponse) {
            ListReusableDelegationSetsResponse result = (ListReusableDelegationSetsResponse) obj;
            for (DelegationSet delegationSet : result.delegationSets()) {
                removePrefix(delegationSet);
            }
        }
    }

    private void removePrefix(ResourceRecordSet rrset) {
        if (rrset == null) {
            return;
        }

        removePrefix(rrset.aliasTarget());
        ImmutableObjectUtils.setObjectMember(rrset, "healthCheckId", removePrefix(rrset.healthCheckId()));
        ImmutableObjectUtils.setObjectMember(rrset, "setIdentifier", removePrefix(rrset.setIdentifier()));
    }

    private void removePrefix(AliasTarget aliasTarget) {
        if (aliasTarget == null) {
            return;
        }

        ImmutableObjectUtils.setObjectMember(aliasTarget, "hostedZoneId", removePrefix(aliasTarget.hostedZoneId()));
        //aliasTarget.setHostedZoneId(removePrefix(aliasTarget.hostedZoneId()));
    }

    private void removePrefix(ChangeInfo changeInfo) {
        if (changeInfo == null) {
            return;
        }

        if (changeInfo.id() != null) {
            ImmutableObjectUtils.setObjectMember(changeInfo, "id", removePrefix(changeInfo.id()));
            //changeInfo.setId(removePrefix(changeInfo.getId()));
        }
    }

    private void removePrefix(HostedZone hostedZone) {
        if (hostedZone == null) {
            return;
        }

        if (hostedZone.id() != null) {
            ImmutableObjectUtils.setObjectMember(hostedZone, "id", removePrefix(hostedZone.id()));
            //hostedZone.setId(removePrefix(hostedZone.getId()));
        }
    }

    private void removePrefix(HealthCheck healthCheck) {
        if (healthCheck == null) {
            return;
        }

        if (healthCheck.id() != null) {
            ImmutableObjectUtils.setObjectMember(healthCheck, "id", removePrefix(healthCheck.id()));
            //healthCheck.setId(removePrefix(healthCheck.getId()));
        }
    }

    private void removePrefix(DelegationSet delegationSet) {
        if (delegationSet == null) {
            return;
        }

        if (delegationSet.id() != null) {
            ImmutableObjectUtils.setObjectMember(delegationSet, "id", removePrefix(delegationSet.id()));
            //delegationSet.setId(removePrefix(delegationSet.getId()));
        }
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

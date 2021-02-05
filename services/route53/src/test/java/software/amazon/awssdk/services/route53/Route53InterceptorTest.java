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

package software.amazon.awssdk.services.route53;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.services.route53.internal.Route53IdInterceptor;
import software.amazon.awssdk.services.route53.model.ActivateKeySigningKeyResponse;
import software.amazon.awssdk.services.route53.model.ChangeInfo;
import software.amazon.awssdk.services.route53.model.CreateKeySigningKeyResponse;
import software.amazon.awssdk.services.route53.model.CreateHostedZoneRequest;
import software.amazon.awssdk.services.route53.model.CreateHostedZoneResponse;
import software.amazon.awssdk.services.route53.model.CreateReusableDelegationSetResponse;
import software.amazon.awssdk.services.route53.model.DeactivateKeySigningKeyResponse;
import software.amazon.awssdk.services.route53.model.DelegationSet;
import software.amazon.awssdk.services.route53.model.DeleteKeySigningKeyResponse;
import software.amazon.awssdk.services.route53.model.DisableHostedZoneDnssecResponse;
import software.amazon.awssdk.services.route53.model.EnableHostedZoneDnssecResponse;
import software.amazon.awssdk.services.route53.model.GetHostedZoneResponse;
import software.amazon.awssdk.services.route53.model.GetReusableDelegationSetResponse;
import software.amazon.awssdk.services.route53.model.ListReusableDelegationSetsResponse;

/**
 * Unit test for request handler customization of delegation set id's
 */
//TODO: fix test, see comment on line 80")
public class Route53InterceptorTest {

    private static final String delegationPrefix = "delegationset";
    private static final String changeInfoPrefix = "change";

    private static final String id = "delegationSetId";
    private static final String changeInfoId = "changeInfoId";

    private static final String delegationSetId = "/" + delegationPrefix + "/" + id;
    private static final String changeInfoIdWithPrefix = "/" + changeInfoPrefix + "/" + changeInfoId;

    /**
     * Tests if the request handler strips the delegation set prefixes. Asserts
     * that the result object has prefix removed.
     */
    @Test
    public void testDelegationSetPrefixRemoval() {

        Route53IdInterceptor interceptor = new Route53IdInterceptor();

        DelegationSet delegationSet = DelegationSet.builder().id(delegationSetId).build();

        CreateHostedZoneResponse createResult = CreateHostedZoneResponse.builder()
                .delegationSet(delegationSet)
                .build();

        createResult = (CreateHostedZoneResponse) modifyResponse(interceptor, createResult);

        assertEquals(createResult.delegationSet().id(), id);

        CreateReusableDelegationSetResponse createResuableResult = CreateReusableDelegationSetResponse.builder()
                .delegationSet(delegationSet)
                .build();

        createResuableResult = (CreateReusableDelegationSetResponse) modifyResponse(interceptor, createResuableResult);

        assertEquals(createResuableResult.delegationSet().id(), id);

        GetHostedZoneResponse getZoneResult = GetHostedZoneResponse.builder()
                .delegationSet(delegationSet)
                .build();

        getZoneResult = (GetHostedZoneResponse) modifyResponse(interceptor, getZoneResult);

        // This assert works, but only because of the other operations the are sequenced before this, that modify the id.
        assertEquals(getZoneResult.delegationSet().id(), id);

        GetReusableDelegationSetResponse getResuableResult = GetReusableDelegationSetResponse.builder()
                .delegationSet(delegationSet)
                .build();

        getResuableResult = (GetReusableDelegationSetResponse) modifyResponse(interceptor, getResuableResult);

        assertEquals(getResuableResult.delegationSet().id(), id);

        ListReusableDelegationSetsResponse listResult = ListReusableDelegationSetsResponse.builder()
                .delegationSets(delegationSet)
                .build();

        listResult = (ListReusableDelegationSetsResponse) modifyResponse(interceptor, listResult);

        assertEquals(listResult.delegationSets().get(0).id(), id);

        delegationSet = delegationSet.toBuilder().id(id).build();

        createResult = CreateHostedZoneResponse.builder()
                .delegationSet(delegationSet)
                .build();

        createResult = (CreateHostedZoneResponse) modifyResponse(interceptor, createResult);

        assertEquals(createResult.delegationSet().id(), id);
    }

    private SdkResponse modifyResponse(ExecutionInterceptor interceptor, SdkResponse responseObject) {
        return interceptor.modifyResponse(InterceptorContext.builder()
                                                            .request(CreateHostedZoneRequest.builder().build())
                                                            .response(responseObject)
                                                            .build(),
                                          new ExecutionAttributes());
    }

    @Test
    public void testChangeInfoPrefixRemoval() {

        Route53IdInterceptor interceptor = new Route53IdInterceptor();

        ChangeInfo changeInfo = ChangeInfo.builder().id(changeInfoIdWithPrefix).build();

        CreateKeySigningKeyResponse createKeySigningKeyResponse = CreateKeySigningKeyResponse.builder()
                .changeInfo(changeInfo).build();
        createKeySigningKeyResponse = (CreateKeySigningKeyResponse) modifyResponse(interceptor, createKeySigningKeyResponse);
        assertEquals(createKeySigningKeyResponse.changeInfo().id(), changeInfoId);

        DeleteKeySigningKeyResponse deleteKeySigningKeyResponse = DeleteKeySigningKeyResponse.builder()
                .changeInfo(changeInfo).build();
        deleteKeySigningKeyResponse = (DeleteKeySigningKeyResponse) modifyResponse(interceptor, deleteKeySigningKeyResponse);
        assertEquals(deleteKeySigningKeyResponse.changeInfo().id(), changeInfoId);

        ActivateKeySigningKeyResponse activateKeySigningKeyResponse = ActivateKeySigningKeyResponse.builder()
                .changeInfo(changeInfo).build();
        activateKeySigningKeyResponse = (ActivateKeySigningKeyResponse) modifyResponse(interceptor, activateKeySigningKeyResponse);
        assertEquals(activateKeySigningKeyResponse.changeInfo().id(), changeInfoId);

        DeactivateKeySigningKeyResponse deactivateKeySigningKeyResponse = DeactivateKeySigningKeyResponse.builder()
                .changeInfo(changeInfo).build();
        deactivateKeySigningKeyResponse = (DeactivateKeySigningKeyResponse) modifyResponse(interceptor, deactivateKeySigningKeyResponse);
        assertEquals(deactivateKeySigningKeyResponse.changeInfo().id(), changeInfoId);

        EnableHostedZoneDnssecResponse enableHostedZoneDnssecResponse = EnableHostedZoneDnssecResponse.builder()
                .changeInfo(changeInfo).build();
        enableHostedZoneDnssecResponse = (EnableHostedZoneDnssecResponse) modifyResponse(interceptor, enableHostedZoneDnssecResponse);
        assertEquals(enableHostedZoneDnssecResponse.changeInfo().id(), changeInfoId);

        DisableHostedZoneDnssecResponse disableHostedZoneDnssecResponse = DisableHostedZoneDnssecResponse.builder()
                .changeInfo(changeInfo).build();
        disableHostedZoneDnssecResponse = (DisableHostedZoneDnssecResponse) modifyResponse(interceptor, disableHostedZoneDnssecResponse);
        assertEquals(disableHostedZoneDnssecResponse.changeInfo().id(), changeInfoId);
    }
}

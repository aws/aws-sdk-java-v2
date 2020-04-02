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

package software.amazon.awssdk.services.ses;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.DeleteIdentityRequest;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.GetIdentityVerificationAttributesRequest;
import software.amazon.awssdk.services.ses.model.GetIdentityVerificationAttributesResponse;
import software.amazon.awssdk.services.ses.model.GetSendQuotaRequest;
import software.amazon.awssdk.services.ses.model.GetSendQuotaResponse;
import software.amazon.awssdk.services.ses.model.IdentityType;
import software.amazon.awssdk.services.ses.model.IdentityVerificationAttributes;
import software.amazon.awssdk.services.ses.model.ListIdentitiesRequest;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.MessageRejectedException;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.VerificationStatus;
import software.amazon.awssdk.services.ses.model.VerifyDomainIdentityRequest;
import software.amazon.awssdk.services.ses.model.VerifyEmailIdentityRequest;

public class EmailIntegrationTest extends IntegrationTestBase {

    private static final String DOMAIN = "invalid-test-domain";
    private static final String EMAIL = "no-reply@amazon.com";
    private static String DOMAIN_VERIFICATION_TOKEN;

    @BeforeClass
    public static void setup() {
        email.verifyEmailIdentity(VerifyEmailIdentityRequest.builder().emailAddress(EMAIL).build());
        DOMAIN_VERIFICATION_TOKEN = email.verifyDomainIdentity(VerifyDomainIdentityRequest.builder().domain(DOMAIN).build())
                                         .verificationToken();

    }

    @AfterClass
    public static void tearDown() {
        email.deleteIdentity(DeleteIdentityRequest.builder().identity(EMAIL).build());
        email.deleteIdentity(DeleteIdentityRequest.builder().identity(DOMAIN).build());
    }

    @Test
    public void getSendQuota_ReturnsNonZeroQuotas() {
        GetSendQuotaResponse result = email.getSendQuota(GetSendQuotaRequest.builder().build());
        assertThat(result.max24HourSend(), greaterThan(0.0));
        assertThat(result.maxSendRate(), greaterThan(0.0));
    }

    @Test
    public void listIdentities_WithNonVerifiedIdentity_ReturnsIdentityInList() {
        // Don't need to actually verify for it to show up in listIdentities
        List<String> identities = email.listIdentities(ListIdentitiesRequest.builder().build()).identities();
        assertThat(identities, hasItem(EMAIL));
        assertThat(identities, hasItem(DOMAIN));
    }

    @Test
    public void listIdentities_FilteredForDomainIdentities_OnlyHasDomainIdentityInList() {
        List<String> identities = email.listIdentities(
                ListIdentitiesRequest.builder().identityType(IdentityType.DOMAIN).build()).identities();
        assertThat(identities, not(hasItem(EMAIL)));
        assertThat(identities, hasItem(DOMAIN));
    }

    @Test
    public void listIdentities_FilteredForEmailIdentities_OnlyHasEmailIdentityInList() {
        List<String> identities = email.listIdentities(
                ListIdentitiesRequest.builder().identityType(IdentityType.EMAIL_ADDRESS).build()).identities();
        assertThat(identities, hasItem(EMAIL));
        assertThat(identities, not(hasItem(DOMAIN)));
    }

    @Test
    public void listIdentitites_MaxResultsSetToOne_HasNonNullNextToken() {
        assertNotNull(email.listIdentities(ListIdentitiesRequest.builder().maxItems(1).build()).nextToken());
    }

    @Test(expected = SdkServiceException.class)
    public void listIdentities_WithInvalidNextToken_ThrowsException() {
        email.listIdentities(ListIdentitiesRequest.builder().nextToken("invalid-next-token").build());
    }

    @Test(expected = MessageRejectedException.class)
    public void sendEmail_ToUnverifiedIdentity_ThrowsException() {
        email.sendEmail(SendEmailRequest.builder().destination(Destination.builder().toAddresses(EMAIL).build())
                                              .message(newMessage("test")).source(EMAIL).build());
    }

    @Test
    public void getIdentityVerificationAttributes_ForNonVerifiedEmail_ReturnsPendingVerificatonStatus() {
        GetIdentityVerificationAttributesResponse result = email
                .getIdentityVerificationAttributes(GetIdentityVerificationAttributesRequest.builder().identities(EMAIL).build());
        IdentityVerificationAttributes identityVerificationAttributes = result.verificationAttributes().get(EMAIL);
        assertEquals(VerificationStatus.PENDING, identityVerificationAttributes.verificationStatus());
        // Verificaton token not applicable for email identities
        assertNull(identityVerificationAttributes.verificationToken());
    }

    @Test
    public void getIdentityVerificationAttributes_ForNonVerifiedDomain_ReturnsPendingVerificatonStatus() {
        GetIdentityVerificationAttributesResponse result = email
                .getIdentityVerificationAttributes(GetIdentityVerificationAttributesRequest.builder()
                                                           .identities(DOMAIN).build());
        IdentityVerificationAttributes identityVerificationAttributes = result.verificationAttributes().get(DOMAIN);
        assertEquals(VerificationStatus.PENDING, identityVerificationAttributes.verificationStatus());
        assertEquals(DOMAIN_VERIFICATION_TOKEN, identityVerificationAttributes.verificationToken());
    }

    private Message newMessage(String subject) {
        Content content = Content.builder().data(subject).build();
        Message message = Message.builder().subject(content).body(Body.builder().text(content).build()).build();

        return message;
    }

}

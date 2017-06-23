package software.amazon.awssdk.services.cloudfront;

import static org.junit.Assert.assertEquals;
import static software.amazon.awssdk.services.cloudfront.util.SignerUtils.loadPrivateKey;
import static software.amazon.awssdk.test.util.DateUtils.yyMMddhhmmss;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import org.junit.Test;
import software.amazon.awssdk.services.cloudfront.CloudFrontCookieSigner.CookiesForCannedPolicy;
import software.amazon.awssdk.services.cloudfront.util.SignerUtils;
import software.amazon.awssdk.services.cloudfront.util.SignerUtils.Protocol;
import software.amazon.awssdk.util.StringUtils;
import software.amazon.awssdk.utils.Base64Utils;

public class CloudFrontSignerIntegrationTest {

    private static final String PRIVATE_KEY_FILE = "pk-APKAIATTOHFWCYDFKICQ.pem";
    private static final String PRIVATE_KEY_FILE_DER = "pk-APKAIATTOHFWCYDFKICQ.der";
    private static final String PRIVATE_KEY_ID = "APKAIATTOHFWCYDFKICQ";
    private static String callerReference = yyMMddhhmmss();
    private static final String bucketName = StringUtils.lowerCase(CloudFrontSignerIntegrationTest.class.getSimpleName())
                                             + "." + callerReference;

    private static String domainName = bucketName + ".s3.amazonaws.com";

    @Test
    public void testPreSignedUrl() throws Exception {

        Date dateLessThan = new Date(System.currentTimeMillis() + 10 * 1000);
        String cannedSignedURL = CloudFrontUrlSigner.getSignedUrlWithCannedPolicy(
                Protocol.https, domainName,
                getResourceAsFile(PRIVATE_KEY_FILE), "key", PRIVATE_KEY_ID,
                dateLessThan);
        String cannedSignedURL_der = CloudFrontUrlSigner.getSignedUrlWithCannedPolicy(
                Protocol.https, domainName, getResourceAsFile(PRIVATE_KEY_FILE_DER),
                "key", PRIVATE_KEY_ID,
                dateLessThan);
        assertEquals(cannedSignedURL_der, cannedSignedURL);
    }

    private File getResourceAsFile(String resourcePath) throws URISyntaxException {
        return new File(getClass().getResource("/" + resourcePath).toURI());
    }

    @Test
    public void testPreSignedCookie() throws Exception {
        Date dateLessThan = new Date(System.currentTimeMillis() + 10 * 1000);

        CookiesForCannedPolicy pemCookies = CloudFrontCookieSigner.getCookiesForCannedPolicy(
                Protocol.https, domainName, getResourceAsFile(PRIVATE_KEY_FILE),
                "key", PRIVATE_KEY_ID,
                dateLessThan);

        CookiesForCannedPolicy derCookies = CloudFrontCookieSigner.getCookiesForCannedPolicy(
                Protocol.https, domainName, getResourceAsFile(PRIVATE_KEY_FILE_DER),
                "key", PRIVATE_KEY_ID,
                dateLessThan);

        assertEquals(pemCookies.getSignature().getValue(), derCookies.getSignature().getValue());
    }

    @Test
    public void makeBytesUrlSafe() {
        String expectedB64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/AAA=";
        String expectedEnc = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-~AAA_";
        byte[] input = Base64Utils.decode(expectedB64);
        String b64 = Base64Utils.encodeAsString(input);
        assertEquals(expectedB64, b64);
        String encoded = SignerUtils.makeBytesUrlSafe(input);
        assertEquals(expectedEnc, encoded);
    }

    @Test
    public void buildCustomPolicyForSignedUrl() {
        String expected = "{\"Statement\": [{\"Resource\":\"resourcePath\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":654321},\"IpAddress\":{\"AWS:SourceIp\":\"limitToIpAddressCIDR\"},\"DateGreaterThan\":{\"AWS:EpochTime\":123456}}}]}";
        Date epochDateLessThan = new Date(654321000L);
        Date epochDateGreaterThan = new Date(123456000L);
        String policy = CloudFrontUrlSigner
                .buildCustomPolicyForSignedUrl("resourcePath", epochDateLessThan, "limitToIpAddressCIDR", epochDateGreaterThan);
        System.out.println("buildPolicyForSignedUrl: " + policy);
        assertEquals(expected, policy);
    }

    @Test
    public void buildCannedPolicy() {
        String expected = "{\"Statement\":[{\"Resource\":\"resourceUrlOrPath\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":654321}}}]}";
        Date dateLessThan = new Date(654321000L);
        String policy = SignerUtils.buildCannedPolicy("resourceUrlOrPath", dateLessThan);
        System.out.println("makeCannedPolicy: " + policy);
        assertEquals(expected, policy);
    }

    @Test
    public void buildCustomPolicy() {
        String expected = "{\"Statement\": [{\"Resource\":\"resourcePath\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":654321},\"IpAddress\":{\"AWS:SourceIp\":\"1.2.3.4\"},\"DateGreaterThan\":{\"AWS:EpochTime\":123456}}}]}";
        Date epochDateGreaterThan = new Date(123456000L);
        Date epochDateLessThan = new Date(654321000L);
        String ipAddress = "1.2.3.4";
        String policy = SignerUtils.buildCustomPolicy("resourcePath", epochDateLessThan, epochDateGreaterThan, ipAddress);
        System.out.println("buildPolicy: " + policy);
        assertEquals(expected, policy);
    }

    @Test
    public void getSignedURLWithCannedPolicy() throws InvalidKeySpecException, IOException, URISyntaxException {
        String pkResource = "pk-APKAJM22QV32R3I2XVIQ.pem";
        String expected = "https://distributionDomain/s3ObjectKey?Expires=654321&Signature=goxB1fYnUvUQ6oMoH4ouQPbjBsW87piYWrlNgfRvn5NQ7jHAEW~KeWc7-xpsxt66FT044af7nP2JkDKhzMVsg9t-SnXaCJ8phnNsoPsMlCLVVAGQxY9Pz-8bZtmPo8MZD7Zb9PNP2nPabPcFBYdpdsjv2ivnrM2X8WP7KGHogazSOuH8z0PULsOnvaa2~izOmF3~HnDXEsDNIJsoP8jXhI160pyu1dIxyrCmYxE~3wIdTUUZ53OLkb-6dC~c4k0d8cMTD8M-owgHO5bThJl~lKFkpXXbtpvWciMhLb6sLU2sgeWCG8~ScY7b4OD9Qvisp-iyp1l1mjIPe0aXV053tQ__&Key-Pair-Id=keyPairId";
        Date dateLessThan = new Date(654321000L);
        File pkFile = getResourceAsFile(pkResource);
        {
            String signedUrl = CloudFrontUrlSigner
                    .getSignedUrlWithCannedPolicy(Protocol.https, "distributionDomain", pkFile, "s3ObjectKey", "keyPairId",
                                                  dateLessThan);
            System.out.println("getSignedURLWithCannedPolicy: " + signedUrl);
            assertEquals(expected, signedUrl);
        }
        {
            PrivateKey pk = loadPrivateKey(getResourceAsFile("pk-APKAJM22QV32R3I2XVIQ.pem"));
            String signedUrl = CloudFrontUrlSigner
                    .getSignedUrlWithCannedPolicy("https://distributionDomain/s3ObjectKey", "keyPairId", pk, dateLessThan);
            assertEquals(expected, signedUrl);
        }
    }

    @Test
    public void getSignedURLWithCustomPolicy() throws InvalidKeySpecException, IOException, URISyntaxException {
        String pkResource = "pk-APKAJM22QV32R3I2XVIQ.pem";
        String expected = "https://distributionDomain/s3ObjectKey?Policy=eyJTdGF0ZW1lbnQiOiBbeyJSZXNvdXJjZSI6Imh0dHBzOi8vZGlzdHJpYnV0aW9uRG9tYWluL3MzT2JqZWN0S2V5IiwiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6eyJBV1M6RXBvY2hUaW1lIjo2NTQzMjF9LCJJcEFkZHJlc3MiOnsiQVdTOlNvdXJjZUlwIjoiMS4yLjMuNCJ9LCJEYXRlR3JlYXRlclRoYW4iOnsiQVdTOkVwb2NoVGltZSI6MTIzNDU2fX19XX0_&Signature=ljUQZ3boG4VlB-z7k7yOhaHUyKKeo4zY6mdT0OEF4Xd86mLEACwtu~Gn~MeuaLs~2p-5KPthWllUsID4r6SBhEwALlvecEsDjtw3yVtgno4POglBR5gTMHMKW-HzA7QxKNwvZYx5t7FUl0VrxFhy0ND124UonmMDkc1fSc7b2jp5VB7nrKFtqdSt0CVAMSM5cBUFVU28gIPU-ji2UgjMP0Q8M6jE82gQuxplqY9X-yTmTWmZhyZcV5utxFpyP9DI4DtycqsymGSQMIH3aStFZhPoH1UUrsWzzOOyzTC9plRFHcux3sIOuEHgElnxR2oqQFrq2qQw~Ibb5kwDPaOk9g__&Key-Pair-Id=keyPairId";
        Date dateGreaterThan = new Date(123456000L);
        Date dateLessThan = new Date(654321000L);
        String ipRange = "1.2.3.4";
        File pkFile = getResourceAsFile(pkResource);
        {
            String signedUrl = CloudFrontUrlSigner
                    .getSignedUrlWithCustomPolicy(Protocol.https, "distributionDomain", pkFile, "s3ObjectKey", "keyPairId",
                                                  dateLessThan, dateGreaterThan, ipRange);
            System.out.println("getSignedURLWithCustomPolicy: " + signedUrl);
            assertEquals(expected, signedUrl);
        }
        {
            PrivateKey pk = loadPrivateKey(getResourceAsFile("pk-APKAJM22QV32R3I2XVIQ.pem"));
            String policy = CloudFrontUrlSigner
                    .buildCustomPolicyForSignedUrl("https://distributionDomain/s3ObjectKey", dateLessThan, ipRange,
                                                   dateGreaterThan);
            String signedUrl = CloudFrontUrlSigner
                    .getSignedUrlWithCustomPolicy("https://distributionDomain/s3ObjectKey", "keyPairId", pk, policy);
            System.out.println("getSignedURLWithCustomPolicy 2: " + signedUrl);
            assertEquals(expected, signedUrl);
        }
    }

}

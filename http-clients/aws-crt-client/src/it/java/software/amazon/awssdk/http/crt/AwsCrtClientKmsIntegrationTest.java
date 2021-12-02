package software.amazon.awssdk.http.crt;

import static software.amazon.awssdk.testutils.service.AwsTestBase.CREDENTIALS_PROVIDER_CHAIN;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.crt.io.EventLoopGroup;
import software.amazon.awssdk.crt.io.HostResolver;
import software.amazon.awssdk.crt.io.TlsCipherPreference;
import software.amazon.awssdk.crt.io.TlsContextOptions;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsAsyncClient;
import software.amazon.awssdk.services.kms.model.CreateAliasRequest;
import software.amazon.awssdk.services.kms.model.CreateAliasResponse;
import software.amazon.awssdk.services.kms.model.CreateKeyRequest;
import software.amazon.awssdk.services.kms.model.CreateKeyResponse;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.DescribeKeyRequest;
import software.amazon.awssdk.services.kms.model.DescribeKeyResponse;
import software.amazon.awssdk.services.kms.model.EncryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptResponse;


public class AwsCrtClientKmsIntegrationTest {
    private static String KEY_ALIAS = "alias/aws-sdk-java-v2-integ-test";
    private static Region REGION = Region.US_EAST_1;
    private static List<SdkAsyncHttpClient> awsCrtHttpClients = new ArrayList<>();
    private static EventLoopGroup eventLoopGroup;
    private static HostResolver hostResolver;

    @Before
    public void setup() {
        CrtResource.waitForNoResources();

        // Create an Http Client for each TLS Cipher Preference supported on the current platform
        for (TlsCipherPreference pref: TlsCipherPreference.values()) {
            if (!TlsContextOptions.isCipherPreferenceSupported(pref)) {
                continue;
            }

            int numThreads = 1;
            eventLoopGroup = new EventLoopGroup(numThreads);
            hostResolver = new HostResolver(eventLoopGroup);

            SdkAsyncHttpClient awsCrtHttpClient = AwsCrtAsyncHttpClient.builder()
                    .build();

            awsCrtHttpClients.add(awsCrtHttpClient);
        }
    }


    @After
    public void tearDown() {
        hostResolver.close();
        eventLoopGroup.close();
        CrtResource.waitForNoResources();
    }

    private boolean doesKeyExist(KmsAsyncClient kms, String keyAlias) {
        try {
            DescribeKeyRequest req = DescribeKeyRequest.builder().keyId(keyAlias).build();
            DescribeKeyResponse resp = kms.describeKey(req).get();
            Assert.assertEquals(200, resp.sdkHttpResponse().statusCode());
            return resp.sdkHttpResponse().isSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    private void createKeyAlias(KmsAsyncClient kms, String keyId, String keyAlias) throws Exception {
        CreateAliasRequest req = CreateAliasRequest.builder().aliasName(keyAlias).targetKeyId(keyId).build();
        CreateAliasResponse resp = kms.createAlias(req).get();
        Assert.assertEquals(200, resp.sdkHttpResponse().statusCode());
    }

    private String createKey(KmsAsyncClient kms) throws Exception {
        CreateKeyRequest req = CreateKeyRequest.builder().build();
        CreateKeyResponse resp = kms.createKey(req).get();
        Assert.assertEquals(200, resp.sdkHttpResponse().statusCode());
        return resp.keyMetadata().keyId();
    }

    private void createKeyIfNotExists(KmsAsyncClient kms, String keyAlias) throws Exception {
        if (!doesKeyExist(kms, keyAlias)) {
            String keyId = createKey(kms);
            createKeyAlias(kms, keyId, KEY_ALIAS);
        }
    }

    private SdkBytes encrypt(KmsAsyncClient kms, String keyId, String plaintext) throws Exception {
        SdkBytes bytes = SdkBytes.fromUtf8String(plaintext);
        EncryptRequest req = EncryptRequest.builder().keyId(keyId).plaintext(bytes).build();
        EncryptResponse resp = kms.encrypt(req).get();
        Assert.assertEquals(200, resp.sdkHttpResponse().statusCode());
        return resp.ciphertextBlob();
    }

    private String decrypt(KmsAsyncClient kms, SdkBytes ciphertext) throws Exception {
        DecryptRequest req = DecryptRequest.builder().ciphertextBlob(ciphertext).build();
        DecryptResponse resp = kms.decrypt(req).get();
        Assert.assertEquals(200, resp.sdkHttpResponse().statusCode());
        return resp.plaintext().asUtf8String();
    }

    private void testEncryptDecryptWithKms(KmsAsyncClient kms) throws Exception {
        createKeyIfNotExists(kms, KEY_ALIAS);
        Assert.assertTrue(doesKeyExist(kms, KEY_ALIAS));
        Assert.assertFalse(doesKeyExist(kms, "alias/does-not-exist-" + UUID.randomUUID()));

        String secret = UUID.randomUUID().toString();
        SdkBytes cipherText = encrypt(kms, KEY_ALIAS, secret);
        String plainText = decrypt(kms, cipherText);

        Assert.assertEquals(plainText, secret);
    }

    @Test
    public void testEncryptDecryptWithKms() throws Exception {
        for (SdkAsyncHttpClient awsCrtHttpClient: awsCrtHttpClients) {
            KmsAsyncClient kms = KmsAsyncClient.builder()
                                    .region(REGION)
                                    .httpClient(awsCrtHttpClient)
                                    .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                    .build();

            testEncryptDecryptWithKms(kms);

            kms.close();
            awsCrtHttpClient.close();
        }
    }
}

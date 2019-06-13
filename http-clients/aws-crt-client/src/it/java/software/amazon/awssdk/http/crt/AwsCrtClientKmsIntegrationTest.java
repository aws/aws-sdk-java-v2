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
import software.amazon.awssdk.crt.io.ClientBootstrap;
import software.amazon.awssdk.crt.io.SocketOptions;
import software.amazon.awssdk.crt.io.TlsContext;
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
    private static SdkAsyncHttpClient client;
    private static KmsAsyncClient kms;

    List<CrtResource> crtResources = new ArrayList<>();

    private void addResource(CrtResource resource) {
        crtResources.add(resource);
    }

    @Before
    public void setup() {
        Assert.assertEquals("Expected Zero allocated AwsCrtResources", 0, CrtResource.getAllocatedNativeResourceCount());

        ClientBootstrap bootstrap = new ClientBootstrap(1);
        SocketOptions socketOptions = new SocketOptions();
        TlsContext tlsContext = new TlsContext();

        addResource(bootstrap);
        addResource(socketOptions);
        addResource(tlsContext);

        client = AwsCrtAsyncHttpClient.builder()
                .bootstrap(bootstrap)
                .socketOptions(socketOptions)
                .tlsContext(tlsContext)
                .build();

        kms = KmsAsyncClient.builder()
                .region(REGION)
                .httpClient(client)
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .build();
    }

    @After
    public void tearDown() {
        kms.close();
        client.close();

        for (CrtResource r: crtResources) {
            r.close();
        }

        Assert.assertEquals("Expected Zero allocated AwsCrtResources", 0, CrtResource.getAllocatedNativeResourceCount());
    }

    private boolean doesKeyExist(String keyAlias) {
        try {
            DescribeKeyRequest req = DescribeKeyRequest.builder().keyId(keyAlias).build();
            DescribeKeyResponse resp = kms.describeKey(req).get();
            Assert.assertEquals(200, resp.sdkHttpResponse().statusCode());
            return resp.sdkHttpResponse().isSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    private void createKeyAlias(String keyId, String keyAlias) throws Exception {
        CreateAliasRequest req = CreateAliasRequest.builder().aliasName(keyAlias).targetKeyId(keyId).build();
        CreateAliasResponse resp = kms.createAlias(req).get();
        Assert.assertEquals(200, resp.sdkHttpResponse().statusCode());
    }

    private String createKey() throws Exception {
        CreateKeyRequest req = CreateKeyRequest.builder().build();
        CreateKeyResponse resp = kms.createKey(req).get();
        Assert.assertEquals(200, resp.sdkHttpResponse().statusCode());
        return resp.keyMetadata().keyId();
    }

    private void createKeyIfNotExists(String keyAlias) throws Exception {
        if (!doesKeyExist(keyAlias)) {
            String keyId = createKey();
            createKeyAlias(keyId, KEY_ALIAS);
        }
    }

    private SdkBytes encrypt(String keyId, String plaintext) throws Exception {
        SdkBytes bytes = SdkBytes.fromUtf8String(plaintext);
        EncryptRequest req = EncryptRequest.builder().keyId(keyId).plaintext(bytes).build();
        EncryptResponse resp = kms.encrypt(req).get();
        Assert.assertEquals(200, resp.sdkHttpResponse().statusCode());
        return resp.ciphertextBlob();
    }

    private String decrypt(SdkBytes ciphertext) throws Exception {
        DecryptRequest req = DecryptRequest.builder().ciphertextBlob(ciphertext).build();
        DecryptResponse resp = kms.decrypt(req).get();
        Assert.assertEquals(200, resp.sdkHttpResponse().statusCode());
        return resp.plaintext().asUtf8String();
    }

    @Test
    public void testEncryptDecryptWithKms() throws Exception {
        createKeyIfNotExists(KEY_ALIAS);
        Assert.assertTrue(doesKeyExist(KEY_ALIAS));
        Assert.assertFalse(doesKeyExist("alias/does-not-exist-" + UUID.randomUUID()));

        String secret = UUID.randomUUID().toString();
        SdkBytes cipherText = encrypt(KEY_ALIAS, secret);
        String plainText = decrypt(cipherText);

        Assert.assertEquals(plainText, secret);
    }
}

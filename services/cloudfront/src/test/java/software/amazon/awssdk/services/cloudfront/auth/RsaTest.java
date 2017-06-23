package software.amazon.awssdk.services.cloudfront.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import org.junit.Test;
import software.amazon.awssdk.utils.IoUtils;

public class RsaTest {
    @Test
    public void loadPrivateKeyFromDER() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("pk-APKAIATTOHFWCYDFKICQ.der");
        byte[] pkcs8 = IoUtils.toByteArray(is);
        is.close();
        PrivateKey pk = Rsa.privateKeyFromPkcs8(pkcs8);
        assertEquals("PKCS#8", pk.getFormat());
        assertEquals("RSA", pk.getAlgorithm());
        try {
            Rsa.privateKeyFromPkcs1(pkcs8);
            fail();
        } catch(IllegalArgumentException expected) {
        }
    }

    @Test
    public void loadPrivateKeyFromPEM() throws InvalidKeySpecException, IOException {
        String[] resourceNames = {
                "pk-APKAJM22QV32R3I2XVIQ_pk8.pem",  // encoded in PKCS8 format
                "pk-APKAJM22QV32R3I2XVIQ.pem", // encoded in PKCS1 format
                "pk-APKAIATTOHFWCYDFKICQ.pem", // encoded in PKCS1 format
                "test-private-key.pem",
                "test-keypair.pem",
        };
        for (String resource: resourceNames) {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
            doLoadPrivateKey(is);
            is.close();
        }
    }
    
    private void doLoadPrivateKey(InputStream is) throws InvalidKeySpecException, IOException {
        PrivateKey pk = Pem.readPrivateKey(is);
        // funny a private key loaded from PKCS1 encoded format would still report as PKCS#8
        assertEquals("PKCS#8", pk.getFormat());
        assertEquals("RSA", pk.getAlgorithm());
    }


    @Test
    public void loadPublicKeyFromPEM() throws NoSuchAlgorithmException, FileNotFoundException, InvalidKeySpecException, IOException {
        String[] resourceNames = {
            "test-public-key.pem",
            "test-keypair.pem",
        };
        for (String resource : resourceNames) {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
            PublicKey pk = Pem.readPublicKey(is);
            is.close();
            assertEquals("X.509", pk.getFormat());
            assertEquals("RSA", pk.getAlgorithm());
        }
    }
}

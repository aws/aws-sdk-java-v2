package software.amazon.awssdk.services.cloudfront.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.Test;

public class PemTest {

    @Test
    public void test() throws IOException {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("test-keypair.pem");
        List<PemObject> pos = Pem.readPenObjects(is);
        is.close();
        assertTrue(pos.size() == 2);
        PemObject o1 = pos.get(0);
        assertEquals(PemObjectType.PRIVATE_KEY_PKCS1, o1.getPemObjectType());
        PemObject o2 = pos.get(1);
        assertEquals(PemObjectType.PUBLIC_KEY_X509, o2.getPemObjectType());
    }

}

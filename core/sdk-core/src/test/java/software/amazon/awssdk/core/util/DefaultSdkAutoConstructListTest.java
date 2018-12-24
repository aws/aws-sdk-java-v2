package software.amazon.awssdk.core.util;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

public class DefaultSdkAutoConstructListTest {

    @Test
    public void testSerialization() throws Exception {
        DefaultSdkAutoConstructList<Object> original = DefaultSdkAutoConstructList.getInstance();
        byte[] originalBytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos)){
            oos.writeObject(original);
            originalBytes = baos.toByteArray();
        }
        DefaultSdkAutoConstructList<Object> deserializedObject;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(originalBytes))) {
             deserializedObject = (DefaultSdkAutoConstructList<Object>) ois.readObject();
        }
        assertEquals(original, deserializedObject);
    }
}

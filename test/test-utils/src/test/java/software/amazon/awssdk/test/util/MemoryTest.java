package software.amazon.awssdk.test.util;

import org.junit.Test;

public class MemoryTest {

    @Test
    public void test() {
        System.out.println(Memory.heapSummary());
        System.out.println(Memory.poolSummaries());
    }

}

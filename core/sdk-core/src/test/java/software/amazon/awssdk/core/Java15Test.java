package software.amazon.awssdk.core;


import java.lang.invoke.VarHandle;
import java.lang.reflect.RecordComponent;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.annotation.Nullable;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryHandles;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.SequenceLayout;
import software.amazon.awssdk.annotations.SdkTestInternalApi;

public class Java15Test {

    public void localRecords() {

        record Book(@Nullable String id, @SdkTestInternalApi String name) {

        }

        Book book = new Book(null, "test");
        RecordComponent[] recordComponents = book.getClass().getRecordComponents();
        boolean isRecord = book.getClass().isRecord();
    }

    public void localEnum() {
        enum WeekDay {
            MONDAY,
            TUESDAY,
            WEDNESDAY,
            THURSDAY,
            FRIDAY
        }

        WeekDay friday = WeekDay.FRIDAY;
    }

    public void localInterface() {
        interface LocalInterface {
            void method();
        }

        class LocalClass implements LocalInterface {
            @Override
            public void method() {

            }
        }
    }

    public void foreignMemoryAccess() {
        byte[] bytes = new byte[100];

        // using ByteBuffer
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(100);
        byteBuffer = ByteBuffer.wrap(bytes);

        // using the new APIs
        MemorySegment memorySegment = MemorySegment.allocateNative(100);
        memorySegment = MemorySegment.ofArray(bytes);
        memorySegment = MemorySegment.ofByteBuffer(byteBuffer);

        // define layout - how the memory is broken up into elements
        SequenceLayout intArrayLayout
            = MemoryLayout.ofSequence(25,
                                      MemoryLayout.ofValueBits(32,
                                                               ByteOrder.nativeOrder()));
        memorySegment = MemorySegment.allocateNative(intArrayLayout);


        // store and retrieve data
        try (MemorySegment segment = MemorySegment.allocateNative(4)) {
            MemoryAddress address = segment.baseAddress();
            VarHandle varHandle = MemoryHandles.varHandle(int.class, ByteOrder.nativeOrder());
            varHandle.set(address, 1);
            int value = (int) varHandle.get(address);
        }
    }
}

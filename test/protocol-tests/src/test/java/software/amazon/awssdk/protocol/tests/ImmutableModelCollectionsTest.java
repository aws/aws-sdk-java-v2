package software.amazon.awssdk.protocol.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesRequest;
import software.amazon.awssdk.services.protocolrestjson.model.SimpleStruct;

/**
 * Verifies that the collections inside of models are immutable.
 */
public class ImmutableModelCollectionsTest {
    @Test
    public void mapsAreImmutable() {
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");

        AllTypesRequest request = AllTypesRequest.builder().mapOfStringToString(map).build();
        map.put("key2", "value2");

        assertThat(request.mapOfStringToString()).doesNotContainKey("key2");
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> request.mapOfStringToString().put("key2", "value2"));
    }

    @Test
    public void listsAreImmutable() {
        SimpleStruct struct = SimpleStruct.builder().stringMember("value").build();
        SimpleStruct struct2 = SimpleStruct.builder().stringMember("value2").build();

        List<SimpleStruct> list = new ArrayList<>();
        list.add(struct);

        AllTypesRequest request = AllTypesRequest.builder().listOfStructs(list).build();
        list.add(struct2);

        assertThat(request.listOfStructs()).doesNotContain(struct2);
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> request.listOfStructs().add(struct2));
    }

    @Test
    public void mapsOfListsAreImmutable() {
        List<Integer> list = new ArrayList<>();
        list.add(1);

        Map<String, List<Integer>> map = Collections.singletonMap("key", list);

        AllTypesRequest request = AllTypesRequest.builder().mapOfStringToIntegerList(map).build();

        list.add(2);

        assertThat(request.mapOfStringToIntegerList().get("key")).doesNotContain(2);
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> request.mapOfStringToIntegerList().get("key").add(2));
    }
}

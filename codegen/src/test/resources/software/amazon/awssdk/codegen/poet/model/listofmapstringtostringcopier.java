package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;

@Generated("software.amazon.awssdk:codegen")
final class ListOfMapStringToStringCopier {
    static List<Map<String, String>> copy(Collection<Map<String, String>> listOfMapStringToStringParam) {
        if (listOfMapStringToStringParam == null) {
            return null;
        }
        List<Map<String, String>> listOfMapStringToStringParamCopy = new ArrayList<>(listOfMapStringToStringParam.size());
        for (Map<String, String> e : listOfMapStringToStringParam) {
            listOfMapStringToStringParamCopy.add(MapOfStringToStringCopier.copy(e));
        }
        return listOfMapStringToStringParamCopy;
    }
}


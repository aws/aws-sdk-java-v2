package software.amazon.awssdk.codegen.poet.model;

import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;

import java.util.HashMap;
import java.util.Map;

class TestMemberModels {
    private final IntermediateModel intermediateModel;
    private Map<String, MemberModel> shapeToMemberMap;

    public TestMemberModels(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
    }

    /**
     * Returns a mapping of c2jshapes to a single MemberModel in the intermediate model.
     *
     * @return
     */
    public Map<String, MemberModel> shapeToMemberMap() {
        if (shapeToMemberMap == null) {
            shapeToMemberMap = new HashMap<>();


            intermediateModel.getShapes().values().stream()
                    .flatMap(s -> s.getMembers().stream())
                    .forEach(m -> shapeToMemberMap.put(m.getC2jShape(), m));
        }

        return shapeToMemberMap;
    }
}

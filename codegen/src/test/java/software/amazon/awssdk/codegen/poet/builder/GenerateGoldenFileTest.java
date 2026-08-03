package software.amazon.awssdk.codegen.poet.builder;

import static software.amazon.awssdk.codegen.poet.ClientTestModels.queryServiceModelsNoRegionEndpointRules;

import com.squareup.javapoet.JavaFile;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;

public class GenerateGoldenFileTest {

    @Test
    void generateNoRegionGoldenFile() throws Exception {
        IntermediateModel model = queryServiceModelsNoRegionEndpointRules();
        BaseClientBuilderClass generator = new BaseClientBuilderClass(model);
        JavaFile javaFile = JavaFile.builder(generator.className().packageName(), generator.poetClass()).build();
        StringBuilder sb = new StringBuilder();
        javaFile.writeTo(sb);
        Files.write(Paths.get("/tmp/test-no-region-client-builder-class.java"), sb.toString().getBytes());
        System.out.println("Golden file written to /tmp/test-no-region-client-builder-class.java");
    }
}

package software.amazon.awssdk.codegen.internal;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class UtilsTest {
    final Map<String,String> capitalizedToUncapitalized = new HashMap<String,String>() {{
        put("A", "a");
        put("AB", "ab");
        put("ABC", "abc");
        put("ABCD", "abcd");

        put("AToken", "aToken");
        put("MFAToken", "mfaToken");
        put("AWSRequest", "awsRequest");

        put("MfaToken", "mfaToken");
        put("AwsRequest", "awsRequest");
    }};

    @Test
    public void testUnCapitalize() {
        capitalizedToUncapitalized.forEach((capitalized,unCapitalized) ->
                assertThat(Utils.unCapitialize(capitalized), is(equalTo(unCapitalized))));
    }
}

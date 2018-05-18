package software.amazon.awssdk.auth.signer;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.internal.AwsSignerParams;
import software.amazon.awssdk.regions.Region;

public class AwsSignerParamsTest {

    @Test
    public void testBuildWorksProperly() {
        AwsCredentials awsCredentials = AwsCredentials.create("foo", "bar");
        String signingName = "demo";
        Region region = Region.AP_NORTHEAST_2;
        Integer timeOffset = new Integer(10);

        AwsSignerParams params = AwsSignerParams.builder()
                                                .awsCredentials(awsCredentials)
                                                .signingName(signingName)
                                                .region(region)
                                                .timeOffset(timeOffset)
                                                .build();

        assertThat(params.awsCredentials()).isEqualToComparingFieldByField(awsCredentials);
        assertThat(params.signingName()).isEqualTo(signingName);
        assertThat(params.region()).isEqualTo(region);
        assertThat(params.timeOffset()).isEqualTo(timeOffset);
        assertThat(params.doubleUrlEncode()).isEqualTo(Boolean.TRUE);
    }
}

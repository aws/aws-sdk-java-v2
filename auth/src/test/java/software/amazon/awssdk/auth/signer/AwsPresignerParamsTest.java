package software.amazon.awssdk.auth.signer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.internal.AwsPresignerParams;
import software.amazon.awssdk.regions.Region;

public class AwsPresignerParamsTest {

    @Test
    public void testBuildWorksProperly() {
        AwsCredentials awsCredentials = AwsCredentials.create("foo", "bar");
        Date expirationDate = new Date();
        String signingName = "demo";
        Region region = Region.AP_NORTHEAST_2;
        Integer timeOffset = new Integer(10);
        Boolean doubleUrlEncode = Boolean.FALSE;

        AwsPresignerParams params = AwsPresignerParams.builder()
                                                      .awsCredentials(awsCredentials)
                                                      .expirationDate(expirationDate)
                                                      .signingName(signingName)
                                                      .region(region)
                                                      .timeOffset(timeOffset)
                                                      .doubleUrlEncode(doubleUrlEncode)
                                                      .build();

        assertThat(params.awsCredentials()).isEqualToComparingFieldByField(awsCredentials);
        assertThat(params.expirationDate()).isEqualTo(expirationDate);
        assertThat(params.signingName()).isEqualTo(signingName);
        assertThat(params.region()).isEqualTo(region);
        assertThat(params.timeOffset()).isEqualTo(timeOffset);
        assertThat(params.doubleUrlEncode()).isEqualTo(doubleUrlEncode);
    }
}

package software.amazon.awssdk.auth.signer.internal;


import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;

/**
 * Parameters that are used during signing.
 *
 * Required parameters vary based on signer implementations. Signer implementations might only use a
 * subset of params in this class.
 */
public class AwsSignerParams {

    private boolean DEFAULT_DOUBLE_URL_ENCODE = true;

    private boolean doubleUrlEncode = DEFAULT_DOUBLE_URL_ENCODE;

    private AwsCredentials awsCredentials;

    private String signingName;

    private Region region;

    private Integer timeOffset;


    public boolean isDoubleUrlEncode() {
        return doubleUrlEncode;
    }

    public void setDoubleUrlEncode(boolean doubleUrlEncode) {
        this.doubleUrlEncode = doubleUrlEncode;
    }


    public AwsCredentials getAwsCredentials() {
        return awsCredentials;
    }

    public void setAwsCredentials(AwsCredentials awsCredentials) {
        this.awsCredentials = awsCredentials;
    }

    public String getSigningName() {
        return signingName;
    }

    public void setSigningName(String signingName) {
        this.signingName = signingName;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public Integer getTimeOffset() {
        return timeOffset;
    }

    public void setTimeOffset(Integer timeOffset) {
        this.timeOffset = timeOffset;
    }
}
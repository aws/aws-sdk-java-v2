package software.amazon.awssdk.auth.signer.internal;

import java.util.Date;

public class AwsPresignerParams extends AwsSignerParams {

    private Date expirationDate;

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }
}
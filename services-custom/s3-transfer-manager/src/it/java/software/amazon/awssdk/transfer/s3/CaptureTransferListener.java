/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.transfer.s3;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

public class CaptureTransferListener implements TransferListener {
    public Boolean isTransferInitiated() {
        return transferInitiated;
    }

    public Boolean isTransferComplete() {
        return transferComplete;
    }

    public List<Double> getRatioTransferredList() {
        return ratioTransferredList;
    }

    public Throwable getExceptionCaught() {
        return exceptionCaught;
    }

    private Boolean transferInitiated = false;
    private Boolean transferComplete = false;

    private List<Double> ratioTransferredList = new ArrayList<>();
    private Throwable exceptionCaught;

    @Override
    public void transferInitiated(Context.TransferInitiated context) {
        transferInitiated = true;
        context.progressSnapshot().ratioTransferred().ifPresent(ratioTransferredList::add);

    }

    @Override
    public void bytesTransferred(Context.BytesTransferred context) {
        context.progressSnapshot().ratioTransferred().ifPresent(ratioTransferredList::add);
    }

    @Override
    public void transferComplete(Context.TransferComplete context) {
        context.progressSnapshot().ratioTransferred().ifPresent(ratioTransferredList::add);
        transferComplete = true;
    }

    @Override
    public void transferFailed(Context.TransferFailed context) {
        exceptionCaught = context.exception();
    }
}

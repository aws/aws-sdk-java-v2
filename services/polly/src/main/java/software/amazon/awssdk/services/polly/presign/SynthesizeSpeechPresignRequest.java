/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.polly.presign;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import software.amazon.awssdk.SdkRequest;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.services.polly.model.OutputFormat;
import software.amazon.awssdk.services.polly.model.SpeechMarkType;
import software.amazon.awssdk.services.polly.model.TextType;
import software.amazon.awssdk.services.polly.model.VoiceId;

/**
 * Presigning input for {@link PollyClientPresigners#getPresignedSynthesizeSpeechUrl(SynthesizeSpeechPresignRequest)}.
 */
@ReviewBeforeRelease("Immutable? Generate?")
public class SynthesizeSpeechPresignRequest extends SdkRequest implements Serializable {

    private Date expirationDate;

    private AwsCredentialsProvider signingCredentials;

    private java.util.List<String> lexiconNames;

    private String outputFormat;

    private String sampleRate;

    private String text;

    private String textType;

    private String voiceId;

    private java.util.List<String> speechMarkTypes;

    /**
     * @return Expiration of the presigned request. Default is
     *     {@link PollyClientPresigners#SYNTHESIZE_SPEECH_DEFAULT_EXPIRATION_MINUTES} minutes if not overridden.
     */
    public Date getExpirationDate() {
        return expirationDate;
    }

    /**
     * Sets the expiration of the presigned request. Default is
     * {@link PollyClientPresigners#SYNTHESIZE_SPEECH_DEFAULT_EXPIRATION_MINUTES} minutes if not overridden.
     */
    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * Sets the expiration of the presigned request. Default is
     * {@link PollyClientPresigners#SYNTHESIZE_SPEECH_DEFAULT_EXPIRATION_MINUTES} minutes if not overridden.
     *
     * @return This object for method chaining.
     */
    public SynthesizeSpeechPresignRequest withExpirationDate(Date date) {
        setExpirationDate(date);
        return this;
    }

    /**
     * @return Credentials to use in presigning the request. If not provided, client credentials are used.
     */
    public AwsCredentialsProvider getSigningCredentials() {
        return signingCredentials;
    }

    /**
     * @param signingCredentials Credentials to use in presigning the request. If not provided, client credentials are used.
     */
    public void setSigningCredentials(AwsCredentialsProvider signingCredentials) {
        this.signingCredentials = signingCredentials;
    }

    /**
     * @param signingCredentials Credentials to use in presigning the request. If not provided, client credentials are used.
     * @return This object for method chaining.
     */
    public SynthesizeSpeechPresignRequest withSigningCredentials(AwsCredentialsProvider signingCredentials) {
        this.signingCredentials = signingCredentials;
        return this;
    }

    public java.util.List<String> getLexiconNames() {
        return lexiconNames;
    }

    public void setLexiconNames(java.util.Collection<String> lexiconNames) {
        if (lexiconNames == null) {
            this.lexiconNames = null;
            return;
        }

        this.lexiconNames = new java.util.ArrayList<>(lexiconNames);
    }

    /**
     * <p> <b>NOTE:</b> This method appends the values to the existing list (if any). Use {@link
     * #setLexiconNames(java.util.Collection)} or {@link #withLexiconNames(java.util.Collection)} if you want to override the
     * existing values. </p>
     *
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    public SynthesizeSpeechPresignRequest withLexiconNames(String... lexiconNames) {
        if (this.lexiconNames == null) {
            setLexiconNames(new java.util.ArrayList<>(lexiconNames.length));
        }
        for (String ele : lexiconNames) {
            this.lexiconNames.add(ele);
        }
        return this;
    }

    /**
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    public SynthesizeSpeechPresignRequest withLexiconNames(java.util.Collection<String> lexiconNames) {
        setLexiconNames(lexiconNames);
        return this;
    }

    /**
     * @see OutputFormat
     */
    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    /**
     * @see OutputFormat
     */
    public String getOutputFormat() {
        return this.outputFormat;
    }

    /**
     * @see OutputFormat
     */
    public void setOutputFormat(OutputFormat outputFormat) {
        this.outputFormat = outputFormat.toString();
    }

    /**
     * @return Returns a reference to this object so that method calls can be chained together.
     * @see OutputFormat
     */
    public SynthesizeSpeechPresignRequest withOutputFormat(String outputFormat) {
        setOutputFormat(outputFormat);
        return this;
    }

    /**
     * @return Returns a reference to this object so that method calls can be chained together.
     * @see software.amazon.awssdk.services.polly.model.OutputFormat
     */
    public SynthesizeSpeechPresignRequest withOutputFormat(OutputFormat outputFormat) {
        setOutputFormat(outputFormat);
        return this;
    }

    /**
     */
    public String getSampleRate() {
        return this.sampleRate;
    }

    /**
     */
    public void setSampleRate(String sampleRate) {
        this.sampleRate = sampleRate;
    }

    /**
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    public SynthesizeSpeechPresignRequest withSampleRate(String sampleRate) {
        setSampleRate(sampleRate);
        return this;
    }

    /**
     */
    public String getText() {
        return this.text;
    }

    /**
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    public SynthesizeSpeechPresignRequest withText(String text) {
        setText(text);
        return this;
    }

    /**
     * @see TextType
     */
    public void setTextType(String textType) {
        this.textType = textType;
    }

    /**
     * @see TextType
     */
    public String getTextType() {
        return this.textType;
    }

    /**
     * @see TextType
     */
    public void setTextType(TextType textType) {
        this.textType = textType.toString();
    }

    /**
     * @return Returns a reference to this object so that method calls can be chained together.
     * @see TextType
     */
    public SynthesizeSpeechPresignRequest withTextType(String textType) {
        setTextType(textType);
        return this;
    }

    /**
     * @return Returns a reference to this object so that method calls can be chained together.
     * @see TextType
     */
    public SynthesizeSpeechPresignRequest withTextType(TextType textType) {
        setTextType(textType);
        return this;
    }

    /**
     * @see VoiceId
     */
    public void setVoiceId(String voiceId) {
        this.voiceId = voiceId;
    }

    /**
     * @see VoiceId
     */
    public String getVoiceId() {
        return this.voiceId;
    }

    /**
     * @see VoiceId
     */
    public void setVoiceId(VoiceId voiceId) {
        this.voiceId = voiceId.toString();
    }

    /**
     * @return Returns a reference to this object so that method calls can be chained together.
     * @see VoiceId
     */
    public SynthesizeSpeechPresignRequest withVoiceId(String voiceId) {
        setVoiceId(voiceId);
        return this;
    }

    /**
     * @return Returns a reference to this object so that method calls can be chained together.
     * @see VoiceId
     */
    public SynthesizeSpeechPresignRequest withVoiceId(VoiceId voiceId) {
        setVoiceId(voiceId);
        return this;
    }

    /**
     * @see SpeechMarkType
     */
    public java.util.List<String> getSpeechMarkTypes() {
        return speechMarkTypes;
    }

    /**
     * @see SpeechMarkType
     */
    public void setSpeechMarkTypes(java.util.Collection<String> speechMarkTypes) {
        if (speechMarkTypes == null) {
            this.speechMarkTypes = null;
            return;
        }

        this.speechMarkTypes = new java.util.ArrayList<String>(speechMarkTypes);
    }

    /**
     * <p> <b>NOTE:</b> This method appends the values to the existing list (if any). Use {@link
     * #setSpeechMarkTypes(Collection)} or {@link #withSpeechMarkTypes(java.util.Collection)} if you want to override the
     * existing values. </p>
     *
     * @return Returns a reference to this object so that method calls can be chained together.
     *  @see SpeechMarkType
     */
    public SynthesizeSpeechPresignRequest withSpeechMarkTypes(String... speechMarkTypes) {
        if (this.speechMarkTypes == null) {
            setSpeechMarkTypes(new java.util.ArrayList<String>(speechMarkTypes.length));
        }
        for (String ele : speechMarkTypes) {
            this.speechMarkTypes.add(ele);
        }
        return this;
    }

    /**
     * @return Returns a reference to this object so that method calls can be chained together.
     * @see SpeechMarkType
     */
    public SynthesizeSpeechPresignRequest withSpeechMarkTypes(java.util.Collection<String> speechMarkTypes) {
        setSpeechMarkTypes(speechMarkTypes);
        return this;
    }

    /**
     * <p> <b>NOTE:</b> This method appends the values to the existing list (if any). Use {@link
     * #setSpeechMarkTypes(Collection)} or {@link #withSpeechMarkTypes(java.util.Collection)} if you want to override the
     * existing values. </p>
     *
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    public SynthesizeSpeechPresignRequest withSpeechMarkTypes(SpeechMarkType... speechMarkTypes) {
        java.util.ArrayList<String> speechMarkTypesCopy = new java.util.ArrayList<String>(speechMarkTypes.length);
        for (SpeechMarkType value : speechMarkTypes) {
            speechMarkTypesCopy.add(value.toString());
        }
        if (getSpeechMarkTypes() == null) {
            setSpeechMarkTypes(speechMarkTypesCopy);
        } else {
            getSpeechMarkTypes().addAll(speechMarkTypesCopy);
        }
        return this;
    }
}
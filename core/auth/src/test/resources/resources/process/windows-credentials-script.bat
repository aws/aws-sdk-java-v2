@ECHO OFF
SET input=%1
ECHO {
ECHO "Version": 1,
ECHO "AccessKeyId": "%1",
ECHO "SecretAccessKey": "%2"
IF NOT "%3"=="" (
    ECHO ,
    IF "%3"=="RANDOM_TOKEN" (
        ECHO "SessionToken": "%RANDOM%"
    ) ELSE (
        ECHO "SessionToken": "%3"
    )
)
IF NOT "%4"=="" (
    ECHO ,
    ECHO "Expiration": "%4"
)
echo }
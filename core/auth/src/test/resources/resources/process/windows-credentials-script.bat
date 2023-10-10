@ECHO OFF

SET "AccessKeyId=%~1"
SET "SecretAccessKey=%~2"
SET "SessionToken=%~3"
SET "Expiration=%~4"

IF NOT DEFINED AccessKeyId SET "AccessKeyId=%ACCESS_KEY_ID%"
IF NOT DEFINED SecretAccessKey SET "SecretAccessKey=%SECRET_ACCESS_KEY%"
IF NOT DEFINED SessionToken SET "SessionToken=%SESSION_TOKEN%"
IF NOT DEFINED Expiration SET "Expiration=%EXPIRATION%"

ECHO {
ECHO "Version": 1,
ECHO "AccessKeyId": "%AccessKeyId%",
ECHO "SecretAccessKey": "%SecretAccessKey%"

IF NOT "%SessionToken%"=="" (
    ECHO ,
    IF "%SessionToken%"=="RANDOM_TOKEN" (
        ECHO "SessionToken": "%RANDOM%"
    ) ELSE (
        ECHO "SessionToken": "%SessionToken%"
    )
)

IF NOT "%Expiration%"=="" (
    ECHO ,
    ECHO "Expiration": "%Expiration%"
)

ECHO }

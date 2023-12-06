@ECHO OFF
ECHO {
ECHO "Version": 1,
ECHO "AccessKeyId": "%1",
SHIFT
ECHO "SecretAccessKey": "%1"
SHIFT

:LOOP
    IF "%1"=="" (
        GOTO :EXITLOOP
    )
    IF "%2"=="" (
        echo "Expected value for param %1!"
        exit /b 1
    )
    ECHO ,
    CALL :PARSE_ARGS %1 %2
    SHIFT
    SHIFT
GOTO :LOOP

:EXITLOOP

ECHO }

GOTO:EOF

:PARSE_ARGS
SET prefix=%1
SET param=%2
IF "%prefix%"=="token" (
    IF "%3"=="RANDOM_TOKEN" (
        ECHO "SessionToken": "%RANDOM%"
    ) ELSE (
        ECHO "SessionToken": "%param%"
    )
) ELSE IF "%prefix%"=="exp" (
    ECHO "Expiration": "%param%"
) ELSE IF "%prefix%"=="credscope" (
    ECHO "CredentialScope": "%param%"
) ELSE (
    ECHO "%prefix%": "%param%"
)
EXIT /B
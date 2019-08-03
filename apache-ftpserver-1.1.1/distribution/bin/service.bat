@echo off

REM Licensed to the Apache Software Foundation (ASF) under one
REM or more contributor license agreements.  See the NOTICE file
REM distributed with this work for additional information
REM regarding copyright ownership.  The ASF licenses this file
REM to you under the Apache License, Version 2.0 (the
REM "License"); you may not use this file except in compliance
REM with the License.  You may obtain a copy of the License at
REM
REM  http://www.apache.org/licenses/LICENSE-2.0
REM
REM Unless required by applicable law or agreed to in writing,
REM software distributed under the License is distributed on an
REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
REM KIND, either express or implied.  See the License for the
REM specific language governing permissions and limitations
REM under the License.


if "%OS%" == "Windows_NT" setlocal
rem ---------------------------------------------------------------------------
rem NT Service Install/Uninstall script
rem
rem Options
rem install                Install the service using ftpd as service name.
rem                        Service is installed using default settings.
rem remove                 Remove the service from the System.
rem
rem name        (optional) If the second argument is present it is considered
rem                        to be new service name                                           
rem
rem $Id: service.bat 467182 2006-10-23 23:47:06Z markt $
rem ---------------------------------------------------------------------------

rem Guess CATALINA_HOME if not defined
set CURRENT_DIR=%cd%
if not "%FTPD_HOME%" == "" goto gotHome
set FTPD_HOME=%cd%
if exist "%FTPD_HOME%\bin\ftpd.exe" goto okHome
rem CD to the upper dir
cd ..
set FTPD_HOME=%cd%
:gotHome
if exist "%FTPD_HOME%\bin\ftpd.exe" goto okHome
echo The ftpd.exe was not found...
echo The FTPD_HOME environment variable is not defined correctly.
echo This environment variable is needed to run this program
goto end
rem Make sure prerequisite environment variables are set
if not "%JAVA_HOME%" == "" goto okHome
echo The JAVA_HOME environment variable is not defined
echo This environment variable is needed to run this program
goto end 
:okHome

set EXECUTABLE=%FTPD_HOME%\bin\ftpd.exe

rem Set default Service name
set SERVICE_NAME=ftpd
set PR_DISPLAYNAME=Apache FtpServer

if "%1" == "" goto displayUsage
if "%2" == "" goto setServiceName
set SERVICE_NAME=%2
set PR_DISPLAYNAME=Apache FtpServer %2
:setServiceName
if %1 == install goto doInstall
if %1 == remove goto doRemove
if %1 == uninstall goto doRemove
echo Unknown parameter "%1"
:displayUsage
echo.
echo Usage: service.bat install/remove [service_name]
goto end

:doRemove
rem Remove the service
"%EXECUTABLE%" //DS//%SERVICE_NAME%
echo The service '%SERVICE_NAME%' has been removed
goto end

:doInstall
rem Install the service
echo Installing the service '%SERVICE_NAME%' ...
echo Using FTPD_HOME:        %FTPD_HOME%
echo Using JAVA_HOME:        %JAVA_HOME%

rem ----- Create CLASSPATH --------------------------------------------
set FTPD_CLASSPATH=%FTPD_HOME%\common\classes
cd /d "%FTPD_HOME%\common\lib"
for %%i in ("*.jar") do call "%FTPD_HOME%\bin\appendcp.bat" "%FTPD_HOME%\common\lib\%%i"
cd /d %FTPD_HOME%

rem Use the environment variables as an example
rem Each command line option is prefixed with PR_

set FTPD_LOGPATH=%FTPD_HOME%\res\log

set PR_DESCRIPTION=Apache FtpServer (http://mina.apache.org/ftpserver/)
set PR_INSTALL=%EXECUTABLE%
set PR_LOGPATH=%FTPD_LOGPATH%
set PR_CLASSPATH=%FTPD_CLASSPATH%
rem Set the server jvm from JAVA_HOME
set PR_JVM=%JAVA_HOME%\jre\bin\server\jvm.dll
if exist "%PR_JVM%" goto foundJvm
rem Set the client jvm from JAVA_HOME
set PR_JVM=%JAVA_HOME%\jre\bin\client\jvm.dll
if exist "%PR_JVM%" goto foundJvm
set PR_JVM=auto
:foundJvm
echo Using JVM:              %PR_JVM%

rem Supply additional command line params as start params

set CMD_LINE_ARGS=start
echo %CMD_LINE_ARGS%
shift
shift
:buildArgs
if %1a==a goto endInit
set CMD_LINE_ARGS=%CMD_LINE_ARGS%;%1
echo %CMD_LINE_ARGS%
shift
goto buildArgs

:endInit
echo %CMD_LINE_ARGS%

"%EXECUTABLE%" //IS//%SERVICE_NAME% --StartClass org.apache.ftpserver.main.Daemon --StartParams %CMD_LINE_ARGS% --StartPath "%FTPD_HOME%" --StopClass org.apache.ftpserver.main.Daemon --StopParams stop 
if not errorlevel 1 goto installed
echo Failed installing '%SERVICE_NAME%' service
goto end
:installed
rem Clear the environment variables. They are not needed any more.
set PR_DISPLAYNAME=
set PR_DESCRIPTION=
set PR_INSTALL=
set PR_LOGPATH=
set PR_CLASSPATH=
set PR_JVM=
rem Set extra parameters
"%EXECUTABLE%" //US//%SERVICE_NAME% --StartMode jvm --StopMode jvm --StdOutput "%FTPD_LOGPATH%\out.log" --StdError "%FTPD_LOGPATH%\error.log"
rem More extra parameters
set PR_LOGPATH=%FTPD_HOME%\logs
set PR_STDOUTPUT=auto
set PR_STDERROR=auto
REM "%EXECUTABLE%" //US//%SERVICE_NAME% ++JvmOptions "-Djava.io.tmpdir=%CATALINA_BASE%\temp" --JvmMs 128 --JvmMx 256
echo The service '%SERVICE_NAME%' has been installed.

:end
cd %CURRENT_DIR%

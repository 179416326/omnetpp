@echo off              
: &*$@ Windows sets errorlevel if variable doesn't exist
if defined INCLUDE set INCLUDE=
if defined LIB set LIB=
set PATH=%SystemRoot%\system32;%SystemRoot%;%SystemRoot%\System32\Wbem

:XXX for stock .NET 2003 installation, use:
:XXX "%VS71COMNTOOLS%vsvars32.bat"
call D:\home\tools\vc71\vcvars32.bat
call d:\home\tools\setenv.cmd
PATH %~dp0\bin;%PATH%


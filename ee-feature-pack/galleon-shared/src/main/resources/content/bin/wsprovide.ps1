##################################################################
#                                                               ##
#    wsprovide tool script for Windows                          ##
#                                                               ##
##################################################################
$scripts = (Get-ChildItem $MyInvocation.MyCommand.Path).Directory.FullName;
. "$scripts\common.ps1"

$JAVA_OPTS = Get-Java-Opts

# Sample JPDA settings for remote socket debugging
#$JAVA_OPTS+="-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=y"

$JAVA_OPTS+="-Dprogram.name=wsprovide.ps1"

$PROG_ARGS = Get-Java-Arguments -entryModule "org.jboss.ws.tools.wsprovide" -logFileProperties $null -serverOpts $ARGS
Display-Environment $global:FINAL_JAVA_OPTS
& $JAVA $PROG_ARGS

Env-Clean-Up
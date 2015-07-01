# dynatrace-vertx-aspect
Among various other things the Vert.x Framework offers the possibility to receive HTTP requests and handle them asynchronously. The dynaTrace Sensors are currently not able to recognize such requests out of the box.

Using AspectJ it is however possible to inject the necessary calls in order to teach the dynaTrace Agent for Java how to trace these transactions.

In order to activate this library within your JVM alongside with the dynaTrace Agent, the AspectJ runtime weaver must be attached.

set DT_HOME=C:\Program Files\dynaTrace\dynaTrace 6.2
set ASPECTJ_HOME=C:\devtools\aspectj1.8
set CLASSPATH=%CLASSPATH%;%DT_HOME%\agent\com.dynatrace.adk.jar;C:\vert-x-aspects.jar;%ASPECTJ_HOME%\lib\aspectjrt.jar
set JVM_OPTS=-javaagent:%ASPECTJ_HOME%\lib\aspectjweaver.jar -agentpath:"%DT_HOME%\agent\lib64\dtagent.dll"=name=vertx

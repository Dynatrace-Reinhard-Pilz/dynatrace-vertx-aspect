# dynatrace-vertx-aspect
Among various other things the [Vert.x Framework](http://www.vertx.io) offers the possibility to receive HTTP requests and handle them asynchronously. The [dynaTrace](http://www.dynatrace.com) Sensors are currently not able to recognize such requests out of the box.

Using [AspectJ](https://eclipse.org/aspectj) it is however possible to inject the necessary calls in order to teach the dynaTrace Agent for Java how to trace these transactions.
The purpose of AspectJ is to inject additional code into existing binaries. Although the general use cases in mind were different ones, it can also be utilized to add calls to the dynaTrace Java ADK into existing binaries, which this project takes advantage of.

In order to activate this library within your JVM alongside with the dynaTrace Agent, the AspectJ runtime weaver must be attached. Required herefore is having `aspectjrt.jar` (located within the installation folder of AspectJ), `com.dynatrace.adk.jar` (included within this project), `vert-x-aspects.jar` (the binaries produced from this project) and
[javax.servlet-api-3.0.1.jar](http://mvnrepository.com/artifact/javax.servlet/javax.servlet-api/3.0.1) within your class path.
Furthermore the AspectJ Weaver Agent (`aspectjweaver.jar` located within the installation folder of AspectJ) needs to get attached to the JVM arguments alongside with the dynaTrace Agent (`dtagent.dll` or `libdtagent.so` located within the installation folder of the dynaTrace Agent).

Here is an example for configuring the JVM Arguments within a Windows Batch File:
```
set DT_HOME=C:\Program Files\dynaTrace\dynaTrace 6.2
set CLASSPATH=%CLASSPATH%;%DT_HOME%\agent\com.dynatrace.adk.jar;%VERTX_ASPECT_HOME%\vert-x-aspects.jar;%VERTX_ASPECT_HOME%\javax.servlet-api-3.0.1.jar;%ASPECTJ_HOME%\lib\aspectjrt.jar
set JVM_OPTS=-javaagent:%ASPECTJ_HOME%\lib\aspectjweaver.jar -agentpath:"%DT_HOME%\agent\lib64\dtagent.dll"=name=vertx
```

With this aspect injected the dynaTrace Servlet Sensor will pick up HTTP Requests sent to a Vert.x JVM. Furthermore the most common use cases for asynchronous request handling should be covered.

The current implementation is compatible with Vert.x 2.5.1.

# ONOS-Apps

### Spec
package | version
------- | -------
onos | 2.0.0

### Required Apps
* org.onosproject.openflow
```shell
karaf@root > app activate org.onosproject.openflow
```

### Usage
1. compile

	```shell
	cd ~/ONOS-Apps/MACLearning
	mvn clean install -DskipTests
	```
2. install it to onos controller

	```shell
	~/onos-2.0.0/bin/onos-app localhost install ~/ONOS-Apps/MACLearning/target/maclearning-2.0.0.oar
	```
3. activate app

	```shell
	karaf@root > app activate org.jaja.maclearning
	```
4. check log

	you can use either
	```shell
	karaf@root > log:display | tail
	```
	or
	```shell
	karaf@root > log:tail jaja.AppComponent
	```
	> The name of log file is as same as the name of the class
	> ```java 
	> public class AppComponent { 
	>   ...
	>   private final Logger log = LoggerFactory.getLogger(getClass())
	>   ...
	> }
	> ```

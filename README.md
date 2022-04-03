# spring4shell intro

## build

set JAVA_HOME=C:\Users\a.vergnaud\dev\jdk-11

C:\Users\a.vergnaud\dev\apache-maven-3.6.3\bin\mvn clean package

## deploy

* C:\Users\a.vergnaud\dev\apache-tomcat-9.0.60
* or : deploy on pi

## run

http://localhost:8080/spring4shell-intro/greeting

http://192.168.0.31:8080/spring4shell-intro/greeting

## exploit

C:\Users\a.vergnaud\AppData\Local\Programs\Python\Python39\python.exe exp.py --url http://localhost:8080/spring4shell-intro/greeting

C:\Users\a.vergnaud\AppData\Local\Programs\Python\Python39\python.exe exploit.py --url http://localhost:8080/spring4shell-intro/greeting

C:\Users\a.vergnaud\AppData\Local\Programs\Python\Python39\python.exe exp.py --url http://192.168.0.31:8080/spring4shell-intro/greeting

C:\Users\a.vergnaud\AppData\Local\Programs\Python\Python39\python.exe exploit.py --url http://192.168.0.31:8080/spring4shell-intro/greeting

## patch

https://www.lunasec.io/docs/blog/spring-rce-vulnerabilities/

## mitigate

https://www.lunasec.io/docs/blog/spring-rce-vulnerabilities/

https://www.praetorian.com/blog/spring-core-jdk9-rce/

wget https://dlcdn.apache.org/tomcat/tomcat-9/v9.0.60/bin/apache-tomcat-9.0.60.tar.gz

sudo bash

echo 'export CATALINA_HOME='/opt/apache-tomcat-9.0.60'' >> /etc/environment
echo 'export JAVA_HOME='/usr/lib/jvm/java-11-openjdk-armhf'' >> /etc/environment
echo 'export JRE_HOME='/usr/lib/jvm/java-11-openjdk-armhf'' >> /etc/environment
source ~/.bashrc

http://192.168.0.31:8080/spring4shell-intro/greeting

C:\Users\a.vergnaud\AppData\Local\Programs\Python\Python39\python.exe C:/Users/a.vergnaud/dev/spring4shell/spring4shell-intro/exp.py --url http://192.168.0.31:8080/spring4shell-intro/greeting
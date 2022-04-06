# spring4shell intro

Spring4Shell (ou SpringShell) est une faille de sécurité importante, révélée le 29 mars, patchée le 31.

Il s'agit de la CVE-2022-22965, qui permet d'exécuter du code arbitraire sur le serveur (Remote Code Execution).
* [https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-22965](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-22965)

Les applications sont vulnérables si elles utilisent :
* les dépendences spring-webmvc ou spring-webflux
* du framework spring dans toutes les versions strictement inférieures aux 5.2.20 et 5.3.18 (Spring Boot 2.5.12 et 2.6.6)
* packagées en .war
* déployées sur Tomcat dans toutes les versions strictement inférieures aux 8.5.78, 9.0.62, 10.0.20
* qui s'exécutent avec Java en version 9 ou supérieure

Les solutions de contournement ou de patch sont décrites ici :
* [https://spring.io/blog/2022/03/31/spring-framework-rce-early-announcement](https://spring.io/blog/2022/03/31/spring-framework-rce-early-announcement)

L'application "getting started, Handling Form Submission" est vulnérable : [https://spring.io/guides/gs/handling-form-submission/](https://spring.io/guides/gs/handling-form-submission/)

## build

set JAVA_HOME=C:\Users\a.vergnaud\dev\jdk-11

C:\Users\a.vergnaud\dev\apache-maven-3.6.3\bin\mvn clean package

## deploy

C:\Users\a.vergnaud\dev\apache-tomcat-9.0.60

## run

http://localhost:8080/spring4shell-intro/greeting

http://192.168.0.31:8080/spring4shell-intro/greeting

## exploit

curl

C:\Users\a.vergnaud\AppData\Local\Programs\Python\Python39\python.exe exp.py --url http://localhost:8080/spring4shell-intro/greeting

C:\Users\a.vergnaud\AppData\Local\Programs\Python\Python39\python.exe exploit.py --url http://localhost:8080/spring4shell-intro/greeting

C:\Users\a.vergnaud\AppData\Local\Programs\Python\Python39\python.exe exp.py --url http://192.168.0.31:8080/spring4shell-intro/greeting

C:\Users\a.vergnaud\AppData\Local\Programs\Python\Python39\python.exe exploit.py --url http://192.168.0.31:8080/spring4shell-intro/greeting

## patch

* Upgrader en spring-core 5.2.20 (Spring Boot 2.5.12)

ou

* Upgrader en spring-core 5.3.18 (Spring Boot 2.6.6)

ou

* Upgrader Tomccat en 8.5.78, 9.0.62 ou 10.0.20

## work-around

[https://spring.io/blog/2022/03/31/spring-framework-rce-early-announcement#suggested-workarounds](https://spring.io/blog/2022/03/31/spring-framework-rce-early-announcement#suggested-workarounds)

* Utiliser Spring AOP
* Récupérer l'instance de WebDataBinder
* Lui interdire de _bind_ les champs nommé "class"

# Explication

## setup

![stack](./doc/spring4shell_setup.drawio.png?raw=true)

## call stack

GreetingController.java :
```
@PostMapping("/greeting")
public String greetingSubmit(@ModelAttribute Greeting greeting, Model model) {
    model.addAttribute("greeting", greeting);
    return "result";
}
```

Le framework Spring a la responsabilité d'instancier `greeting` à partir du body de la requête HTTP :
![wireshark capture](./doc/POST_greeting_capture_0.PNG?raw=true)
![remote debug](./doc/POST_greeting_remote_debug.png?raw=true)

Voilà une pile d'appels exécutée par l'application, au moment où elle valorise `greeting` à partir du HTTP POST data :
![spring4shell_CL_AccessLogValve_1](./doc/spring4shell_CL_AccessLogValve_1.drawio.png?raw=true)

(1) A l'exécution, on remarque que l'objet BeanWrapperImpl encapsule l'instance `greeting`, et référence 3 `propertyDescriptors` :
* "id"
* "content"
* et "class" !

On voit que Spring conserve une référence à l'objet Class de l'instance `greeting`
![spring4shell_BeanWrapperImpl_2](./doc/spring4shell_BeanWrapperImpl_2.PNG?raw=true)

(2) Spring peut aussi d'accéder à des attributs imbriqués. Cette fonctionnalité est founrnie par la classe `AbstractNestablePropertyAccessor`
* [source](https://github.com/spring-projects/spring-framework/blob/8baf404893037951ac29393a41d40af4fa11775b/spring-beans/src/main/java/org/springframework/beans/AbstractNestablePropertyAccessor.java#L622)
* [javadoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/beans/AbstractNestablePropertyAccessor.html)





C:\Users\a.vergnaud\AppData\Local\Programs\Python\Python39\python.exe C:/Users/a.vergnaud/dev/spring4shell/spring4shell-intro/exp.py --url http://192.168.0.31:8080/spring4shell-intro/greeting
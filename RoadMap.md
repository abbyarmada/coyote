# Road Map

This is a list of capabilities currently planned for the Coyote Data Exchange Toolkit. Their implementation timeline is based on developer availability and client projects. If we have a client who wants Salesforce integration today, then that will be worked on next. Otherwise the order here is roughly the current priority.

Coyote IoT is a separate project targeting Makers and Prototypers. It leverages capabilities of the core platform to enable more sophisticated solutions with less code, smaller footprint and reduced resource requirements.

### Core Functionality or Module
The primary deciding factor for locating a feature is its applicability. If any process could make use of it, it will probably go in the core (CoyoteDX) project. If it is specific to a technology or protocol which is not widely used, then it will probably go in a module to keep things small and deployment options open.

The secondary consideration for placing something in a separate module is its dependencies. If an external library is required, then it will go in a separate project / module. This gives the user the choice of using the third party library. 

## RunJob Tasks
This task executes a specific Transfer Job if its condition is met. This provided the capability to chain together complex orchestration of Jobs.

#### Features
* Jobs can be run in serial or parallel, each with their own set of data
* Processing results from Jobs can be accessed in other Jobs via Job Contexts. 

## CoyoteJS - JavaScript Processing
There is no need to develop custom Java component to implement your business logic. Transformers can be written in JavaScript to perform just about anything you need.

This bridges the gap between configuration and custom development with simple scripts to handle more than the basic components. Of course, you can create complex scripts, but you have complete control over when to make the jump into developing your own custom components in Java, Scala or other compiler, JVM compatible language. 

Maybe you just want to prototype an idea and test the waters before setting up a development project. It's possible all you need is a slight tweak to an existing job. In any case, running a scripted component may be the best approach to your customization needs.

#### Features
* centralized script management
* ability to include other libraries
* load scripts from network or file system 


## CoyoteGS - Groovy Scripting
If JavaScript is not for you, it is possible to implement your custom business logic in Groovy. While you can create compiled components in Groovy, you may want to prototype functionality in in scripts first and move to compiled bytecode later.


## CoyoteWQ - Worker Queues
A set of application components which enable setting up horizontally scalable solutions that execute jobs in the cloud or your data center. Set up any number of workers to read job requests from a queue and perform data transfer jobs remotely.

Not just for massive integrations, Worker Queues can implement nearly any processing asynchronously. If an event comes in one of the queues or topics, a Coyote worker can execute a specific job for that event. For example, if a customer's mobile device sends a geofencing event to your workers, they can trigger almost any processing you configure. This means customers with your mobile application can send you an event when they enter your store and a worker can process that event and send them a coupon to trigger an impulse sale. The possibilities are endless.

#### Features
* Implement your own IFTTT servers


## CoyoteSF Salesforce
Just like the ServiceNow support (CoyoteSN) this set of components allows a variety of integration capabilities through the Coyote Toolkit. Maybe you want to combine Salesforce data into some of your Transfer Jobs; this provides the ability to efficiently access any and all your Salesforce data.


# Coyote IoT
This is a project for the Single Board Computer (SBC) platform which provides access to Serial communications and GPIO (General Purpose IO) pins. It abstracts the native libraries of some popular project (RxTx, Pi4J, et. al.) into an API which will make your data transfer jobs more portable.

For example, CIOT makes it easier to develop and test data transfer jobs on your Windows and Mac, then run those same jobs on your SBC platform. If one platform uses one library and another platform uses another, your jobs should still run.

Demonstrations and Proofs of Concept will be centered around Home Automation devices as they are widely available, portable and provide value to IoT developers. The value proposition is that a consumer can have complete control of their data and help alleviate data security and privacy concerns.

The architecture involves a Data Transfer Service (in contrast to a Job) which listens for Z-Wave events and metrics with a device connected via serial communications. As events are passed to filters, validators and transformers, these events can then trigger other jobs or write specific data records to components connected to other devices. A web service can give a web page or mobile app access to the current state of any Z-Wave device connected to the mesh. Multiple readers can be running in parallel to support multiple technologies and protocols.

#### Features
* Specific devices can be supported with small device modules (Readers and Writers) enabling hardware to be connected using their native protocols and models
* All sub-projects can be used in the Coyote Data Transfer toolkit (CoyoteDX) and those modules used with CIOT. Modules are interchangeable.
* Proprietary modules can be written for the platform and not distributed via Open Source. 


## CoyoteMQTT MQ Telemetry Transfer
This module is a simple, lightweight MQTT client with readers and writers for the MQTT protocol. This allows you to prototype IoT solutions using simple SBCs running minimal operating systems and potentially no file system access. Because it uses the same API as all the other Coyote components, this can be used in all other Coyote Jobs.


## CoyoteSNMP SNMP Support
While SNMP is a bit long in the tooth, it is still being used as a network management protocol. This allows the creation of agents and managers running on the SBC platform. Again, this can be used in any Coyote Job as it uses the same API as all other Coyote components.


## CoyoteNMEA NMEA 0183 
Listen on serial ports for NMEA sentences from GPS, compass, transponder, sonar and nearly any other device which supports National Marine Electronics Association (NMEA) sentences. 

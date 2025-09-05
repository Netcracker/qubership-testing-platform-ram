# Qubership Testing Platform RAM Service

## System Requirements
The system consists of two services: ATP RAM and ATP RAM Report Receiver.

|              |          ATP RAM           |  ATP RAM Report Receiver   |
|--------------|:--------------------------:|:--------------------------:|
|              | **Requests** / **Limits** | **Requests** / **Limits** |
| **CPU**      |        100m / 200m         |        500m / 500m         |
| **RAM**      |         1Gi / 1Gi          |         1Gi / 1Gi          |
| **replicas** |             1              |             2              |

## Database System Requirements

|         |         MongoDB           |         GridFS            |
|---------|:-------------------------:|:-------------------------:|
|         | **Requests** / **Limits** | **Requests** / **Limits** |
| **CPU** |        100m / 100m        |        100m / 100m        |
| **RAM** |         1Gi / 1Gi         |       512Mi / 512Mi       |

## Description

Qubership Testing Platform (QSTP) is a system intended to analyze the results of the execution of test scenarios and to publish the results for the project team.

Run and Analysis Management (RAM) is a tool that allows collecting information on AT runs on different projects in a single database, helps in analyzing the results and allows obtaining graphs of statistical data.

## Tasks Covered

Users running a large number of tests need to collect information about the runs and to analyze it.

To solve this problem, RAM Service was implemented, which covers the following tasks:

- data storage;
- analysis of the results;
- viewing information about steps (status, message with logged information, screenshots, etc.);
- viewing statistics;
- results reporting via email (both automatic upon completion of tests and manual);
- comparing two or more runs, etc.

## Entities

**Project**

It is an entity in the QSTP system intended for managing testing of a project. In QSTP, the projects are independent of each other and can be developed in parallel.

**Test Plan**

This entity stores Test Case(s), settings necessary for its execution and Test Run(s) execution results.

Test Plan is intended for preparing and organizing functional tests within single/multiple project versions.

**Execution Request**

This entity allows the user to execute a set of automated and semi-automated Test Cases (TCs). Possible values for execution statuses are described below.

**Test Case**

The Test Case entity stores the testing script.

**Test Suite**

This entity is intended to group Test Case(s) by an attribute.

**Test Run**

The Test Run entity is created when a Test Case is added to Execution Request. Test Run (TR) displays the Test Case execution status in the Execution Request. The possible values for testing and execution statuses are described below.

**Log Record**

The Log Record entity is a child object of Test Run that stores information on the execution of a Test Case step. The possible values for testing statuses are described below.

**AKB Record**

Analysis Knowledge Base Record (AKB Record) is a tool for detecting errors in Log Records based on regular expressions.

By means of AKB Records, a user can create a list of regular expressions for the subsequent search of errors in Test Runs logs. AKB Records are valid within a current project.

**Global AKB Record**

Analysis Knowledge Base Record (AKB Record) is a tool for detecting errors in Log Records (LRs) based on regular expressions.

By means of AKB Records, a user can create a list of regular expressions for the subsequent search of errors in Test Runs logs. Global AKB Records are valid within a system, thus the same Global AKB Record can be used within several projects.

**Defect**

It is an entity designed to register issues detected at the Test Runs/Log Records analysis.

**Labels**

It is an entity in the QSTP system that allows grouping/selecting Execution Requests based on criteria, which are not available in QSTP.

**Manual Root Cause**

This entity is used to manually specify the Failure Reason for Test Run on the Test Run Tree View, Execution Request Tree View pages.

**Notifications**

After Execution Request is completed, the user(s) can receive a notification reporting about ER execution results.

## Local build

In IntelliJ IDEA, one can select 'GitHub' Profile in Maven Settings menu on the right, then expand Lifecycle dropdown of qubership-atp-ram-aggregator module, then select 'clean' and 'install' options and click 'Run Maven Build' green arrow button on the top.

Or, one can execute the command:
```bash
mvn -P github clean install
```

## How to start Backend

### Mongo params for configuring connections (optional step)

Set default value in application.properties for:
* **max.connection.idle.time** - the maximum idle time for a pooled connection im ms (_default: 0ms_)
* **min.connections.per.host** - the minimum size of the connection pool per host (_default: 100_)
* **connections.per.host** - the maximum size of the connection pool per host (_default: 2000_)

## Build project

Go to Maven - 'qubership-atp-ram-app' module
Execute:
**mvn clean package** (in some case with flag `-DskipTests`)
as a result, the **qubership-atp-ram\web** folder will be filled

## Create new Configuration

1. Go to Run menu and click Edit Configuration
2. Add new Application configuration
3. Set parameters:
   - Name = `Local RAM dev01`, for example
   - Run on: `Local machine`
   - JDK/JRE: Java 8 SDK of `qubership-atp-ram-app' module`
   - Module for class path: `qubership-atp-ram-app`
   - Main class: `org.qubership.atp.ram.Main`
   - Program arguments: left empty
   - Working directory: path to your atp-ram project on local machine, `C:\atp-ram` for example
4. Add the following parameters in VM options - click Modify Options and select "Add VM Options":

dev04 (kuber):
-DSPRING_PROFILES=default  with security
```text
-Dspring.config.location=qubership-atp-ram/qubership-atp-ram-app/src/main/config/application.properties
-DSERVICE_NAME=atp-ram-local
-Dbase.url=localhost
-Dspring.resources.static-locations=file:./qubership-atp-ram/web/
-Datp-ram.web.root-page=qubership-atp-ram/web/index.html
-DfixedRate.tr.in.milliseconds=${SCHEDULER_RATE_TR:600000}
-DfixedRate.er.in.milliseconds=${SCHEDULER_RATE_ER:600000}
-Dmin.connections.per.host=5
-Dmongodb.database=some_db
-Dmongodb.host=127.0.0.1
-Dmongodb.port=31143
-Dmongodb.user=some_user
-Dmongodb.password=some_pass
-Dgridfs.database=some_db
-Dgridfs.host=127.0.0.1
-Dgridfs.port=31143
-Dgridfs.user=some_user
-Dgridfs.password=some_pass
-Dmail.smtp.host=iplanet.some-domain.com
-Dmail.smtp.ssl.enable=false
-Dkafka.project.event.enable=${KAFKA_PROJECT_EVENT_ENABLE:false}
-Dkafka.test.plan.event.enable=${KAFKA_TEST_PLAN_EVENT_ENABLE:false}
-Dkeycloak.auth-server-url=https://atp-keycloak-service-address/auth
-Dkeycloak.realm=atp2
-Dkeycloak.resource=ram
-Dkeycloak.credentials.secret=b33ede0f-2834-4f43-aaec-ee8515e21f61
-Datp-auth.project_info_endpoint=/api/v1/users/projects
-Dcatalogue.url=http://atp-catalogue-service-address
-Dfeign.atp.catalogue.url=https://test-atp-catalogue-service-address
-Dfeign.atp.datasets.url=https://test-atp-dataset-service-address
-Dfeign.atp.environments.url=http://test-atp-environments-service-address
-Dfeign.atp.orchestrator.url=http://test-atp-orchestrator-service-address
-Dfeign.atp.mailsender.url=http://test-atp-mail-sender-service-address
-DKEYCLOAK_ENABLED=true
-Dspring.profiles.active=default
-Dspring.cloud.consul.config.enabled=false
-Datp.public.gateway.url=http://test-atp-public-gateway-service-address
-Datp.internal.gateway.url=http://test-atp-internal-gateway-service-address
-Deureka.client.serviceUrl.defaultZone=http://atp-registry-service-service-address:8761
-Deureka.client.enabled=true
-Deureka.client.fetchRegistry=true
```

For local work with feign clients (for every needed services):
In lens create new resource (Network->Ingress)
copy basic and change these arguments:

name: atp-ram22
.....
```yaml
spec:
    ingressClassName: nginx
    rules:
       - host: test-atp-ram-service-address
         http:
            paths:
              - path: /
                pathType: Prefix
                backend:
                    service:
                        name: atp-ram
                        port:
                            number: 8080
```

-DSPRING_PROFILES=disable-security  without security
```text
-Dspring.config.location=qubership-atp-ram-app/src/main/config/application.properties
-Dbase.url=localhost
-Dspring.resources.static-locations=file:./qubership-atp-ram/web/
-Datp-ram.web.root-page=qubership-atp-ram/web/index.html
-DfixedRate.tr.in.milliseconds=${SCHEDULER_RATE_TR:600000}
-DfixedRate.er.in.milliseconds=${SCHEDULER_RATE_ER:300000}
-Dmongodb.database=dev01_ram
-Dmongodb.host=127.0.0.1
-Dmongodb.port=31143
-Dmongodb.user=dev01_ram
-Dmongodb.password=dev01_ram
-Dgridfs.database=dev01_gridfs
-Dgridfs.host=dev-atp-cloud.some-domain.com
-Dgridfs.port=32763
-Dgridfs.user=dev01_gridfs
-Dgridfs.password=dev01_gridfs
-Dcatalogue.url=https://atp-catalogue-service-address
-Dmail.smtp.host=iplanet.some-domain.com
-Dmail.smtp.ssl.enable=false
-Dkafka.project.event.enable=${KAFKA_PROJECT_EVENT_ENABLE:false}
-Dkeycloak.auth-server-url=https://atp-keycloak-service-address/auth
-Dkeycloak.realm=atp2
-Dkeycloak.resource=ram
-Dkeycloak.credentials.secret=b33ede0f-2834-4f43-aaec-ee8515e21f61
-Datp-auth.project_info_endpoint=/api/v1/projects
-Dfeign.atp.catalogue.url=https://atp-catalogue-service-address
-Dfeign.atp.datasets.url=https://atp-dataset-service-address
-Dfeign.atp.environments.url=http://atp-environments-service-address
-Dfeign.atp.orchestrator.url=http://atp-orchestrator-service-address
-Dfeign.atp.mailsender.url=http://atp-mail-sender-service-address
-DKEYCLOAK_ENABLED=false
-DSPRING_PROFILES=disable-security
-Dspring.cloud.consul.config.enabled=false
-Dkafka.mails.responses.enable=false
-Dkafka.project.event.enable=false
-Dkafka.rca.enable=false
-Dkafka.fdr.enable=false
-Dkafka.mails.enable=false
-Dkafka.test.plan.event.enable=false
```

dev222:
```text
-Dspring.config.location=qubership-atp-ram/qubership-atp-ram-app/src/main/config/application.properties
-Dbase.url=localhost
-Dspring.resources.static-locations=file:./qubership-atp-ram/web/
-Datp-ram.web.root-page=qubership-atp-ram/web/index.html
-DfixedRate.tr.in.milliseconds=${SCHEDULER_RATE_TR:600000}
-DfixedRate.er.in.milliseconds=${SCHEDULER_RATE_ER:600000}
-Dmongodb.database=dev222_ram
-Dmongodb.host=localhost
-Dmongodb.port=27017
-Dmongodb.user=dev222_ram
-Dmongodb.password=dev222_ram
-Dgridfs.database=dev222_ram
-Dgridfs.host=localhost
-Dgridfs.port=27017
-Dgridfs.user=dev222_ram
-Dgridfs.password=dev222_ram
-Dcatalogue.url=https://atp-catalogue-dev222.dev-atp-cloud.some-domain.com
-Dmail.smtp.host=iplanet.some-domain.com
-Dmail.smtp.ssl.enable=false
-Dkafka.project.event.enable=${KAFKA_PROJECT_EVENT_ENABLE:false}
-Dkafka.test.plan.event.enable=${KAFKA_TEST_PLAN_EVENT_ENABLE:false}
-Dkeycloak.auth-server-url=https://atp-keycloak-dev222.dev-atp-cloud.some-domain.com/auth
-Dkeycloak.realm=atp2
-Dkeycloak.resource=ram
-Dkeycloak.credentials.secret=b33ede0f-2834-4f43-aaec-ee8515e21f61
-Datp-auth.project_info_endpoint=/api/v1/users/projects
-Dfeign.atp.catalogue.url=https://atp-catalogue-dev222.dev-atp-cloud.some-domain.com
-Dfeign.atp.datasets.url=https://atp-dataset-dev222.dev-atp-cloud.some-domain.com
-Dfeign.atp.environments.url=http://atp-environments-dev222.dev-atp-cloud.some-domain.com
-Dfeign.atp.orchestrator.url=http://atp-orchestrator-dev222.dev-atp-cloud.some-domain.com
-Dfeign.atp.mailsender.url=http://atp-mail-sender-dev222.dev-atp-cloud.some-domain.com
-DKEYCLOAK_ENABLED=true
-DSPRING_PROFILES=default
-Dspring.cloud.consul.config.enabled=false
```

Use `atp1.integration.enable=false`, when need to disable integration with ATP1 (optional)

## Create mongo and gridfs users (optional step - when creation mongo and users is needed)
1. Connect to mongo (Search knowledge base for: Short. Connect to Mongo cluster in Openshift from local environment)
2. Run command: `use atpram`
3. Run command: `db.createUser({user: "ramuser",pwd: "rampass",roles: ["readWrite"]})`
4. Run command: `use gridfs`
5. Run command: `db.createUser({user: "ramuser",pwd: "rampass",roles: ["readWrite"]})`

## Connect and Port Forward to Mongo and GridFs
1. Connect to mongo:
   - for OpenShift use separate instructions
   - for Kubernetes use separate instructions
   - Note! Use the same ports for port forward as specified in VM options configuration, for example:
```text
   -Dmongodb.port=27017
   -Dgridfs.port=27017
   oc port-forward <pod> 27017:27017
```

## MailSender selection (optional step)
### The current version implements two mail senders
- mail-sender: used rest request to atp-mail-sender
- kafka: used kafka
### You can select the kafka sender type via property
```properties
kafka.enable=true
```
Used by default the mail-sender
### If you used kafka sender then you need to set the spring.kafka.bootstrap-servers and kafka.mails.topic properties
```properties
kafka.mails.topic=mails
spring.kafka.bootstrap-servers=kafka:9092
```
### Full example
```properties
kafka.enable=${KAFKA_ENABLE:false}
kafka.mails.topic=${KAFKA_MAILS_TOPIC:mails}
spring.kafka.bootstrap-servers=${KAFKA_SERVERS:kafka:9092}
```
## Turn off connection to Catalog (optional step)
To turn off connection to Catalog, please comment the following lines in class **CatalogRestClient**
```text
        try {
            m2mRestTemplate.exchange(catalogueUrl + CATALOGUE_ENDPOINT, HttpMethod.POST, entity, List.class);
        } catch (Exception e) {
            log.error("Unable update test cases in catalogue", e);
        }
```

## Run Main

Just run Main#main with args from step "Create new Configuration"

## Set authorization token

1. Open cloud catalogue
2. Open DevTools by click F12
3. Go to DevTools > Application tab > Storage > Session Storage
4. Copy values for token and user parameters
5. Go to local catalogue
6. Open DevTools by click F12
7. Go to DevTools > Application tab > Storage > Session Storage
8. Create token and user parameters and paste copied values from cloud catalogue

## How to create dump on production and restore it to local DB (optional step)
[Create and restore dump]




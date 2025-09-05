# How to start local RAM via real data

## Example for dev2 database

1. Make oc port-forward for Mongo and GridFs

2. Enable atp-auth and keycloak but disable CORS
   - keycloak.cors=false

3. Go to [keycloak Service](http://atp-keycloak-dev2.dev-atp-cloud.some-domain.com/auth) and add [URL](http://localhost:4200/*) to Valid Redirect URIs List
   - Administration Console > Clients > ram (your client in realm)

4. You must get application.properties smth like:
```properties
base.url=localhost
server.port=8001
spring.resources.static-locations=file:./web/
atp-ram.web.root-page=web/index.html
logging.level.org.qubership.atp.ram=INFO
fixedRate.tr.in.milliseconds=600000
fixedRate.er.in.milliseconds=600000
atp.ram.singleui.enabled=false
gridfs.chunk.size=6000
##==================MongoDb=====================
mongodb.database=atpram
mongodb.host=localhost
mongodb.port=27019
mongodb.user=atpram
mongodb.password=atpram
##==================GridFS======================
gridfs.database=ramgridfs
gridfs.host=localhost
gridfs.port=27018
gridfs.user=ramgridfs
gridfs.password=ramgridfs
##==================ATP=====================
atp.editor.link=https://atp.some-domain.com/common/uobject.jsp?tab=_Editor&object=
orchestrator.url=http://localhost:8084

##==================MailSettings=====================
mail.smtp.host=iplanet.some-domain.com
mail.smtp.port=25
mail.smtps.auth=false
mail.smtp.ssl.enable=false
##==================atp-auth-spring-boot-starter=====================
# spring
spring.profiles.active=${SPRING_PROFILES_ACTIVE:disable-security}
spring.main.allow-bean-definition-overriding=true
spring.cache.cache-names=projects
spring.cache.caffeine.spec=maximumSize=100, expireAfterAccess=120s
# keycloak
keycloak.enabled=true
keycloak.auth-server-url=http://atp-keycloak-dev2.dev-atp-cloud.some-domain.com/auth
keycloak.realm=atp2
keycloak.resource=ram
keycloak.credentials.secret=2bd919af-2d63-4afe-8662-be168b8e4078
keycloak.public-client=true
keycloak.ssl-required=external
keycloak.bearer-only=true
keycloak.cors=false
# atp-auth
atp-auth.project_info_endpoint=/api/v1/users/projects
atp-auth.enable-m2m=true
##================Kafka===========================
kafka.enable=${KAFKA_ENABLE:false}
kafka.mails.topic=${KAFKA_MAILS_TOPIC:mails}
kafka.fdr.producer.topic.name=${KAFKA_FDR_PRODUCER_TOPIC_NAME:fdr}
kafka.fdr.consumer.topic.name=${KAFKA_FDR_CONSUMER_TOPIC_NAME:fdr-links}
spring.kafka.bootstrap-servers=${KAFKA_SERVERS:kafka:9092}
spring.kafka.consumer.bootstrap-servers=${KAFKA_SERVERS:kafka:9092}
spring.kafka.consumer.group-id=${KAFKA_GROUP_ID:ts-fdr}
spring.kafka.consumer.value-deserializer=${KAFKA_VALUE_DESERIALIZER:org.springframework.kafka.support.serializer.JsonDeserializer}
spring.kafka.consumer.key-deserializer=${KAFKA_KEY_DESERIALIZER:org.springframework.kafka.support.serializer.JsonDeserializer}
spring.kafka.consumer.properties.spring.json.trusted.packages=${KAFKA_TRUSTED_PACKAGES:*}
spring.kafka.consumer.properties.spring.json.type.mapping=${KAFKA_TYPE_MAPPING:\
  com.some-domain.automation.receiver.entities.pojo.RamLink:org.qubership.atp.ram.tsg.model.FdrResponse}
atp.mailsender.url=
atp.mailsender.port=7070
catalogue.url=http://atp-catalogue-dev2.dev-atp-cloud.some-domain.com/
tsg.receiver.url=${TSG_RECEIVER_URL:http://tshooter-fdr-test.cloud.sdnoshm420cn.some-domain.com}

##==================Integration with Spring Cloud======================
spring.application.name=${SERVICE_NAME:atp-ram}
eureka.client.serviceUrl.defaultZone=${SERVICE_REGISTRY_URL:http://atp-registry-service:8761/eureka}
eureka.client.enabled=${EUREKA_CLIENT_ENABLED:false}
eureka.instance.preferIpAddress=true
atp1.integration.enable=false
```
## FRONTEND

5. Make sure proxy.conf.json contains
```json
  {
    "/api/**": {
      "target": "http://localhost:8001",
      "secure": false,
      "changeOrigin": true
    }
  }
```

6. routes.json
```json
{
    "loginRequired": "false",
    "idp": {
        "realm": "atp2",
        "url": "http://atp-keycloak-dev2.dev-atp-cloud.some-domain.com",
        "loginEndPoint": "/auth/realms/atp2/protocol/openid-connect/auth",
        "logoutEndPoint": "/auth/realms/atp2/protocol/openid-connect/logout"
    },
    "services": [
        {
            "name": "RAM",
            "url": "http://localhost:8001"
        }
    ]
}
```

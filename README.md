# NotificationService

This application offers a graphQL websocket based API which has a subscription functionality to receive 
notifications in real time from a Kafka topic. Notifications are sent by another application.

This app also makes use of reactive redis and redis pub/sub, and together with the NotificationService app is a proof of concept to solve the following problem:
   1. The NotificationService is scaled in a cloud environment, having multiple instances.
   2. An end user opens a subscription (from Postman or browser/FE app) 
      to a single instance of NotificationService.
   3. A message is received on the kafka topic, but is read by another app instance.
   4. The message disappears from the Kafka topic, but the user never sees it despite having 
      an open subscription.

Redis pub/sub acts as a distributed cache - every instance of NotificationService will push the message 
to redis after reading it from the Kafka topic, and redis will serve it only to the specific NotificationService 
instance which has the correct open subscription (based on user id). 

If a notification is read from Kafka but the owning user doesn't have an open websocket subscription, it goes in the
DB and is marked as unread, and there is another graphQL endpoint that servers all unread notifications.

This app is securing by spring security via the OAuth 2.0 protocol, 
and uses Keycloak as identity provider / auth server.

Dependencies (Keycloak, Kafka, Postgres, redis) are provided in a docker container.

## How to run locally
Docker Desktop must be installed on your system.

1. Run the terminal command (for example, from the Intellij)
> docker compose up -d
2. Keycloak will start on port **8082**, default user/pass is admin/secureAF123
3. Run the spring boot application. It will start on port **8081**.
4. The application db schema is generated automatically by spring
   via the *"spring.jpa.generate-ddl: true"* property
5. There is a Postman collection that can be imported to test the endpoints

## Integration tests
Integration tests are running by making use of spring testcontainers, which spins up its own
docker container which holds the necessary components for integration testing (test specific kafka, redis etc)

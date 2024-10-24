# NotificationService

This application offers a graphQL websocket based API which offers a subscription functionality to users. 
Users surbscribe to receive notifications. In the back end, the notifications are received in real time from 
another application (TransactionService) via a Kafka topic.

This app also makes use of reactive redis and redis pub/sub, and together with the TransactionService
app is a proof of concept to solve the following problem:
   1. The NotificationService is scaled in a cloud environment, having multiple instances.
   2. An end user opens a subscription (from Postman or browser/FE) 
      to a single instance of NotificationService (let's call it instance 1).
   3. A message is received on the kafka topic, but is read by another app instance (instance 2).
   4. The message disappears from the Kafka topic and is stored in the DB by instance 2, but the user never sees it despite having 
      an open subscription, because the subscription is connected to instance 1.

Redis pub/sub acts as a distributed cache - every instance of NotificationService will push the message 
to redis after reading it from the Kafka topic, and redis will serve it only to the specific NotificationService 
instance which has the correct open subscription (based on user id). 

If a notification is read from Kafka but the owning user doesn't have an open websocket subscription, it goes in the
DB and is marked as unread, and there is another graphQL endpoint that servers all unread notifications.

This app is securing by Spring Security via the OAuth 2.0 protocol, 
and uses Keycloak as identity provider / auth server.

Dependencies (Keycloak, Kafka, Postgres, redis) are provided in a docker container.

## How to run locally
Docker Desktop must be installed on your system.

1. Run the terminal command 
> docker compose up -d
2. Keycloak will start on port **8082**, default user/pass is admin/secureAF123
   From the Keycloak UI:
   1. Configure a realm called NotificationRealm
   2. Create a client (the Postman collection uses a client called: luciantestclient)
   3. Create a user ( in Postman it's called lucianstandarduser)
   4. You will need the *client_secret* of the created client as well as the *username* and *password* of the created user
3. Before starting the app, you need to run the
   > docker compose up -d

   command in the Transaction project as well (https://github.com/NXTRS/Transactions/) because that container hosts the Kafka used by this application.
4. Run the spring boot application. It will start on port **8081**.
5. The application db schema is generated automatically by spring
   via the *"spring.jpa.generate-ddl: true"* property
6. There is a Postman collection that can be imported to test the endpoints

## Integration tests
Integration tests are running by making use of spring testcontainers, which spins up its own
docker container which holds the necessary components for integration testing (test specific kafka, redis etc)

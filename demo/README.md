# Spring Boot Application with Amazon DocumentDB

This is a Spring Boot application that connects to Amazon DocumentDB using MongoDB-compatible drivers.

## How to Run

1. **Set MongoDB Connection URL**
   Open the `src/main/resources/application.properties` file and add your Amazon DocumentDB connection URL under connection.string:
2. **Generate rds-trustStore.jks**
 as mentioned in the https://docs.aws.amazon.com/documentdb/latest/developerguide/connect_programmatically.html and put in the resource folder


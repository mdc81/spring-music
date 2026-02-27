Spring Music
============

This is a sample application for using database services on [Cloud Foundry](https://www.cloudfoundry.org) with the [Spring Framework](https://spring.io) and [Spring Boot](https://spring.io/projects/spring-boot).

This application has been built to store the same domain objects in one of a variety of different persistence technologies - relational, document, and key-value stores. This is not meant to represent a realistic use case for these technologies, since you would typically choose the one most applicable to the type of data you need to store, but it is useful for testing and experimenting with different types of services on Cloud Foundry.

The application uses Spring Java configuration and [bean profiles](https://docs.spring.io/spring-boot/reference/features/profiles.html) to configure the application and the connection objects needed to use the persistence stores. It also uses the [Java CFEnv](https://github.com/pivotal-cf/java-cfenv/) library to inspect the environment when running on Cloud Foundry.

## Tech stack

| Component | Version |
|-----------|---------|
| Java | 25 |
| Spring Boot | 4.0.3 |
| Gradle | 9.3.1 |
| Java CFEnv | 4.0.0 |

## Building

This project requires Java 25 or later to compile.

> [!NOTE]
> If you need to use an earlier Java version, check out the [`spring-boot-2` branch](https://github.com/cloudfoundry-samples/spring-music/tree/spring-boot-2), which can be built with Java 8 and later.

To build a runnable Spring Boot jar file, run the following command:

~~~
$ ./gradlew clean assemble
~~~

## Running the application locally

One Spring bean profile should be activated to choose the database provider that the application should use. The profile is selected by setting the system property `spring.profiles.active` when starting the app.

The application can be started locally using the following command:

~~~
$ java -jar -Dspring.profiles.active=<profile> build/libs/spring-music-1.0.jar
~~~

where `<profile>` is one of the following values:

* `mysql`
* `postgres`
* `mongodb`
* `redis`

If no profile is provided, an in-memory H2 relational database will be used. If any other profile is provided, the appropriate database server must be started separately. Spring Boot will auto-configure a connection to the database using its auto-configuration defaults. The connection parameters can be configured by setting the appropriate [Spring Boot properties](https://docs.spring.io/spring-boot/appendix/application-properties/index.html).

If more than one of these profiles is provided, the application will throw an exception and fail to start.

## Running the application on Cloud Foundry

When running on Cloud Foundry, the application will detect the type of database service bound to the application (if any). If a service of one of the supported types (MySQL, Postgres, Oracle, MongoDB, or Redis) is bound to the app, the appropriate Spring profile will be configured to use the database service. The connection strings and credentials needed to use the service will be extracted from the Cloud Foundry environment.

If no bound services are found containing any of these values in the name, an in-memory relational database will be used.

If more than one service containing any of these values is bound to the application, the application will throw an exception and fail to start.

After installing the `cf` [command-line interface for Cloud Foundry](https://docs.cloudfoundry.org/cf-cli/), targeting a Cloud Foundry instance, and logging in, the application can be built and pushed using these commands:

~~~
$ cf push
~~~

The application will be pushed using settings in the provided `manifest.yml` file. The output from the command will show the URL that has been assigned to the application.

### Creating and binding services

Using the provided manifest, the application will be created without an external database (using the in-memory H2 profile). You can create and bind database services to the application using the information below.

#### System-managed services

Depending on the Cloud Foundry service provider, persistence services might be offered and managed by the platform. These steps can be used to create and bind a service that is managed by the platform:

~~~
# view the services available
$ cf marketplace
# create a service instance
$ cf create-service <service> <service plan> <service name>
# bind the service instance to the application
$ cf bind-service <app name> <service name>
# restart the application so the new service is detected
$ cf restart
~~~

#### User-provided services

Cloud Foundry also allows service connection information and credentials to be provided by a user. In order for the application to detect and connect to a user-provided service, a single `uri` field should be given in the credentials using the form `<dbtype>://<username>:<password>@<hostname>:<port>/<databasename>`.

These steps use examples for username, password, host name, and database name that should be replaced with real values.

~~~
# create a user-provided Oracle database service instance
$ cf create-user-provided-service oracle-db -p '{"uri":"oracle://root:secret@dbserver.example.com:1521/mydatabase"}'
# create a user-provided MySQL database service instance
$ cf create-user-provided-service mysql-db -p '{"uri":"mysql://root:secret@dbserver.example.com:3306/mydatabase"}'
# bind a service instance to the application
$ cf bind-service <app name> <service name>
# restart the application so the new service is detected
$ cf restart
~~~

#### Changing bound services

To test the application with different services, you can simply stop the app, unbind a service, bind a different database service, and start the app:

~~~
$ cf unbind-service <app name> <service name>
$ cf bind-service <app name> <service name>
$ cf restart
~~~

#### Database drivers

Database drivers for MySQL, Postgres, Microsoft SQL Server, MongoDB, and Redis are included in the project.

To connect to an Oracle database, you will need to download the appropriate driver from the [Oracle JDBC Downloads page](https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html). Then make a `libs` directory in the `spring-music` project, and move the driver (`ojdbc8.jar` or `ojdbc7.jar`) into the `libs` directory.
In `build.gradle`, uncomment the appropriate line:

~~~
implementation files('libs/ojdbc8.jar')
// or
implementation files('libs/ojdbc7.jar')
~~~

Then run `./gradlew assemble`.

## Alternate Java versions

By default, the application is built and deployed using Java 25. To use a different version, update two places.

In `build.gradle`, change the `targetCompatibility` version:

~~~
java {
  ...
  targetCompatibility = JavaVersion.VERSION_25
}
~~~

In `manifest.yml`, change the Java buildpack JRE version:

~~~
    JBP_CONFIG_OPEN_JDK_JRE: '{ jre: { version: 25.+ } }'
~~~

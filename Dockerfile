FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline
COPY src src
RUN mvn -q package

FROM quay.io/wildfly/wildfly:36.0.1.Final-jdk21
USER root
RUN curl -fsSL https://jdbc.postgresql.org/download/postgresql-42.7.7.jar -o /tmp/postgresql.jar
COPY docker/configure-wildfly.cli /opt/jboss/wildfly/configure.cli
RUN /opt/jboss/wildfly/bin/jboss-cli.sh --file=/opt/jboss/wildfly/configure.cli && rm /opt/jboss/wildfly/configure.cli /tmp/postgresql.jar && rm -rf /opt/jboss/wildfly/standalone/configuration/standalone_xml_history
COPY --from=build /build/target/clinic.war /opt/jboss/wildfly/standalone/deployments/clinic.war
RUN mkdir -p /opt/clinic/uploads && chown -R jboss:root /opt/clinic /opt/jboss/wildfly/standalone
USER jboss
EXPOSE 8080 9990

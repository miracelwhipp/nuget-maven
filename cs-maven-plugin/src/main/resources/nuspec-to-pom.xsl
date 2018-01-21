<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">

	<xsl:output method="xml" indent="no"/>

	<xsl:template match="/package">

		<project>
			<modelVersion>4.0.0</modelVersion>

			<groupId>
				<xsl:value-of select="metadata/id"/>
			</groupId>
			<artifactId>
				<xsl:value-of select="metadata/id"/>
			</artifactId>
			<version>
				<xsl:value-of select="metadata/version"/>
			</version>
			<packaging>pom</packaging>

			<dependencies>
				<xsl:apply-templates select="metadata/dependencies"/>
			</dependencies>


		</project>

	</xsl:template>

	<xsl:template match="dependency">
		<dependency>
			<groupId>
				<xsl:value-of select="@id"/>
			</groupId>
			<artifactId>
				<xsl:value-of select="@id"/>
			</artifactId>
			<version>
				<xsl:value-of select="@version"/>
			</version>
			<type>dll</type>
		</dependency>
	</xsl:template>

</xsl:stylesheet>
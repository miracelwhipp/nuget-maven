<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">

	<!--xmlns:nuget1="http://schemas.microsoft.com/packaging/2013/01/nuspec.xsd"-->
	<!--xmlns:nuget5="http://schemas.microsoft.com/packaging/2013/05/nuspec.xsd"-->


	<xsl:output method="xml" indent="yes"/>

	<!--<xsl:strip-space elements="*"/>-->

	<xsl:param name="targetFramework"/>

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

	<xsl:template match="group[@targetFramework != $targetFramework]">
	</xsl:template>

	<!--<xsl:template-->
			<!--match="dependency[@id = 'Microsoft.NETCore.Platforms' or @id = 'Microsoft.NETCore.DotNetAppHost']">-->
	<!--</xsl:template>-->

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
			<type>nupkg</type>
		</dependency>
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
			<optional>true</optional>
		</dependency>
	</xsl:template>

	<!--<xsl:template match="/nuget5:package">-->

		<!--<project>-->
			<!--<modelVersion>4.0.0</modelVersion>-->

			<!--<groupId>-->
				<!--<xsl:value-of select="nuget5:metadata/nuget5:id"/>-->
			<!--</groupId>-->
			<!--<artifactId>-->
				<!--<xsl:value-of select="nuget5:metadata/nuget5:id"/>-->
			<!--</artifactId>-->
			<!--<version>-->
				<!--<xsl:value-of select="nuget5:metadata/nuget5:version"/>-->
			<!--</version>-->
			<!--<packaging>pom</packaging>-->

			<!--<dependencies>-->
				<!--<xsl:apply-templates select="nuget5:metadata/nuget5:dependencies"/>-->
			<!--</dependencies>-->


		<!--</project>-->

	<!--</xsl:template>-->

	<!--<xsl:template match="nuget5:group[@targetFramework != $targetFramework]">-->
	<!--</xsl:template>-->

	<!--<xsl:template-->
			<!--match="nuget5:dependency[@id = 'Microsoft.NETCore.Platforms' or @id = 'Microsoft.NETCore.DotNetAppHost']">-->
	<!--</xsl:template>-->

	<!--<xsl:template match="nuget5:dependency">-->
		<!--<dependency>-->
			<!--<groupId>-->
				<!--<xsl:value-of select="@id"/>-->
			<!--</groupId>-->
			<!--<artifactId>-->
				<!--<xsl:value-of select="@id"/>-->
			<!--</artifactId>-->
			<!--<version>-->
				<!--<xsl:value-of select="@version"/>-->
			<!--</version>-->
			<!--<type>nupkg</type>-->
		<!--</dependency>-->
		<!--<dependency>-->
			<!--<groupId>-->
				<!--<xsl:value-of select="@id"/>-->
			<!--</groupId>-->
			<!--<artifactId>-->
				<!--<xsl:value-of select="@id"/>-->
			<!--</artifactId>-->
			<!--<version>-->
				<!--<xsl:value-of select="@version"/>-->
			<!--</version>-->
			<!--<type>dll</type>-->
		<!--</dependency>-->
	<!--</xsl:template>-->

	<!--<xsl:template match="/nuget1:package">-->

		<!--<project>-->
			<!--<modelVersion>4.0.0</modelVersion>-->

			<!--<groupId>-->
				<!--<xsl:value-of select="nuget1:metadata/nuget1:id"/>-->
			<!--</groupId>-->
			<!--<artifactId>-->
				<!--<xsl:value-of select="nuget1:metadata/nuget1:id"/>-->
			<!--</artifactId>-->
			<!--<version>-->
				<!--<xsl:value-of select="nuget1:metadata/nuget1:version"/>-->
			<!--</version>-->
			<!--<packaging>pom</packaging>-->

			<!--<dependencies>-->
				<!--<xsl:apply-templates select="nuget1:metadata/nuget1:dependencies"/>-->
			<!--</dependencies>-->


		<!--</project>-->

	<!--</xsl:template>-->

	<!--<xsl:template match="nuget1:group[@targetFramework != $targetFramework]">-->
	<!--</xsl:template>-->

	<!--<xsl:template-->
			<!--match="nuget1:dependency[@id = 'Microsoft.NETCore.Platforms' or @id = 'Microsoft.NETCore.DotNetAppHost']">-->
	<!--</xsl:template>-->

	<!--<xsl:template match="nuget1:dependency">-->
		<!--<dependency>-->
			<!--<groupId>-->
				<!--<xsl:value-of select="@id"/>-->
			<!--</groupId>-->
			<!--<artifactId>-->
				<!--<xsl:value-of select="@id"/>-->
			<!--</artifactId>-->
			<!--<version>-->
				<!--<xsl:value-of select="@version"/>-->
			<!--</version>-->
			<!--<type>nupkg</type>-->
		<!--</dependency>-->
		<!--<dependency>-->
			<!--<groupId>-->
				<!--<xsl:value-of select="@id"/>-->
			<!--</groupId>-->
			<!--<artifactId>-->
				<!--<xsl:value-of select="@id"/>-->
			<!--</artifactId>-->
			<!--<version>-->
				<!--<xsl:value-of select="@version"/>-->
			<!--</version>-->
			<!--<type>dll</type>-->
		<!--</dependency>-->
	<!--</xsl:template>-->

</xsl:stylesheet>
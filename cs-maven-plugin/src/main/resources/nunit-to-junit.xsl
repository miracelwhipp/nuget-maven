<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:output method="xml" indent="yes"/>

	<xsl:param name="target-directory"/>

	<xsl:param name="nunit-result"/>

	<xsl:strip-space elements="*"/>


	<xsl:template match="/test-run">

		<failsafe-summary result="{$nunit-result}" timeout="false">
			<completed>
				<xsl:value-of select="test-suite/@passed"/>
			</completed>
			<errors>0</errors>
			<failures>
				<xsl:value-of select="test-suite/@failed"/>
			</failures>
			<skipped>
				<xsl:value-of select="test-suite/@skipped"/>
			</skipped>
		</failsafe-summary>

		<xsl:apply-templates/>

	</xsl:template>


	<xsl:template match="*"/>

	<xsl:template match="test-suite">
		<xsl:apply-templates/>
	</xsl:template>

	<xsl:template match="test-suite[@type = 'TestFixture']">

		<xsl:result-document href="/{$target-directory}/TEST-{@classname}.xml" method="xml" indent="yes">

			<testsuite name="{@classname}" tests="{count(.//test-case)}" time="{@duration}" failures="{count(.//test-case/failure)}" errors="0" skipped="{count(.//test-case[@executed='False'])}">

				<xsl:apply-templates/>

			</testsuite>

		</xsl:result-document>
	</xsl:template>

	<xsl:template match="test-case">

		<testcase classname="{@classname}" name="{@name}" time="{@duration}">

			<xsl:variable name="generalfailure" select="failure"/>

			<xsl:if test="failure">
				<xsl:variable name="failstack" select="count(failure/stack-trace/*) + count(failure/stack-trace/text())"/>
				<failure>
					<xsl:choose>
						<xsl:when test="$failstack &gt; 0 or not($generalfailure)"><![CDATA[
MESSAGE:
]]><xsl:value-of select="failure/message"/><![CDATA[
+++++++++++++++++++
STACK TRACE:]]><xsl:value-of select="./failure/stack-trace"/>
						</xsl:when>
						<xsl:otherwise><![CDATA[
MESSAGE:
]]><xsl:value-of select="$generalfailure/message"/><![CDATA[
+++++++++++++++++++
STACK TRACE:]]><xsl:value-of select="$generalfailure/stack-trace"/>
						</xsl:otherwise>
					</xsl:choose>
				</failure>
			</xsl:if>

			<xsl:if test="@executed='False'">
				<skipped>
					<xsl:attribute name="message">
						<xsl:value-of select="reason/message"/>
					</xsl:attribute>
				</skipped>
			</xsl:if>
		</testcase>


	</xsl:template>

</xsl:stylesheet>
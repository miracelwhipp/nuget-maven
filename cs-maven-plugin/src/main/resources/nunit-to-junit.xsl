<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:output method="xml" indent="yes"/>
	<xsl:param name="target-directory"/>
	<xsl:param name="nunit-result"/>

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


		<xsl:for-each select="test-suite//test-case[1]">

			<xsl:for-each select="..">

				<xsl:result-document href="/{$target-directory}/TEST-{@classname}.xml">


					<xsl:variable name="firstTestName"
								  select="test-case[1]/@name"/>

					<xsl:variable name="assembly">
						<xsl:choose>
							<xsl:when test="substring($firstTestName, string-length($firstTestName)) = ')'">
								<xsl:value-of select="substring-before($firstTestName, concat('.', @name))"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="concat(substring-before($firstTestName, @name), @name)"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:variable>

					<testsuite name="{$assembly}"
							   tests="{count(./test-case)}" time="{@time}"
							   failures="{count(./test-case/failure)}" errors="0"
							   skipped="{count(./test-case[@executed='False'])}">
						<xsl:for-each select="./test-case">
							<xsl:variable name="testcaseName">
								<xsl:choose>
									<xsl:when test="contains(./@name, concat($assembly,'.'))">
										<xsl:value-of select="substring-after(./@name, concat($assembly,'.'))"/>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="./@name"/>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:variable>

							<testcase classname="{$assembly}"
									  name="{$testcaseName}">
								<xsl:if test="@time!=''">
									<xsl:attribute name="time">
										<xsl:value-of select="@time"/>
									</xsl:attribute>
								</xsl:if>

								<xsl:variable name="generalfailure"
											  select="./failure"/>

								<xsl:if test="./failure">
									<xsl:variable name="failstack"
												  select="count(./failure/stack-trace/*) + count(./failure/stack-trace/text())"/>
									<failure>
										<xsl:choose>
											<xsl:when test="$failstack &gt; 0 or not($generalfailure)"><![CDATA[
MESSAGE:
]]><xsl:value-of select="./failure/message"/><![CDATA[
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
											<xsl:value-of select="./reason/message"/>
										</xsl:attribute>
									</skipped>
								</xsl:if>
							</testcase>
						</xsl:for-each>
					</testsuite>
				</xsl:result-document>
			</xsl:for-each>
		</xsl:for-each>
	</xsl:template>
</xsl:stylesheet>
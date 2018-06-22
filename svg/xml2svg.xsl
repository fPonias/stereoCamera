<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:android="http://schemas.android.com/apk/res/android" >
<xsl:template match="/">
  <svg xmlns="http://www.w3.org/2000/svg">
    <xsl:attribute name="width">
      <xsl:value-of select="substring-before(vector/@android:width, 'dp')"/>
    </xsl:attribute>
    <xsl:attribute name="height">
      <xsl:value-of select="substring-before(vector/@android:height, 'dp')"/>
    </xsl:attribute>
    <xsl:attribute name="viewBox">
      <xsl:value-of select="concat('0 0 ', vector/@android:viewportWidth, ' ', vector/@android:viewportHeight)" />
    </xsl:attribute>
  <xsl:for-each select="vector/path">
    <path>
      <xsl:attribute name="fill">
        <xsl:value-of select="@android:fillColor"/>
      </xsl:attribute>
      <xsl:attribute name="d">
        <xsl:value-of select="@android:pathData"/>
      </xsl:attribute>
    </path>
  </xsl:for-each>
  </svg>
</xsl:template>
</xsl:stylesheet>

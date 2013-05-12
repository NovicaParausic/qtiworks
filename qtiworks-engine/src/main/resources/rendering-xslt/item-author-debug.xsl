<?xml version="1.0" encoding="UTF-8"?>
<!--

Renders the author/debug view of a standalone assessmentItem

Input document: assessmentItem (slightly inappropriate here, but never mind!)

-->
<xsl:stylesheet version="2.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:qti="http://www.imsglobal.org/xsd/imsqti_v2p1"
  xmlns:m="http://www.w3.org/1998/Math/MathML"
  xmlns:qw="http://www.ph.ed.ac.uk/qtiworks"
  xmlns="http://www.w3.org/1999/xhtml"
  xpath-default-namespace="http://www.w3.org/1999/xhtml"
  exclude-result-prefixes="xs qti qw m">

  <!-- ************************************************************ -->

  <xsl:import href="qti-fallback.xsl"/>
  <xsl:import href="item-common.xsl"/>
  <xsl:import href="utils.xsl"/>

  <!-- ************************************************************ -->

  <xsl:function name="qw:format-optional-date" as="xs:string?">
    <xsl:param name="date" as="xs:string?"/>
    <xsl:param name="default" as="xs:string?"/>
    <xsl:sequence select="if ($date!='') then $date else $default"/>
  </xsl:function>

  <!-- ************************************************************ -->

  <xsl:template match="/" as="element(html)">
    <html>
      <head>
        <title>Author Debug View</title>
        <link rel="stylesheet" href="//fonts.googleapis.com/css?family=Open+Sans:400,400italic,700,700italic|Ubuntu:500"/>
        <link rel="stylesheet" href="{$webappContextPath}/lib/960/reset.css"/>
        <link rel="stylesheet" href="{$webappContextPath}/lib/960/text.css"/>
        <link rel="stylesheet" href="{$webappContextPath}/lib/960/960.css"/>
        <link rel="stylesheet" href="{$webappContextPath}/includes/qtiworks.css?{$qtiWorksVersion}"/>
        <link rel="stylesheet" href="{$webappContextPath}/includes/author-mode.css?{$qtiWorksVersion}"/>
        <link rel="stylesheet" href="//ajax.googleapis.com/ajax/libs/jqueryui/1.10.1/themes/smoothness/jquery-ui.css"/>
        <script src="//ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js"/>
        <script src="//ajax.googleapis.com/ajax/libs/jqueryui/1.10.1/jquery-ui.min.js"/>
        <script src="{$webappContextPath}/rendering/javascript/QtiWorksRendering.js?{$qtiWorksVersion}"/>
        <script src="{$webappContextPath}/rendering/javascript/AuthorMode.js?{$qtiWorksVersion}"/>
      </head>
      <body class="qtiworks authorInfo">
        <div class="container_12">
          <header>
            <h1>QTIWorks</h1>
          </header>
          <h2>QTI author's feedback</h2>

          <xsl:call-template name="qw:session-controls"/>
          <xsl:apply-templates select="$itemSessionState"/>
        </div>
      </body>
    </html>
  </xsl:template>

  <xsl:template name="qw:session-controls">
    <div class="sessionControl">
      <ul class="controls">
        <li>
          <form action="{$webappContextPath}{$sourceUrl}" method="get" class="showXmlInDialog" title="Item Source XML">
            <input type="submit" value="View Item source XML"/>
          </form>
        </li>
        <li>
          <form action="{$webappContextPath}{$stateUrl}" method="get" class="showXmlInDialog" title="Item State XML">
            <input type="submit" value="View Item state XML"/>
          </form>
        </li>
        <li>
          <form action="{$webappContextPath}{$resultUrl}" method="get" class="showXmlInDialog" title="Item Result XML">
            <input type="submit" value="View Item &lt;assessmentResult&gt; XML"/>
          </form>
        </li>
      </ul>
    </div>
  </xsl:template>

  <xsl:template match="qw:itemSessionState">
    <div class="resultPanel">
      <h4>Item session state</h4>
      <div class="resultPanel">
        <h4>Key status information</h4>
        <div class="details">
          <ul>
            <li>Entry time: <xsl:value-of select="qw:format-optional-date($itemSessionState/@entryTime, '(Not Yet Entered)')"/></li>
            <li>End time: <xsl:value-of select="qw:format-optional-date($itemSessionState/@endTime, '(Not Yet Ended)')"/></li>
            <li>Duration accumulated: <xsl:value-of select="$itemSessionState/@durationAccumulated div 1000.0"/> s</li>
            <li>Initialized: <xsl:value-of select="$itemSessionState/@initialized"/></li>
            <li>Responded: <xsl:value-of select="$itemSessionState/@responded"/></li>
            <li><code>sessionStatus</code>: <xsl:value-of select="$sessionStatus"/></li>
            <li><code>numAttempts</code>: <xsl:value-of select="$itemSessionState/@numAttempts"/></li>
            <li><code>completionStatus</code>: <xsl:value-of select="$itemSessionState/@completionStatus"/></li>
            <li>Solution mode? <xsl:value-of select="$solutionMode"/></li>
          </ul>
        </div>
      </div>
      <div class="resultPanel">
        <h4>Response state</h4>
        <div class="details">
          <xsl:call-template name="unboundResponsesPanel"/>
          <xsl:call-template name="invalidResponsesPanel"/>
        </div>
      </div>
      <div class="resultPanel">
        <h4>Variable state</h4>
        <div class="details">
          <p>The values of all variables are shown below.</p>
          <xsl:call-template name="variableValuesPanel"/>
        </div>
      </div>
      <xsl:call-template name="notificationsPanel"/>
      <xsl:call-template name="shuffleStatePanel"/>
    </div>
  </xsl:template>

  <xsl:template name="unboundResponsesPanel" as="element(div)">
    <div class="resultPanel {if (exists($unboundResponseIdentifiers)) then 'failure' else 'success'}">
      <h4>Unbound responses (<xsl:value-of select="count($unboundResponseIdentifiers)"/>)</h4>
      <div class="details">
        <xsl:choose>
          <xsl:when test="exists($unboundResponseIdentifiers)">
            <p>
              The responses listed below were not successfully bound to their corresponding variables.
              This might happen, for example, if you bind a <code>&lt;textEntryInteraction&gt;</code> to
              a numeric variable and the candidate enters something that is not a number.
            </p>
            <ul>
              <xsl:for-each select="$unboundResponseIdentifiers">
                <li>
                  <span class="variableName">
                    <xsl:value-of select="."/>
                  </span>
                </li>
              </xsl:for-each>
            </ul>
          </xsl:when>
          <xsl:otherwise>
            <p>
              All responses were successfully bound to response variables.
            </p>
          </xsl:otherwise>
        </xsl:choose>
      </div>
    </div>
  </xsl:template>

  <xsl:template name="invalidResponsesPanel" as="element(div)">
    <div class="resultPanel {if (exists($invalidResponseIdentifiers)) then 'failure' else 'success'}">
      <h4>Invalid responses (<xsl:value-of select="count($invalidResponseIdentifiers)"/>)</h4>
      <div class="details">
        <xsl:choose>
          <xsl:when test="exists($invalidResponseIdentifiers)">
            <p>
              The responses were successfully bound to their corresponding variables,
              but failed to satisfy the constraints specified by their corresponding interactions:
            </p>
            <ul>
              <xsl:for-each select="$invalidResponseIdentifiers">
                <li>
                  <span class="variableName">
                    <xsl:value-of select="."/>
                  </span>
                </li>
              </xsl:for-each>
            </ul>
          </xsl:when>
          <xsl:otherwise>
            <p>
              All responses satisfied the constraints specified by their correpsonding interactions.
            </p>
          </xsl:otherwise>
        </xsl:choose>
      </div>
    </div>
  </xsl:template>

  <xsl:template name="variableValuesPanel" as="element(div)*">
    <xsl:if test="exists($outcomeValues)">
      <div class="resultPanel">
        <h4>Outcome values (<xsl:value-of select="count($outcomeValues)"/>)</h4>
        <div class="details">
          <xsl:call-template name="dump-values">
            <xsl:with-param name="valueHolders" select="$outcomeValues"/>
          </xsl:call-template>
        </div>
      </div>
    </xsl:if>
    <xsl:if test="exists($responseValues)">
      <div class="resultPanel">
        <h4>Response values (<xsl:value-of select="count($responseValues)"/>)</h4>
        <div class="details">
          <xsl:call-template name="dump-values">
            <xsl:with-param name="valueHolders" select="$responseValues"/>
          </xsl:call-template>
        </div>
      </div>
    </xsl:if>
    <xsl:if test="exists($templateValues)">
      <div class="resultPanel">
        <h4>Template values (<xsl:value-of select="count($templateValues)"/>)</h4>
        <div class="details">
          <xsl:call-template name="dump-values">
            <xsl:with-param name="valueHolders" select="$templateValues"/>
          </xsl:call-template>
        </div>
      </div>
    </xsl:if>
  </xsl:template>

  <xsl:template name="notificationsPanel" as="element(div)">
    <div class="resultPanel {if ($notifications[not(@level='INFO')]) then 'failure' else 'success'}">
      <h4>Processing notifications (<xsl:value-of select="count($notifications)"/>)</h4>
      <div class="details">
        <xsl:choose>
          <xsl:when test="count($notifications) > 0">
            <p>
              The following notifications were recorded during this processing run on this item.
              These may indicate issues with your item that need fixed.
            </p>
            <xsl:call-template name="notificationsLevelPanel">
              <xsl:with-param name="level" select="'ERROR'"/>
              <xsl:with-param name="title" select="'Errors'"/>
            </xsl:call-template>
            <xsl:call-template name="notificationsLevelPanel">
              <xsl:with-param name="level" select="'WARNING'"/>
              <xsl:with-param name="title" select="'Warnings'"/>
            </xsl:call-template>
            <xsl:call-template name="notificationsLevelPanel">
              <xsl:with-param name="level" select="'INFO'"/>
              <xsl:with-param name="title" select="'Informational'"/>
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
            <p>
              No notifications were recorded during this processing run on this item.
            </p>
          </xsl:otherwise>
        </xsl:choose>
      </div>
    </div>
  </xsl:template>

  <xsl:template name="notificationsLevelPanel" as="element(div)?">
    <xsl:param name="level" as="xs:string"/>
    <xsl:param name="title" as="xs:string"/>
    <xsl:variable name="notificationsAtLevel" select="$notifications[@level=$level]" as="element(qw:notification)*"/>
    <xsl:if test="exists($notificationsAtLevel)">
      <div class="resultPanel">
        <h4><xsl:value-of select="concat($title, ' (', count($notificationsAtLevel), ')')"/></h4>
        <div class="details">
          <table class="notificationsTable">
            <thead>
              <tr>
                <th>Type</th>
                <th>QTI Class</th>
                <th>Attribute</th>
                <th>Line Number</th>
                <th>Column Number</th>
                <th>Message</th>
              </tr>
            </thead>
            <tbody>
              <xsl:for-each select="$notificationsAtLevel">
                <tr>
                  <td><xsl:value-of select="@type"/></td>
                  <td><xsl:value-of select="if (exists(@nodeQtiClassName)) then @nodeQtiClassName else 'N/A'"/></td>
                  <td><xsl:value-of select="if (exists(@attrLocalName)) then @attrLocalName else 'N/A'"/></td>
                  <td><xsl:value-of select="if (exists(@lineNumber)) then @lineNumber else 'Unknown'"/></td>
                  <td><xsl:value-of select="if (exists(@columnNumber)) then @columnNumber else 'Unknown'"/></td>
                  <td><xsl:value-of select="."/></td>
                </tr>
              </xsl:for-each>
            </tbody>
          </table>
        </div>
      </div>
    </xsl:if>
  </xsl:template>

  <xsl:template name="shuffleStatePanel" as="element(div)">
    <div class="resultPanel">
      <h4>Interaction shuffle state</h4>
      <div class="details">
        <xsl:choose>
          <xsl:when test="exists($shuffledChoiceOrders)">
            <ul>
              <xsl:for-each select="$shuffledChoiceOrders">
                <li>
                  <span class="variableName">
                    <xsl:value-of select="@responseIdentifier"/>
                  </span>
                  <xsl:text> = [</xsl:text>
                  <xsl:value-of select="tokenize(@choiceSequence, ' ')" separator=", "/>
                  <xsl:text>]</xsl:text>
                </li>
              </xsl:for-each>
            </ul>
          </xsl:when>
          <xsl:otherwise>
            <p>There are no shuffled interactions in this item.</p>
          </xsl:otherwise>
        </xsl:choose>
      </div>
    </div>
  </xsl:template>

  <!-- ************************************************************ -->

  <xsl:template name="dump-values" as="element(ul)">
    <xsl:param name="valueHolders" as="element()*"/>
    <ul>
      <xsl:for-each select="$valueHolders">
        <xsl:call-template name="dump-value">
          <xsl:with-param name="valueHolder" select="."/>
        </xsl:call-template>
      </xsl:for-each>
    </ul>
  </xsl:template>

  <xsl:template name="dump-value" as="element(li)">
    <xsl:param name="valueHolder" as="element()"/>
    <li>
      <span class="variableName">
        <xsl:value-of select="@identifier"/>
      </span>
      <xsl:text> = </xsl:text>
      <xsl:choose>
        <xsl:when test="not(*)">
          <xsl:text>NULL</xsl:text>
        </xsl:when>
        <xsl:when test="qw:is-maths-content-value($valueHolder)">
          <!-- We'll handle MathsContent variables specially to help question authors -->
          <span class="type">MathsContent :: </span>
          <xsl:copy-of select="qw:extract-maths-content-pmathml($valueHolder)"/>

          <!-- Make the raw record fields available via a toggle -->
          <xsl:text> </xsl:text>
          <a id="qtiworks_id_toggle_debugMathsContent_{@identifier}" class="debugButton"
            href="javascript:void(0)">Toggle Details</a>
          <div id="qtiworks_id_debugMathsContent_{@identifier}" class="debugMathsContent">
            <xsl:call-template name="dump-record-entries">
              <xsl:with-param name="valueHolders" select="$valueHolder/qw:value"/>
            </xsl:call-template>
          </div>
          <script>
            $(document).ready(function() {
              $('a#qtiworks_id_toggle_debugMathsContent_<xsl:value-of select="@identifier"/>').click(function() {
                $('#qtiworks_id_debugMathsContent_<xsl:value-of select="@identifier"/>').toggle();
              })
            });
          </script>
        </xsl:when>
        <xsl:otherwise>
          <!-- Other variables will be output in a fairly generic way -->
          <span class="type">
            <xsl:value-of select="(@cardinality, @baseType, ':: ')" separator=" "/>
          </span>
          <xsl:choose>
            <xsl:when test="@cardinality='single'">
              <xsl:variable name="text" select="$valueHolder/qw:value" as="xs:string"/>
              <xsl:choose>
                <xsl:when test="contains($text, '&#x0a;')">
                  <pre><xsl:value-of select="$text"/></pre>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="$text"/>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:when>
            <xsl:when test="@cardinality='multiple'">
              <xsl:text>{</xsl:text>
              <xsl:value-of select="$valueHolder/qw:value" separator=", "/>
              <xsl:text>}</xsl:text>
            </xsl:when>
            <xsl:when test="@cardinality='ordered'">
              <xsl:text>[</xsl:text>
              <xsl:value-of select="$valueHolder/qw:value" separator=", "/>
              <xsl:text>]</xsl:text>
            </xsl:when>
            <xsl:when test="@cardinality='record'">
              <xsl:text>(</xsl:text>
              <xsl:call-template name="dump-record-entries">
                <xsl:with-param name="valueHolders" select="$valueHolder/qw:value"/>
              </xsl:call-template>
              <xsl:text>)</xsl:text>
            </xsl:when>
          </xsl:choose>
        </xsl:otherwise>
      </xsl:choose>
    </li>
  </xsl:template>

  <xsl:template name="dump-record-entries" as="element(ul)">
    <xsl:param name="valueHolders" as="element()*"/>
    <ul>
      <xsl:for-each select="$valueHolders">
        <li>
          <span class="variableName">
            <xsl:value-of select="@fieldIdentifier"/>
          </span>
          <xsl:text> = </xsl:text>
          <xsl:choose>
            <xsl:when test="not(*)">
              <xsl:text>NULL</xsl:text>
            </xsl:when>
            <xsl:otherwise>
              <!-- Other variables will be output in a fairly generic way -->
              <span class="type">
                <xsl:value-of select="(@baseType, ':: ')" separator=" "/>
              </span>
              <xsl:variable name="text" select="qw:value" as="xs:string"/>
              <xsl:choose>
                <xsl:when test="contains($text, '&#x0a;')">
                  <pre><xsl:value-of select="$text"/></pre>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="$text"/>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:otherwise>
          </xsl:choose>
        </li>
      </xsl:for-each>
    </ul>
  </xsl:template>

  <xsl:template name="dump-unbound-response-inputs" as="element(ul)">
    <ul>
      <xsl:for-each select="$responseInputs[@identifier = $unboundResponseIdentifiers]">
        <li>
          <span class="variableName">
            <xsl:value-of select="@identifier"/>
          </span>
          <xsl:text> = </xsl:text>
          <xsl:choose>
            <xsl:when test="@type='string'">
              <xsl:text>[</xsl:text>
              <xsl:value-of select="qw:value" separator=", "/>
              <xsl:text>]</xsl:text>
            </xsl:when>
            <xsl:when test="@type='file'">
              (Uploaded file)
            </xsl:when>
          </xsl:choose>
        </li>
      </xsl:for-each>
    </ul>
  </xsl:template>

</xsl:stylesheet>
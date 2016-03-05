package sparqldesignruleplugin;

import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.project.ProjectsManager;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.magicdraw.ui.browser.actions.DefaultBrowserAction;
import com.nomagic.magicdraw.ui.dialogs.MDDialogParentProvider;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdinformationflows.InformationFlow;
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdmodels.Model;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.DataType;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Expression;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.LiteralBoolean;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.LiteralString;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.OpaqueExpression;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.ValueSpecification;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdinternalstructures.Connector;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdinternalstructures.ConnectorEnd;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdports.Port;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;

import designrules.MagicDrawManager;
import designrules.MagicDrawManager2;
import dnl.utils.text.table.TextTable;

public class SPARQLDesignRuleAction extends DefaultBrowserAction {

	private Constraint constraint;

	public SPARQLDesignRuleAction(Constraint constraint) {
		super("", "Execute SPARQL Design Rule", null, null);
		this.constraint = constraint;
	}

	private static final long serialVersionUID = 1L;

	public void actionPerformed(ActionEvent e) {

		ValueSpecification valueSpecification = constraint.getSpecification();
		if (valueSpecification == null) {
			JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider()
					.getDialogParent(),
					"ERROR: Constraint does not contain an expression");
			return;
		}

		OpaqueExpression opaqueExpression = null;
		if (valueSpecification instanceof OpaqueExpression) {
			opaqueExpression = (OpaqueExpression) valueSpecification;
			List<String> languages = opaqueExpression.getLanguage();
			boolean isLanguageSPARQL = false;
			for (String language : languages) {
				if (language.equals("SPARQL")) {
					isLanguageSPARQL = true;
					break;
				}
			}
			if (!isLanguageSPARQL) {
				JOptionPane.showMessageDialog(MDDialogParentProvider
						.getProvider().getDialogParent(),
						"ERROR: Constraint expression language is not SPARQL");
				return;
			}
		}

		Application magicdrawApplication = Application.getInstance();
		ProjectsManager projectsManager = magicdrawApplication
				.getProjectsManager();
		Project project = projectsManager.getActiveProject();

		if (!SessionManager.getInstance().isSessionCreated()) {
			SessionManager.getInstance().createSession(
					"MagicDraw OSLC Session for projectId");
		}

		MagicDrawManager2.convertSysMLDataIntoRDF(project);
		MagicDrawManager2.collectingRDFData();

		// get SPARQL query
		String sparqlQuery = null;
		List<String> expressionBodies = opaqueExpression.getBody();
		if (expressionBodies.size() == 0) {
			JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider()
					.getDialogParent(),
					"ERROR: Constraint expression does not contain any body");
			return;
		} else {
			sparqlQuery = expressionBodies.get(0);
		}

		if (sparqlQuery.isEmpty()) {
			JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider()
					.getDialogParent(),
					"ERROR: Constraint expression body is empty");
			return;
		}

		// parse query and check if query is an ASK query or a SELECT query
		String timeStamp = new Date().toString();
		String constraintName = "unnamed constraint";
		if (constraint.getName() != null) {
			constraintName = constraint.getName();
		} else {
			constraintName = "unnamed constraint owned by "
					+ ((NamedElement) constraint.getOwner()).getName();
		}

		if (sparqlQuery.startsWith("for each")
				| sparqlQuery.startsWith("FOR EACH")) {

			// make sure that it has a for each/do structure
			// check if foreach query is well structured
			Pattern forEachPattern = Pattern.compile(
					"for each.+\\[.+\\].+do.+\\[.+\\]",
					Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
			Matcher forEachMatcher = forEachPattern.matcher(sparqlQuery);
			boolean isQueryWellStructured = forEachMatcher.find();

			if (!isQueryWellStructured) {
				Pattern justforEachPattern = Pattern.compile("for each.+do.+.",
						Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
				Matcher justforEachMatcher = justforEachPattern
						.matcher(sparqlQuery);
				boolean isForEachQuery = justforEachMatcher.find();

				if (!isForEachQuery) {
					JOptionPane
							.showMessageDialog(MDDialogParentProvider
									.getProvider().getDialogParent(),
									"ERROR: The FOREACH SPARQL query does not contain a for each / do structure");
					return;
				} else {
					JOptionPane
							.showMessageDialog(
									MDDialogParentProvider.getProvider()
											.getDialogParent(),
									"ERROR: The FOREACH SPARQL query does not contain correctly defined square brackets []");
					return;
				}

			}

			// get foreach query variable
			int firstOpeningBracketIndex = sparqlQuery.indexOf("[");
			int firstClosingBracketIndex = sparqlQuery.indexOf("]");
			String selectLine = sparqlQuery.substring(0,
					firstOpeningBracketIndex);
			Pattern pattern = Pattern.compile("(?=\\$).+?(?=\\s|$)");
			Matcher matcher = pattern.matcher(selectLine);
			ArrayList<String> foreachQueryVariables = new ArrayList<String>();
			while (matcher.find()) {
				// System.out.print("Start index: " + matcher.start());
				// System.out.print(" End index: " + matcher.end() + " ");
				// System.out.println(matcher.group());
				String match = matcher.group();
				match = match.replace("$", "");
				foreachQueryVariables.add(match);
			}

			String foreachQueryVariable = null;
			if (foreachQueryVariables.size() != 1) {
				JOptionPane
						.showMessageDialog(
								MDDialogParentProvider.getProvider()
										.getDialogParent(),
								"ERROR: The FOREACH SPARQL query must have a single query variable (defined by a $ in the first line)");
				return;
			} else {
				foreachQueryVariable = foreachQueryVariables.get(0);
			}

			// get the for query of the foreach query
			String firstQuery = sparqlQuery.substring(firstOpeningBracketIndex,
					firstClosingBracketIndex);
			firstQuery = firstQuery.replace("[", "");
			firstQuery = firstQuery.replace("]", "");

			String remainingQuery = sparqlQuery.substring(
					firstClosingBracketIndex + 1, sparqlQuery.length());
			int secondOpeningBracketIndex = remainingQuery.indexOf("[");
			int secondtClosingBracketIndex = remainingQuery.indexOf("]");
			String doQuery = remainingQuery.substring(
					secondOpeningBracketIndex, secondtClosingBracketIndex);
			doQuery = doQuery.replace("[", "");
			doQuery = doQuery.replace("]", "");

			StringBuffer htmlTableBuffer = prepareHTMLTableOfForEachQueryResults(
					firstQuery, doQuery, foreachQueryVariable);

			// make sure that the for query only has 1 query variable
			ArrayList<String> firstQueryVariables = getQueryVariables(firstQuery);

			if (firstQueryVariables.size() != 1) {
				JOptionPane
						.showMessageDialog(
								MDDialogParentProvider.getProvider()
										.getDialogParent(),
								"ERROR: The for SPARQL query of the FOREACH SPARQL query must have a single query variable");
				return;
			}

			ArrayList<ArrayList<Element>> firstMatchingElements = MagicDrawManager2
					.performSPARQLSELECTQuery(firstQuery, firstQueryVariables);

			ArrayList<Element> matchingElementsOfFORQuery = new ArrayList<Element>();
			for (ArrayList<Element> matchResult : firstMatchingElements) {
				matchingElementsOfFORQuery.add(matchResult.get(0));
			}

			// perform the do query for each match result
			ArrayList<Boolean> resultsOfASKQueries = new ArrayList<Boolean>();
			for (Element matchingElement : matchingElementsOfFORQuery) {
				// get URI of matching element
				String elementType = null;

				if (MagicDrawManager2.mdSysmlRequirements
						.contains(matchingElement)) {
					elementType = "requirements";
				} else if (MagicDrawManager2.mdSysmlBlocks
						.contains(matchingElement)) {
					elementType = "blocks";
				} else if (MagicDrawManager2.mdSysmlInterfaceBlocks
						.contains(matchingElement)) {
					elementType = "interfaceblocks";
				} else if (MagicDrawManager2.mdSysmlItemFlows
						.contains(matchingElement)) {
					elementType = "itemflows";
				} else if (MagicDrawManager2.mdSysmlValueTypes
						.contains(matchingElement)) {
					elementType = "valuetypes";
				} else if (MagicDrawManager2.mdSysmlPartProperties
						.contains(matchingElement)) {
					elementType = "partproperties";
				} else if (MagicDrawManager2.mdSysmlConnectors
						.contains(matchingElement)) {
					elementType = "connectors";
				} else if (MagicDrawManager2.mdSysmlPorts
						.contains(matchingElement)) {
					elementType = "ports";
				} else if (MagicDrawManager2.mdSysmlValueProperties
						.contains(matchingElement)) {
					elementType = "valueproperties";
				} else if (MagicDrawManager2.mdSysmlFlowProperties
						.contains(matchingElement)) {
					elementType = "flowproperties";
				} else if (MagicDrawManager2.mdSysmlAssociationBlocks
						.contains(matchingElement)) {
					elementType = "associationblocks";
				} else if (MagicDrawManager2.mdSysmlPackages
						.contains(matchingElement)) {
					elementType = "packages";
				}

				String qualifiedName = null;
				if (matchingElement instanceof NamedElement) {
					qualifiedName = ((NamedElement) matchingElement)
							.getQualifiedName();
				} else {
					qualifiedName = matchingElement.getID();
				}

				URI elementURI = URI.create(MagicDrawManager2.baseHTTPURI
						+ "/services/" + MagicDrawManager2.projectId + "/"
						+ elementType + "/" + qualifiedName);

				// replace do query with result of first query
				String doQueryWithReplacedQueryVariable = doQuery.replace("$"
						+ foreachQueryVariable, elementURI.toString());
				doQueryWithReplacedQueryVariable = doQueryWithReplacedQueryVariable
						.replace("\u00A0", " ");
				if (doQueryWithReplacedQueryVariable.contains("ASK")) {
					boolean isPatternFound = executeSPARQLAskQuery(
							doQueryWithReplacedQueryVariable, project,
							timeStamp, constraintName);
					resultsOfASKQueries.add(isPatternFound);
				} else if (doQueryWithReplacedQueryVariable.contains("SELECT")) {
					executeSPARQLSelectQuery(doQueryWithReplacedQueryVariable,
							project, timeStamp, constraintName);
				}

			}

			StringBuffer queryResultsBuffer = createHTMLTableOfForEachQueryResults(
					firstQueryVariables, matchingElementsOfFORQuery,
					resultsOfASKQueries);
			htmlTableBuffer.append(queryResultsBuffer);
			String tableInHTML = htmlTableBuffer.toString();
			Application.getInstance().getGUILog().log(tableInHTML);

			// StringBuffer htmlTableBuffer =
			// prepareHTMLTableOfQueryResults(sparqlQuery);
			// StringBuffer queryResultsBuffer = createHTMLTableOfQueryResults(
			// matchingElements, queryVariables);
			// htmlTableBuffer.append(queryResultsBuffer);
			// String tableInHTML = htmlTableBuffer.toString();
			// Application.getInstance().getGUILog().log(tableInHTML);
			 saveQueryResultsInConstrainedElements(tableInHTML, project,
			 constraintName, timeStamp);

			// int selectIndex = sparqlQuery.indexOf("SELECT");
			// int whereIndex = sparqlQuery.indexOf("WHERE");
			// String selectLine = sparqlQuery.substring(selectIndex,
			// whereIndex);
			// Pattern pattern = Pattern.compile("(?=\\?).+?(?=\\s|$)");
			// Matcher matcher = pattern.matcher(selectLine);
			// ArrayList<String> queryVariables = new ArrayList<String>();
			// while (matcher.find()) {
			// // System.out.print("Start index: " + matcher.start());
			// // System.out.print(" End index: " + matcher.end() + " ");
			// // System.out.println(matcher.group());
			// String match = matcher.group();
			// match = match.replace("?", "");
			// queryVariables.add(match);
			// }

		} else if (sparqlQuery.contains("ASK ")) {
			executeSPARQLAskQuery(sparqlQuery, project, timeStamp,
					constraintName);

		} else if (sparqlQuery.contains("SELECT ")) {
			executeSPARQLSelectQuery(sparqlQuery, project, timeStamp,
					constraintName);
		} else {
			JOptionPane
					.showMessageDialog(
							MDDialogParentProvider.getProvider()
									.getDialogParent(),
							"ERROR: Constraint expression body is not a SPARQL ASK nor a SPARQL SELECT query");
			return;
		}

		SessionManager.getInstance().closeSession();
	}

	private StringBuffer createHTMLTableOfForEachQueryResults(
			ArrayList<String> firstQueryVariables,
			ArrayList<Element> matchingElementsOfFORQuery,
			ArrayList<Boolean> resultsOfASKQueries) {
		StringBuffer strBuffer = new StringBuffer();

		// create HTML table
		String[] columnNames = new String[3];
		columnNames[0] = "Match";
		columnNames[1] = firstQueryVariables.get(0);
		columnNames[2] = "Result of ASK Query";

		strBuffer.append("<tr>");
		for (String columnName : columnNames) {
			strBuffer
					.append("<td  style=\"padding: 5px;text-align: left;font-weight:bold;\">"
							+ columnName + "</td>");
		}
		strBuffer.append("</tr>");

		Object[][] data = new Object[matchingElementsOfFORQuery.size()][2];
		int j = 0;
		for (Element element : matchingElementsOfFORQuery) {

			// start new row in HTML table
			strBuffer.append("<tr>");

			int k = 0;

			// add match number to HTML table
			if (matchingElementsOfFORQuery.size() > 0) {
				strBuffer
						.append("<td style=\"padding: 5px;text-align: left;\">"
								+ (j + 1) + "</td>");
			}

			String elementDescription = getElementDescription(element);
			if (element == null) {
				data[j][k] = "null";
				continue;
			}

			data[j][k] = elementDescription;
//			strBuffer.append("<td style=\"padding: 5px;text-align: left;\">"
//					+ elementDescription + "</td>");
			
			String hyperlink = "mdel://" + element.getID();
			strBuffer
			.append("<td style=\"padding: 5px;text-align: left;\">"
					+ "<a href=\"" + hyperlink + "\">" + elementDescription + "</a>" + "</td>");
			
			strBuffer.append("<td style=\"padding: 5px;text-align: left;\">"
					+ resultsOfASKQueries.get(j) + "</td>");
			
			
			k++;

			
			
			// close new row in HTML table
			strBuffer.append("</tr>");

			j++;
		}

		// close HTML table
		strBuffer.append("</table>");
		return strBuffer;

	}

	private StringBuffer prepareHTMLTableOfForEachQueryResults(
			String firstQuery, String doQuery, String foreachQueryVariable) {
		StringBuffer strBuffer = new StringBuffer();
		int selectIndex = firstQuery.indexOf("SELECT");
		String miniQuery = firstQuery.substring(selectIndex,
				firstQuery.length());
		String queryPrefixes = firstQuery.substring(0, selectIndex);

		// add html line breaks and replace special characters
		queryPrefixes = queryPrefixes.replace("\n", "<br>");
		queryPrefixes = queryPrefixes.replace("<http:/", "&lt;http:/");
		miniQuery = miniQuery.replace("\n", "<br>");
		miniQuery = miniQuery.replace("<http:/", "&lt;http:/");

		strBuffer.append("<h2>SPARQL FOREACH QUERY</h2>");
		strBuffer.append("<h2> FOR EACH " + foreachQueryVariable + "</h2>");
		strBuffer.append("<i>" + queryPrefixes + "</i>");
		strBuffer.append("<h3>" + miniQuery + "</h3>");

		// do query
		int selectIndex2 = doQuery.indexOf("ASK");
		String miniQuery2 = doQuery.substring(selectIndex2, doQuery.length());
		String queryPrefixes2 = doQuery.substring(0, selectIndex2);

		// add html line breaks and replace special characters
		queryPrefixes2 = queryPrefixes2.replace("\n", "<br>");
		queryPrefixes2 = queryPrefixes2.replace("<http:/", "&lt;http:/");
		miniQuery2 = miniQuery2.replace("\n", "<br>");
		miniQuery2 = miniQuery2.replace("<http:/", "&lt;http:/");

		strBuffer.append("<h2> DO " + "</h2>");
		strBuffer.append("<i>" + queryPrefixes2 + "</i>");
		strBuffer.append("<h3>" + miniQuery2 + "</h3>");

		// creating the HTML table
		strBuffer.append("<table style=\"border: 1px solid black;\">");
		return strBuffer;
	}

	private boolean executeSPARQLAskQuery(String sparqlQuery, Project project,
			String timeStamp, String constraintName) {
		StringBuffer htmlTableBuffer = prepareHTMLTableOfASKQueryResults(sparqlQuery);
		// execute SPARQL ASK query
		boolean isPatternFound = MagicDrawManager2
				.performSPARQLASKQuery(sparqlQuery);
		htmlTableBuffer.append("<h3>" + "Result: " + isPatternFound + "</h3>");
		Application.getInstance().getGUILog().log(htmlTableBuffer.toString());
		saveASKQueryResultInConstrainedElements(isPatternFound, project,
				constraintName, timeStamp);
		return isPatternFound;
	}

	private StringBuffer prepareHTMLTableOfASKQueryResults(String sparqlQuery) {
		StringBuffer strBuffer = new StringBuffer();

		// get all query variables
		int askIndex = sparqlQuery.indexOf("ASK");
		String miniQuery = sparqlQuery
				.substring(askIndex, sparqlQuery.length());
		miniQuery = miniQuery.replace("<http:/", "&lt;http:/");
		String queryPrefixes = sparqlQuery.substring(0, askIndex);

		// add html line breaks
		// queryPrefixes = queryPrefixes.replace("\n", "<br>");
		queryPrefixes = queryPrefixes.replace("<http:/", "&lt;http:/");

		strBuffer.append("<h2>SPARQL ASK QUERY</h2>");
		strBuffer.append("<i>" + queryPrefixes + "</i>");
		strBuffer.append("<h3>" + miniQuery + "</h3>");
		return strBuffer;
	}

	private void saveASKQueryResultInConstrainedElements(
			boolean isPatternFound, Project project, String constraintName,
			String timeStamp) {
		List<Element> constraintedElements = constraint.getConstrainedElement();
		for (Element element : constraintedElements) {
			if (element instanceof Property) {
				Property property = (Property) element;
				LiteralString litString = project.getElementsFactory()
						.createLiteralStringInstance();
				String propertyValue = "RESULT of " + constraintName + " @ "
						+ timeStamp + ":   " + isPatternFound;
				litString.setValue(propertyValue);
				property.setDefaultValue(litString);
				litString.setValue(propertyValue);
				ModelHelper.setComment(property, "<html>" + "<p>" + timeStamp
						+ "</p>" + "<p>" + "SPARQL ASK QUERY" + "</p>" + "<p>"
						+ "Result: " + isPatternFound + "</p>" + "</html>");
			}
		}

	}

	private void saveQueryResultsInConstrainedElements(String tableInHTML,
			Project project, String constraintName, String timeStamp) {
		// save query results in constrained elements
		List<Element> constraintedElements = constraint.getConstrainedElement();
		for (Element element : constraintedElements) {
			if (element instanceof Property) {
				Property property = (Property) element;
				LiteralString litString = project.getElementsFactory()
						.createLiteralStringInstance();
				String propertyValue = "RESULT of " + constraintName + " @ "
						+ timeStamp;
				litString.setValue(propertyValue);
				property.setDefaultValue(litString);
				ModelHelper.setComment(property, "<html>" + "<p>" + timeStamp
						+ "</p>" + tableInHTML + "</html>");
			}
		}

	}

	private ArrayList<String> getQueryVariables(String sparqlQuery) {
		// get all query variables

		int selectIndex = sparqlQuery.indexOf("SELECT");
		int whereIndex = sparqlQuery.indexOf("WHERE");
		String selectLine = sparqlQuery.substring(selectIndex, whereIndex);
		Pattern pattern = Pattern.compile("(?=\\?).+?(?=\\s|$)");
		Matcher matcher = pattern.matcher(selectLine);
		ArrayList<String> queryVariables = new ArrayList<String>();
		while (matcher.find()) {
			// System.out.print("Start index: " + matcher.start());
			// System.out.print(" End index: " + matcher.end() + " ");
			// System.out.println(matcher.group());
			String match = matcher.group();
			match = match.replace("?", "");
			// quick fix - better aapproach is to write a better regex
						if(!(selectLine.contains(match + " AS") | selectLine.contains(match + "AS")) | selectLine.contains(match + " as") | selectLine.contains(match + "as")){
							match = match.replace(")", "");
							queryVariables.add(match);
						}
//			queryVariables.add(match);
		}
		return queryVariables;
	}

	private void executeSPARQLSelectQuery(String sparqlQuery, Project project,
			String timeStamp, String constraintName) {
		ArrayList<String> queryVariables = getQueryVariables(sparqlQuery);
		ArrayList<ArrayList<Element>> matchingElements = MagicDrawManager2
				.performSPARQLSELECTQuery(sparqlQuery, queryVariables);
		StringBuffer htmlTableBuffer = prepareHTMLTableOfQueryResults(sparqlQuery);
		StringBuffer queryResultsBuffer = createHTMLTableOfQueryResults(
				matchingElements, queryVariables);
		htmlTableBuffer.append(queryResultsBuffer);
		String tableInHTML = htmlTableBuffer.toString();
		Application.getInstance().getGUILog().log(tableInHTML);
		saveQueryResultsInConstrainedElements(tableInHTML, project,
				constraintName, timeStamp); 
//		saveMatchingElementsAsHyperLinks(matchingElements);

		// TextTable tt = new TextTable(columnNames, data);
		// // this adds the numbering on the left
		// tt.setAddRowNumbering(true);
		// // sort by the first column
		// tt.setSort(0);
		// // tt.printTable();
		// ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// PrintStream ptr = new PrintStream(baos);
		// tt.printTable(ptr, 0);

	}

	private void saveMatchingElementsAsHyperLinks(
			ArrayList<ArrayList<Element>> matchingElements, Project project) {


		// get a stereotype
		Stereotype stereotype = StereotypesHelper.getStereotype(project,
		"HyperlinkOwner");
		
		// remove any previous stereoptype applications and stereoptype values
		StereotypesHelper.removeStereotype(constraint, stereotype);
		
		// add hyperlinks
		// apply a stereotype
		StereotypesHelper.addStereotype(constraint, stereotype);
		
//		for (ArrayList<Element> arrayList : matchingElements) {
//			// add hyperlinks
//			StereotypesHelper.setStereotypePropertyValue(constraint, stereotype,
//			"hyperlinkModel", linkedElement, true);
//			StereotypesHelper.setStereotypePropertyValue(element, stereotype,
//			"hyperlinkModel", activeLinkedElement, true);
//			String activeHttpLink = "http://www.nomagic.com";
//			StereotypesHelper.setStereotypePropertyValue(element, stereotype,
//			"hyperlinkText", "http://www.nomagic.com", true);
//			StereotypesHelper.setStereotypePropertyValue(element, stereotype,
//			"hyperlinkText", activeHttpLink, true);
//		}
		

		
		
		
	}

	private StringBuffer prepareHTMLTableOfQueryResults(String sparqlQuery) {
		StringBuffer strBuffer = new StringBuffer();
		int selectIndex = sparqlQuery.indexOf("SELECT");
		String miniQuery = sparqlQuery.substring(selectIndex,
				sparqlQuery.length());
		String queryPrefixes = sparqlQuery.substring(0, selectIndex);

		// add html line breaks and replace special characters
		queryPrefixes = queryPrefixes.replace("\n", "<br>");
		queryPrefixes = queryPrefixes.replace("<http:/", "&lt;http:/");
		miniQuery = miniQuery.replace("\n", "<br>");
		miniQuery = miniQuery.replace("< ", "&lt; ");

		strBuffer.append("<h2>SPARQL SELECT QUERY</h2>");
		strBuffer.append("<i>" + queryPrefixes + "</i>");
		strBuffer.append("<h3>" + miniQuery + "</h3>");

		// creating the HTML table
		strBuffer.append("<table style=\"border: 1px solid black;\">");
		return strBuffer;
	}

	private StringBuffer createHTMLTableOfQueryResults(
			ArrayList<ArrayList<Element>> matchingElements,
			ArrayList<String> queryVariables) {
		StringBuffer strBuffer = new StringBuffer();

		// create HTML table
		String[] columnNames = new String[queryVariables.size()];
		int i = 0;
		for (String queryVariable : queryVariables) {
			columnNames[i] = queryVariable;
			i++;
		}

		strBuffer.append("<tr>");
		strBuffer
				.append("<td  style=\"padding: 5px;text-align: left;font-weight:bold;\">"
						+ "Match" + "</td>");
		for (String columnName : columnNames) {
//			strBuffer
//					.append("<td  style=\"padding: 5px;text-align: left;font-weight:bold;\">"
//							+ columnName + "</td>");
			strBuffer
			.append("<td  style=\"padding: 5px;text-align: left;font-weight:bold;\">"
					+ columnName );
//			strBuffer
//			.append("<a href=\"http://www.w3schools.com\">Visit W3Schools</a>");
			
//			mdel://_17_0_3_1_3a6018b_1407985200685_95613_13821
				
				
			strBuffer
			.append("</td>");
			
			
		}
		strBuffer.append("</tr>");

		Object[][] data = new Object[matchingElements.size()][queryVariables
				.size()];
		int j = 0;
		for (ArrayList<Element> matchresults : matchingElements) {

			// start new row in HTML table
			strBuffer.append("<tr>");

			int k = 0;

			// add match number to HTML table
			if (matchresults.size() > 0) {
				strBuffer
						.append("<td style=\"padding: 5px;text-align: left;\">"
								+ (j + 1) + "</td>");
			}

			for (Element element : matchresults) {
				String elementDescription = getElementDescription(element);
				if (element == null) {
					data[j][k] = "null";
					continue;
				}

				data[j][k] = elementDescription;
//				strBuffer
//						.append("<td style=\"padding: 5px;text-align: left;\">"
//								+ elementDescription + "</td>");
				String hyperlink = "mdel://" + element.getID();
				strBuffer
				.append("<td style=\"padding: 5px;text-align: left;\">"
						+ "<a href=\"" + hyperlink + "\">" + elementDescription + "</a>" + "</td>");
				

				
				k++;
			}

			// close new row in HTML table
			strBuffer.append("</tr>");

			j++;
		}

		// close HTML table
		strBuffer.append("</table>");
		return strBuffer;

	}

	private String getElementDescription(Element element) {

		String elementDescription = null;
		if (element == null) {
			return null;
		}
		if (element instanceof LiteralString) {
			LiteralString literalString = (LiteralString)element;
			elementDescription = literalString.getValue();
		}
		else if (element instanceof NamedElement) {
			if (element.getHumanType().equals("Connector")) {
				StringBuffer connectorDescription = new StringBuffer();

				Connector connector = (Connector) element;
				if (connector.getEnd().size() == 2) {
					ConnectorEnd connectorEnd1 = connector.getEnd().get(0);
					ConnectorEnd connectorEnd2 = connector.getEnd().get(1);

					if (connectorEnd1.getPartWithPort() != null) {
						// connectorDescription.append("PartWithPort: ");
						connectorDescription.append(connectorEnd1
								.getPartWithPort().getName());
					}
					connectorDescription.append(":");
					if (connectorEnd1.getRole() != null) {
						// connectorDescription.append("Role: ");
						connectorDescription.append(connectorEnd1.getRole()
								.getName());
					}
					connectorDescription.append(" &lt;---> ");

					if (connectorEnd2.getPartWithPort() != null) {
						// connectorDescription.append("PartWithPort: ");
						connectorDescription.append(connectorEnd2
								.getPartWithPort().getName());
					}
					connectorDescription.append(":");
					if (connectorEnd2.getRole() != null) {
						// connectorDescription.append("Role: ");
						connectorDescription.append(connectorEnd2.getRole()
								.getName());
					}
					elementDescription = new String(
							"Type: <b>"
									+ element.getHumanType()
									+ "</b>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
									+ connectorDescription.toString()
									+ "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; Owner qualified name: <b>"
									+ ((NamedElement) element.getOwner())
											.getQualifiedName() + "</b>\r\n");
				}

			} else {
				NamedElement namedElement = (NamedElement) element;
				elementDescription = new String("Type: "
						+ element.getHumanType() + " --- QualifiedName: "
						+ namedElement.getQualifiedName() + " --- Owner name: "
						+ ((NamedElement) element.getOwner()).getName()
						+ "\r\n");
			}

		} 		 
		else {

			elementDescription = new String("Type: " + element.getHumanType()
					+ " --- ID: " + element.getHumanName()
					+ " --- Owner name: "
					+ ((NamedElement) element.getOwner()).getName() + "\r\n");

		}

		return elementDescription;
	}

}

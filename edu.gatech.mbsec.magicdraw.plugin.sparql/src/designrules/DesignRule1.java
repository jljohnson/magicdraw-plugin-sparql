package designrules;

import java.io.InputStream;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.util.FileManager;

public class DesignRule1 {

	public static void main(String[] args) {
		
		boolean isDesignRuleViolated = false;
		
		String triplestoreVersion = "TractorModel";
		
		// load model from triplestore
		String directory = "C:\\Program Files\\eclipse-jee-kepler-SR1-2\\workspace\\sparql-design-rule\\triplestore\\tdb" + triplestoreVersion;
		Dataset dataset = TDBFactory.createDataset(directory);
		Model model = dataset.getDefaultModel();
		
		
		
		// Create a new query
//		String queryString = 
//			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
//			"PREFIX sysml: <http://omg.org/sysml/rdf#> \n" +
//			"SELECT ?s \n" +
//			"WHERE {\n" +
//			"    ?s  rdf:type sysml:Block . \n" +			
//			"      }\n";
		
		
//		String queryString = 
//		"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
//		"PREFIX sysml: <http://omg.org/sysml/rdf#> \n" +
//		"PREFIX sysml_namedelement: <http://omg.org/sysml/rdf#NamedElement/> \n" +
//		"PREFIX sysml_block: <http://omg.org/sysml/rdf#Block/> \n" +
//		"PREFIX sysml_property: <http://omg.org/sysml/rdf#Property/> \n" +
//		"SELECT ?partProperty ?partPropertyType \n" +
//		"WHERE {\n" +
//		"    ?block  rdf:type sysml:Block . \n" +
//		"    ?block  sysml_namedelement:name \"Tractor\"^^rdf:XMLLiteral . \n" +
//		"    ?block  sysml_block:partProperty ?partProperty . \n" +
//		"    ?partProperty  sysml_property:type ?partPropertyType . \n" +
//		"      }\n";
		
//		String queryString = 
//				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
//				"PREFIX sysml: <http://omg.org/sysml/rdf#> \n" +
//				"PREFIX sysml_namedelement: <http://omg.org/sysml/rdf#NamedElement/> \n" +
//				"PREFIX sysml_block: <http://omg.org/sysml/rdf#Block/> \n" +
//				"PREFIX sysml_partproperty: <http://omg.org/sysml/rdf#PartProperty/> \n" +
//				"SELECT ?partProperty ?partPropertyType \n" +
//				"WHERE {\n" +
//				"    ?partProperty  rdf:type sysml:PartProperty . \n" +
//				"      }\n";
		
		String queryString = 
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
				"PREFIX sysml: <http://omg.org/sysml/rdf#> \n" +
				"PREFIX sysml_namedelement: <http://omg.org/sysml/rdf#NamedElement/> \n" +
				"PREFIX sysml_block: <http://omg.org/sysml/rdf#Block/> \n" +
				"PREFIX sysml_property: <http://omg.org/sysml/rdf#Property/> \n" +
				"SELECT ?partProperty  \n" +
				"WHERE {\n" +
				"    ?block  rdf:type sysml:Block . \n" +
				"    ?block  sysml_namedelement:name \"Tractor\"^^rdf:XMLLiteral . \n" +
				"    ?block  sysml_block:partProperty ?partProperty . \n" +
				" }\n"
				
				; 
		
//		String designRule1QueryString = 
//				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
//				"PREFIX sysml: <http://omg.org/sysml/rdf#> \n" +
//				"PREFIX sysml_namedelement: <http://omg.org/sysml/rdf#NamedElement/> \n" +
//				"PREFIX sysml_block: <http://omg.org/sysml/rdf#Block/> \n" +
//				"PREFIX sysml_property: <http://omg.org/sysml/rdf#Property/> \n" +
//				"PREFIX sysml_connector: <http://omg.org/sysml/rdf#Connector/> \n" +
//				"PREFIX sysml_connectorend: <http://omg.org/sysml/rdf#ConnectorEnd/> \n" +				
//				"ASK WHERE {\n" +
//				"    ?connector  rdf:type sysml:Connector . \n" +
//				
//				" }\n"				
//				; 
		
		String designRule1QueryString = 
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
				"PREFIX sysml: <http://omg.org/sysml/rdf#> \n" +
				"PREFIX sysml_namedelement: <http://omg.org/sysml/rdf#NamedElement/> \n" +
				"PREFIX sysml_block: <http://omg.org/sysml/rdf#Block/> \n" +
				"PREFIX sysml_property: <http://omg.org/sysml/rdf#Property/> \n" +
				"PREFIX sysml_connector: <http://omg.org/sysml/rdf#Connector/> \n" +
				"PREFIX sysml_connectorend: <http://omg.org/sysml/rdf#ConnectorEnd/> \n" +
				"SELECT DISTINCT ?connector ?connectorEnd1Type ?connectorEnd2Type ?commonConnectorEndType \n" +			
				"WHERE {\n" +
				"    ?connector  rdf:type sysml:Connector . \n" +
				"    ?connector  sysml_connector:end ?connectorEnd1 . \n" +
				"    ?connectorEnd1  sysml_connectorend:role ?connectorEnd1Role . \n" +
				"    ?connectorEnd1Role  sysml_property:type ?connectorEnd1Type . \n" +
				"    ?connector  sysml_connector:end ?connectorEnd2 . \n" +
				"    ?connectorEnd2  sysml_connectorend:role ?connectorEnd2Role . \n" +
				"    ?connectorEnd2Role  sysml_property:type ?connectorEnd2Type . \n" +	
				"	 OPTIONAL { ?connectorEnd1Type sysml_block:inheritedBlock+ ?commonConnectorEndType . "
				+ "				?connectorEnd2Type sysml_block:inheritedBlock+ ?commonConnectorEndType } . \n" +	
				"   FILTER (?connectorEnd1Type != ?connectorEnd2Type) \n" +						
//				"   FILTER (NOT EXISTS { ?connectorEnd1Type sysml_block:inheritedBlock+ ?connectorEnd2Type }) \n" +	
//				"   FILTER (NOT EXISTS { ?connectorEnd2Type sysml_block:inheritedBlock+ ?connectorEnd1Type }) \n" +	
				"   FILTER (NOT EXISTS { ?connectorEnd1Type sysml_block:inheritedBlock+ ?commonConnectorEndType .  \n" +	
				"    				 ?connectorEnd2Type sysml_block:inheritedBlock+ ?commonConnectorEndType }) \n" +
				" }\n"
				
				; 
		
		
		
//		// SPARQL SELECT for demo
//		String queryString = 
//				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
//				"PREFIX sysml: <http://omg.org/sysml/rdf#> \n" +
//				"PREFIX sysml_namedelement: <http://omg.org/sysml/rdf#NamedElement/> \n" +
//				"PREFIX sysml_block: <http://omg.org/sysml/rdf#Block/> \n" +
//				"PREFIX sysml_property: <http://omg.org/sysml/rdf#Property/> \n" +
//				"SELECT ?block ?partProperty ?partPropertyType ?fullPort ?fullPortType \n" +
//				"WHERE {\n" +
//				"    ?block  rdf:type sysml:Block . \n" +
//				"    ?block  sysml_namedelement:name \"Tractor\"^^rdf:XMLLiteral . \n" +
//				"    ?block  sysml_block:partProperty ?partProperty . \n" +
//				"    ?partProperty  sysml_property:type ?partPropertyType . \n" +
//				"    ?partPropertyType  sysml_block:fullPort ?fullPort . \n" +
//				"    ?fullPort  sysml_property:type ?fullPortType . \n" +
//				"    FILTER NOT EXISTS { ?fullPortType sysml_namedelement:name \"CAN\"^^rdf:XMLLiteral } \n" +	
//				" }\n";
		
//		String queryString = 
//				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
//				"PREFIX sysml: <http://omg.org/sysml/rdf#> \n" +
//				"PREFIX sysml_namedelement: <http://omg.org/sysml/rdf#NamedElement/> \n" +
//				"PREFIX sysml_block: <http://omg.org/sysml/rdf#Block/> \n" +
//				"PREFIX sysml_property: <http://omg.org/sysml/rdf#Property/> \n" +
//				"ASK WHERE \n" +
//				" {\n" +
//				"    ?block  rdf:type sysml:Block . \n" +
//				"    ?block  sysml_namedelement:name \"Tractor\"^^rdf:XMLLiteral . \n" +
//				"    ?block  sysml_block:partProperty ?partProperty . \n" +
//				"    ?partProperty  sysml_property:type ?partPropertyType . \n" +
//				"    ?partPropertyType  sysml_block:fullPort ?fullPort . \n" +
//				"    ?fullPort  sysml_property:type ?fullPortType . \n" +
//				"    ?fullPortType  sysml_namedelement:name \"CAN\"^^rdf:XMLLiteral . \n" +
//				" }\n";
		
//		// SPARQL ASK for demo!!
//		String queryString = 
//				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
//				"PREFIX sysml: <http://omg.org/sysml/rdf#> \n" +
//				"PREFIX sysml_namedelement: <http://omg.org/sysml/rdf#NamedElement/> \n" +
//				"PREFIX sysml_block: <http://omg.org/sysml/rdf#Block/> \n" +
//				"PREFIX sysml_property: <http://omg.org/sysml/rdf#Property/> \n" +
//				"ASK WHERE \n" +
//				" {\n" +
//				"    ?block  rdf:type sysml:Block . \n" +
//				"    ?block  sysml_namedelement:name \"Tractor\"^^rdf:XMLLiteral . \n" +
//				"    ?block  sysml_block:partProperty ?partProperty . \n" +
//				"    ?partProperty  sysml_property:type ?partPropertyType . \n" +
//				"    ?partPropertyType  sysml_block:fullPort ?fullPort . \n" +
//				"    ?fullPort  sysml_property:type ?fullPortType . \n" +
//				"    FILTER NOT EXISTS { ?fullPortType sysml_namedelement:name \"CAN\"^^rdf:XMLLiteral } \n" +	
//				"    OPTIONAL { ?partPropertyType  sysml_block:fullPort ?fullPort2 .  \n" +
//				"    			?fullPort2  sysml_property:type ?fullPortType2 . \n" +
//				"    	FILTER NOT EXISTS { ?fullPortType2 sysml_namedelement:name \"CAN\"^^rdf:XMLLiteral } \n" +
//				" 	 }\n" + 
//				" }\n";
		
//		String queryString = 
//				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
//				"PREFIX sysml: <http://omg.org/sysml/rdf#> \n" +
//				"PREFIX sysml_namedelement: <http://omg.org/sysml/rdf#NamedElement/> \n" +
//				"ASK WHERE \n" +
//				" {\n" +
//				"    ?s  rdf:type sysml:Block . \n" +
////				"    FILTER ( regex( ?s, \"Tractor\", \"i\")) \n" +
//				"    FILTER ( ?s sysml_namedelement:name \"Tractor\") \n" +
//				"      }\n";
		
		Query query = QueryFactory.create(designRule1QueryString);
//		System.out.println("*** SPARQL Query ***");
//		System.out.println("");
//		System.out.println(queryString);
//		System.out.println("");
//		System.out.println("*** SPARQL Result ***");
//		System.out.println("");
		
		// Execute the query and obtain results
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		
		
		ResultSet results = qe.execSelect();
		ResultSetFormatter.out(System.out, results, query);
		
//		System.out.println(qe.execAsk());

		
		
		
			
			
			
		

		// Important - free up resources used running the query
		qe.close();		
	}
}

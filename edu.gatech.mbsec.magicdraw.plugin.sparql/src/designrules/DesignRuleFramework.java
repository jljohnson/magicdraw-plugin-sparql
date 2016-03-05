package designrules;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.util.FileManager;

public class DesignRuleFramework {

	public static void main(String[] args) {
		String projectId = "TractorModel";		
		MagicDrawManager.loadSysMLProject(projectId);
		removeTripleStoreTriples(projectId);
		createAndPopulateTriplestore(projectId);
		
		
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
				"   FILTER (NOT EXISTS { ?connectorEnd1Type sysml_block:inheritedBlock+ ?connectorEnd2Type }) \n" +	
				"   FILTER (NOT EXISTS { ?connectorEnd2Type sysml_block:inheritedBlock+ ?connectorEnd1Type }) \n" +	
				"   FILTER (NOT EXISTS { ?connectorEnd1Type sysml_block:inheritedBlock+ ?commonConnectorEndType .  \n" +	
				"    				 ?connectorEnd2Type sysml_block:inheritedBlock+ ?commonConnectorEndType }) \n" +
				" }\n"
				
				; 
		
		
		runDesignRule(projectId, designRule1QueryString);
	}

	
	public static void removeTripleStoreTriples(String projectID) {
				
		//load model from triplestore
		String directory = "C:\\Program Files\\eclipse-jee-kepler-SR1-2\\workspace\\sparql-design-rule\\triplestore\\tdb" + projectID;
		Dataset dataset = TDBFactory.createDataset(directory);
		Model model = dataset.getDefaultModel();
		
		model.removeAll();
		dataset.close();
	}
	
	public static void createAndPopulateTriplestore(String projectID) {
		
		String triplestoreVersion = projectID;
		
		// create TDB dataset
		String directory = "C:\\Program Files\\eclipse-jee-kepler-SR1-2\\workspace\\sparql-design-rule\\triplestore\\tdb" + triplestoreVersion;
		
		
		Dataset dataset = TDBFactory.createDataset(directory);
		
		
		
		// populate model of TDB dataset with PTC Integrity requirements of RDF
		// file
		Model tdbModel = dataset.getDefaultModel();
		tdbModel.removeAll();
		
		String source = "file:C:\\Program Files\\eclipse-jee-kepler-SR1-2\\workspace\\sparql-design-rule\\sysml_blocks_tractor_model" + triplestoreVersion + ".rdf";
		FileManager.get().readModel(tdbModel, source);
				
		String source2 = "file:C:\\Program Files\\eclipse-jee-kepler-SR1-2\\workspace\\sparql-design-rule\\sysml_partproperties_tractor_model" + triplestoreVersion + ".rdf";
		FileManager.get().readModel(tdbModel, source2);
		
		String source3 = "file:C:\\Program Files\\eclipse-jee-kepler-SR1-2\\workspace\\sparql-design-rule\\sysml_fullports_tractor_model" + triplestoreVersion + ".rdf";
		FileManager.get().readModel(tdbModel, source3);
		
		String source4 = "file:C:\\Program Files\\eclipse-jee-kepler-SR1-2\\workspace\\sparql-design-rule\\sysml_connectors_tractor_model" + triplestoreVersion + ".rdf";
		FileManager.get().readModel(tdbModel, source4);
		
		String source5 = "file:C:\\Program Files\\eclipse-jee-kepler-SR1-2\\workspace\\sparql-design-rule\\sysml_connectorends_tractor_model" + triplestoreVersion + ".rdf";
		FileManager.get().readModel(tdbModel, source5);
		
		tdbModel.close();
		dataset.close();
	}
	
public static void runDesignRule(String projectID, String queryString) {
		
		boolean isDesignRuleViolated = false;
		
		String triplestoreVersion = projectID;
		
		// load model from triplestore
		String directory = "C:\\Program Files\\eclipse-jee-kepler-SR1-2\\workspace\\sparql-design-rule\\triplestore\\tdb" + triplestoreVersion;
		Dataset dataset = TDBFactory.createDataset(directory);
		Model model = dataset.getDefaultModel();
		
		Query query = QueryFactory.create(queryString);

		
		// Execute the query and obtain results
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		
		
		ResultSet results = qe.execSelect();
		ResultSetFormatter.out(System.out, results, query);
		
//		System.out.println(qe.execAsk());



		// Important - free up resources used running the query
		qe.close();		
	}
}

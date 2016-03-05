package designrules;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.hp.hpl.jena.assembler.assemblers.FileModelAssembler;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.util.FileManager;

public class CreateAndPopulateTriplestore {

	public static void main(String[] args) {
		
		String triplestoreVersion = "52";
		
		// create TDB dataset
		String directory = "C:\\Program Files\\eclipse-jee-luna-R-win32-x86_64\\workspace\\MagicDrawDesignRulePlugin\\triplestore\\tdb" + triplestoreVersion;
		
		
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
}

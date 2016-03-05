package designrules;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.tdb.TDBFactory;

public class RemoveTripleStoreTriples {

	public static void main(String[] args) {
		String triplestoreVersion = "52";
		
		//load model from triplestore
		String directory = "C:\\Program Files\\eclipse-jee-kepler-SR1-2\\workspace\\sparql-design-rule\\triplestore\\tdb" + triplestoreVersion;
		Dataset dataset = TDBFactory.createDataset(directory);
		Model model = dataset.getDefaultModel();
		
		model.removeAll();
		dataset.close();
	}

}

/*********************************************************************************************
 * Copyright (c) 2014 Model-Based Systems Engineering Center, Georgia Institute of Technology.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 *  
 *  The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *  and the Eclipse Distribution License is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 *  
 *  Contributors:
 *  
 *	   Axel Reichwein (axel.reichwein@koneksys.com)		- initial implementation 
 *	   Sebastian Herzig (sebastian.herzig@me.gatech.edu) - support for loading multiple MagicDraw models at the same time      
 *******************************************************************************************/

package designrules;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.xml.datatype.DatatypeConfigurationException;

import org.eclipse.lyo.adapter.magicdraw.resources.SysMLAssociationBlock;
import org.eclipse.lyo.adapter.magicdraw.resources.SysMLBlock;
import org.eclipse.lyo.adapter.magicdraw.resources.SysMLBlockDiagram;
import org.eclipse.lyo.adapter.magicdraw.resources.SysMLBoundReference;
import org.eclipse.lyo.adapter.magicdraw.resources.SysMLConnector;
import org.eclipse.lyo.adapter.magicdraw.resources.SysMLConnectorEnd;
import org.eclipse.lyo.adapter.magicdraw.resources.SysMLFlowDirection;
import org.eclipse.lyo.adapter.magicdraw.resources.SysMLFlowProperty;
import org.eclipse.lyo.adapter.magicdraw.resources.SysMLFullPort;
import org.eclipse.lyo.adapter.magicdraw.resources.SysMLInterfaceBlock;
import org.eclipse.lyo.adapter.magicdraw.resources.SysMLInternalBlockDiagram;
import org.eclipse.lyo.adapter.magicdraw.resources.SysMLItemFlow;
import org.eclipse.lyo.adapter.magicdraw.resources.SysMLModel;
import org.eclipse.lyo.adapter.magicdraw.resources.SysMLPackage;
import org.eclipse.lyo.adapter.magicdraw.resources.SysMLPartProperty;
import org.eclipse.lyo.adapter.magicdraw.resources.SysMLPort;
import org.eclipse.lyo.adapter.magicdraw.resources.SysMLProxyPort;
import org.eclipse.lyo.adapter.magicdraw.resources.SysMLReferenceProperty;
import org.eclipse.lyo.adapter.magicdraw.resources.SysMLRequirement;
import org.eclipse.lyo.adapter.magicdraw.resources.SysMLValueProperty;
import org.eclipse.lyo.adapter.magicdraw.resources.SysMLValueType;
import org.eclipse.lyo.oslc4j.core.exception.OslcCoreApplicationException;
import org.eclipse.lyo.oslc4j.core.model.Link;
import org.eclipse.lyo.oslc4j.provider.jena.ErrorHandler;
import org.eclipse.lyo.oslc4j.provider.jena.JenaModelHelper;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.algebra.AlgebraGenerator;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.OpAsQuery;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.nomagic.magicdraw.commandline.CommandLine;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.project.ProjectDescriptor;
import com.nomagic.magicdraw.core.project.ProjectDescriptorsFactory;
import com.nomagic.magicdraw.core.project.ProjectsManager;
import com.nomagic.magicdraw.export.image.ImageExporter;
import com.nomagic.magicdraw.openapi.uml.ModelElementsManager;
import com.nomagic.magicdraw.openapi.uml.ReadOnlyElementException;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.magicdraw.sysml.util.SysMLConstants;
import com.nomagic.magicdraw.ui.dialogs.MDDialogParentProvider;
import com.nomagic.magicdraw.uml.BaseElement;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.runtime.ApplicationExitedException;
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdinformationflows.InformationFlow;
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdmodels.Model;
import com.nomagic.uml2.ext.magicdraw.classes.mdassociationclasses.AssociationClass;
import com.nomagic.uml2.ext.magicdraw.classes.mddependencies.Dependency;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.AggregationKindEnum;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Association;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.DataType;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.DirectedRelationship;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.EnumerationLiteral;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Generalization;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.LiteralString;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Namespace;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.PackageableElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.ParameterDirectionKindEnum;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Type;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.ValueSpecification;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdinternalstructures.ConnectableElement;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdinternalstructures.Connector;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdinternalstructures.ConnectorEnd;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdports.Port;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;
import com.nomagic.uml2.impl.ElementsFactory;

import dnl.utils.text.table.TextTable;

/**
 * MagicDrawManager is responsible for converting SysML data into RDF. It is used to load
 * MagicDraw SysML projects, retrieve SysML elements from projects, and map
 * SysML elements to OSLC resources described as POJOs
 * 
 * @author Axel Reichwein (axel.reichwein@koneksys.com)
 */
public class MagicDrawManager2 {

	public static String magicdrawModelsDirectory = "C:/Program Files/eclipse-jee-luna-R-win32-x86_64/workspace/MagicDrawDesignRulePlugin/Magicdraw Models/";
	static String triplestoreVersion = "53";

	static int sessionID = 1;
	static Collection<String> predefinedMagicDrawSysMLPackageNames = new HashSet<String>();

	public static Collection<Class> mdSysmlRequirements = new ArrayList<Class>();
	public static Collection<Class> mdSysmlBlocks = new ArrayList<Class>();
	public static Collection<Class> mdSysmlInterfaceBlocks = new ArrayList<Class>();
	public static Collection<InformationFlow> mdSysmlItemFlows = new ArrayList<InformationFlow>();
	public static Collection<DataType> mdSysmlValueTypes = new ArrayList<DataType>();
	public static Collection<Property> mdSysmlPartProperties = new ArrayList<Property>();
	public static Collection<Connector> mdSysmlConnectors = new ArrayList<Connector>();
	public static Collection<Port> mdSysmlPorts = new ArrayList<Port>();
	public static Collection<Property> mdSysmlValueProperties = new ArrayList<Property>();
	public static Collection<Property> mdSysmlFlowProperties = new ArrayList<Property>();
	public static Collection<Property> mdSysmlBoundReferences = new ArrayList<Property>();
	public static Collection<DiagramPresentationElement> mdSysmlBlockDiagrams = new ArrayList<DiagramPresentationElement>();
	public static Collection<DiagramPresentationElement> mdSysmlInternalBlockDiagrams = new ArrayList<DiagramPresentationElement>();
	public static Collection<com.nomagic.uml2.ext.magicdraw.classes.mdassociationclasses.AssociationClass> mdSysmlAssociationBlocks = new ArrayList<com.nomagic.uml2.ext.magicdraw.classes.mdassociationclasses.AssociationClass>();
	public static Collection<com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package> mdSysmlPackages = new ArrayList<com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package>();

	static Collection<SysMLRequirement> oslcSysmlRequirements = new ArrayList<SysMLRequirement>();
	static Collection<SysMLBlock> oslcSysmlBlocks = new ArrayList<SysMLBlock>();

	static Map<String, Class> idMdSysmlRequirementMap = new HashMap<String, Class>();
	static Map<String, SysMLRequirement> idOslcSysmlRequirementMap = new HashMap<String, SysMLRequirement>();

	// static Map<String, Class> qNameMdSysmlBlockMap = new HashMap<String,
	// Class>();
	static Map<String, SysMLBlock> qNameOslcSysmlBlockMap = new HashMap<String, SysMLBlock>();

	static Map<String, SysMLPartProperty> qNameOslcSysmlPartPropertyMap = new HashMap<String, SysMLPartProperty>();
	static Map<String, SysMLReferenceProperty> qNameOslcSysmlReferencePropertyMap = new HashMap<String, SysMLReferenceProperty>();

	static Map<String, SysMLModel> oslcSysmlModelMap = new HashMap<String, SysMLModel>();
	static Map<String, SysMLPackage> qNameOslcSysmlPackageMap = new HashMap<String, SysMLPackage>();

	static Map<String, SysMLAssociationBlock> qNameOslcSysmlAssociationBlockMap = new HashMap<String, SysMLAssociationBlock>();
	static Map<String, SysMLConnector> qNameOslcSysmlConnectorMap = new HashMap<String, SysMLConnector>();
	static Map<String, SysMLConnectorEnd> qNameOslcSysmlConnectorEndMap = new HashMap<String, SysMLConnectorEnd>();

	static Map<String, SysMLPort> qNameOslcSysmlPortMap = new HashMap<String, SysMLPort>();
	static Map<String, SysMLProxyPort> qNameOslcSysmlProxyPortMap = new HashMap<String, SysMLProxyPort>();
	static Map<String, SysMLFullPort> qNameOslcSysmlFullPortMap = new HashMap<String, SysMLFullPort>();

	static Map<String, SysMLInterfaceBlock> qNameOslcSysmlInterfaceBlockMap = new HashMap<String, SysMLInterfaceBlock>();
	static Map<String, SysMLFlowProperty> qNameOslcSysmlFlowPropertyMap = new HashMap<String, SysMLFlowProperty>();
	static Map<String, SysMLBoundReference> qNameOslcSysmlBoundReferenceMap = new HashMap<String, SysMLBoundReference>();
	
	
	
	static Map<String, SysMLItemFlow> qNameOslcSysmlItemFlowMap = new HashMap<String, SysMLItemFlow>();
	static Map<String, SysMLValueProperty> qNameOslcSysmlValuePropertyMap = new HashMap<String, SysMLValueProperty>();
	static Map<String, SysMLValueType> qNameOslcSysmlValueTypeMap = new HashMap<String, SysMLValueType>();

	static Map<String, SysMLBlockDiagram> qNameOslcSysmlBlockDiagramMap = new HashMap<String, SysMLBlockDiagram>();
	static Map<String, SysMLInternalBlockDiagram> qNameOslcSysmlInternalBlockDiagramMap = new HashMap<String, SysMLInternalBlockDiagram>();

	static StringBuffer buffer;

	public static String baseHTTPURI = "http://localhost:" + "8080"
			+ "/oslc4jmagicdraw";
	public static String projectId;

//	static Application magicdrawApplication;
	static Model model;
	static Project projectGlobal;
	static ProjectsManager projectsManager;
	static Map<String, Project> loadedProjects = new HashMap<String, Project>();

	static String magicDrawFileName;
	
	public static com.hp.hpl.jena.rdf.model.Model sysMLModelInRDF = null;

	static {
		
	}

	/**
	 * This method retrieves SysML elements from a specific MagicDraw project
	 * (mdzip file).
	 * 
	 * This method is invoked by most or all web services of the OSLC MagicDraw
	 * adapter.
	 * 
	 * @param projectId
	 *            the name of the MagicDraw mdzip file (not the name of the
	 *            SysML model contained in the mdzip file!)
	 * 
	 */
	public static synchronized void convertSysMLDataIntoRDF(Project project) {

		projectGlobal = project;
		
		initializeCollections();

		magicDrawFileName = project.getName().replaceAll("\\n", "-").replaceAll(" ", "_");
		projectId = project.getName().replaceAll("\\n", "-").replaceAll(" ", "_");

		// List of packages not to load
		predefinedMagicDrawSysMLPackageNames.add("SysML");
		predefinedMagicDrawSysMLPackageNames.add("Matrix Templates Profile");
		predefinedMagicDrawSysMLPackageNames.add("UML Standard Profile");
		predefinedMagicDrawSysMLPackageNames.add("QUDV Library");
		predefinedMagicDrawSysMLPackageNames.add("PrimitiveValueTypes");
		predefinedMagicDrawSysMLPackageNames.add("MD Customization for SysML");

		// logging info saved in buffer and file
		buffer = new StringBuffer();

		try {

			// mapping MagicDraw SysML model
			model = mapSysMLModel(project);

			// collecting all MagicDraw SysML blocks and requirements
			mdSysmlBlocks = getAllSysMLBlocks(model);
			mdSysmlRequirements = getAllSysMLRequirements(model);
			mdSysmlPackages = getAllSysMLPackages(model);
			mdSysmlAssociationBlocks = getAllSysMLAssociationBlocks(model);
			mdSysmlInterfaceBlocks = getAllSysMLInterfaceBlocks(model);
			mdSysmlItemFlows = getAllSysMLItemFlows(model);

			predefinedMagicDrawSysMLPackageNames.remove("QUDV Library");
			mdSysmlValueTypes = getAllSysMLValueTypes(model);
			predefinedMagicDrawSysMLPackageNames.add("QUDV Library");

			

			// mapping MagicDraw SysML packages into OSLC packages
			mapSysMLPackages();

			// mapping MagicDraw SysML requirements into OSLC requirements
			mapSysMLRequirements();

			// mapping MagicDraw SysML blocks into OSLC blocks
			mapSysMLBlocks();

			// mapping MagicDraw SysML interface blocks into OSLC interface
			// blocks
			mapSysMLInterfaceBlocks();

			// mapping MagicDraw SysML association blocks into OSLC association
			// blocks
			mapSysMLAssociationBlocks();

			// mapping MagicDraw SysML value types into OSLC value types
			mapSysMLValueTypes();

			// mapping MagicDraw SysML item flows into OSLC item flows
			mapSysMLItemFlows();

			// mapping MagicDraw SysML package relationships
			mapSysMLPackageRelationships();

			// mapping MagicDraw SysML requirements relationships
			mapSysMLRequirementRelationships();

			// mapping MagicDraw SysML block relationships
			mapSysMLBlockRelationships();

			// map SysML block diagrams
			mapSysMLBlockDiagrams();

			// map SysML internal block diagrams

		} catch (MDModelLibException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println(e.toString());
		}

		// // writing sysml blocks to file
		// try {
		// com.hp.hpl.jena.rdf.model.Model sysMLBlocksModel =
		// JenaModelHelper.createJenaModel(getBlocks(projectId).toArray());
		//
		// triplestoreVersion = projectId;
		//
		// RDFWriter sysMLBlocksWriter = sysMLBlocksModel.getWriter("RDF/XML");
		// sysMLBlocksWriter.setProperty("showXmlDeclaration", "false");
		// sysMLBlocksWriter.setErrorHandler(new ErrorHandler());
		// OutputStream blocksoutputStream = new FileOutputStream(
		// "sysml_blocks_tractor_model" + triplestoreVersion + ".rdf");
		// sysMLBlocksWriter.write(sysMLBlocksModel, blocksoutputStream, null);
		//
		// // writing sysml part properties to file
		// com.hp.hpl.jena.rdf.model.Model sysMLPartPropertiesModel =
		// JenaModelHelper
		// .createJenaModel(getPartProperties(projectId).toArray());
		// RDFWriter sysMLPartPropertiesWriter = sysMLPartPropertiesModel
		// .getWriter("RDF/XML");
		// sysMLPartPropertiesWriter.setProperty("showXmlDeclaration", "false");
		// sysMLPartPropertiesWriter.setErrorHandler(new ErrorHandler());
		// OutputStream partpropertiesoutputStream = new FileOutputStream(
		// "sysml_partproperties_tractor_model" + triplestoreVersion
		// + ".rdf");
		// sysMLPartPropertiesWriter.write(sysMLPartPropertiesModel,
		// partpropertiesoutputStream, null);
		//
		// // writing sysml full ports to file
		// com.hp.hpl.jena.rdf.model.Model sysMLFullPortsModel = JenaModelHelper
		// .createJenaModel(getFullPorts(projectId).toArray());
		// RDFWriter sysMLFullPortsWriter = sysMLFullPortsModel
		// .getWriter("RDF/XML");
		// sysMLFullPortsWriter.setProperty("showXmlDeclaration", "false");
		// sysMLFullPortsWriter.setErrorHandler(new ErrorHandler());
		// OutputStream fullportoutputStream = new FileOutputStream(
		// "sysml_fullports_tractor_model" + triplestoreVersion + ".rdf");
		// sysMLFullPortsWriter.write(sysMLFullPortsModel, fullportoutputStream,
		// null);
		//
		// // writing sysml connectors to file
		// com.hp.hpl.jena.rdf.model.Model sysMLConnectorsModel =
		// JenaModelHelper
		// .createJenaModel(getConnectors(projectId).toArray());
		// RDFWriter sysMLConnectorsWriter = sysMLConnectorsModel
		// .getWriter("RDF/XML");
		// sysMLConnectorsWriter.setProperty("showXmlDeclaration", "false");
		// sysMLConnectorsWriter.setErrorHandler(new ErrorHandler());
		// OutputStream connectoroutputStream = new FileOutputStream(
		// "sysml_connectors_tractor_model" + triplestoreVersion + ".rdf");
		// sysMLConnectorsWriter.write(sysMLConnectorsModel,
		// connectoroutputStream, null);
		//
		// // writing sysml connector ends to file
		// com.hp.hpl.jena.rdf.model.Model sysMLConnectorEndsModel =
		// JenaModelHelper
		// .createJenaModel(getConnectorEnds(projectId).toArray());
		// RDFWriter sysMLConnectorEndsWriter = sysMLConnectorEndsModel
		// .getWriter("RDF/XML");
		// sysMLConnectorEndsWriter.setProperty("showXmlDeclaration", "false");
		// sysMLConnectorEndsWriter.setErrorHandler(new ErrorHandler());
		// OutputStream connectorendoutputStream = new FileOutputStream(
		// "sysml_connectorends_tractor_model" + triplestoreVersion
		// + ".rdf");
		// sysMLConnectorEndsWriter.write(sysMLConnectorEndsModel,
		// connectorendoutputStream, null);
		//
		// } catch (IllegalAccessException e2) {
		// // TODO Auto-generated catch block
		// e2.printStackTrace();
		// } catch (IllegalArgumentException e2) {
		// // TODO Auto-generated catch block
		// e2.printStackTrace();
		// } catch (InvocationTargetException e2) {
		// // TODO Auto-generated catch block
		// e2.printStackTrace();
		// } catch (DatatypeConfigurationException e2) {
		// // TODO Auto-generated catch block
		// e2.printStackTrace();
		// } catch (OslcCoreApplicationException e2) {
		// // TODO Auto-generated catch block
		// e2.printStackTrace();
		// } catch (FileNotFoundException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		// printing logging info to a file
		FileWriter fileWriter;
		try {
			fileWriter = new FileWriter("magicdraw-log");
			fileWriter.append(buffer);
			fileWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		

	}

	private static void initializeCollections() {

		magicDrawFileName = null;

		mdSysmlBlocks.clear();
		mdSysmlRequirements.clear();
		mdSysmlPackages.clear();
		mdSysmlAssociationBlocks.clear();
		mdSysmlInterfaceBlocks.clear();
		mdSysmlItemFlows.clear();
		mdSysmlValueTypes.clear();
		mdSysmlBlockDiagrams.clear();
		mdSysmlInternalBlockDiagrams.clear();
		mdSysmlPartProperties.clear();
		mdSysmlConnectors.clear();
		mdSysmlPorts.clear();
		mdSysmlValueProperties.clear();
		mdSysmlFlowProperties.clear();
		mdSysmlBoundReferences.clear();

		oslcSysmlRequirements.clear();
		oslcSysmlBlocks.clear();

		idMdSysmlRequirementMap.clear();
		idOslcSysmlRequirementMap.clear();

		oslcSysmlModelMap.clear();
		
		predefinedMagicDrawSysMLPackageNames.clear();

		qNameOslcSysmlBlockMap.clear();
		qNameOslcSysmlPartPropertyMap.clear();
		qNameOslcSysmlReferencePropertyMap.clear();
		qNameOslcSysmlPackageMap.clear();
		qNameOslcSysmlAssociationBlockMap.clear();
		qNameOslcSysmlConnectorMap.clear();
		qNameOslcSysmlConnectorEndMap.clear();
		qNameOslcSysmlPortMap.clear();
		qNameOslcSysmlProxyPortMap.clear();
		qNameOslcSysmlFullPortMap.clear();
		qNameOslcSysmlInterfaceBlockMap.clear();
		qNameOslcSysmlFlowPropertyMap.clear();
		qNameOslcSysmlBoundReferenceMap.clear();
		qNameOslcSysmlItemFlowMap.clear();
		qNameOslcSysmlValuePropertyMap.clear();
		qNameOslcSysmlValueTypeMap.clear();
		qNameOslcSysmlBlockDiagramMap.clear();
		qNameOslcSysmlInternalBlockDiagramMap.clear();
	}

	private static void mapSysMLBlockDiagrams() {
		for (DiagramPresentationElement diagramPresentationElement : mdSysmlBlockDiagrams) {
			String diagramName = diagramPresentationElement.getDiagram()
					.getName();
			// BaseElement baseElement =
			// diagramPresentationElement.getObjectParent();
			// Element owner =
			// diagramPresentationElement.getDiagram().getOwner();

			if (!diagramPresentationElement.isLoaded()) {
				diagramPresentationElement.ensureLoaded();
				System.out.println("diagram not loaded");
			}
			if (!diagramPresentationElement.isLoaded()) {
				System.out.println("diagram still not loaded");
				diagramPresentationElement.open();
			}
			Namespace namespace = diagramPresentationElement.getDiagram()
					.getOwnerOfDiagram();
			String filePathName = "src/main/webapp/images/sysml block diagrams/"
					+ diagramName + ".png";
			// String filePathName =
			// "C:/Users/Axel/git/oslc4jmagicdraw/oslc4jmagicdraw/src/main/webapp/images/sysml block diagrams/"
			// + diagramName + ".png";
			File diagramFile = new File(filePathName);
			// File diagramFile = new File(mDestinationDir,
			// diagram.getHumanName() + diagram.getID() + ".png");
			try {
				ImageExporter.export(diagramPresentationElement,
						ImageExporter.PNG, diagramFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private static void getAllSysMLDiagrams() {
		for (DiagramPresentationElement diagramPresentationElement : projectGlobal.getDiagrams()) {
			String diagramType = diagramPresentationElement.getDiagramType()
					.getType();
			String diagramName = diagramPresentationElement.getDiagram()
					.getName();
			String qfOwner = getQualifiedNameOrID(diagramPresentationElement
					.getDiagram().getOwner());
			String diagramID = qfOwner + "::"
					+ diagramName.replaceAll("\\n", "-").replaceAll(" ", "_");
			if (diagramPresentationElement.isLoaded()) {
				System.out.println("test");
			}
			if (diagramType.equals("SysML Block Definition Diagram")) {
				mdSysmlBlockDiagrams.add(diagramPresentationElement);

				SysMLBlockDiagram sysMLBlockDiagram;
				try {
					sysMLBlockDiagram = new SysMLBlockDiagram();
					qNameOslcSysmlBlockDiagramMap.put(magicDrawFileName
							+ "/blockdiagrams/" + diagramID, sysMLBlockDiagram);
					sysMLBlockDiagram.setAbout(URI.create(baseHTTPURI
							+ "/services/" + projectId + "/blockdiagrams/"
							+ diagramID));
					sysMLBlockDiagram.setName(diagramName.replaceAll(" ", "_"));
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else if (diagramType.equals("SysML Internal Block Diagram")) {
				mdSysmlInternalBlockDiagrams.add(diagramPresentationElement);

				SysMLInternalBlockDiagram sysMLInternalBlockDiagram;
				try {
					sysMLInternalBlockDiagram = new SysMLInternalBlockDiagram();
					qNameOslcSysmlInternalBlockDiagramMap.put(magicDrawFileName
							+ "/internalblockdiagrams/" + diagramID,
							sysMLInternalBlockDiagram);
					sysMLInternalBlockDiagram.setAbout(URI.create(baseHTTPURI
							+ "/services/" + projectId
							+ "/internalblockdiagrams/" + diagramID));
					sysMLInternalBlockDiagram.setName(diagramName.replaceAll(
							" ", "_"));
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			String filePathName = "src/main/webapp/images/sysml diagrams/"
					+ diagramName + ".png";
			File diagramFile = new File(filePathName);
			try {
				ImageExporter.export(diagramPresentationElement,
						ImageExporter.PNG, diagramFile);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

	private static void mapSysMLValueTypes() {
		for (DataType mdSysMLValueType : mdSysmlValueTypes) {
			String qName = mdSysMLValueType.getQualifiedName();

			SysMLValueType sysMLValueType;
			try {
				sysMLValueType = new SysMLValueType();
				qNameOslcSysmlValueTypeMap.put(magicDrawFileName
						+ "/valuetypes/"
						+ qName.replaceAll("\\n", "-").replaceAll(" ", "_"),
						sysMLValueType);

				// name attribute
				String name = mdSysMLValueType.getName();

				// URI
				if (name != null) {
					sysMLValueType.setName(name);
					buffer.append("\r\nSysML Block with Name: "
							+ sysMLValueType.getName());
					sysMLValueType.setAbout(URI.create(baseHTTPURI
							+ "/services/" + projectId + "/valuetypes/"
							+ getQualifiedNameOrID(mdSysMLValueType)));
				}

				// unit attribute
				String humanName = mdSysMLValueType.getHumanName();
				Element unit = (Element) StereotypesHelper
						.getStereotypePropertyFirst(
								mdSysMLValueType,
								StereotypesHelper
										.getFirstVisibleStereotype(mdSysMLValueType),
								"unit");

				Stereotype valueTypeStereotype = StereotypesHelper
						.getStereotype(Application.getInstance().getProject(),
								SysMLConstants.VALUE_TYPE_STEREOTYPE,
								SysMLConstants.SYSML_PROFILE);
				if (valueTypeStereotype != null) {
					unit = ((InstanceSpecification) StereotypesHelper
							.getStereotypePropertyFirst(mdSysMLValueType,
									valueTypeStereotype,
									SysMLConstants.VALUE_TYPE_UNIT_TAG));
				}

				if (unit != null) {
					sysMLValueType.setUnit(URI.create(baseHTTPURI
							+ "/services/" + projectId + "/units/"
							+ getQualifiedNameOrID(unit)));
				}

				// quantity kind attribute
				Element quantityKind = (Element) StereotypesHelper
						.getStereotypePropertyFirst(
								mdSysMLValueType,
								StereotypesHelper
										.getFirstVisibleStereotype(mdSysMLValueType),
								"quantityKind");
				if (quantityKind != null) {
					sysMLValueType.setUnit(URI.create(baseHTTPURI
							+ "/services/" + projectId + "/quantitykinds/"
							+ getQualifiedNameOrID(quantityKind)));
				}

			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private static Collection<DataType> getAllSysMLValueTypes(
			com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package packageableElement)
			throws MDModelLibException {
		Collection<com.nomagic.uml2.ext.magicdraw.classes.mdkernel.DataType> sysmlValueTypes = new ArrayList<com.nomagic.uml2.ext.magicdraw.classes.mdkernel.DataType>();
		if (packageableElement instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package) {
			com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package package_ = (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package) packageableElement;
			for (PackageableElement nestedPackageableElement : package_
					.getPackagedElement()) {
				if (MDSysMLModelHandler.isSysMLElement(
						nestedPackageableElement, "ValueType")) {
					DataType magicDrawSysMLValueType = (DataType) nestedPackageableElement;
					sysmlValueTypes.add(magicDrawSysMLValueType);
				} else if (nestedPackageableElement instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package) {
					com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package nestedPackage = (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package) nestedPackageableElement;
					if (!predefinedMagicDrawSysMLPackageNames
							.contains(nestedPackage.getName())) {
						sysmlValueTypes
								.addAll(getAllSysMLValueTypes(nestedPackage));
					}
				}
			}
		}

		return sysmlValueTypes;
	}

	private static void mapSysMLItemFlows() throws MDModelLibException {
		for (com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdinformationflows.InformationFlow mdSysMLItemFlow : mdSysmlItemFlows) {
			String itemFlowID = mdSysMLItemFlow.getID();
			// qNameMdSysmlAssociationBlockMap.put(
			// qName.replaceAll("\\n", "-").replaceAll(" ", "_"),
			// mdSysMLAssociationBlock);
			SysMLItemFlow sysMLItemFlow;
			try {
				sysMLItemFlow = new SysMLItemFlow();
				qNameOslcSysmlItemFlowMap.put(projectId + "/itemflows/" +  
						getQualifiedNameOrID(mdSysMLItemFlow), sysMLItemFlow);
				sysMLItemFlow.setAbout(URI.create(baseHTTPURI + "/services/"
						+ projectId + "/itemflows/"
						+ getQualifiedNameOrID(mdSysMLItemFlow)));

				// information source
				NamedElement informationSource = (NamedElement) mdSysMLItemFlow
						.getInformationSource().toArray()[0];
				URI linkedInformationSourceURI = null;
				if (MDSysMLModelHandler.isSysMLElement(informationSource,
						"PartProperty")) {
					linkedInformationSourceURI = new URI(baseHTTPURI
							+ "/services/" + projectId + "/partproperties/"
							+ getQualifiedNameOrID(informationSource));
				} else if (MDSysMLModelHandler.isSysMLElement(
						informationSource, "ProxyPort")) {
					linkedInformationSourceURI = new URI(baseHTTPURI
							+ "/services/" + projectId + "/proxyports/"
							+ getQualifiedNameOrID(informationSource));
				} else if (MDSysMLModelHandler.isSysMLElement(
						informationSource, "FullPort")) {
					linkedInformationSourceURI = new URI(baseHTTPURI
							+ "/services/" + projectId + "/fullports/"
							+ getQualifiedNameOrID(informationSource));
				} else if (informationSource instanceof com.nomagic.uml2.ext.magicdraw.compositestructures.mdports.Port) {
					linkedInformationSourceURI = new URI(baseHTTPURI
							+ "/services/" + projectId + "/ports/"
							+ getQualifiedNameOrID(informationSource));
				}
				sysMLItemFlow.setInformationSource(linkedInformationSourceURI);

				// information target
				NamedElement informationTarget = (NamedElement) mdSysMLItemFlow
						.getInformationTarget().toArray()[0];
				URI linkedInformationTargetURI = null;
				if (MDSysMLModelHandler.isSysMLElement(informationTarget,
						"PartProperty")) {
					linkedInformationTargetURI = new URI(baseHTTPURI
							+ "/services/" + projectId + "/partproperties/"
							+ getQualifiedNameOrID(informationTarget));

				} else if (MDSysMLModelHandler.isSysMLElement(
						informationTarget, "ProxyPort")) {
					linkedInformationTargetURI = new URI(baseHTTPURI
							+ "/services/" + projectId + "/proxyports/"
							+ getQualifiedNameOrID(informationTarget));
				} else if (MDSysMLModelHandler.isSysMLElement(
						informationTarget, "FullPort")) {
					linkedInformationTargetURI = new URI(baseHTTPURI
							+ "/services/" + projectId + "/fullports/"
							+ getQualifiedNameOrID(informationTarget));
				} else if (informationTarget instanceof com.nomagic.uml2.ext.magicdraw.compositestructures.mdports.Port) {
					linkedInformationTargetURI = new URI(baseHTTPURI
							+ "/services/" + projectId + "/ports/"
							+ getQualifiedNameOrID(informationTarget));
				}
				sysMLItemFlow.setInformationTarget(linkedInformationTargetURI);

				// realizingConnector
				if (mdSysMLItemFlow.getRealizingConnector().size() > 0) {
					Connector connector = (Connector) mdSysMLItemFlow
							.getRealizingConnector().toArray()[0];
					URI realizingConnectorURI = new URI(baseHTTPURI
							+ "/services/" + projectId + "/connectors/"
							+ getQualifiedNameOrID(connector));
					sysMLItemFlow.setRealizingConnector(realizingConnectorURI);
				}

				// itemProperty
				Property itemProperty = (Property) StereotypesHelper
						.getStereotypePropertyFirst(
								mdSysMLItemFlow,
								StereotypesHelper
										.getFirstVisibleStereotype(mdSysMLItemFlow),
								"itemProperty");
				URI itemPropertyURI = null;
				if (itemProperty != null) {
					if (MDSysMLModelHandler.isSysMLElement(itemProperty,
							"FlowProperty")) {
						itemPropertyURI = new URI(baseHTTPURI + "/services/"
								+ projectId + "/flowproperties/"
								+ getQualifiedNameOrID(itemProperty));
					} else if (MDSysMLModelHandler.isSysMLElement(itemProperty,
							"ReferenceProperty")) {
						itemPropertyURI = new URI(baseHTTPURI + "/services/"
								+ projectId + "/referenceproperties/"
								+ getQualifiedNameOrID(itemProperty));
					}
					sysMLItemFlow.setItemProperty(itemPropertyURI);
				}

				// conveyed classifiers
				Collection<Classifier> conveyedClassifiers = mdSysMLItemFlow
						.getConveyed();
				Link[] conveyedBlocksLinkArray = new Link[conveyedClassifiers
						.size()];

				int inheritedBlocksLinkArrayIndex = 0;

				buffer.append("\r\n\tconveyedBlocks: "
						+ conveyedBlocksLinkArray.length);
				for (Classifier conveyedclassifier : conveyedClassifiers) {
					String qNameConveyedclassifier = conveyedclassifier
							.getQualifiedName();
					SysMLBlock conveyedBlock = qNameOslcSysmlBlockMap
							.get(magicDrawFileName
									+ "/blocks/"
									+ qNameConveyedclassifier.replaceAll("\\n",
											"-").replaceAll(" ", "_"));

					URI conveyedBlockURI = null;
					try {
						conveyedBlockURI = new URI(baseHTTPURI
								+ "/services/"
								+ projectId
								+ "/blocks/"
								+ qNameConveyedclassifier
										.replaceAll("\\n", "-").replaceAll(" ",
												"_"));
						Link conveyedBlockLink = new Link(conveyedBlockURI);
						conveyedBlocksLinkArray[inheritedBlocksLinkArrayIndex] = conveyedBlockLink;
						inheritedBlocksLinkArrayIndex++;
					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				if (conveyedBlocksLinkArray.length > 0) {
					sysMLItemFlow.setConveyedBlocks(conveyedBlocksLinkArray);

					buffer.append("\r\n\tconveyedBlocks: ");
					for (Link link : conveyedBlocksLinkArray) {
						buffer.append("\r\n\t\t " + link.getValue());
					}
				}

			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private static void mapSysMLAssociationBlocks() {
		for (com.nomagic.uml2.ext.magicdraw.classes.mdassociationclasses.AssociationClass mdSysMLAssociationBlock : mdSysmlAssociationBlocks) {
			String qName = mdSysMLAssociationBlock.getQualifiedName();
			// qNameMdSysmlAssociationBlockMap.put(
			// qName.replaceAll("\\n", "-").replaceAll(" ", "_"),
			// mdSysMLAssociationBlock);
			SysMLAssociationBlock sysMLAssociationBlock;
			try {
//				SysMLItemFlow sdds = new SysMLItemFlow();
//				sdds.getRealizingConnector()
				
				sysMLAssociationBlock = new SysMLAssociationBlock();
				qNameOslcSysmlAssociationBlockMap.put(magicDrawFileName
						+ "/associationblocks/"
						+ qName.replaceAll("\\n", "-").replaceAll(" ", "_"),
						sysMLAssociationBlock);

				// SysML association block Name attribute
				String name = mdSysMLAssociationBlock.getName();
				if (name != null) {
					sysMLAssociationBlock.setName(name);
					buffer.append("\r\nSysML Block with Name: "
							+ sysMLAssociationBlock.getName());
					sysMLAssociationBlock.setAbout(URI
							.create(baseHTTPURI
									+ "/services/"
									+ projectId
									+ "/associationblocks/"
									+ qName.replaceAll("\\n", "-").replaceAll(
											" ", "_")));
				}

				// SysML association block memberEnd attribute
				Association mdSysMAssociation = (Association) mdSysMLAssociationBlock;
				Link[] linksArray = new Link[2];

				int linksArrayIndex = 0;
				for (Property memberEnd : mdSysMAssociation.getMemberEnd()) {
					URI linkedElementURI = null;
					linkedElementURI = new URI(baseHTTPURI
							+ "/services/"
							+ projectId
							+ "/referenceproperties/"
							+ memberEnd.getQualifiedName()
									.replaceAll("\\n", "-")
									.replaceAll(" ", "_"));
					Link link = new Link(linkedElementURI);
					linksArray[linksArrayIndex] = link;
					linksArrayIndex++;
				}
				if (linksArrayIndex > 0) {
					sysMLAssociationBlock.setMemberEnds(linksArray);
				}
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private static void mapSysMLInterfaceBlocks() throws MDModelLibException {
		for (Class mdSysMLBlock : mdSysmlInterfaceBlocks) {
			String qName = mdSysMLBlock.getQualifiedName();
			// qNameMdSysmlInterfaceBlockMap.put(
			// qName.replaceAll("\\n", "-").replaceAll(" ", "_"),
			// mdSysMLBlock);
			SysMLInterfaceBlock sysMLInterfaceBlock;
			try {
				sysMLInterfaceBlock = new SysMLInterfaceBlock();
				qNameOslcSysmlInterfaceBlockMap.put(magicDrawFileName
						+ "/interfaceblocks/"
						+ qName.replaceAll("\\n", "-").replaceAll(" ", "_"),
						sysMLInterfaceBlock);

				// SysML Block Name attribute
				String name = mdSysMLBlock.getName();
				if (name != null) {
					sysMLInterfaceBlock.setName(name);
					buffer.append("\r\nSysML Interface Block with Name: "
							+ sysMLInterfaceBlock.getName());
					sysMLInterfaceBlock.setAbout(URI
							.create(baseHTTPURI
									+ "/services/"
									+ projectId
									+ "/interfaceblocks/"
									+ qName.replaceAll("\\n", "-").replaceAll(
											" ", "_")));
				}

				// SysML Block Flow Properties
				mapSysMLFlowProperties(mdSysMLBlock, sysMLInterfaceBlock);

				// SysML Proxy Ports
				mapSysMLProxyPorts(mdSysMLBlock, sysMLInterfaceBlock);

			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private static void mapSysMLFlowProperties(Class mdSysMLBlock,
			SysMLInterfaceBlock sysMLInterfaceBlock) {
		Link[] flowPropertiesLinksArray = getLinkedStereotypedSysMLElements(
				mdSysMLBlock.getOwnedAttribute(), "FlowProperty", baseHTTPURI
						+ "/services/" + projectId + "/flowproperties/");

		if (flowPropertiesLinksArray != null) {
			sysMLInterfaceBlock.setFlowProperties(flowPropertiesLinksArray);

			buffer.append("\r\n " + sysMLInterfaceBlock.getName());
			buffer.append("\r\n\tblock reference properties: "
					+ flowPropertiesLinksArray.length);
			buffer.append("\r\n " + sysMLInterfaceBlock.getName());
			buffer.append("\r\n\tblock reference properties: ");
			for (Link link : flowPropertiesLinksArray) {
				buffer.append("\r\n\t\t " + link.getValue());
			}
		}

		for (Property property : mdSysMLBlock.getOwnedAttribute()) {

			if (property.getAppliedStereotypeInstance() != null) {
				InstanceSpecification stereotypeInstance = property
						.getAppliedStereotypeInstance();
				if (stereotypeInstance.getClassifier().get(0).getName()
						.contains("FlowProperty")) {
					SysMLFlowProperty sysmlFlowProperty;
					try {
						sysmlFlowProperty = new SysMLFlowProperty();
						qNameOslcSysmlFlowPropertyMap.put(
								magicDrawFileName
										+ "/flowproperties/"
										+ property.getQualifiedName()
												.replaceAll("\\n", "-")
												.replaceAll(" ", "_"),
								sysmlFlowProperty);
						mdSysmlFlowProperties.add(property);
						
						// referenceProperty name
						sysmlFlowProperty.setName(property.getName());

						String qName = property.getQualifiedName();
						sysmlFlowProperty.setAbout(URI.create(baseHTTPURI
								+ "/services/"
								+ projectId
								+ "/flowproperties/"
								+ qName.replaceAll("\\n", "-").replaceAll(" ",
										"_")));

						// referenceProperty type
						if (property.getType() != null) {
							sysmlFlowProperty.setType(new URI(baseHTTPURI
									+ "/services/"
									+ projectId
									+ "/blocks/"
									+ property.getType().getQualifiedName()
											.replaceAll("\\n", "-")
											.replaceAll(" ", "_")));
						}

						// referenceProperty multiplicity
						// String lowerMultiplicity = Integer.toString(property
						// .getLower());
						// String upperMultiplicity = Integer.toString(property
						// .getUpper());
						// sysmlFlowProperty.setLower(lowerMultiplicity);
						// sysmlFlowProperty.setUpper(upperMultiplicity);

						// direction
						Object directionObject = StereotypesHelper
								.getStereotypePropertyFirst(property,
										(Stereotype) property
												.getAppliedStereotypeInstance()
												.getClassifier().get(0),
										"direction");
						if (directionObject instanceof EnumerationLiteral) {
							EnumerationLiteral enumLit = (EnumerationLiteral) directionObject;
							String enumLitName = enumLit.getName();
							if (enumLitName.equals("in")) {
								// sysmlFlowProperty
								// .setDirection(SysMLFlowDirection.IN);
								sysmlFlowProperty.setDirection("in");
							} else if (enumLitName.equals("out")) {
								// sysmlFlowProperty
								// .setDirection(SysMLFlowDirection.OUT);
								sysmlFlowProperty.setDirection("out");
							}

						}

					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}
		}

	}

	private static Collection<com.nomagic.uml2.ext.magicdraw.classes.mdassociationclasses.AssociationClass> getAllSysMLAssociationBlocks(
			Model model) {
		Collection<com.nomagic.uml2.ext.magicdraw.classes.mdassociationclasses.AssociationClass> sysmlAssociationBlocks = new ArrayList<com.nomagic.uml2.ext.magicdraw.classes.mdassociationclasses.AssociationClass>();
		java.lang.Class[] classArray = new java.lang.Class[1];
		classArray[0] = com.nomagic.uml2.ext.magicdraw.classes.mdassociationclasses.AssociationClass.class;
		Collection<? extends Element> elementsOfType = ModelHelper
				.getElementsOfType(model, classArray, true, true);
		sysmlAssociationBlocks = (Collection<AssociationClass>) elementsOfType;
		buffer.append("**** Elements of type AssociationClass ****");
		for (Element element : elementsOfType) {
			buffer.append("\r\n" + element.getHumanName());
		}
		return sysmlAssociationBlocks;
	}

	// static Collection<com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class>
	// getAllSysMLAssociationBlocks(
	// com.nomagic.uml2.ext.magicdraw.classes.mdkernel.PackageableElement
	// packageableElement) throws MDModelLibException {
	// Collection<com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class>
	// sysmlBlocks = new
	// ArrayList<com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class>();
	// if (packageableElement instanceof
	// com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package) {
	// com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package package_ =
	// (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package)
	// packageableElement;
	// for (PackageableElement nestedPackageableElement : package_
	// .getPackagedElement()) {
	// if (MDSysMLModelHandler.isSysMLElement(nestedPackageableElement,
	// "AssociationBlock")) {
	// Class magicDrawSysMLBlock = (Class) nestedPackageableElement;
	// sysmlBlocks.add(magicDrawSysMLBlock);
	// sysmlBlocks
	// .addAll(getAllSysMLAssociationBlocks(magicDrawSysMLBlock));
	// } else if (nestedPackageableElement instanceof
	// com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package) {
	// com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package nestedPackage =
	// (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package)
	// nestedPackageableElement;
	// if (!predefinedMagicDrawSysMLPackageNames
	// .contains(nestedPackage.getName())) {
	// sysmlBlocks
	// .addAll(getAllSysMLAssociationBlocks(nestedPackage));
	// }
	// }
	// }
	// } else if (packageableElement instanceof
	// com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class) {
	// com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class class_ =
	// (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class)
	// packageableElement;
	// for (Classifier nestedClassifier : class_.getNestedClassifier()) {
	// if (MDSysMLModelHandler.isSysMLElement(nestedClassifier,
	// "AssociationBlock")) {
	// sysmlBlocks
	// .add((com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class)
	// nestedClassifier);
	// }
	// sysmlBlocks.addAll(getAllSysMLAssociationBlocks(nestedClassifier));
	// }
	// }
	// return sysmlBlocks;
	// }

	private static Collection<com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdinformationflows.InformationFlow> getAllSysMLItemFlows(
			Model model) throws MDModelLibException {
		Collection<com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdinformationflows.InformationFlow> sysmlInformationFlows = new ArrayList<com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdinformationflows.InformationFlow>();
		java.lang.Class[] classArray = new java.lang.Class[1];
		classArray[0] = com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdinformationflows.InformationFlow.class;
		Collection<? extends Element> elementsOfType = ModelHelper
				.getElementsOfType(model, classArray, true, true);
		sysmlInformationFlows = (Collection<com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdinformationflows.InformationFlow>) elementsOfType;
		Collection<com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdinformationflows.InformationFlow> itemFlows = new ArrayList<com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdinformationflows.InformationFlow>();
		for (InformationFlow sysmlInformationFlow : sysmlInformationFlows) {
			if (MDSysMLModelHandler.isSysMLElement(sysmlInformationFlow,
					"ItemFlow")) {
				itemFlows.add(sysmlInformationFlow);
			}
		}
		
		return itemFlows;
	}

	private static void mapSysMLPackageRelationships() {
		for (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package mdSysMLPackage : mdSysmlPackages) {

			SysMLPackage sysMLPackage = qNameOslcSysmlPackageMap
					.get(magicDrawFileName
							+ "/packages/"
							+ mdSysMLPackage.getQualifiedName()
									.replaceAll("\\n", "-")
									.replaceAll(" ", "_"));

			// get nested blocks
			Link[] packageBlocksLinksArray = getLinkedStereotypedSysMLElements(
					mdSysMLPackage.getOwnedType(), "Block", baseHTTPURI
							+ "/services/" + projectId + "/blocks/");
			if (packageBlocksLinksArray != null) {
				sysMLPackage.setBlocks(packageBlocksLinksArray);
				buffer.append("\r\n " + sysMLPackage.getName());
				buffer.append("\r\n\tpackageBlocks: "
						+ packageBlocksLinksArray.length);
				buffer.append("\r\n " + sysMLPackage.getName());
				buffer.append("\r\n\tpackageBlocks: ");
				for (Link link : packageBlocksLinksArray) {
					buffer.append("\r\n\t\t " + link.getValue());
				}
			}

			// get nested requirements
			Link[] packageRequirementsLinksArray = getLinkedStereotypedSysMLElements(
					mdSysMLPackage.getOwnedType(), "Requirement", baseHTTPURI
							+ "/services/" + projectId + "/requirements/");
			if (packageRequirementsLinksArray != null) {
				sysMLPackage.setRequirements(packageRequirementsLinksArray);
				buffer.append("\r\n " + sysMLPackage.getName());
				buffer.append("\r\n\tpackageRequirements: "
						+ packageRequirementsLinksArray.length);
				buffer.append("\r\n " + sysMLPackage.getName());
				buffer.append("\r\n\tpackageRequirements: ");
				for (Link link : packageRequirementsLinksArray) {
					buffer.append("\r\n\t\t " + link.getValue());
				}
			}
		}

	}

	private static void mapSysMLPackages() {
		for (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package mdSysMLPackage : mdSysmlPackages) {
			String qName = mdSysMLPackage.getQualifiedName();
			// qNameMdSysmlPackageMap
			// .put(qName.replaceAll("\\n", "-"), mdSysMLPackage);
			SysMLPackage sysMLPackage;
			try {
				sysMLPackage = new SysMLPackage();
				qNameOslcSysmlPackageMap.put(magicDrawFileName + "/packages/"
						+ qName.replaceAll("\\n", "-").replaceAll(" ", "_"),
						sysMLPackage);

				// SysML Package Name attribute
				String name = mdSysMLPackage.getName();
				if (name != null) {
					sysMLPackage.setName(name);
					buffer.append("\r\nSysML Package with Name: "
							+ sysMLPackage.getName());
					sysMLPackage.setAbout(URI
							.create(baseHTTPURI
									+ "/services/"
									+ projectId
									+ "/packages/"
									+ qName.replaceAll("\\n", "-").replaceAll(
											" ", "_")));
				}

			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private static Model mapSysMLModel(Project project) {
		Model model = project.getModel();
		SysMLModel sysMLModel;
		try {
			sysMLModel = new SysMLModel();
			if (model.getName() == null) {
				sysMLModel.setName("Data");
			} else {
				sysMLModel.setName(model.getName());
			}

			sysMLModel.setAbout(URI.create(baseHTTPURI + "/services/"
					+ projectId + "/model/" + model.getName()));

			oslcSysmlModelMap.put(
					model.getQualifiedName().replaceAll("\\n", "-")
							.replaceAll(" ", "_"), sysMLModel);

			Collection<com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package> modelPackages = new ArrayList<com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package>();
			for (PackageableElement packageableElement : model
					.getPackagedElement()) {
				if (packageableElement instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package) {
					if (!predefinedMagicDrawSysMLPackageNames
							.contains(packageableElement.getName())) {
						modelPackages
								.add((com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package) packageableElement);
					}

				}
			}

			if (modelPackages.size() > 0) {
				Link[] linksArray = new Link[modelPackages.size()];

				int linksArrayIndex = 0;
				for (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package package1 : modelPackages) {
					if (!predefinedMagicDrawSysMLPackageNames.contains(package1
							.getName())) {
						URI linkedElementURI = null;
						linkedElementURI = new URI(baseHTTPURI
								+ "/services/"
								+ projectId
								+ "/packages/"
								+ package1.getQualifiedName()
										.replaceAll("\\n", "-")
										.replaceAll(" ", "_"));
						Link link = new Link(linkedElementURI);
						linksArray[linksArrayIndex] = link;
						linksArrayIndex++;
					}
				}

				sysMLModel.setPackages(linksArray);
				buffer.append("\r\n " + sysMLModel.getName());
				buffer.append("\r\n\tmodel packages: " + linksArray.length);
				buffer.append("\r\n " + sysMLModel.getName());
				buffer.append("\r\n\tmodel packages: ");
				for (Link link2 : linksArray) {
					buffer.append("\r\n\t\t " + link2.getValue());
				}
			}

			// map SysML packages
			// mapSysMLPackages(modelPackages);

		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return model;

	}

	private static void mapSysMLBlockRelationships() throws MDModelLibException {
		for (Class mdSysmlBlock : mdSysmlBlocks) {
			SysMLBlock sysmlBlock = qNameOslcSysmlBlockMap
					.get(magicDrawFileName
							+ "/blocks/"
							+ mdSysmlBlock.getQualifiedName()
									.replaceAll("\\n", "-")
									.replaceAll(" ", "_"));

			// SysML Block generalization
			Collection<Classifier> inheritedClassifiers = mdSysmlBlock
					.getGeneral();
			Link[] inheritedBlocksLinkArray = new Link[inheritedClassifiers
					.size()];

			int inheritedBlocksLinkArrayIndex = 0;
			buffer.append("\r\n " + sysmlBlock.getName());
			buffer.append("\r\n\tinheritedBlocks: "
					+ inheritedBlocksLinkArray.length);
			for (Classifier inheritedclassifier : inheritedClassifiers) {
				String qNameInheritedclassifier = inheritedclassifier
						.getQualifiedName();
				SysMLBlock inheritedBlock = qNameOslcSysmlBlockMap
						.get(magicDrawFileName
								+ "/blocks/"
								+ qNameInheritedclassifier.replaceAll("\\n",
										"-").replaceAll(" ", "_"));

				URI inheritedBlockURI = null;
				try {
					inheritedBlockURI = new URI(baseHTTPURI
							+ "/services/"
							+ projectId
							+ "/blocks/"
							+ qNameInheritedclassifier.replaceAll("\\n", "-")
									.replaceAll(" ", "_"));
					Link inheritedBlockLink = new Link(inheritedBlockURI);
					inheritedBlocksLinkArray[inheritedBlocksLinkArrayIndex] = inheritedBlockLink;
					inheritedBlocksLinkArrayIndex++;
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (inheritedBlocksLinkArray.length > 0) {
				sysmlBlock.setInheritedBlocks(inheritedBlocksLinkArray);
				buffer.append("\r\n " + sysmlBlock.getName());
				buffer.append("\r\n\tinheritedBlocks: ");
				for (Link link : inheritedBlocksLinkArray) {
					buffer.append("\r\n\t\t " + link.getValue());
				}
			}

			// SysML Block nesting
			Collection<Classifier> nestedClassifiers = mdSysmlBlock
					.getNestedClassifier();
			Link[] nestedBlocksLinkArray = new Link[nestedClassifiers.size()];
			int nestedBlocksLinkArrayIndex = 0;
			buffer.append("\r\n " + sysmlBlock.getName());
			buffer.append("\r\n\tnestedBlocks: " + nestedBlocksLinkArray.length);
			for (Classifier nestedClassifier : nestedClassifiers) {
				String qNameNestedclassifier = nestedClassifier
						.getQualifiedName().replaceAll("\\n", "-")
						.replaceAll(" ", "_");
//				SysMLBlock nestedBlock = qNameOslcSysmlBlockMap
//						.get(magicDrawFileName
//								+ "/blocks/"
//								+ qNameNestedclassifier.replaceAll("\\n", "-")
//										.replaceAll(" ", "_"));
				// nested block may not be loaded into memory
				URI nestedBlockURI = null;
				try {
					nestedBlockURI = new URI(baseHTTPURI + "/services/"
							+ projectId + "/blocks/" + qNameNestedclassifier);
					Link nestedBlockLink = new Link(nestedBlockURI);
					nestedBlocksLinkArray[nestedBlocksLinkArrayIndex] = nestedBlockLink;
					nestedBlocksLinkArrayIndex++;
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (nestedBlocksLinkArray.length > 0) {
				sysmlBlock.setNestedBlocks(nestedBlocksLinkArray);
				buffer.append("\r\n " + sysmlBlock.getName());
				buffer.append("\r\n\tnestedBlocks: ");
				for (Link link : nestedBlocksLinkArray) {
					buffer.append("\r\n\t\t " + link.getValue());
				}
			}

			// satisfies relationships (Block satisfies Requirement)
			Link[] satisfiesLinks = getDirectedLinksOfSysMLElement(true,
					mdSysmlBlock, "Satisfy");
			if (satisfiesLinks != null) {
				sysmlBlock.setSatisfies(satisfiesLinks);
				buffer.append("\r\n " + sysmlBlock.getName());
				buffer.append("\r\n\tSatisfies: ");
				for (Link link : satisfiesLinks) {
					buffer.append("\r\n\t\t " + link.getValue());
				}
			}

		}

	}

	private static void mapSysMLRequirementRelationships()
			throws MDModelLibException {
		for (Class mdSysMLRequirement : mdSysmlRequirements) {

			// String sourceReqQualifiedName = mdSysMLRequirement
			// .getQualifiedName().replaceAll("\\n", "-")
			// .replaceAll(" ", "_");
			// SysMLRequirement sysMLRequirement = idOslcSysmlRequirementMap
			// .get(sourceReqQualifiedName);

			String id = (String) StereotypesHelper.getStereotypePropertyFirst(
					mdSysMLRequirement, StereotypesHelper
							.getFirstVisibleStereotype(mdSysMLRequirement),
					"Id");
			SysMLRequirement sysMLRequirement = idOslcSysmlRequirementMap
					.get(id);

			// subRequirements
			Collection<Class> subRequirements = new ArrayList<Class>();
			for (Classifier nestedClassifier : mdSysMLRequirement
					.getNestedClassifier()) {
				if (MDSysMLModelHandler.isSysMLElement(nestedClassifier,
						"Requirement")) {
					subRequirements.add((Class) nestedClassifier);
				}
			}
			if (subRequirements.size() > 0) {
				Link[] subRequirementsLinksArray = new Link[subRequirements
						.size()];
				int linksArrayIndex = 0;
				for (NamedElement namedElement : subRequirements) {
					try {
						URI linkedElementURI = null;

						if (namedElement instanceof Class) {
							Class mdSysMLClass = (Class) namedElement;
							String linkedRequirementID = (String) StereotypesHelper
									.getStereotypePropertyFirst(
											mdSysMLClass,
											StereotypesHelper
													.getFirstVisibleStereotype(mdSysMLRequirement),
											"Id");
							if (linkedRequirementID != null) {
								linkedElementURI = new URI(baseHTTPURI
										+ "/services/" + projectId
										+ "/requirements/"
										+ linkedRequirementID);
								Link link = new Link(linkedElementURI);
								subRequirementsLinksArray[linksArrayIndex] = link;
								linksArrayIndex++;
							}
						}

					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				sysMLRequirement.setSubRequirements(subRequirementsLinksArray);
			}

			// master relationship
			URI masterURI = getDirectedLinkSysMLElement(true,
					mdSysMLRequirement, "Copy");
			if (masterURI != null) {
				sysMLRequirement.setMaster(masterURI);
			}

			// derivedFrom relationships (Requirement derivedFrom Requirements)
			Link[] derivedFromLinks = getDirectedLinksOfSysMLElement(true,
					mdSysMLRequirement, "DeriveReqt");
			if (derivedFromLinks != null) {
				sysMLRequirement.setDerivedFromElements(derivedFromLinks);
				buffer.append("\r\n " + sysMLRequirement.getIdentifier());
				buffer.append("\r\n\tDerivedFrom: ");
				for (Link link : derivedFromLinks) {
					buffer.append("\r\n\t\t " + link.getValue());
				}
			}

			// derived relationships (Requirement has derived Requirements)
			Link[] derivedLinks = getDirectedLinksOfSysMLElement(false,
					mdSysMLRequirement, "DeriveReqt");
			if (derivedLinks != null) {
				sysMLRequirement.setDerivedElements(derivedLinks);
				buffer.append("\r\n " + sysMLRequirement.getIdentifier());
				buffer.append("\r\n\tDerived: ");
				for (Link link : derivedLinks) {
					buffer.append("\r\n\t\t " + link.getValue());
				}
			}

			// satisfiedBy relationships (Requirement satisfied By X)
			Link[] satisfiedByLinks = getDirectedLinksOfSysMLElement(false,
					mdSysMLRequirement, "Satisfy");
			if (satisfiedByLinks != null) {
				sysMLRequirement.setSatisfiedBy(satisfiedByLinks);
				buffer.append("\r\n " + sysMLRequirement.getIdentifier());
				buffer.append("\r\n\tSatisfiedBy: ");
				for (Link link : satisfiedByLinks) {
					buffer.append("\r\n\t\t " + link.getValue());
				}
			}

			// refinedBy relationships (Requirement refined By X)
			Link[] refinedByLinks = getDirectedLinksOfSysMLElement(false,
					mdSysMLRequirement, "Refine");
			if (refinedByLinks != null) {
				sysMLRequirement.setElaboratedBy(refinedByLinks);
				buffer.append("\r\n " + sysMLRequirement.getIdentifier());
				buffer.append("\r\n\tRefinedBy: ");
				for (Link link : refinedByLinks) {
					buffer.append("\r\n\t\t " + link.getValue());
				}
			}

		}
	}

	private static void mapSysMLBlocks() throws MDModelLibException {

		for (Class mdSysMLBlock : mdSysmlBlocks) {
			String qName = mdSysMLBlock.getQualifiedName();
			// qNameMdSysmlBlockMap.put(
			// qName.replaceAll("\\n", "-").replaceAll(" ", "_"),
			// mdSysMLBlock);
			SysMLBlock sysMLBlock;
			try {
				sysMLBlock = new SysMLBlock();
				qNameOslcSysmlBlockMap.put(magicDrawFileName + "/blocks/"
						+ qName.replaceAll("\\n", "-").replaceAll(" ", "_"),
						sysMLBlock);

				// SysML Block Name attribute
				String name = mdSysMLBlock.getName();
				if (name != null) {
					sysMLBlock.setName(name);
					buffer.append("\r\nSysML Block with Name: "
							+ sysMLBlock.getName());
					sysMLBlock.setAbout(URI
							.create(baseHTTPURI
									+ "/services/"
									+ projectId
									+ "/blocks/"
									+ qName.replaceAll("\\n", "-").replaceAll(
											" ", "_")));
				}

				// SysML Block Parts
				mapSysMLPartProperties(mdSysMLBlock, sysMLBlock);

				// SysML Block References
				mapSysMLReferenceProperties(mdSysMLBlock, sysMLBlock);

				// SysML Block Value Properties
				mapSysMLValueProperties(mdSysMLBlock, sysMLBlock);

				// SysML Block Flow Properties
				mapSysMLFlowProperties(mdSysMLBlock, sysMLBlock);
				
				// SysML Block Bound References
				mapSysMLBoundReferences(mdSysMLBlock, sysMLBlock); 

				// SysML Block Connectors
				mapSysMLConnectors(mdSysMLBlock, sysMLBlock);

				// SysML Block Ports
				mapSysMLPorts(mdSysMLBlock, sysMLBlock);

			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static void mapSysMLBoundReferences(Class mdSysmlBlock,
			SysMLBlock sysMLBlock) {
		Link[] boundReferencesLinksArray = getLinkedStereotypedSysMLElements(
				mdSysmlBlock.getOwnedAttribute(), "BoundReference", baseHTTPURI
						+ "/services/" + projectId + "/boundreferences/");

		if (boundReferencesLinksArray != null) {
			sysMLBlock.setBoundReferences(boundReferencesLinksArray);

			buffer.append("\r\n " + sysMLBlock.getName());
			buffer.append("\r\n\tblock bound references: "
					+ boundReferencesLinksArray.length);
			buffer.append("\r\n " + sysMLBlock.getName());
			buffer.append("\r\n\tblock bound references: ");
			for (Link link : boundReferencesLinksArray) {
				buffer.append("\r\n\t\t " + link.getValue());
			}
		}

		for (Property property : mdSysmlBlock.getOwnedAttribute()) {

			if (property.getAppliedStereotypeInstance() != null) {
				InstanceSpecification stereotypeInstance = property
						.getAppliedStereotypeInstance();
				if (stereotypeInstance.getClassifier().get(0).getName()
						.contains("BoundReference")) {
					SysMLBoundReference sysmlBoundReference;
					try {
						sysmlBoundReference = new SysMLBoundReference();
						qNameOslcSysmlBoundReferenceMap.put(
								magicDrawFileName
										+ "/boundreferences/"
										+ property.getQualifiedName()
												.replaceAll("\\n", "-")
												.replaceAll(" ", "_"),
								sysmlBoundReference);
						mdSysmlBoundReferences.add(property);
						
						
						// boundreference name
						sysmlBoundReference.setName(property.getName());

						String qName = property.getQualifiedName();
						sysmlBoundReference.setAbout(URI.create(baseHTTPURI
								+ "/services/"
								+ projectId
								+ "/boundreferences/"
								+ qName.replaceAll("\\n", "-").replaceAll(" ",
										"_")));

						// boundreference type
						if(property.getType() != null){
							sysmlBoundReference.setType(new URI(baseHTTPURI
									+ "/services/"
									+ projectId
									+ "/blocks/"
									+ property.getType().getQualifiedName()
											.replaceAll("\\n", "-")
											.replaceAll(" ", "_")));
						}
						

						// boundreference multiplicity
						String lowerMultiplicity = Integer.toString(property
								.getLower());
						String upperMultiplicity = Integer.toString(property
								.getUpper());
						sysmlBoundReference.setLower(lowerMultiplicity);
						sysmlBoundReference.setUpper(upperMultiplicity);

						// boundreference boundEnd
						Object boundEnd = StereotypesHelper
								.getStereotypePropertyFirst(property,
										(Stereotype) property
												.getAppliedStereotypeInstance()
												.getClassifier().get(0),
										"boundEnd");
						if(boundEnd != null){
							if (boundEnd instanceof ConnectorEnd) {
								ConnectorEnd connectorEnd = (ConnectorEnd) boundEnd;
								// get boundEndURI
								sysmlBoundReference.setBoundEnd(new URI(baseHTTPURI
										+ "/services/"
										+ projectId
										+ "/connectorends/"
										+ connectorEnd.getID()));							
							}
						}
						

					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}
		}
		
	}

	private static void mapSysMLValueProperties(Class mdSysMLBlock,
			SysMLBlock sysMLBlock) throws MDModelLibException {
		Link[] valuePropertiesLinksArray = getLinkedStereotypedSysMLElements(
				mdSysMLBlock.getOwnedAttribute(), "ValueProperty", baseHTTPURI
						+ "/services/" + projectId + "/valueproperties/");

		if (valuePropertiesLinksArray != null) {
			sysMLBlock.setValueProperties(valuePropertiesLinksArray);

			buffer.append("\r\n " + sysMLBlock.getName());
			buffer.append("\r\n\tblock reference properties: "
					+ valuePropertiesLinksArray.length);
			buffer.append("\r\n " + sysMLBlock.getName());
			buffer.append("\r\n\tblock reference properties: ");
			for (Link link : valuePropertiesLinksArray) {
				buffer.append("\r\n\t\t " + link.getValue());
			}
		}

		for (Property property : mdSysMLBlock.getOwnedAttribute()) {

			if (property.getAppliedStereotypeInstance() != null) {
				InstanceSpecification stereotypeInstance = property
						.getAppliedStereotypeInstance();
				if (stereotypeInstance.getClassifier().get(0).getName()
						.contains("ValueProperty")) {
					SysMLValueProperty sysmlValueProperty;
					try {
						sysmlValueProperty = new SysMLValueProperty();
						qNameOslcSysmlValuePropertyMap.put(magicDrawFileName
								+ "/valueproperties/"
								+ getQualifiedNameOrID(property),
								sysmlValueProperty);
						mdSysmlValueProperties.add(property);

						// valueProperty name
						sysmlValueProperty.setName(property.getName());
						sysmlValueProperty.setAbout(URI.create(baseHTTPURI
								+ "/services/" + projectId
								+ "/valueproperties/"
								+ getQualifiedNameOrID(property)));

						// valueProperty type
						if (property.getType() != null) {
							if (MDSysMLModelHandler.isSysMLElement(
									property.getType(), "Block")) {
								sysmlValueProperty.setType(new URI(baseHTTPURI
										+ "/services/"
										+ projectId
										+ "/blocks/"
										+ getQualifiedNameOrID(property
												.getType())));
							} else if (MDSysMLModelHandler.isSysMLElement(
									property.getType(), "ValueType")) {
								sysmlValueProperty.setType(new URI(baseHTTPURI
										+ "/services/"
										+ projectId
										+ "/valuetypes/"
										+ getQualifiedNameOrID(property
												.getType())));
							}
						}

						// referenceProperty multiplicity
						String lowerMultiplicity = Integer.toString(property
								.getLower());
						String upperMultiplicity = Integer.toString(property
								.getUpper());
						sysmlValueProperty.setLower(lowerMultiplicity);
						sysmlValueProperty.setUpper(upperMultiplicity);

						// defaultValue
						ValueSpecification valueSpecification = property
								.getDefaultValue();
						if (valueSpecification instanceof LiteralString) {
							LiteralString literalString = (LiteralString) valueSpecification;
							sysmlValueProperty.setDefaultValue(literalString
									.getValue());
						}

					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}
		}

	}

	private static void mapSysMLPorts(Class mdSysMLBlock, SysMLBlock sysMLBlock)
			throws MDModelLibException {
		ArrayList<Port> proxyPortsList = new ArrayList<Port>();
		ArrayList<Port> fullPortsList = new ArrayList<Port>();
		ArrayList<Port> portsList = new ArrayList<Port>();

		for (Port port : mdSysMLBlock.getOwnedPort()) {
			if (MDSysMLModelHandler.isSysMLElement(port, "ProxyPort")) {
				proxyPortsList.add(port);
			} else if (MDSysMLModelHandler.isSysMLElement(port, "FullPort")) {
				fullPortsList.add(port);
			}
			// else if (MDSysMLModelHandler.isSysMLElement(port, "FlowPort")){
			// flowPortsList.add(port);
			// }
			else if (port instanceof com.nomagic.uml2.ext.magicdraw.compositestructures.mdports.Port) {
				// standard port
				portsList.add(port);
			}
		}

		Link[] proxyPortsLinksArray;
		Link[] fullPortsLinksArray;
		Link[] portsLinksArray;

		if (proxyPortsList.size() > 0) {
			proxyPortsLinksArray = new Link[proxyPortsList.size()];
			String proxyPortBaseURI = baseHTTPURI + "/services/" + projectId
					+ "/proxyports/";
			int proxyPortsLinksArrayIndex = 0;
			for (Port port : proxyPortsList) {
				URI linkedElementURI = null;
				try {
					linkedElementURI = new URI(proxyPortBaseURI
							+ getQualifiedNameOrID(port));
					Link link = new Link(linkedElementURI);
					proxyPortsLinksArray[proxyPortsLinksArrayIndex] = link;
					proxyPortsLinksArrayIndex++;

					SysMLProxyPort sysMLProxyPort = new SysMLProxyPort();
					qNameOslcSysmlProxyPortMap.put(magicDrawFileName
							+ "/proxyports/" + getQualifiedNameOrID(port),
							sysMLProxyPort);

					// port name
					sysMLProxyPort.setName(port.getName());

					// port URI
					String qName = port.getQualifiedName();
					sysMLProxyPort.setAbout(URI.create(baseHTTPURI
							+ "/services/" + projectId + "/proxyports/"
							+ getQualifiedNameOrID(port)));

					// port type
					if (port.getType() != null) {
						if (MDSysMLModelHandler.isSysMLElement(port.getType(),
								"Block")) {
							sysMLProxyPort.setType(new URI(baseHTTPURI
									+ "/services/"
									+ projectId
									+ "/blocks/"
									+ port.getType().getQualifiedName()
											.replaceAll("\\n", "-")
											.replaceAll(" ", "_")));
						} else if (MDSysMLModelHandler.isSysMLElement(
								port.getType(), "InterfaceBlock")) {
							sysMLProxyPort.setType(new URI(baseHTTPURI
									+ "/services/"
									+ projectId
									+ "/interfaceblocks/"
									+ port.getType().getQualifiedName()
											.replaceAll("\\n", "-")
											.replaceAll(" ", "_")));
						}
					}

					// isService
					sysMLProxyPort.setIsService(port.isService());

					// isBehavior
					sysMLProxyPort.setIsBehavior(port.isBehavior());

					// isConjugated
					sysMLProxyPort.setIsConjugated(port.isConjugated());

					// port multiplicity
					String lowerMultiplicity = Integer
							.toString(port.getLower());
					String upperMultiplicity = Integer
							.toString(port.getUpper());
					sysMLProxyPort.setLower(lowerMultiplicity);
					sysMLProxyPort.setUpper(upperMultiplicity);

				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			sysMLBlock.setProxyPorts(proxyPortsLinksArray);
		}

		if (fullPortsList.size() > 0) {
			fullPortsLinksArray = new Link[fullPortsList.size()];
			String fullPortBaseURI = baseHTTPURI + "/services/" + projectId
					+ "/fullports/";
			int fullPortsLinksArrayIndex = 0;
			for (Port port : fullPortsList) {
				URI linkedElementURI = null;
				try {
					linkedElementURI = new URI(fullPortBaseURI
							+ getQualifiedNameOrID(port));
					Link link = new Link(linkedElementURI);
					fullPortsLinksArray[fullPortsLinksArrayIndex] = link;
					fullPortsLinksArrayIndex++;

					SysMLFullPort sysMLFullPort = new SysMLFullPort();
					qNameOslcSysmlFullPortMap.put(magicDrawFileName
							+ "/fullports/" + getQualifiedNameOrID(port),
							sysMLFullPort);

					// port name
					sysMLFullPort.setName(port.getName());

					// port URI
					sysMLFullPort.setAbout(URI.create(baseHTTPURI
							+ "/services/" + projectId + "/fullports/"
							+ getQualifiedNameOrID(port)));

					// port type
					if (port.getType() != null) {
						if (MDSysMLModelHandler.isSysMLElement(port.getType(),
								"Block")) {
							sysMLFullPort.setType(new URI(baseHTTPURI
									+ "/services/"
									+ projectId
									+ "/blocks/"
									+ port.getType().getQualifiedName()
											.replaceAll("\\n", "-")
											.replaceAll(" ", "_")));
						} else if (MDSysMLModelHandler.isSysMLElement(
								port.getType(), "InterfaceBlock")) {
							sysMLFullPort.setType(new URI(baseHTTPURI
									+ "/services/"
									+ projectId
									+ "/interfaceblocks/"
									+ port.getType().getQualifiedName()
											.replaceAll("\\n", "-")
											.replaceAll(" ", "_")));
						}
					}

					// isService
					sysMLFullPort.setIsService(port.isService());

					// isBehavior
					sysMLFullPort.setIsBehavior(port.isBehavior());

					// isConjugated
					sysMLFullPort.setIsConjugated(port.isConjugated());

					// port multiplicity
					String lowerMultiplicity = Integer
							.toString(port.getLower());
					String upperMultiplicity = Integer
							.toString(port.getUpper());
					sysMLFullPort.setLower(lowerMultiplicity);
					sysMLFullPort.setUpper(upperMultiplicity);

				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			sysMLBlock.setFullPorts(fullPortsLinksArray);
		}

		if (portsList.size() > 0) {
			portsLinksArray = new Link[portsList.size()];
			String fullPortBaseURI = baseHTTPURI + "/services/" + projectId
					+ "/ports/";
			int portsLinksArrayIndex = 0;
			for (Port port : portsList) {
				URI linkedElementURI = null;
				try {
					linkedElementURI = new URI(fullPortBaseURI
							+ getQualifiedNameOrID(port));
					Link link = new Link(linkedElementURI);
					portsLinksArray[portsLinksArrayIndex] = link;
					portsLinksArrayIndex++;

					SysMLPort sysMLPort = new SysMLPort();
					qNameOslcSysmlPortMap.put(magicDrawFileName + "/ports/"
							+ getQualifiedNameOrID(port), sysMLPort);
					mdSysmlPorts.add(port);

					// port name
					sysMLPort.setName(port.getName());

					// port URI
					String qName = port.getQualifiedName();
					sysMLPort.setAbout(URI.create(baseHTTPURI + "/services/"
							+ projectId + "/ports/"
							+ getQualifiedNameOrID(port)));

					// port type
					if (port.getType() != null) {
						if (MDSysMLModelHandler.isSysMLElement(port.getType(),
								"Block")) {
							sysMLPort.setType(new URI(baseHTTPURI
									+ "/services/"
									+ projectId
									+ "/blocks/"
									+ port.getType().getQualifiedName()
											.replaceAll("\\n", "-")
											.replaceAll(" ", "_")));
						} else if (MDSysMLModelHandler.isSysMLElement(
								port.getType(), "InterfaceBlock")) {
							sysMLPort.setType(new URI(baseHTTPURI
									+ "/services/"
									+ projectId
									+ "/interfaceblocks/"
									+ port.getType().getQualifiedName()
											.replaceAll("\\n", "-")
											.replaceAll(" ", "_")));
						}
					}

					// port owner
					if (port.getOwner() != null) {
						NamedElement portOwnerNamedElement = (NamedElement) port
								.getOwner();
						sysMLPort.setOwner(new URI(baseHTTPURI
								+ "/services/"
								+ projectId
								+ "/blocks/"
								+ portOwnerNamedElement.getQualifiedName()
										.replaceAll("\\n", "-")
										.replaceAll(" ", "_")));
					}

					// isService
					sysMLPort.setIsService(port.isService());

					// isBehavior
					sysMLPort.setIsBehavior(port.isBehavior());

					// isConjugated
					sysMLPort.setIsConjugated(port.isConjugated());

					// port multiplicity
					String lowerMultiplicity = Integer
							.toString(port.getLower());
					String upperMultiplicity = Integer
							.toString(port.getUpper());
					sysMLPort.setLower(lowerMultiplicity);
					sysMLPort.setUpper(upperMultiplicity);

				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			sysMLBlock.setPorts(portsLinksArray);
		}

	}

	private static void mapSysMLProxyPorts(Class mdSysMLBlock,
			SysMLInterfaceBlock sysMLInterfaceBlock) throws MDModelLibException {
		ArrayList<Port> proxyPortsList = new ArrayList<Port>();

		for (Port port : mdSysMLBlock.getOwnedPort()) {
			if (MDSysMLModelHandler.isSysMLElement(port, "ProxyPort")) {
				proxyPortsList.add(port);
			} else {
				// flow port
			}
		}

		Link[] proxyPortsLinksArray;

		if (proxyPortsList.size() > 0) {
			proxyPortsLinksArray = new Link[proxyPortsList.size()];
			String proxyPortBaseURI = baseHTTPURI + "/services/" + projectId
					+ "/proxyports/";
			int proxyPortsLinksArrayIndex = 0;
			for (Port port : proxyPortsList) {
				URI linkedElementURI = null;
				try {
					linkedElementURI = new URI(proxyPortBaseURI
							+ getQualifiedNameOrID(port));
					Link link = new Link(linkedElementURI);
					proxyPortsLinksArray[proxyPortsLinksArrayIndex] = link;
					proxyPortsLinksArrayIndex++;

					SysMLProxyPort sysMLProxyPort = new SysMLProxyPort();
					qNameOslcSysmlProxyPortMap.put(magicDrawFileName
							+ "/proxyports/" + getQualifiedNameOrID(port),
							sysMLProxyPort);

					// port name
					sysMLProxyPort.setName(port.getName());

					// port URI
					sysMLProxyPort.setAbout(URI.create(baseHTTPURI
							+ "/services/" + projectId + "/proxyports/"
							+ getQualifiedNameOrID(port)));

					// port type
					if (port.getType() != null) {
						if (MDSysMLModelHandler.isSysMLElement(port.getType(),
								"Block")) {
							sysMLProxyPort.setType(new URI(baseHTTPURI
									+ "/services/"
									+ projectId
									+ "/blocks/"
									+ port.getType().getQualifiedName()
											.replaceAll("\\n", "-")
											.replaceAll(" ", "_")));
						} else if (MDSysMLModelHandler.isSysMLElement(
								port.getType(), "InterfaceBlock")) {
							sysMLProxyPort.setType(new URI(baseHTTPURI
									+ "/services/"
									+ projectId
									+ "/interfaceblocks/"
									+ port.getType().getQualifiedName()
											.replaceAll("\\n", "-")
											.replaceAll(" ", "_")));
						}
					}

					// isService
					sysMLProxyPort.setIsService(port.isService());

					// isBehavior
					sysMLProxyPort.setIsBehavior(port.isBehavior());

					// isConjugated
					sysMLProxyPort.setIsConjugated(port.isConjugated());

				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			sysMLInterfaceBlock.setProxyPorts(proxyPortsLinksArray);
		}
	}

	private static void mapSysMLConnectors(Class mdSysMLBlock,
			SysMLBlock sysMLBlock) throws MDModelLibException {
		Link[] connectorsLinksArray = getLinkedSysMLElements(
				mdSysMLBlock.getOwnedConnector(), baseHTTPURI + "/services/"
						+ projectId + "/connectors/");

		if (connectorsLinksArray != null) {
			sysMLBlock.setConnectors(connectorsLinksArray);
			buffer.append("\r\n " + sysMLBlock.getName());
			buffer.append("\r\n\tblock connectors: "
					+ connectorsLinksArray.length);
			buffer.append("\r\n " + sysMLBlock.getName());
			buffer.append("\r\n\tblock connectors: ");
			for (Link link : connectorsLinksArray) {
				buffer.append("\r\n\t\t " + link.getValue());
			}
		}

		for (Connector connector : mdSysMLBlock.getOwnedConnector()) {
			SysMLConnector sysMLConnector;
			try {
				sysMLConnector = new SysMLConnector();

				qNameOslcSysmlConnectorMap.put(magicDrawFileName
						+ "/connectors/" + getQualifiedNameOrID(connector),
						sysMLConnector);
				mdSysmlConnectors.add(connector);
				if (!connector.getName().equals("")) {
					// connector name
					sysMLConnector.setName(connector.getName());
				}
				sysMLConnector.setAbout(URI.create(baseHTTPURI + "/services/"
						+ projectId + "/connectors/"
						+ getQualifiedNameOrID(connector)));

				// connector ends
				Link[] connectorsEndsLinksArray = getLinkedSysMLElements(
						connector.getEnd(), baseHTTPURI + "/services/"
								+ projectId + "/connectorends/");
				sysMLConnector.setEnds(connectorsEndsLinksArray);

				// connector type
				if (connector.getType() != null) {

					// connector type is an association block
					if (MDSysMLModelHandler.isSysMLElement(connector.getType(),
							"Block")) {
						sysMLConnector.setType(new URI(baseHTTPURI
								+ "/services/"
								+ projectId
								+ "/associationblocks/"
								+ connector.getType().getQualifiedName()
										.replaceAll("\\n", "-")
										.replaceAll(" ", "_")));
					} else if (connector.getType() instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Association) {
						sysMLConnector.setType(new URI(baseHTTPURI
								+ "/services/"
								+ projectId
								+ "/associations/"
								+ connector.getType().getQualifiedName()
										.replaceAll("\\n", "-")
										.replaceAll(" ", "_")));
					}
				}

				// connector owner
				if (connector.getOwner() != null) {
					if (connector.getOwner() instanceof NamedElement) {
						NamedElement namedElement = (NamedElement) connector
								.getOwner();
						sysMLConnector.setOwner(new URI(baseHTTPURI
								+ "/services/"
								+ projectId
								+ "/blocks/"
								+ namedElement.getQualifiedName()
										.replaceAll("\\n", "-")
										.replaceAll(" ", "_")));
					}
				}

				// map connector ends
				mapSysMLConnectorEnds(connector, sysMLConnector);

			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static void mapSysMLConnectorEnds(Connector connector,
			SysMLConnector sysMLConnector) throws MDModelLibException {

		for (ConnectorEnd connectorEnd : connector.getEnd()) {

			try {
				SysMLConnectorEnd sysMLConnectorEnd = new SysMLConnectorEnd();
				sysMLConnectorEnd.setAbout(URI.create(baseHTTPURI
						+ "/services/" + projectId + "/connectorends/"
						+ connectorEnd.getID()));
				qNameOslcSysmlConnectorEndMap.put(magicDrawFileName
						+ "/connectorends/" + connectorEnd.getID(),
						sysMLConnectorEnd);

				ConnectableElement role = connectorEnd.getRole();

				Property definingEnd = connectorEnd.getDefiningEnd();
				Property partWithPort = connectorEnd.getPartWithPort();

				// role
				if (MDSysMLModelHandler.isSysMLElement(role, "PartProperty")) {
					sysMLConnectorEnd.setRole(new URI(baseHTTPURI
							+ "/services/"
							+ projectId
							+ "/partproperties/"
							+ connectorEnd.getRole().getQualifiedName()
									.replaceAll("\\n", "-")
									.replaceAll(" ", "_")));
				} else if (MDSysMLModelHandler
						.isSysMLElement(role, "ProxyPort")) {
					sysMLConnectorEnd.setRole(new URI(baseHTTPURI
							+ "/services/"
							+ projectId
							+ "/proxyports/"
							+ connectorEnd.getRole().getQualifiedName()
									.replaceAll("\\n", "-")
									.replaceAll(" ", "_")));
				} else if (MDSysMLModelHandler.isSysMLElement(role, "FullPort")) {
					sysMLConnectorEnd.setRole(new URI(baseHTTPURI
							+ "/services/"
							+ projectId
							+ "/fullports/"
							+ connectorEnd.getRole().getQualifiedName()
									.replaceAll("\\n", "-")
									.replaceAll(" ", "_")));
				} else if (role instanceof com.nomagic.uml2.ext.magicdraw.compositestructures.mdports.Port) {
					sysMLConnectorEnd.setRole(new URI(baseHTTPURI
							+ "/services/"
							+ projectId
							+ "/ports/"
							+ connectorEnd.getRole().getQualifiedName()
									.replaceAll("\\n", "-")
									.replaceAll(" ", "_")));
				}

				// definingEnd
				if (definingEnd != null) {
					sysMLConnectorEnd.setDefiningEnd(new URI(baseHTTPURI
							+ "/services/"
							+ projectId
							+ "/partproperties/"
							+ connectorEnd.getDefiningEnd().getQualifiedName()
									.replaceAll("\\n", "-")
									.replaceAll(" ", "_")));
				}

				// partWithPort
				if (partWithPort != null) {
					sysMLConnectorEnd.setPartWithPort(new URI(baseHTTPURI
							+ "/services/"
							+ projectId
							+ "/partproperties/"
							+ connectorEnd.getPartWithPort().getQualifiedName()
									.replaceAll("\\n", "-")
									.replaceAll(" ", "_")));
				}

			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	private static void mapSysMLReferenceProperties(Class mdSysmlBlock,
			SysMLBlock sysMLBlock) throws MDModelLibException {

		Link[] blockReferencesLinksArray = getLinkedStereotypedSysMLElements(
				mdSysmlBlock.getOwnedAttribute(), "ReferenceProperty",
				baseHTTPURI + "/services/" + projectId
						+ "/referenceproperties/");

		if (blockReferencesLinksArray != null) {
			sysMLBlock.setReferenceProperties(blockReferencesLinksArray);

			buffer.append("\r\n " + sysMLBlock.getName());
			buffer.append("\r\n\tblock reference properties: "
					+ blockReferencesLinksArray.length);
			buffer.append("\r\n " + sysMLBlock.getName());
			buffer.append("\r\n\tblock reference properties: ");
			for (Link link : blockReferencesLinksArray) {
				buffer.append("\r\n\t\t " + link.getValue());
			}
		}

		for (Property property : mdSysmlBlock.getOwnedAttribute()) {

			if (property.getAppliedStereotypeInstance() != null) {
				InstanceSpecification stereotypeInstance = property
						.getAppliedStereotypeInstance();
				if (stereotypeInstance.getClassifier().get(0).getName()
						.contains("ReferenceProperty")) {
					SysMLReferenceProperty sysmlReferenceProperty;
					try {
						sysmlReferenceProperty = new SysMLReferenceProperty();
						qNameOslcSysmlReferencePropertyMap.put(
								magicDrawFileName
										+ "/referenceproperties/"
										+ property.getQualifiedName()
												.replaceAll("\\n", "-")
												.replaceAll(" ", "_"),
								sysmlReferenceProperty);

						// referenceProperty name
						sysmlReferenceProperty.setName(property.getName());

						String qName = property.getQualifiedName();
						sysmlReferenceProperty.setAbout(URI.create(baseHTTPURI
								+ "/services/"
								+ projectId
								+ "/referenceproperties/"
								+ qName.replaceAll("\\n", "-").replaceAll(" ",
										"_")));

						// referenceProperty type
						sysmlReferenceProperty.setType(new URI(baseHTTPURI
								+ "/services/"
								+ projectId
								+ "/blocks/"
								+ property.getType().getQualifiedName()
										.replaceAll("\\n", "-")
										.replaceAll(" ", "_")));

						// referenceProperty multiplicity
						String lowerMultiplicity = Integer.toString(property
								.getLower());
						String upperMultiplicity = Integer.toString(property
								.getUpper());
						sysmlReferenceProperty.setLower(lowerMultiplicity);
						sysmlReferenceProperty.setUpper(upperMultiplicity);

						// referenceProperty association attribute
						Association mdSysMAssociation = property
								.getAssociation();
						if (mdSysMAssociation != null) {
							URI linkedElementURI = null;
							String baseURI = baseHTTPURI + "/services/"
									+ projectId + "/unknown/";
							// check if property has an associationBlock as
							// association
							if (MDSysMLModelHandler.isSysMLElement(
									property.getAssociation(), "Block")) {
								// if (!mdSysmlAssociationBlocks
								// .contains((com.nomagic.uml2.ext.magicdraw.classes.mdassociationclasses.AssociationClass)
								// property.getAssociation())) {
								// mdSysmlAssociationBlocks.add((com.nomagic.uml2.ext.magicdraw.classes.mdassociationclasses.AssociationClass)
								// property
								// .getAssociation());
								// }
								baseURI = baseHTTPURI + "/services/"
										+ projectId + "/associationblocks/";
							} else if (property.getAssociation() instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Association) {
								baseURI = baseHTTPURI + "/services/"
										+ projectId + "/associations/";
							}
							linkedElementURI = new URI(baseURI
									+ mdSysMAssociation.getQualifiedName()
											.replaceAll("\\n", "-")
											.replaceAll(" ", "_"));
							sysmlReferenceProperty
									.setAssociation(linkedElementURI);
						}

					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}
		}

	}

	private static void mapSysMLFlowProperties(Class mdSysmlBlock,
			SysMLBlock sysMLBlock) {

		Link[] flowPropertiesLinksArray = getLinkedStereotypedSysMLElements(
				mdSysmlBlock.getOwnedAttribute(), "FlowProperty", baseHTTPURI
						+ "/services/" + projectId + "/flowproperties/");

		if (flowPropertiesLinksArray != null) {
			sysMLBlock.setFlowProperties(flowPropertiesLinksArray);

			buffer.append("\r\n " + sysMLBlock.getName());
			buffer.append("\r\n\tblock reference properties: "
					+ flowPropertiesLinksArray.length);
			buffer.append("\r\n " + sysMLBlock.getName());
			buffer.append("\r\n\tblock reference properties: ");
			for (Link link : flowPropertiesLinksArray) {
				buffer.append("\r\n\t\t " + link.getValue());
			}
		}

		for (Property property : mdSysmlBlock.getOwnedAttribute()) {

			if (property.getAppliedStereotypeInstance() != null) {
				InstanceSpecification stereotypeInstance = property
						.getAppliedStereotypeInstance();
				if (stereotypeInstance.getClassifier().get(0).getName()
						.contains("FlowProperty")) {
					SysMLFlowProperty sysmlFlowProperty;
					try {
						sysmlFlowProperty = new SysMLFlowProperty();
						qNameOslcSysmlFlowPropertyMap.put(
								magicDrawFileName
										+ "/flowproperties/"
										+ property.getQualifiedName()
												.replaceAll("\\n", "-")
												.replaceAll(" ", "_"),
								sysmlFlowProperty);
						mdSysmlFlowProperties.add(property);
						
						
						// referenceProperty name
						sysmlFlowProperty.setName(property.getName());

						String qName = property.getQualifiedName();
						sysmlFlowProperty.setAbout(URI.create(baseHTTPURI
								+ "/services/"
								+ projectId
								+ "/flowproperties/"
								+ qName.replaceAll("\\n", "-").replaceAll(" ",
										"_")));

						// referenceProperty type
						sysmlFlowProperty.setType(new URI(baseHTTPURI
								+ "/services/"
								+ projectId
								+ "/blocks/"
								+ property.getType().getQualifiedName()
										.replaceAll("\\n", "-")
										.replaceAll(" ", "_")));

						// referenceProperty multiplicity
						String lowerMultiplicity = Integer.toString(property
								.getLower());
						String upperMultiplicity = Integer.toString(property
								.getUpper());
						sysmlFlowProperty.setLower(lowerMultiplicity);
						sysmlFlowProperty.setUpper(upperMultiplicity);

						// direction
						Object directionObject = StereotypesHelper
								.getStereotypePropertyFirst(property,
										(Stereotype) property
												.getAppliedStereotypeInstance()
												.getClassifier().get(0),
										"direction");
						if (directionObject instanceof EnumerationLiteral) {
							EnumerationLiteral enumLit = (EnumerationLiteral) directionObject;
							String enumLitName = enumLit.getName();
							if (enumLitName.equals("in")) {
								// sysmlFlowProperty
								// .setDirection(SysMLFlowDirection.IN);
								sysmlFlowProperty.setDirection("in");
							} else if (enumLitName.equals("out")) {
								// sysmlFlowProperty
								// .setDirection(SysMLFlowDirection.OUT);
								sysmlFlowProperty.setDirection("out");
							}

						}

					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}
		}

	}

	private static Link[] getLinkedSysMLElements(
			Collection<? extends Element> elementCollection,
			String linkedElementBaseURI) {

		// counting the number of links
		int linksCount = elementCollection.size();

		// creating linksArray
		Link[] linksArray = null;
		if (linksCount > 0) {
			linksArray = new Link[linksCount];
		}

		// populating linksArray
		int linksArrayIndex = 0;
		for (Element element : elementCollection) {
			try {
				URI linkedElementURI = null;
				linkedElementURI = new URI(linkedElementBaseURI
						+ getQualifiedNameOrID(element));
				Link link = new Link(linkedElementURI);
				linksArray[linksArrayIndex] = link;
				linksArrayIndex++;
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return linksArray;
	}

	private static Link[] getLinkedStereotypedSysMLElements(
			Collection<? extends NamedElement> namedElementCollection,
			String stereotypeName, String linkedElementBaseURI) {

		// counting the number of links
		int linksCount = 0;
		for (NamedElement namedElement : namedElementCollection) {
			if (namedElement.getAppliedStereotypeInstance() != null) {
				InstanceSpecification stereotypeInstance = namedElement
						.getAppliedStereotypeInstance();
				if (stereotypeInstance.getClassifier().get(0).getName()
						.equals(stereotypeName)) {
					linksCount++;
				}
			}
		}

		// creating linksArray
		Link[] linksArray = null;
		if (linksCount > 0) {
			linksArray = new Link[linksCount];
		}

		// populating linksArray
		int linksArrayIndex = 0;
		for (NamedElement namedElement : namedElementCollection) {
			if (namedElement.getAppliedStereotypeInstance() != null) {
				InstanceSpecification stereotypeInstance = namedElement
						.getAppliedStereotypeInstance();
				if (stereotypeInstance.getClassifier().get(0).getName()
						.equals(stereotypeName)) {
					try {
						URI linkedElementURI = null;
						if (stereotypeName.equals("Requirement")
								& namedElement instanceof Class) {
							String linkedElementID = (String) StereotypesHelper
									.getStereotypePropertyFirst(
											namedElement,
											StereotypesHelper
													.getFirstVisibleStereotype(namedElement),
											"Id");
							linkedElementURI = new URI(linkedElementBaseURI
									+ linkedElementID);
						} else {
							linkedElementURI = new URI(linkedElementBaseURI
									+ getQualifiedNameOrID(namedElement));
						}

						Link link = new Link(linkedElementURI);
						linksArray[linksArrayIndex] = link;
						linksArrayIndex++;
					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		return linksArray;

	}

	private static URI getDirectedLinkSysMLElement(boolean isElementSource,
			Element element, String relationshipType)
			throws MDModelLibException {

		Collection<DirectedRelationship> directedRelationships;
		if (isElementSource) {
			directedRelationships = element.get_directedRelationshipOfSource();
		} else {
			directedRelationships = element.get_directedRelationshipOfTarget();
		}

		// getting relationships of that specific type
		Collection<DirectedRelationship> relationshipsOfType = new ArrayList<DirectedRelationship>();
		for (DirectedRelationship directedRelationship : directedRelationships) {
			try {
				String directedRelationshipType = directedRelationship
						.getAppliedStereotypeInstance().getClassifier().get(0)
						.getName();
				if (directedRelationshipType.equals(relationshipType)) {
					relationshipsOfType.add(directedRelationship);
				}
			} catch (Exception e) {
			}
		}

		URI linkedElementURI = null;
		for (DirectedRelationship directedRelationship : relationshipsOfType) {
			Collection<Element> linkedElements;
			if (isElementSource) {
				linkedElements = directedRelationship.getTarget();
			} else {
				linkedElements = directedRelationship.getSource();
			}
			for (Element linkedElement : linkedElements) {
				if (linkedElement instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement) {
					NamedElement linkedNamedElement = (NamedElement) linkedElement;
					String linkedElementBaseURI = baseHTTPURI + "/services/"
							+ projectId + "/unknown/";
					String linkedElementID = null;
					boolean isLinkedElementRequirement = false;
					if (MDSysMLModelHandler.isSysMLElement(linkedNamedElement,
							"Block")) {
						linkedElementBaseURI = baseHTTPURI + "/services/"
								+ projectId + "/blocks/";
					} else if (MDSysMLModelHandler.isSysMLElement(
							linkedNamedElement, "Requirement")) {
						linkedElementID = (String) StereotypesHelper
								.getStereotypePropertyFirst(
										linkedNamedElement,
										StereotypesHelper
												.getFirstVisibleStereotype(linkedNamedElement),
										"Id");
						isLinkedElementRequirement = true;
						linkedElementBaseURI = baseHTTPURI + "/services/"
								+ projectId + "/requirements/";
					} else if (linkedNamedElement instanceof com.nomagic.uml2.ext.magicdraw.mdusecases.UseCase) {
						linkedElementBaseURI = baseHTTPURI + "/services/"
								+ projectId + "/usecases/";
					}
					String linkedElementQName = null;
					if (!isLinkedElementRequirement) {
						linkedElementQName = linkedNamedElement
								.getQualifiedName().replaceAll("\\n", "-")
								.replaceAll(" ", "_");
					} else {
						linkedElementQName = linkedElementID;
					}

					try {
						linkedElementURI = new URI(linkedElementBaseURI
								+ linkedElementQName);
					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		return linkedElementURI;
	}

	private static Link[] getDirectedLinksOfSysMLElement(
			boolean isElementSource, Element element, String relationshipType)
			throws MDModelLibException {

		Collection<DirectedRelationship> directedRelationships;
		if (isElementSource) {
			directedRelationships = element.get_directedRelationshipOfSource();
		} else {
			directedRelationships = element.get_directedRelationshipOfTarget();
		}

		// counting the number of relationships of that specific type
		Collection<DirectedRelationship> relationshipsOfType = new ArrayList<DirectedRelationship>();
		for (DirectedRelationship directedRelationship : directedRelationships) {
			try {
				String directedRelationshipType = directedRelationship
						.getAppliedStereotypeInstance().getClassifier().get(0)
						.getName();
				if (directedRelationshipType.equals(relationshipType)) {
					relationshipsOfType.add(directedRelationship);
				}
			} catch (Exception e) {
			}
		}

		// creating the links array
		Link[] linksArray = null;
		if (relationshipsOfType.size() > 0) {
			linksArray = new Link[relationshipsOfType.size()];
		}

		// populating the links array
		int linksArrayIndex = 0;
		for (DirectedRelationship directedRelationship : relationshipsOfType) {
			Collection<Element> linkedElements;
			if (isElementSource) {
				linkedElements = directedRelationship.getTarget();
			} else {
				linkedElements = directedRelationship.getSource();
			}
			for (Element linkedElement : linkedElements) {
				if (linkedElement instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement) {
					NamedElement linkedNamedElement = (NamedElement) linkedElement;
					String linkedElementBaseURI = baseHTTPURI + "/services/"
							+ projectId + "/unknown/";
					String linkedElementID = null;
					boolean isLinkedElementRequirement = false;
					if (MDSysMLModelHandler.isSysMLElement(linkedNamedElement,
							"Block")) {
						linkedElementBaseURI = baseHTTPURI + "/services/"
								+ projectId + "/blocks/";
					} else if (MDSysMLModelHandler.isSysMLElement(
							linkedNamedElement, "Requirement")) {
						linkedElementID = (String) StereotypesHelper
								.getStereotypePropertyFirst(
										linkedNamedElement,
										StereotypesHelper
												.getFirstVisibleStereotype(linkedNamedElement),
										"Id");
						isLinkedElementRequirement = true;
						linkedElementBaseURI = baseHTTPURI + "/services/"
								+ projectId + "/requirements/";
					} else if (linkedNamedElement instanceof com.nomagic.uml2.ext.magicdraw.mdusecases.UseCase) {
						linkedElementBaseURI = baseHTTPURI + "/services/"
								+ projectId + "/usecases/";
					}
					String linkedElementQName = null;
					if (!isLinkedElementRequirement) {
						linkedElementQName = linkedNamedElement
								.getQualifiedName().replaceAll("\\n", "-")
								.replaceAll(" ", "_");
					} else {
						linkedElementQName = linkedElementID;
					}
					URI linkedElementURI = null;
					try {
						linkedElementURI = new URI(linkedElementBaseURI
								+ linkedElementQName);
						Link link = new Link(linkedElementURI);
						linksArray[linksArrayIndex] = link;
						linksArrayIndex++;
					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		return linksArray;
	}

	private static void mapSysMLPartProperties(Class mdSysmlBlock,
			SysMLBlock sysMLBlock) {

		Link[] blockPartsLinksArray = getLinkedStereotypedSysMLElements(
				mdSysmlBlock.getOwnedAttribute(), "PartProperty", baseHTTPURI
						+ "/services/" + projectId + "/partproperties/");

		if (blockPartsLinksArray != null) {
			sysMLBlock.setPartProperties(blockPartsLinksArray);

			buffer.append("\r\n " + sysMLBlock.getName());
			buffer.append("\r\n\tblockParts: " + blockPartsLinksArray.length);
			buffer.append("\r\n " + sysMLBlock.getName());
			buffer.append("\r\n\tblockParts: ");
			for (Link link : blockPartsLinksArray) {
				buffer.append("\r\n\t\t " + link.getValue());
			}
		}

		for (Property property : mdSysmlBlock.getOwnedAttribute()) {

			if (property.getAppliedStereotypeInstance() != null) {
				InstanceSpecification stereotypeInstance = property
						.getAppliedStereotypeInstance();
				if (stereotypeInstance.getClassifier().get(0).getName()
						.contains("PartProperty")) {
					SysMLPartProperty sysmlPartProperty;
					try {
						sysmlPartProperty = new SysMLPartProperty();
						qNameOslcSysmlPartPropertyMap.put(magicDrawFileName
								+ "/partproperties/"
								+ getQualifiedNameOrID(property),
								sysmlPartProperty);
						mdSysmlPartProperties.add(property);

						// partProperty name
						sysmlPartProperty.setName(property.getName());

						String qName = property.getQualifiedName();
						sysmlPartProperty.setAbout(URI.create(baseHTTPURI
								+ "/services/" + projectId + "/partproperties/"
								+ getQualifiedNameOrID(property)));

						// partProperty type
						sysmlPartProperty.setType(new URI(baseHTTPURI
								+ "/services/"
								+ projectId
								+ "/blocks/"
								+ property.getType().getQualifiedName()
										.replaceAll("\\n", "-")
										.replaceAll(" ", "_")));

						// partProperty owner
						NamedElement partPropertyOwnerNamedElement = (NamedElement) property
								.getOwner();
						sysmlPartProperty.setOwner(new URI(baseHTTPURI
								+ "/services/"
								+ projectId
								+ "/blocks/"
								+ partPropertyOwnerNamedElement
										.getQualifiedName()
										.replaceAll("\\n", "-")
										.replaceAll(" ", "_")));

						// partProperty multiplicity
						String lowerMultiplicity = Integer.toString(property
								.getLower());
						String upperMultiplicity = Integer.toString(property
								.getUpper());
						sysmlPartProperty.setLower(lowerMultiplicity);
						sysmlPartProperty.setUpper(upperMultiplicity);

					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}
		}

	}

	private static void mapSysMLRequirements() {
		for (Class mdSysMLRequirement : mdSysmlRequirements) {
			String id = (String) StereotypesHelper.getStereotypePropertyFirst(
					mdSysMLRequirement, StereotypesHelper
							.getFirstVisibleStereotype(mdSysMLRequirement),
					"Id");
			if (id != null) {
				idMdSysmlRequirementMap.put(id, mdSysMLRequirement);
				// qNameMdSysmlRequirementMap.put(
				// mdSysMLRequirement.getQualifiedName()
				// .replaceAll("\\n", "-").replaceAll(" ", "_"),
				// mdSysMLRequirement);

				SysMLRequirement sysMLRequirement;
				try {
					sysMLRequirement = new SysMLRequirement();

					// SysML Requirement id attribute
					sysMLRequirement.setIdentifier(id);
					idOslcSysmlRequirementMap.put(id, sysMLRequirement);

					sysMLRequirement
							.setAbout(URI.create(baseHTTPURI + "/services/"
									+ projectId + "/requirements/" + id));

					// qNameOslcSysmlRequirementMap.put(mdSysMLRequirement
					// .getQualifiedName().replaceAll("\\n", "-")
					// .replaceAll(" ", "_"), sysMLRequirement);
					buffer.append("\r\nSysML Requirement with ID: "
							+ sysMLRequirement.getIdentifier());

					// SysML Requirement Name attribute
					String name = mdSysMLRequirement.getName();
					if (name != null) {
						sysMLRequirement.setTitle(name);
						buffer.append("\r\n\tTitle: "
								+ sysMLRequirement.getTitle());
					}

					// SysML Requirement Text attribute
					String text = (String) StereotypesHelper
							.getStereotypePropertyFirst(
									mdSysMLRequirement,
									StereotypesHelper
											.getFirstVisibleStereotype(mdSysMLRequirement),
									"Text");
					if (text != null) {
						sysMLRequirement.setDescription(text);
						buffer.append("\r\n\tDescription: "
								+ sysMLRequirement.getDescription());
					}

					// SysML Requirement hyperlink attribute
					Stereotype hyperlinkOwnerStereotype = StereotypesHelper
							.getStereotype(
									Project.getProject(mdSysMLRequirement),
									"HyperlinkOwner");
					List textValues = StereotypesHelper
							.getStereotypePropertyValue(mdSysMLRequirement,
									hyperlinkOwnerStereotype, "hyperlinkText");
					if (textValues.size() > 0) {
						String hyperlinkText = (String) textValues.get(0);
						sysMLRequirement.setHyperlink(hyperlinkText);
					}
					// for (int i = textValues.size() - 1; i >= 0; --i) {
					// String link = (String) textValues.get(i);
					// }

				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	public static SysMLBlockDiagram getBlockDiagramByQualifiedName(
			String qualifiedName) throws URISyntaxException {
		SysMLBlockDiagram sysMLBlockDiagram = qNameOslcSysmlBlockDiagramMap
				.get(qualifiedName);
		return sysMLBlockDiagram;
	}

	public static List<SysMLBlockDiagram> getBlockDiagrams(String projectName) {
		List<SysMLBlockDiagram> sysMLBlocks = new ArrayList<SysMLBlockDiagram>();
		for (String qNameOslcSysmlElement : qNameOslcSysmlBlockDiagramMap
				.keySet()) {
			if (qNameOslcSysmlElement.startsWith(projectName
					+ "/blockdiagrams/")) {
				sysMLBlocks.add(qNameOslcSysmlBlockDiagramMap
						.get(qNameOslcSysmlElement));
			}
		}
		return sysMLBlocks;
	}

	public static SysMLInternalBlockDiagram getInternalBlockDiagramByQualifiedName(
			String qualifiedName) throws URISyntaxException {
		SysMLInternalBlockDiagram sysMLInternalBlockDiagram = qNameOslcSysmlInternalBlockDiagramMap
				.get(qualifiedName);
		return sysMLInternalBlockDiagram;
	}

	public static List<SysMLInternalBlockDiagram> getInternalBlockDiagrams(
			String projectName) {
		List<SysMLInternalBlockDiagram> sysMLBlocks = new ArrayList<SysMLInternalBlockDiagram>();
		for (String qNameOslcSysmlElement : qNameOslcSysmlInternalBlockDiagramMap
				.keySet()) {
			if (qNameOslcSysmlElement.startsWith(projectName
					+ "/internalblockdiagrams/")) {
				sysMLBlocks.add(qNameOslcSysmlInternalBlockDiagramMap
						.get(qNameOslcSysmlElement));
			}
		}
		return sysMLBlocks;
	}

	public static SysMLRequirement getRequirementByID(String qualifiedName)
			throws URISyntaxException {
		SysMLRequirement sysMLRequirement = idOslcSysmlRequirementMap
				.get(qualifiedName);

		return sysMLRequirement;
	}

	public static List<SysMLRequirement> getRequirements() {
		List<SysMLRequirement> sysMLRequirements = new ArrayList<SysMLRequirement>();
		for (String id : idOslcSysmlRequirementMap.keySet()) {
			sysMLRequirements.add(idOslcSysmlRequirementMap.get(id));
		}
		return sysMLRequirements;
	}

	public static org.eclipse.lyo.adapter.magicdraw.resources.SysMLBlock getBlockByQualifiedName(
			String qualifiedName) {
		SysMLBlock sysMLBlock = qNameOslcSysmlBlockMap.get(qualifiedName);
		return sysMLBlock;
	}

	public static List<SysMLBlock> getBlocks(String projectName) {
		List<SysMLBlock> sysMLBlocks = new ArrayList<SysMLBlock>();
		for (String qNameOslcSysmlElement : qNameOslcSysmlBlockMap.keySet()) {
			if (qNameOslcSysmlElement.startsWith(projectName + "/blocks/")) {
				sysMLBlocks.add(qNameOslcSysmlBlockMap
						.get(qNameOslcSysmlElement));
			}
		}
		return sysMLBlocks;
	}

	static Collection<com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package> getAllSysMLPackages(
			com.nomagic.uml2.ext.magicdraw.classes.mdkernel.PackageableElement packageableElement) {
		Collection<com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package> sysmlPackages = new ArrayList<com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package>();

		if (packageableElement instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package) {
			com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package package_ = (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package) packageableElement;
			if (!predefinedMagicDrawSysMLPackageNames.contains(package_
					.getName())) {
				if (!(packageableElement instanceof Model)) {
					sysmlPackages.add(package_);
				}
				// add nested packages
				for (PackageableElement nestedPackageableElement : package_
						.getPackagedElement()) {
					if (nestedPackageableElement instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package) {
						com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package nestedPackage = (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package) nestedPackageableElement;
						if (!predefinedMagicDrawSysMLPackageNames
								.contains(nestedPackage.getName())) {
							sysmlPackages
									.addAll(getAllSysMLPackages(nestedPackage));
						}
					}
				}
			}
		}

		return sysmlPackages;
	}

	static Collection<com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class> getAllSysMLBlocks(
			com.nomagic.uml2.ext.magicdraw.classes.mdkernel.PackageableElement packageableElement)
			throws MDModelLibException {
		Collection<com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class> sysmlBlocks = new ArrayList<com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class>();
		if (packageableElement instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package) {
			com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package package_ = (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package) packageableElement;
			for (PackageableElement nestedPackageableElement : package_
					.getPackagedElement()) {
				if (MDSysMLModelHandler.isSysMLElement(
						nestedPackageableElement, "Block")
						| MDSysMLModelHandler.isSysMLElement(
								nestedPackageableElement, "System")) {
					Class magicDrawSysMLBlock = (Class) nestedPackageableElement;
					sysmlBlocks.add(magicDrawSysMLBlock);
					sysmlBlocks.addAll(getAllSysMLBlocks(magicDrawSysMLBlock));
				} else if (nestedPackageableElement instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package) {
					com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package nestedPackage = (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package) nestedPackageableElement;
					if (!predefinedMagicDrawSysMLPackageNames
							.contains(nestedPackage.getName())) {
						sysmlBlocks.addAll(getAllSysMLBlocks(nestedPackage));
					}
				}
			}
		} else if (packageableElement instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class) {
			com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class class_ = (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class) packageableElement;
			for (Classifier nestedClassifier : class_.getNestedClassifier()) {
				if (MDSysMLModelHandler.isSysMLElement(nestedClassifier,
						"Block")
						| MDSysMLModelHandler.isSysMLElement(nestedClassifier,
								"System")) {
					sysmlBlocks
							.add((com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class) nestedClassifier);
				}
				sysmlBlocks.addAll(getAllSysMLBlocks(nestedClassifier));
			}
		}
		return sysmlBlocks;
	}

	static Collection<com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class> getAllSysMLInterfaceBlocks(
			com.nomagic.uml2.ext.magicdraw.classes.mdkernel.PackageableElement packageableElement)
			throws MDModelLibException {
		Collection<com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class> sysmlBlocks = new ArrayList<com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class>();
		if (packageableElement instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package) {
			com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package package_ = (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package) packageableElement;
			for (PackageableElement nestedPackageableElement : package_
					.getPackagedElement()) {
				if (MDSysMLModelHandler.isSysMLElement(
						nestedPackageableElement, "InterfaceBlock")) {
					Class magicDrawSysMLBlock = (Class) nestedPackageableElement;
					sysmlBlocks.add(magicDrawSysMLBlock);
					sysmlBlocks
							.addAll(getAllSysMLInterfaceBlocks(magicDrawSysMLBlock));
				} else if (nestedPackageableElement instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package) {
					com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package nestedPackage = (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package) nestedPackageableElement;
					if (!predefinedMagicDrawSysMLPackageNames
							.contains(nestedPackage.getName())) {
						sysmlBlocks
								.addAll(getAllSysMLInterfaceBlocks(nestedPackage));
					}
				}
			}
		} else if (packageableElement instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class) {
			com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class class_ = (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class) packageableElement;
			for (Classifier nestedClassifier : class_.getNestedClassifier()) {
				if (MDSysMLModelHandler.isSysMLElement(nestedClassifier,
						"InterfaceBlock")) {
					sysmlBlocks
							.add((com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class) nestedClassifier);
				}
				sysmlBlocks
						.addAll(getAllSysMLInterfaceBlocks(nestedClassifier));
			}
		}
		return sysmlBlocks;
	}

	static Collection<com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class> getAllSysMLRequirements(
			com.nomagic.uml2.ext.magicdraw.classes.mdkernel.PackageableElement packageableElement)
			throws MDModelLibException {
		Collection<com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class> sysmlRequirements = new ArrayList<com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class>();
		if (packageableElement instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package) {
			com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package package_ = (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package) packageableElement;
			for (PackageableElement nestedPackageableElement : package_
					.getPackagedElement()) {
				if (MDSysMLModelHandler.isSysMLElement(
						nestedPackageableElement, "Requirement")) {
					com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class sysMLRequirement = (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class) nestedPackageableElement;
					sysmlRequirements.add(sysMLRequirement);
					sysmlRequirements
							.addAll(getAllSysMLRequirements(sysMLRequirement));
				} else if (nestedPackageableElement instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package) {
					com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package nestedPackage = (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package) nestedPackageableElement;
					if (!predefinedMagicDrawSysMLPackageNames
							.contains(nestedPackage.getName())) {
						sysmlRequirements
								.addAll(getAllSysMLRequirements(nestedPackage));
					}
				}
			}
		} else if (packageableElement instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class) {
			com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class class_ = (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class) packageableElement;
			for (Classifier nestedClassifier : class_.getNestedClassifier()) {
				if (MDSysMLModelHandler.isSysMLElement(nestedClassifier,
						"Requirement")) {
					sysmlRequirements
							.add((com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class) nestedClassifier);
				}
				sysmlRequirements
						.addAll(getAllSysMLRequirements(nestedClassifier));
			}
		}
		return sysmlRequirements;
	}

	public static SysMLPartProperty getPartPropertyByQualifiedName(
			String propertyQualifiedName) {
		SysMLPartProperty sysMLPartProperty = qNameOslcSysmlPartPropertyMap
				.get(propertyQualifiedName);
		return sysMLPartProperty;
	}

	public static List<SysMLPartProperty> getPartProperties(String projectName) {
		List<SysMLPartProperty> elements = new ArrayList<SysMLPartProperty>();
		for (String qNameOslcSysmlElement : qNameOslcSysmlPartPropertyMap
				.keySet()) {
			if (qNameOslcSysmlElement.startsWith(projectName
					+ "/partproperties/")) {
				elements.add(qNameOslcSysmlPartPropertyMap
						.get(qNameOslcSysmlElement));
			}
		}
		return elements;
	}

	public static SysMLReferenceProperty getReferencePropertyByQualifiedName(
			String propertyQualifiedName) {
		SysMLReferenceProperty sysMLReferenceProperty = qNameOslcSysmlReferencePropertyMap
				.get(propertyQualifiedName);
		return sysMLReferenceProperty;
	}

	public static List<SysMLReferenceProperty> getReferenceProperties(
			String projectName) {
		List<SysMLReferenceProperty> elements = new ArrayList<SysMLReferenceProperty>();
		for (String qNameOslcSysmlElement : qNameOslcSysmlReferencePropertyMap
				.keySet()) {
			if (qNameOslcSysmlElement.startsWith(projectName
					+ "/referenceproperties/")) {
				elements.add(qNameOslcSysmlReferencePropertyMap
						.get(qNameOslcSysmlElement));
			}
		}
		return elements;
	}

	public static SysMLValueProperty getValuePropertyByQualifiedName(
			String propertyQualifiedName) {
		SysMLValueProperty sysMLValueProperty = qNameOslcSysmlValuePropertyMap
				.get(propertyQualifiedName);
		return sysMLValueProperty;
	}

	public static List<SysMLValueProperty> getValueProperties(String projectName) {
		List<SysMLValueProperty> elements = new ArrayList<SysMLValueProperty>();
		for (String qNameOslcSysmlElement : qNameOslcSysmlValuePropertyMap
				.keySet()) {
			if (qNameOslcSysmlElement.startsWith(projectName
					+ "/valueproperties/")) {
				elements.add(qNameOslcSysmlValuePropertyMap
						.get(qNameOslcSysmlElement));
			}
		}
		return elements;
	}

	public static SysMLValueType getValueTypeByQualifiedName(
			String propertyQualifiedName) {
		SysMLValueType sysMLValueType = qNameOslcSysmlValueTypeMap
				.get(propertyQualifiedName);
		return sysMLValueType;
	}

	public static List<SysMLValueType> getValueTypes(String projectName) {
		List<SysMLValueType> elements = new ArrayList<SysMLValueType>();
		for (String qNameOslcSysmlElement : qNameOslcSysmlValueTypeMap.keySet()) {
			if (qNameOslcSysmlElement.startsWith(projectName + "/valuetypes/")) {
				elements.add(qNameOslcSysmlValueTypeMap
						.get(qNameOslcSysmlElement));
			}
		}
		return elements;
	}

	public static SysMLFlowProperty getFlowPropertyByQualifiedName(
			String propertyQualifiedName) {
		SysMLFlowProperty sysMLFlowProperty = qNameOslcSysmlFlowPropertyMap
				.get(propertyQualifiedName);
		return sysMLFlowProperty;
	}

	public static List<SysMLFlowProperty> getFlowProperties(String projectName) {
		List<SysMLFlowProperty> elements = new ArrayList<SysMLFlowProperty>();
		for (String qNameOslcSysmlElement : qNameOslcSysmlFlowPropertyMap
				.keySet()) {
			if (qNameOslcSysmlElement.startsWith(projectName
					+ "/flowproperties/")) {
				elements.add(qNameOslcSysmlFlowPropertyMap
						.get(qNameOslcSysmlElement));
			}
		}
		return elements;
	}

	public static List<SysMLBoundReference> getBoundReferences(String projectName) {
		List<SysMLBoundReference> elements = new ArrayList<SysMLBoundReference>();
		for (String qNameOslcSysmlElement : qNameOslcSysmlBoundReferenceMap
				.keySet()) {
			if (qNameOslcSysmlElement.startsWith(projectName
					+ "/boundreferences/")) {
				elements.add(qNameOslcSysmlBoundReferenceMap
						.get(qNameOslcSysmlElement));
			}
		}
		return elements;
	}
	
	public static SysMLInterfaceBlock getInterfaceBlockByQualifiedName(
			String propertyQualifiedName) {
		SysMLInterfaceBlock sysMLInterfaceBlock = qNameOslcSysmlInterfaceBlockMap
				.get(propertyQualifiedName);
		return sysMLInterfaceBlock;
	}

	public static List<SysMLInterfaceBlock> getInterfaceBlocks(
			String projectName) {
		List<SysMLInterfaceBlock> elements = new ArrayList<SysMLInterfaceBlock>();
		for (String qNameOslcSysmlElement : qNameOslcSysmlInterfaceBlockMap
				.keySet()) {
			if (qNameOslcSysmlElement.startsWith(projectName
					+ "/interfaceblocks/")) {
				elements.add(qNameOslcSysmlInterfaceBlockMap
						.get(qNameOslcSysmlElement));
			}
		}
		return elements;
	}

	public static SysMLItemFlow getItemFlowByQualifiedName(
			String propertyQualifiedName) {
		SysMLItemFlow sysMLItemFlow = qNameOslcSysmlItemFlowMap
				.get(propertyQualifiedName);
		return sysMLItemFlow;
	}

	public static List<SysMLItemFlow> getItemFlows() {
		List<SysMLItemFlow> sysMLItemFlows = new ArrayList<SysMLItemFlow>();
		for (String qNameOslcSysmlElement : qNameOslcSysmlItemFlowMap.keySet()) {
			sysMLItemFlows.add(qNameOslcSysmlItemFlowMap
					.get(qNameOslcSysmlElement));
		}
		return sysMLItemFlows;
	}

	public static SysMLPort getPortByQualifiedName(String propertyQualifiedName) {
		SysMLPort sysMLPort = qNameOslcSysmlPortMap.get(propertyQualifiedName);
		return sysMLPort;
	}

	public static List<SysMLPort> getPorts(String projectName) {
		List<SysMLPort> elements = new ArrayList<SysMLPort>();
		for (String qNameOslcSysmlElement : qNameOslcSysmlPortMap.keySet()) {
			if (qNameOslcSysmlElement.startsWith(projectName + "/ports/")) {
				elements.add(qNameOslcSysmlPortMap.get(qNameOslcSysmlElement));
			}
		}
		return elements;
	}

	public static SysMLProxyPort getProxyPortByQualifiedName(
			String propertyQualifiedName) {
		SysMLProxyPort sysMLProxyPort = qNameOslcSysmlProxyPortMap
				.get(propertyQualifiedName);
		return sysMLProxyPort;
	}

	public static List<SysMLProxyPort> getProxyPorts(String projectName) {
		List<SysMLProxyPort> elements = new ArrayList<SysMLProxyPort>();
		for (String qNameOslcSysmlElement : qNameOslcSysmlProxyPortMap.keySet()) {
			if (qNameOslcSysmlElement.startsWith(projectName + "/proxyports/")) {
				elements.add(qNameOslcSysmlProxyPortMap
						.get(qNameOslcSysmlElement));
			}
		}
		return elements;
	}

	public static SysMLFullPort getFullPortByQualifiedName(
			String propertyQualifiedName) {
		SysMLFullPort sysMLFullPort = qNameOslcSysmlFullPortMap
				.get(propertyQualifiedName);
		return sysMLFullPort;
	}

	public static List<SysMLFullPort> getFullPorts(String projectName) {
		List<SysMLFullPort> elements = new ArrayList<SysMLFullPort>();
		for (String qNameOslcSysmlElement : qNameOslcSysmlFullPortMap.keySet()) {
			if (qNameOslcSysmlElement.startsWith(projectName + "/fullports/")) {
				elements.add(qNameOslcSysmlFullPortMap
						.get(qNameOslcSysmlElement));
			}
		}
		return elements;
	}

	public static SysMLConnector getConnectorByQualifiedName(
			String propertyQualifiedName) {
		SysMLConnector sysMLConnector = qNameOslcSysmlConnectorMap
				.get(propertyQualifiedName);
		return sysMLConnector;
	}

	public static List<SysMLConnector> getConnectors(String projectName) {
		List<SysMLConnector> elements = new ArrayList<SysMLConnector>();
		for (String qNameOslcSysmlElement : qNameOslcSysmlConnectorMap.keySet()) {
			if (qNameOslcSysmlElement.startsWith(projectName + "/connectors/")) {
				elements.add(qNameOslcSysmlConnectorMap
						.get(qNameOslcSysmlElement));
			}
		}
		return elements;
	}

	public static SysMLConnectorEnd getConnectorEndByQualifiedName(
			String propertyQualifiedName) {
		// if(qNameOslcSysmlConnectorEndMap.keySet().size() > 0){
		// String key = (String)
		// qNameOslcSysmlConnectorEndMap.keySet().toArray()[0];
		// if(key.startsWith("/oslc4jmagicdraw/services/")){
		// propertyQualifiedName = "/oslc4jmagicdraw/services/" + projectId +
		// "/connectorends/" + propertyQualifiedName;
		// }
		// }
		SysMLConnectorEnd sysMLConnectorEnd = qNameOslcSysmlConnectorEndMap
				.get(propertyQualifiedName);
		return sysMLConnectorEnd;
	}

	public static List<SysMLConnectorEnd> getConnectorEnds(String projectName) {
		List<SysMLConnectorEnd> elements = new ArrayList<SysMLConnectorEnd>();
		for (String qNameOslcSysmlElement : qNameOslcSysmlConnectorEndMap
				.keySet()) {
			if (qNameOslcSysmlElement.startsWith(projectName
					+ "/connectorends/")) {
				elements.add(qNameOslcSysmlConnectorEndMap
						.get(qNameOslcSysmlElement));
			}
		}
		return elements;
	}

	public static SysMLModel getModelByName(String modelName) {
		SysMLModel sysMLModel = oslcSysmlModelMap.get(modelName);
		return sysMLModel;
	}

	public static List<SysMLModel> getModels() {
		List<SysMLModel> sysMLModels = new ArrayList<SysMLModel>();
		for (String id : oslcSysmlModelMap.keySet()) {
			sysMLModels.add(oslcSysmlModelMap.get(id));
		}
		return sysMLModels;
	}

	public static SysMLPackage getPackageByQualifiedName(String qualifiedName) {
		SysMLPackage sysMLPackage = qNameOslcSysmlPackageMap.get(qualifiedName);
		return sysMLPackage;
	}

	public static List<SysMLPackage> getPackages(String projectName) {
		List<SysMLPackage> elements = new ArrayList<SysMLPackage>();
		for (String qNameOslcSysmlElement : qNameOslcSysmlPackageMap.keySet()) {
			if (qNameOslcSysmlElement.startsWith(projectName + "/packages/")) {
				elements.add(qNameOslcSysmlPackageMap
						.get(qNameOslcSysmlElement));
			}
		}
		return elements;
	}

	public static SysMLAssociationBlock getAssociationBlockByQualifiedName(
			String blockQualifiedName) {
		SysMLAssociationBlock sysMLAssociationBlock = qNameOslcSysmlAssociationBlockMap
				.get(blockQualifiedName);
		return sysMLAssociationBlock;
	}

	public static List<SysMLAssociationBlock> getAssociationBlocks(
			String projectName) {
		List<SysMLAssociationBlock> elements = new ArrayList<SysMLAssociationBlock>();
		for (String qNameOslcSysmlElement : qNameOslcSysmlAssociationBlockMap
				.keySet()) {
			if (qNameOslcSysmlElement.startsWith(projectName
					+ "/associationblocks/")) {
				elements.add(qNameOslcSysmlAssociationBlockMap
						.get(qNameOslcSysmlElement));
			}
		}
		return elements;
	}
	
	public static List<SysMLItemFlow> getItemFlows(
			String projectName) {
		List<SysMLItemFlow> elements = new ArrayList<SysMLItemFlow>();
		for (String qNameOslcSysmlElement : qNameOslcSysmlItemFlowMap
				.keySet()) {
			if (qNameOslcSysmlElement.startsWith(projectName
					+ "/itemflows/")) {
				elements.add(qNameOslcSysmlItemFlowMap
						.get(qNameOslcSysmlElement));
			}
		}
		return elements;
	}

	

	

	

	public static String getQualifiedNameOrID(Element element) {
		String qfOrID = null;
		if (element instanceof NamedElement) {
			NamedElement namedElement = (NamedElement) element;
			if (namedElement.getName().equals("")) {
				qfOrID = element.getID();
			} else {
				qfOrID = ((NamedElement) element).getQualifiedName()
						.replaceAll("\\n", "-").replaceAll(" ", "_").replaceAll("\\[", "%5B").replaceAll("\\]", "%5D").replaceAll("\\^", "%5E");
				// ascii encoding at http://www.w3schools.com/tags/ref_urlencode.asp
			}
		} else {
			qfOrID = element.getID();
		}
		return qfOrID;
	}

	public static String getQualifiedNameFromURI(URI uri) {
		String uriString = uri.getRawPath();
		uriString = uriString.replace("/oslc4jmagicdraw/services/", "");
		String[] uriStrings = uriString.split("/");
		String qualifiedName = uriStrings[2];
		// qualifiedName = qualifiedName.replaceAll("\\n", "-").replaceAll(" ",
		// "_");
		return qualifiedName;
	}

	public static String getNameFromQualifiedName(String qualifiedName) {
		String[] elementQNameParts = qualifiedName.split("::");
		int newElementQNamePartsSize = elementQNameParts.length;
		String elementName = elementQNameParts[newElementQNamePartsSize - 1];
		return elementName;
	}

	public static URI getURIFromQualifiedName(String typeAndQualifiedName) {
		String[] typeAndQualifiedNameStrings = typeAndQualifiedName.split("_");
		String type = typeAndQualifiedNameStrings[0];
		String qualifiedName = typeAndQualifiedNameStrings[typeAndQualifiedNameStrings.length - 1];

		// URI cannot contain empty characters
		qualifiedName = qualifiedName.replaceAll("\\n", "-").replaceAll(" ",
				"_");

		String elementType = null;
		if (type.equals("MODEL")) {
			elementType = "model";
		} else if (type.equals("PACKAGE")) {
			elementType = "packages";
		} else if (type.equals("BLOCK")) {
			elementType = "blocks";
		} else if (type.equals("REQUIREMENT")) {
			elementType = "requirements";
		} else if (type.equals("PART")) {
			elementType = "parts";
		} else if (type.equals("PORT")) {
			elementType = "ports";
		}

		URI elementURI = URI.create(baseHTTPURI + "/services/" + projectId
				+ "/" + elementType + "/" + qualifiedName);

		return elementURI;
	}

	// http://javarevisited.blogspot.com/2013/03/generate-md5-hash-in-java-string-byte-array-example-tutorial.html
	// http://stackoverflow.com/questions/2836646/java-serializable-object-to-byte-array
	public static String md5Java(Object object) {
		String digest = null;
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(bos);
			out.writeObject(object);
			byte[] objectBytes = bos.toByteArray();
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] hash = md.digest(objectBytes);
			StringBuilder sb = new StringBuilder(2 * hash.length);
			for (byte b : hash) {
				sb.append(String.format("%02x", b & 0xff));
			}
			digest = sb.toString();
		} catch (UnsupportedEncodingException ex) {
		} catch (NoSuchAlgorithmException ex) {
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return digest;
	}

	public static void collectingRDFData() {
		
		try {
			sysMLModelInRDF = JenaModelHelper
					.createJenaModel(getBlocks(projectId).toArray());
			RDFWriter sysMLModelWriter = sysMLModelInRDF
					.getWriter("RDF/XML");
			sysMLModelWriter.setProperty("showXmlDeclaration", "false");
			sysMLModelWriter.setErrorHandler(new ErrorHandler());
			OutputStream sysmlModelOutputStream;
			try {
				sysmlModelOutputStream = new FileOutputStream("sysml-blocks" + ".rdf");
				sysMLModelWriter.write(sysMLModelInRDF, sysmlModelOutputStream,
						null);
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			
			com.hp.hpl.jena.rdf.model.Model sysMLPartPropertiesModel = JenaModelHelper
					.createJenaModel(getPartProperties(projectId).toArray());
			sysMLModelInRDF.add(sysMLPartPropertiesModel);
			com.hp.hpl.jena.rdf.model.Model sysMLFullPortsModel = JenaModelHelper
					.createJenaModel(getFullPorts(projectId).toArray());
			sysMLModelInRDF.add(sysMLFullPortsModel);
			com.hp.hpl.jena.rdf.model.Model sysMLConnectorsModel = JenaModelHelper
					.createJenaModel(getConnectors(projectId).toArray());
			sysMLModelInRDF.add(sysMLConnectorsModel);
			com.hp.hpl.jena.rdf.model.Model sysMLConnectorEndsModel = JenaModelHelper
					.createJenaModel(getConnectorEnds(projectId).toArray());
			sysMLModelInRDF.add(sysMLConnectorEndsModel);
			
			com.hp.hpl.jena.rdf.model.Model sysMLReferencePropertiesModel = JenaModelHelper
					.createJenaModel(getReferenceProperties(projectId).toArray());
			sysMLModelInRDF.add(sysMLReferencePropertiesModel);
			
			com.hp.hpl.jena.rdf.model.Model sysMLPackagesModel = JenaModelHelper
					.createJenaModel(getPackages(projectId).toArray());
			sysMLModelInRDF.add(sysMLPackagesModel);
			
			com.hp.hpl.jena.rdf.model.Model sysMLAssociationBlocksModel = JenaModelHelper
					.createJenaModel(getAssociationBlocks(projectId).toArray());
			sysMLModelInRDF.add(sysMLAssociationBlocksModel);
			
			com.hp.hpl.jena.rdf.model.Model sysMLPortsModel = JenaModelHelper
					.createJenaModel(getPorts(projectId).toArray());
			sysMLModelInRDF.add(sysMLPortsModel);
			
			com.hp.hpl.jena.rdf.model.Model sysMLProxyPortsModel = JenaModelHelper
					.createJenaModel(getProxyPorts(projectId).toArray());
			sysMLModelInRDF.add(sysMLProxyPortsModel);
			
			com.hp.hpl.jena.rdf.model.Model sysMLInterfaceBlocksModel = JenaModelHelper
					.createJenaModel(getInterfaceBlocks(projectId).toArray());
			sysMLModelInRDF.add(sysMLInterfaceBlocksModel);
			
			com.hp.hpl.jena.rdf.model.Model sysMLFlowPropertiesModel = JenaModelHelper
					.createJenaModel(getFlowProperties(projectId).toArray());
			sysMLModelInRDF.add(sysMLFlowPropertiesModel);
			
			com.hp.hpl.jena.rdf.model.Model sysMLBoundReferencesModel = JenaModelHelper
					.createJenaModel(getBoundReferences(projectId).toArray());
			sysMLModelInRDF.add(sysMLBoundReferencesModel);
			
			com.hp.hpl.jena.rdf.model.Model sysMLItemFlowsModel = JenaModelHelper
					.createJenaModel(getItemFlows(projectId).toArray());
			sysMLModelInRDF.add(sysMLItemFlowsModel);
			
			com.hp.hpl.jena.rdf.model.Model sysMLValuePropertiesModel = JenaModelHelper
					.createJenaModel(getValueProperties(projectId).toArray());
			sysMLModelInRDF.add(sysMLValuePropertiesModel);
			
			com.hp.hpl.jena.rdf.model.Model sysMLValueTypesModel = JenaModelHelper
					.createJenaModel(getValueTypes(projectId).toArray());
			sysMLModelInRDF.add(sysMLValueTypesModel);
			
			com.hp.hpl.jena.rdf.model.Model sysMLBlockDiagramsModel = JenaModelHelper
					.createJenaModel(getBlockDiagrams(projectId).toArray());
			sysMLModelInRDF.add(sysMLBlockDiagramsModel);
			
			com.hp.hpl.jena.rdf.model.Model sysMLInternalBlockDiagramsModel = JenaModelHelper
					.createJenaModel(getInternalBlockDiagrams(projectId).toArray());
			sysMLModelInRDF.add(sysMLInternalBlockDiagramsModel);
			
			
		} catch (IllegalAccessException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (IllegalArgumentException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (InvocationTargetException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (DatatypeConfigurationException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (OslcCoreApplicationException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

	}

	public static boolean performSPARQLASKQuery(String sparqlQuery) {
		
		
		
//		String designRule1QueryString = 
//				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
//				"PREFIX sysml: <http://omg.org/sysml/rdf#> \n" +
//				"PREFIX sysml_namedelement: <http://omg.org/sysml/rdf#NamedElement/> \n" +
//				"PREFIX sysml_block: <http://omg.org/sysml/rdf#Block/> \n" +
//				"PREFIX sysml_property: <http://omg.org/sysml/rdf#Property/> \n" +
//				"PREFIX sysml_connector: <http://omg.org/sysml/rdf#Connector/> \n" +
//				"PREFIX sysml_connectorend: <http://omg.org/sysml/rdf#ConnectorEnd/> \n" +
//				"SELECT DISTINCT ?connector \n" +			
//				"WHERE {\n" +
//				"    ?connector  rdf:type sysml:Connector . \n" + 
//				" }\n"
//				
//				; 
		
//		String designRule1QueryString = 
//				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
//				"PREFIX sysml: <http://omg.org/sysml/rdf#> \n" +
//				"PREFIX sysml_namedelement: <http://omg.org/sysml/rdf#NamedElement/> \n" +
//				"PREFIX sysml_block: <http://omg.org/sysml/rdf#Block/> \n" +
//				"PREFIX sysml_property: <http://omg.org/sysml/rdf#Property/> \n" +
//				"PREFIX sysml_connector: <http://omg.org/sysml/rdf#Connector/> \n" +
//				"PREFIX sysml_connectorend: <http://omg.org/sysml/rdf#ConnectorEnd/> \n" +
//				"ASK  \n" +			
//				"WHERE {\n" +
//				"    ?connector  rdf:type sysml:Connector . \n" + 
//				" }\n"
//				
//				; 
		
		Query query = QueryFactory.create(sparqlQuery);
//		System.out.println("*** SPARQL Query ***");
//		System.out.println("");
//		System.out.println(queryString);
//		System.out.println("");
//		System.out.println("*** SPARQL Result ***");
//		System.out.println("");
		
		// Execute the query and obtain results
		QueryExecution qe = QueryExecutionFactory.create(query, sysMLModelInRDF);
		
		
//		ResultSet results = qe.execSelect();
		
		
//	    if(results.hasNext()) {		
//	        QuerySolution soln = results.next();	        
//	        RDFNode node = soln.get("connector");
//	        Resource resource = node.asResource();
//	        String uri = resource.getURI();
//	    }
		
		
//		com.hp.hpl.jena.rdf.model.Model queryResultModel = results.getResourceModel();
//		// writing sysml full ports to file
//		
//		
//		// write sysml model to file to check
//		RDFWriter sysMLModelWriter = sysMLModelInRDF
//				.getWriter("RDF/XML");
//		sysMLModelWriter.setProperty("showXmlDeclaration", "false");
//		sysMLModelWriter.setErrorHandler(new ErrorHandler());
//		OutputStream sysmlModelOutputStream;
//		try {
//			sysmlModelOutputStream = new FileOutputStream("sysml-model" + ".rdf");
//			sysMLModelWriter.write(sysMLModelInRDF, sysmlModelOutputStream,
//					null);
//		} catch (FileNotFoundException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		
//		// write query results to file to check
//		RDFWriter sysMLQueryResultsWriter = queryResultModel
//				.getWriter("RDF/XML");
//		sysMLQueryResultsWriter.setProperty("showXmlDeclaration", "false");
//		sysMLQueryResultsWriter.setErrorHandler(new ErrorHandler());
//		OutputStream queryResultOutputStream;
//		try {
//			queryResultOutputStream = new FileOutputStream("query-results" + ".rdf");
//			sysMLQueryResultsWriter.write(queryResultModel, queryResultOutputStream,
//					null);
//		} catch (FileNotFoundException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		
		
		
		
//		ResultSetFormatter.out(System.out, results, query);
		boolean hasFoundPattern = qe.execAsk(); 
		System.out.println(hasFoundPattern);


		// Important - free up resources used running the query
		qe.close();	
		return hasFoundPattern;
	}

	public static ArrayList<ArrayList<Element>> performSPARQLSELECTQuery(String sparqlQuery, ArrayList<String> queryVariables) {
		
		Query query = QueryFactory.create(sparqlQuery);

//		Query initialquery = QueryFactory.create(sparqlQuery, Syntax.syntaxARQ);
//		AlgebraGenerator ag = new AlgebraGenerator();
//		Op op = ag.compile(initialquery);
//		Query query = OpAsQuery.asQuery(op);
		
		// Execute the query and obtain results
		QueryExecution qe = QueryExecutionFactory.create(query, sysMLModelInRDF);
		
		Iterator<QuerySolution> results = qe.execSelect();
//		ResultSet results = qe.execSelect();
		
		
		
		ArrayList<ArrayList<Element>> queryResults = new ArrayList<ArrayList<Element>>();
		
	    while(results.hasNext()) {		
	    	QuerySolution soln = results.next();
	    	
	    	ArrayList<Element> queryVariablesOfMatch = new ArrayList<Element>();
	    	
	    	for (String queryVariable : queryVariables) {
	    		RDFNode node = soln.get(queryVariable);
	    		if(node == null){
	    			queryVariablesOfMatch.add(null);
	    			continue;
	    		}
	    		if(node.isResource()){
	    			 Resource resource = node.asResource();
	 		        String uri = resource.getURI();
	 		        Element element = getElementBasedOnURI(uri);
	 		        queryVariablesOfMatch.add(element);
	    		}
	    		else if (node.isLiteral()){
	    			Literal literal = node.asLiteral();
	 		        String literalString = literal.getString();
	 		       ElementsFactory elementsFactory = projectGlobal
	 						.getElementsFactory();
	 		       com.nomagic.uml2.ext.magicdraw.classes.mdkernel.LiteralString literalStringInstance = elementsFactory
	 						.createLiteralStringInstance();
	 		      literalStringInstance.setValue(literalString);
	 		        queryVariablesOfMatch.add(literalStringInstance);
	    		}
		       
			}
	    	queryResults.add(queryVariablesOfMatch);           
	    }

//		ResultSetFormatter.out(System.out, results, query);



		// Important - free up resources used running the query
		qe.close();
		return queryResults;
	}
	
	public static Element getElementBasedOnURI(String uri){
		Element element = null;
		String shorturi = uri.replace(baseHTTPURI + "/services/" + projectId
				+ "/", "");
		
		if(shorturi.startsWith("blocks")){			
			String qualifiedName = shorturi.replace("blocks/", "");
			for (Class mdElement : mdSysmlBlocks) {
				if (getQualifiedName(mdElement).equals(
						qualifiedName)) {
					element = mdElement;
					break;
				}
			};
		}
		else if(shorturi.startsWith("connectors")){			
//			getMagicDrawElementFromList(mdSysmlConnectors, qualifiedName);
			
			for (Connector mdElement : mdSysmlConnectors) {
				if (("connectors/" + mdElement.getID()).equals(
						shorturi)) {
					element = mdElement;
					break;
				}
			};
		}
		else if(shorturi.startsWith("itemflows")){			
			String qualifiedName = shorturi.replace("itemflows/", "");
			for (NamedElement mdElement : mdSysmlItemFlows) {
				if (getQualifiedName(mdElement).equals(
						qualifiedName)) {
					element = mdElement;
					break;
				}
			}
		}
		else if(shorturi.startsWith("requirements")){			
			String qualifiedName = shorturi.replace("requirements/", "");
			for (NamedElement mdElement : mdSysmlRequirements) {
				if (getQualifiedName(mdElement).equals(
						qualifiedName)) {
					element = mdElement;
					break;
				}
			}
		}
		else if(shorturi.startsWith("interfaceblocks")){			
			String qualifiedName = shorturi.replace("interfaceblocks/", "");
			for (NamedElement mdElement : mdSysmlInterfaceBlocks) {
				if (getQualifiedName(mdElement).equals(
						qualifiedName)) {
					element = mdElement;
					break;
				}
			}
		}
		else if(shorturi.startsWith("valuetypes")){			
			String qualifiedName = shorturi.replace("valuetypes/", "");
			for (NamedElement mdElement : mdSysmlValueTypes) {
				if (getQualifiedName(mdElement).equals(
						qualifiedName)) {
					element = mdElement;
					break;
				}
			}
		}
		else if(shorturi.startsWith("partproperties")){			
			String qualifiedName = shorturi.replace("partproperties/", "");
			for (NamedElement mdElement : mdSysmlPartProperties) {
				if (getQualifiedName(mdElement).equals(
						qualifiedName)) {
					element = mdElement;
					break;
				}
			}
		}
		else if(shorturi.startsWith("valueproperties")){			
			String qualifiedName = shorturi.replace("valueproperties/", "");
			for (NamedElement mdElement : mdSysmlValueProperties) {
				if (getQualifiedName(mdElement).equals(
						qualifiedName)) {
					element = mdElement;
					break;
				}
			}
		}
		else if(shorturi.startsWith("flowproperties")){			
			String qualifiedName = shorturi.replace("flowproperties/", "");
			for (NamedElement mdElement : mdSysmlFlowProperties) {
				if (getQualifiedName(mdElement).equals(
						qualifiedName)) {
					element = mdElement;
					break;
				}
			}
		}
		else if(shorturi.startsWith("boundreferences")){			
			String qualifiedName = shorturi.replace("boundreferences/", "");
			for (NamedElement mdElement : mdSysmlBoundReferences) {
				if (getQualifiedName(mdElement).equals(
						qualifiedName)) {
					element = mdElement;
					break;
				}
			}
		}
		else if(shorturi.startsWith("ports")){			
			String qualifiedName = shorturi.replace("ports/", "");
			for (NamedElement mdElement : mdSysmlPorts) {
				if (getQualifiedName(mdElement).equals(
						qualifiedName)) {
					element = mdElement;
					break;
				}
			}
		}
		else if(shorturi.startsWith("associationblocks")){			
			String qualifiedName = shorturi.replace("associationblocks/", "");
			for (NamedElement mdElement : mdSysmlAssociationBlocks) {
				if (getQualifiedName(mdElement).equals(
						qualifiedName)) {
					element = mdElement;
					break;
				}
			}
		}
		else if(shorturi.startsWith("packages")){			
			String qualifiedName = shorturi.replace("packages/", "");
			for (NamedElement mdElement : mdSysmlPackages) {
				if (getQualifiedName(mdElement).equals(
						qualifiedName)) {
					element = mdElement;
					break;
				}
			}
		}
			
		return element;
	}
	
//	static Element getMagicDrawElementFromList(Collection<Element> mdElements, String qualifiedName){
//		Element element = null;
//		for (Element mdElement : mdElements) {
//			if(mdElement instanceof NamedElement){
//				if (((NamedElement) mdElement).getQualifiedName().equals(
//						qualifiedName)) {
//					element = mdElement;
//					break;
//				}
//			}
//			else{
//				if (mdElement.getID().equals(
//						qualifiedName)) {
//					element = mdElement;
//					break;
//				}
//			}
//		};
//		return element;
//	}
	public static String getQualifiedName(Element element){
		NamedElement namedElemnent = (NamedElement)element;
		String modifiedQualifiedName = namedElemnent.getQualifiedName()
				.replaceAll("\\n", "-")
				.replaceAll(" ", "_"); 
		return modifiedQualifiedName;
	}
}

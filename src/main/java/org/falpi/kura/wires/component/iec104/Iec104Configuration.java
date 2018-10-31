package org.falpi.kura.wires.component.iec104;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.apache.xmlbeans.impl.xb.xsdschema.SchemaDocument;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlObject;

import org.osgi.framework.FrameworkUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;

import org.falpi.kura.Utils;
import org.falpi.kura.XmlUtils;

public class Iec104Configuration {
    
    // #########################################################################################################
    // Proprietà pubbliche
    // #########################################################################################################
	
	// Variabili di configurazione (alterabile per riflettere lo stato dinamico del componente)
	public Boolean Enabled;
	
	// Variabili di configurazione non alterabili (se non attraverso una riconfigurazione)
	public final String DeviceId;
	public final String Host;
	public final Integer Port;
	public final Integer CommonAddr;
	public final Integer Delay;
	public final Boolean GenInt;
	public final Boolean InfoAlert;
	public final Boolean ErrorAlert;
	public final Boolean EnrichmentFilter;
	
	// Variabili di configurazione per l'enrichment
	public Map<String,TypedValue<?>> AlertEnrichment;
	public Map<String,TypedValue<?>> DefaultEnrichment;
	public Map<Integer,Map<String,TypedValue<?>>> MatchingEnrichment;
    
    // #########################################################################################################
    // Proprietà private 
    // #########################################################################################################

	// Dizionario delle proprietà configurabili del componente
	private static final String IEC104_ENABLED_PROP_NAME = "iec104.enabled";
	private static final String IEC104_DEVICE_PROP_NAME = "iec104.device-id";
	private static final String IEC104_HOST_PROP_NAME = "iec104.host";
	private static final String IEC104_PORT_PROP_NAME = "iec104.port";
	private static final String IEC104_COMMON_ADDR_PROP_NAME = "iec104.common-addr";
	private static final String IEC104_DELAY_PROP_NAME = "iec104.delay";
	private static final String IEC104_GENINT_PROP_NAME = "iec104.genint";
	private static final String IEC104_INFO_ALERT_PROP_NAME = "iec104.info-alert";
	private static final String IEC104_ERROR_ALERT_PROP_NAME = "iec104.error-alert";
	private static final String IEC104_ENRICHMENT_PROP_NAME = "iec104.enrichment";
	private static final String IEC104_ENRICHMENT_FILTER_PROP_NAME = "iec104.enrichment-filter";
	
	// Referenzia il logger della classe principale
	private static final Logger logger = LoggerFactory.getLogger(Iec104Subscriber.class);

	// Costanti private 
	private static final String StrSchemaPath = "/assets/enrichmentSchema.xsd"; 
	
	// Definisce gli elementi XML attesi per le tipologie di mapping valide
	private static final List<String> ArrEnrichmentTypes = 
			Arrays.asList(new String[]{"alert","default","matching"});
	
	// Definisce le metriche riservate non usabili per l'enrichment
	private static final List<String> ArrReservedMetrics = 
			Arrays.asList(new String[]{"id","host","port","event","message","type","test","cot","oa","ca","ioa","val","qual","time"});
    
    // #########################################################################################################
    // Costruttore della classe
    // #########################################################################################################
	
	// Costruttore
    public Iec104Configuration(final Map<String, Object> properties) { 
    	        
    	//Dichiara variabili
    	int IntIOA;
    	
    	String StrValidationErrors;
    	String StrEnrichmentSchema;
    	String StrEnrichmentConfig;
    	XmlObject ObjEnrichmentSchema;
    	XmlObject ObjEnrichmentConfig;    	
    	
    	Node ObjAttribute;
    	Node ObjConfigRoot;
    	Node ObjConfigItem;
    	NodeList ObjConfigItems;
    	NamedNodeMap ObjAttributes;
        
    	ArrayList<XmlError> ArrValidationErrors;
    	Map<String, TypedValue<?>> ObjWireValues;
    	
    	// Inizializza parametri 
        this.Enabled = (Boolean)properties.get(IEC104_ENABLED_PROP_NAME);
        this.DeviceId = (String)properties.get(IEC104_DEVICE_PROP_NAME);
        this.Host = (String)properties.get(IEC104_HOST_PROP_NAME);
        this.Port = (Integer)properties.get(IEC104_PORT_PROP_NAME);
        this.CommonAddr = (Integer)properties.get(IEC104_COMMON_ADDR_PROP_NAME);
        this.Delay = (Integer)properties.get(IEC104_DELAY_PROP_NAME);
        this.GenInt = (Boolean)properties.get(IEC104_GENINT_PROP_NAME);
        this.InfoAlert = (Boolean)properties.get(IEC104_INFO_ALERT_PROP_NAME);
        this.ErrorAlert = (Boolean)properties.get(IEC104_ERROR_ALERT_PROP_NAME);
        this.EnrichmentFilter = (Boolean)properties.get(IEC104_ENRICHMENT_FILTER_PROP_NAME);
       
        this.AlertEnrichment = new HashMap<>();
        this.DefaultEnrichment = new HashMap<>();
        this.MatchingEnrichment = new HashMap<>();
        
        // =========================================================================================================
        // Gestione dell'enrichment
        // =========================================================================================================
        
        StrEnrichmentConfig = (String)properties.get(IEC104_ENRICHMENT_PROP_NAME);
        
        // Se è stata fornita una configurazione per l'enrichment la gestisce
        if (StrEnrichmentConfig!="") {
        	
        	// Verifica la validità della configurazione e prepara il DOM XML
	        try {
	            logger.debug("Reading enrichment schema");
	            StrEnrichmentSchema = Utils.getTextResource(FrameworkUtil.getBundle(Iec104Subscriber.class), StrSchemaPath);  
	            
	            logger.debug("Parsing enrichment schema");	
	            ObjEnrichmentSchema = SchemaDocument.Factory.parse(StrEnrichmentSchema);  
	            
	            logger.debug("Parsing enrichment config");	
	            ObjEnrichmentConfig = XmlObject.Factory.parse((String)properties.get(IEC104_ENRICHMENT_PROP_NAME));
	            
	            logger.debug("Validating enrichment config");	
	            ArrValidationErrors = XmlUtils.validate(ObjEnrichmentConfig, new XmlObject[] {ObjEnrichmentSchema});
	            
	            // Se si sono verificati errori di validazione costruisce stringa e genera eccezione
	            if (!ArrValidationErrors.isEmpty()) {
	            	
	                StrValidationErrors = "";
		            for (XmlError ObjError : ArrValidationErrors) {
		            	StrValidationErrors += "\nline "+ObjError.getLine()+": "+ObjError.getMessage()+" ("+ObjError.getErrorCode()+")";
		            }
		            
		            throw new Exception("Validation errors."+StrValidationErrors);
		        }
		        
		        // Enumera gli item di configurazione e prepara l'array di enrichment 
		        ObjConfigRoot = ObjEnrichmentConfig.getDomNode().getChildNodes().item(0);
		        
		        if (ObjConfigRoot.hasChildNodes()) {
		        	
		        	ObjConfigItems = ObjConfigRoot.getChildNodes();
		            	
			        for (int IntIndex=0;IntIndex<ObjConfigItems.getLength();IntIndex++) {
			        
			        	ObjConfigItem = ObjConfigItems.item(IntIndex);
			        	
			        	if (ArrEnrichmentTypes.contains(ObjConfigItem.getNodeName())) {	
			        		
			        		IntIOA = 0;
			        		ObjWireValues = new HashMap<>();
			        		ObjAttributes = ObjConfigItem.getAttributes();
			        		
			        		// Ciclo di scansione degli attributi
			        		for (int IntAttr=0;IntAttr<ObjAttributes.getLength();IntAttr++) {
			        			
			        			ObjAttribute = ObjAttributes.item(IntAttr);
			        			
			        			if ((ObjConfigItem.getNodeName()=="matching")&&(ObjAttribute.getNodeName()=="ioa")) {
			        			   IntIOA = Integer.parseInt(ObjAttribute.getNodeValue());
			        			} else if (!ArrReservedMetrics.contains(ObjAttribute.getNodeName())) {
			        			   ObjWireValues.put(ObjAttribute.getNodeName(),
			        				                 TypedValues.newStringValue(ObjAttribute.getNodeValue()));
			        			} else {
			    		            logger.warn("Enrichment skipped reserved metric ("+ObjAttribute.getNodeName()+")");			        				
			        			}
			        		}
			               	
				            // Se c'è almeno una metrica aggiuntiva crea il mapping per lo IOA
			        		if (ObjWireValues.size()>0) {
			        			switch (ObjConfigItem.getNodeName()) {
			        			   case "alert": this.AlertEnrichment = ObjWireValues; break;
			        			   case "default": this.DefaultEnrichment = ObjWireValues; break;
			        			   case "matching": this.MatchingEnrichment.put(IntIOA,ObjWireValues); break;
			        			}
				     	      	  
			        		}
			        	}	     	    
			        }
		        }
	        }
	        catch (Exception e) {
	        	logger.error("Error during enrichment preparation. Reason: "+e.getMessage());
	 	        return;
	        }
        }
        
        // =========================================================================================================
    	 
    }    
}

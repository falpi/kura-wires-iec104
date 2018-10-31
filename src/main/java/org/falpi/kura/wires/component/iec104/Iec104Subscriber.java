package org.falpi.kura.wires.component.iec104;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openmuc.j60870.ASdu;
import org.openmuc.j60870.InformationObject;
import org.openmuc.j60870.InformationElement;
import org.openmuc.j60870.CauseOfTransmission;
import org.openmuc.j60870.ClientConnectionBuilder;
import org.openmuc.j60870.Connection;
import org.openmuc.j60870.ConnectionEventListener;
import org.openmuc.j60870.IeQualifierOfInterrogation;

public class Iec104Subscriber implements WireEmitter, ConfigurableComponent, ConnectionEventListener, Runnable {

	private enum Alert {
		
		ERROR_ALERT(0,"ERROR"),
		CONNECT_ALERT(1,"CONNECT"),
	    DISCONNECT_ALERT(2,"DISCONNECT");
		
	    public final int id;
	    public final String description;
	    
		private Alert(int id, String description) {
	        this.id = id;
	        this.description = description;
	    }
	}
	
	private static final Logger logger = LoggerFactory.getLogger(Iec104Subscriber.class);
		    
	// Gestione del supporto ai wire
	private WireSupport wireSupport;
	private volatile WireHelperService wireHelperService;
    
	// Gestione dello scheduling del timer
	private ScheduledFuture<?> scheduledHandle;
	private ScheduledExecutorService scheduledWorker;
    
	private Connection serverConnection;
	private Iec104Configuration actualConfiguration;
	private Iec104Configuration desiredConfiguration;
		
	public void bindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == null) {
            this.wireHelperService = wireHelperService;
        }
    }

    public void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
        }
    }
	
	public void activate(final Map<String, Object> properties) throws Exception {
   	
    	// Logging
    	logger.info("Activating IEC-104 Subscriber...");
   	 
        // Registra helper service
		this.wireSupport = this.wireHelperService.newWireSupport(this);		

		// Prepara la nuova configurazione attesa
        this.actualConfiguration = new Iec104Configuration(properties);
        this.desiredConfiguration = this.actualConfiguration;
        
		// Schedula il thread di controllo della connessione 
        this.scheduledWorker = Executors.newSingleThreadScheduledExecutor();    	 
		this.scheduledHandle = this.scheduledWorker.scheduleAtFixedRate(this,1,this.actualConfiguration.Delay,TimeUnit.SECONDS);
		
        // Logging
    	logger.info("Activating IEC-104 Subscriber... Done");
	}
		
    public void modified(final Map<String, Object> properties) throws Exception {
    	
    	// Logging
        logger.info("Updating IEC-104 Subscriber...");
        
		// Prepara la nuova configurazione attesa
        this.desiredConfiguration = new Iec104Configuration(properties);

        // Forza la riconfigurazione senza attendere il prossimo schedule del timer
        run();
        
        // Logging
        logger.info("Updating IEC-104 Subscriber... Done");
    }
	
	public void deactivate() throws Exception {
		
		// Logging
        logger.info("Deactivating IEC-104 Subscriber...");
        
        // Disattiva il timer schedulato
        this.scheduledHandle.cancel(true);
        this.scheduledWorker.shutdown();
        
        // Se è già connesso, disconnette
        if (isConnected()) {		
     	   disconnect();
        }
        
        // Logging
        logger.info("Deactivating IEC-104 Subscriber... Done");
    }

	@Override
	public Object polled(Wire wire) {
		return this.wireSupport.polled(wire);
	}

	@Override
	public void consumersConnected(Wire[] wires) {
		this.wireSupport.consumersConnected(wires);
	}
	
	/////////////////////////////////////////////////////
	// PRIVATE METHODS
	/////////////////////////////////////////////////////
		
	// IsConnected
	private boolean isConnected() {
		return (this.serverConnection!=null); 
    }
	
	// Connect
    private synchronized void connect() {
    	
    	// Dichiara variabili locali
    	InetAddress hostAddress;
    	ClientConnectionBuilder connectionBuilder;
    	
    	// Logging connessione in corso
        logger.info("Connecting... [tcp://"+this.actualConfiguration.Host+":"+this.actualConfiguration.Port+"]");

    	// Se è già connesso genera eccezione
        if (isConnected()) {
        	notifyAlert(Alert.ERROR_ALERT,"Connect failed. Reason: already connected");
        	return;
        }
                
        // Trasla l'IP Address        
        try {        	
            hostAddress = InetAddress.getByName(this.actualConfiguration.Host);   
            
        } catch (UnknownHostException e) {
        	notifyAlert(Alert.ERROR_ALERT,"Connect failed. Reason: unknown host ("+this.actualConfiguration.Host+")");
        	return;
        }          

        // Apre la connessione
        try {        	
        	connectionBuilder = new ClientConnectionBuilder(hostAddress).setPort(this.actualConfiguration.Port);
        	this.serverConnection = connectionBuilder.connect();     
        	
        } catch (IOException e) {     
        	notifyAlert(Alert.ERROR_ALERT,"Connect failed. Reason: "+e.getMessage());
        	return;
        }
        
        // Inizia il trasferimento dati
        try {
        	this.serverConnection.startDataTransfer(this,5000);
        	
        } catch (TimeoutException e) {    
        	
        	this.serverConnection.close();
        	this.serverConnection = null;
        	
        	notifyAlert(Alert.ERROR_ALERT,"Connect failed. Reason: starting data transfer timed out");
        	return;
            
        } catch (IOException e) {      
        	
        	this.serverConnection.close();
        	this.serverConnection = null;
        	
        	notifyAlert(Alert.ERROR_ALERT,"Connect failed. Reason: "+e.getMessage());
        	return;
        }
        
        // Genera evento
        notifyAlert(Alert.CONNECT_ALERT,"Connecting... Done");
		
        // Se richiesto inoltra una general interrogation
        if (this.actualConfiguration.GenInt) {
           genint();
        }
    }
	
	// Connect
    private synchronized void genint() {
    	
    	// Logging connessione terminata
        logger.info("General interrogation...");    

    	// Se non è connesso genera eccezione
        if (!isConnected()) {
        	notifyAlert(Alert.ERROR_ALERT,"General interrogation failed. Reason: not connected");
        	return;
        }
       
        // Esegue la general interrogation
        try {
        	this.serverConnection.interrogation(this.actualConfiguration.CommonAddr, 
        			                            CauseOfTransmission.ACTIVATION,
                                                new IeQualifierOfInterrogation(20));        	
        } catch (IOException e) {
        	notifyAlert(Alert.ERROR_ALERT,"General interrogation failed. Reason: "+e.getMessage());	
        	return;
        } 
        
       	// Logging connessione terminata
    	logger.info("General interrogation... Done");
    }
        	
	// Disconnect
    private synchronized void disconnect() {

    	// Logging disconnessione in corso
        logger.info("Disconnecting... [tcp://"+this.actualConfiguration.Host+":"+this.actualConfiguration.Port+"]");
    	
    	// Se è già disconnesso genera eccezione
        if (!isConnected()) {
           logger.warn("Disconnect failed. Reason: not connected");
        }

        // Esegue disconnessione
        try {
        	this.serverConnection.close();
        } catch (Exception e) {
        	logger.warn("Disconnect failed. Reason: " + e.getMessage());
        }
        
        // Genera evento
        notifyAlert(Alert.DISCONNECT_ALERT,"Disconnecting... Done");
        
        // Resetta l'handler di connessione e la configurazione
        this.serverConnection = null;
   }
	
	// Genera evento di notifica
    private synchronized void notifyAlert(Alert AlertId,String StrMessage) {
    	
    	// Variabili locali
    	List<WireRecord> ObjWireRecords;
    	Map<String, TypedValue<?>> ObjWireValues;
		
		// Manda il messaggio in log con il giusto livello di severity
		switch (AlertId) {		
		   case ERROR_ALERT: logger.error(StrMessage); break;
		   case CONNECT_ALERT: logger.info(StrMessage); break;
		   case DISCONNECT_ALERT: logger.warn(StrMessage); break;
		}

		// Se la notifica dell'alert non è disabilitata emette wire record
		if (((AlertId!=Alert.ERROR_ALERT)&&(this.actualConfiguration.InfoAlert))||
			((AlertId==Alert.ERROR_ALERT)&&(this.actualConfiguration.ErrorAlert))) {
			
	     	// Prepara le proprietà dell'evento
	    	ObjWireValues = new HashMap<>();			
	    	ObjWireValues.put("id", TypedValues.newStringValue(this.actualConfiguration.DeviceId));
	    	ObjWireValues.put("host", TypedValues.newStringValue(this.actualConfiguration.Host));
	    	ObjWireValues.put("port", TypedValues.newIntegerValue(this.actualConfiguration.Port));
	    	ObjWireValues.put("event", TypedValues.newStringValue(AlertId.description));
			
			// Se si tratta di alert di errore ed è stato fornito un messaggio lo aggiunge al record
			if ((StrMessage!="")&&(AlertId==Alert.ERROR_ALERT)) {
				ObjWireValues.put("message", TypedValues.newStringValue(StrMessage));
			}
			
			// Se c'è un enrichment per gli alert lo aggiunge
			if (this.actualConfiguration.AlertEnrichment.size()>0) {
				ObjWireValues.putAll(this.actualConfiguration.AlertEnrichment);
			}
			
			ObjWireRecords = new ArrayList<>();
	        ObjWireRecords.add(new WireRecord(ObjWireValues));   
			
	        // Notifica evento
			this.wireSupport.emit(ObjWireRecords);
		}
    }
    
	/////////////////////////////////////////////////////
	// TIMER CALLBACK
	/////////////////////////////////////////////////////

	@Override
	public void run() {
		
	   // Inizializza nome thread
	   Thread.currentThread().setName(getClass().getSimpleName());

	   // Se la configurazione è cambiata esegue
       if (!this.actualConfiguration.equals(this.desiredConfiguration)) {
 		  
    	  // Se è connesso e c'è un cambio dei parametri di base si disconnette
 	      if (isConnected()&&
 	    	  ((!this.actualConfiguration.Host.equals(this.desiredConfiguration.Host))||
 	    	   (!this.actualConfiguration.Port.equals(this.desiredConfiguration.Port))||
 	    	   (!this.actualConfiguration.CommonAddr.equals(this.desiredConfiguration.CommonAddr)))) {
 	         disconnect();
 	      }
 	      
		  // Se c'è stato un cambio del delay di riconnessione cambia lo scheduling
	      if (!this.actualConfiguration.Delay.equals(this.desiredConfiguration.Delay)) {
	    	   
	    	  logger.info("Changing reconnect delay...");
	    	  
	    	  this.scheduledHandle.cancel(false);
	   	      this.scheduledHandle = this.scheduledWorker.scheduleAtFixedRate(this,this.desiredConfiguration.Delay,this.desiredConfiguration.Delay,TimeUnit.SECONDS);
	   	      
	   	      logger.info("Changing reconnect delay... Done");
 	      }
	   }
       
       // Aggiorna la configurazione
       this.actualConfiguration = this.desiredConfiguration;
              
       // Se è non connesso e deve connettersi esegue
       if (!isConnected()&&(this.actualConfiguration.Enabled==true)) {		
          connect();
       } 
			
       // Se è connesso e deve disconnettersi esegue
       if (isConnected()&&(this.actualConfiguration.Enabled==false)) {		
          disconnect();
       } 
    }
	
	/////////////////////////////////////////////////////
	// IEC-104 CALLBACK
	/////////////////////////////////////////////////////

    @Override
    public void newASdu(ASdu ObjASDU) {
    	
    	// Variabili locali
    	int IntIOA;
    	boolean BolIOA;
    	List<WireRecord> ObjWireRecords;
    	InformationObject[] ObjInformationObjects;	
    	Map<String, TypedValue<?>> ObjWireValues;
    	Map<String, TypedValue<?>> ObjCommonValues;
    	
	    // Esegue logging
    	logger.info("Received ASDU [tcp://"+this.actualConfiguration.Host+":"+this.actualConfiguration.Port+"]");
    	 	
    	// Acquisisce puntatore agli information object
    	ObjInformationObjects = ObjASDU.getInformationObjects();
    	
    	// Se l'ASDU non contiene oggetti genera warning ed esce
    	if ((ObjInformationObjects==null)||(ObjInformationObjects.length==0)) {    		
    	   notifyAlert(Alert.ERROR_ALERT,"Received ASDU cannot be decoded. Reason: empty");
           return;
    	}
		
    	// Prepara le proprietà di testata dell'ASDU condivise da tutti gli information objects
    	ObjCommonValues = new HashMap<>();	
    	
    	ObjCommonValues.put("id", TypedValues.newStringValue(this.actualConfiguration.DeviceId));
    	ObjCommonValues.put("host", TypedValues.newStringValue(this.actualConfiguration.Host));
    	ObjCommonValues.put("port", TypedValues.newIntegerValue(this.actualConfiguration.Port));
    	ObjCommonValues.put("event", TypedValues.newStringValue("DATA"));
		
    	ObjCommonValues.put("type", TypedValues.newStringValue(ObjASDU.getTypeIdentification().name()));
    	ObjCommonValues.put("test", TypedValues.newStringValue(ObjASDU.isTestFrame()?("y"):("n")));
    	ObjCommonValues.put("cot", TypedValues.newStringValue(ObjASDU.getCauseOfTransmission().name()));
    	ObjCommonValues.put("oa", TypedValues.newIntegerValue(ObjASDU.getOriginatorAddress()));
    	ObjCommonValues.put("ca", TypedValues.newIntegerValue(ObjASDU.getCommonAddress()));
		
		// Crea un wirerecord per ciascun oggetto
		ObjWireRecords = new ArrayList<>();
    	
		// Ciclo di scansione di tutti gli information object
		for (InformationObject ObjInformationObject : ObjInformationObjects) {

			// Aggiorna lo IOA condiviso da tutti gli information element
			IntIOA = ObjInformationObject.getInformationObjectAddress();
			
			// Verifica se lo IOA è matchato da una regola di enrichment
            BolIOA = this.actualConfiguration.MatchingEnrichment.containsKey(IntIOA);
            
            // Se l'IOA non matcha ed è attivo il filtro sull'enrichment esegue, altrimenti procede            
            if (!BolIOA&&this.actualConfiguration.EnrichmentFilter) {
        	    
            	// Esegue logging
            	logger.debug("Discarded ASDU [tcp://"+this.actualConfiguration.Host+":"+this.actualConfiguration.Port+"/IOA="+String.valueOf(IntIOA)+"]");
            
            } else {
	            	
				// Inizializza un nuovo wirerecord con le proprietà comuni
				ObjWireValues = new HashMap<>();
				ObjWireValues.putAll(ObjCommonValues);
				
				ObjWireValues.put("ioa", TypedValues.newIntegerValue(IntIOA));
				
				// Se l'IOA matcha con regola di enrichment aggiunge metriche, altrimenti aggiunge quelle di default
				if (BolIOA) {
					ObjWireValues.putAll(this.actualConfiguration.MatchingEnrichment.get(IntIOA));	
				} else if (this.actualConfiguration.DefaultEnrichment.size()>0) {
					ObjWireValues.putAll(this.actualConfiguration.DefaultEnrichment);
				}
	
				// Ciclo di scansione di tutti gli information element
				for (InformationElement[] ObjInformationElementSet : ObjInformationObject.getInformationElements()) {
	                
					// Decodifica l'information element in base al tipo di ASDU e aggiunge record alla lista
					try {
					   Iec104Decoder.decode(ObjASDU.getTypeIdentification(), ObjInformationElementSet, ObjWireValues);
					   ObjWireRecords.add(new WireRecord(ObjWireValues)); 
					} 
					catch (Exception e) {					
						notifyAlert(Alert.ERROR_ALERT,"Received ASDU cannot be decoded. Reason: "+e.getMessage());
					}				
	            }	
            }
		}
		
		// Se ci sono record da trasmettere procede
		if (ObjWireRecords.size()>0) {
		   this.wireSupport.emit(ObjWireRecords);
		}
    }

    @Override
    public void connectionClosed(IOException e) {    	
		
        // Genera evento
        notifyAlert(Alert.ERROR_ALERT,"Received Connection Closed Signal. Reason: "+(e.getMessage().isEmpty()?("unknown"):(e.getMessage())));
    	
    	// Se è già connesso (molto probabile!), disconnette
        if (isConnected()) {		
     	   disconnect();
        }
    }
}

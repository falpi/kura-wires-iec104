package org.falpi.kura.wires.component.iec104;

import java.util.Map;

public class Iec104Configuration {

	// Proprietà configurabili del componente
	public static final String IEC104_ENABLED_PROP_NAME = "iec104.enabled";
	public static final String IEC104_HOST_PROP_NAME = "iec104.host";
	public static final String IEC104_PORT_PROP_NAME = "iec104.port";
	public static final String IEC104_COMMON_ADDR_PROP_NAME = "iec104.common-addr";
	public static final String IEC104_DELAY_PROP_NAME = "iec104.delay";
	public static final String IEC104_GENINT_PROP_NAME = "iec104.genint";
	public static final String IEC104_INFO_ALERT_PROP_NAME = "iec104.info-alert";
	public static final String IEC104_ERROR_ALERT_PROP_NAME = "iec104.error-alert";
	
	// Variabili di stato e configurazione
	public Boolean Enabled;
	
	// Variabili di configurazione
	public final String Host;
	public final Integer Port;
	public final Integer CommonAddr;
	public final Integer Delay;
	public final Boolean GenInt;
	public final Boolean InfoAlert;
	public final Boolean ErrorAlert;
            
	// Costruttore
    public Iec104Configuration(final Map<String, Object> properties) { 
        
        this.Enabled = (Boolean)properties.get(IEC104_ENABLED_PROP_NAME);
        this.Host = (String)properties.get(IEC104_HOST_PROP_NAME);
        this.Port = (Integer)properties.get(IEC104_PORT_PROP_NAME);
        this.CommonAddr = (Integer)properties.get(IEC104_COMMON_ADDR_PROP_NAME);
        this.Delay = (Integer)properties.get(IEC104_DELAY_PROP_NAME);
        this.GenInt = (Boolean)properties.get(IEC104_GENINT_PROP_NAME);
        this.InfoAlert = (Boolean)properties.get(IEC104_INFO_ALERT_PROP_NAME);
        this.ErrorAlert = (Boolean)properties.get(IEC104_ERROR_ALERT_PROP_NAME);
    }    
}

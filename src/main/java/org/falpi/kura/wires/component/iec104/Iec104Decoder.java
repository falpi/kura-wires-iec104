package org.falpi.kura.wires.component.iec104;

import org.openmuc.j60870.TypeId;

import java.util.Map;

import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;

import org.openmuc.j60870.IeCauseOfInitialization;
import org.openmuc.j60870.IeQualifierOfInterrogation;
import org.openmuc.j60870.IeSinglePointWithQuality;
import org.openmuc.j60870.IeDoublePointWithQuality;
import org.openmuc.j60870.IeNormalizedValue;
import org.openmuc.j60870.IeQuality;
import org.openmuc.j60870.IeTime24;
import org.openmuc.j60870.IeTime56;
import org.openmuc.j60870.InformationElement;

public class Iec104Decoder {
	
	// Decodifica elemento
	public static void decode(TypeId TypeId, InformationElement[] ObjElementSet, Map<String, TypedValue<?>> ObjValues) throws Exception {
    	
		// Dichiara variabili
		Object ObjValue = null;		
		int IntQuality = 0;	
		String StrTimestamp = "";
		
		IeTime24 ObjTime24;
		IeTime56 ObjTime56;
		IeQuality ObjQuality;		
		IeNormalizedValue ObjNV;
		IeSinglePointWithQuality ObjSPQ;
		IeDoublePointWithQuality ObjDPQ;
		
		// Decodifica ASDU in base al typeid
        switch (TypeId) {
        
	        // 1
	        case M_SP_NA_1:	        	
	        	ObjSPQ = (IeSinglePointWithQuality)ObjElementSet[0];
	        	
	        	ObjValue = (Integer)decodeSinglePoint(ObjSPQ);
	        	IntQuality = decodeQualityFromSinglePoint(ObjSPQ);     
	        	break;
	        	
	        // 2
	        case M_SP_TA_1:
	        	ObjSPQ = (IeSinglePointWithQuality)ObjElementSet[0];
	        	ObjTime24 = (IeTime24)ObjElementSet[1];
	        	
	        	ObjValue = (Integer)decodeSinglePoint(ObjSPQ);
	        	IntQuality = decodeQualityFromSinglePoint(ObjSPQ);     
	        	StrTimestamp = decodeTime24(ObjTime24);
	        	break;
	        	
	        // 3
	        case M_DP_NA_1:
	        	ObjDPQ = (IeDoublePointWithQuality)ObjElementSet[0];
	        	
	        	ObjValue = (Integer)decodeDoublePoint(ObjDPQ);
	        	IntQuality = decodeQualityFromDoublePoint(ObjDPQ);     
	        	break;
	        	
	        // 4
	        case M_DP_TA_1:
	        	ObjDPQ = (IeDoublePointWithQuality)ObjElementSet[0];
	        	ObjTime24 = (IeTime24)ObjElementSet[1];
	        	
	        	ObjValue = (Integer)decodeDoublePoint(ObjDPQ);
	        	IntQuality = decodeQualityFromDoublePoint(ObjDPQ);     
	        	StrTimestamp = decodeTime24(ObjTime24);
	        	break;
	        
	        // --------------------------------------------------------------------------------
	        	
	        // 9
	        case M_ME_NA_1:	        	
	        	ObjNV = ((IeNormalizedValue)ObjElementSet[0]);
	        	ObjQuality = ((IeQuality)ObjElementSet[1]);
	        	
	        	ObjValue = (Double)ObjNV.getNormalizedValue();
	        	IntQuality = decodeQuality(ObjQuality);     	
	            break;
	            
	         // 34
	        case M_ME_TD_1:	        	
	        	ObjNV = ((IeNormalizedValue)ObjElementSet[0]);	        	
	        	ObjQuality = ((IeQuality)ObjElementSet[1]);
	        	ObjTime56 = ((IeTime56)ObjElementSet[2]);	        	
	        	
	        	ObjValue = (Double)ObjNV.getNormalizedValue();
	        	IntQuality = decodeQuality(ObjQuality);   
	        	StrTimestamp = decodeTime56(ObjTime56);
	            break;
	            
	         // 70
	        case M_EI_NA_1:
	        	ObjValue = (Integer)((IeCauseOfInitialization)ObjElementSet[0]).getValue();
	        	break;    
	            
	        // 100
	        case C_IC_NA_1:
	        	ObjValue = (Integer)((IeQualifierOfInterrogation)ObjElementSet[0]).getValue();
	        	break;	    
	            
	        default:
	            throw new Exception("unsupported type ("+TypeId+")");
	     }
        
        // Aggiorna i valori in uscita        
       	ObjValues.replace("val", TypedValues.newTypedValue(ObjValue));
        ObjValues.replace("qual", TypedValues.newIntegerValue(IntQuality));
    	ObjValues.replace("time", TypedValues.newStringValue(StrTimestamp));
    }	
		
	// Decodifica valore singlepoint
	public static int decodeSinglePoint(IeSinglePointWithQuality ObjSPQ) {
		return (ObjSPQ.isOn()?(1):(0)); 
	}
	
	// Decodifica valore doublepoint
	public static int decodeDoublePoint(IeDoublePointWithQuality ObjDPQ) {
		return ObjDPQ.getDoublePointInformation().ordinal(); 
	}

	// Decodifica classe time24
	public static String decodeTime24(IeTime24 ObjTime24) {
		return String.valueOf(ObjTime24.getTimeInMs());
	}
	
	// Decodifica classe time56
	public static String decodeTime56(IeTime56 ObjTime56) {
		return "20"+
	           String.format("%02d",ObjTime56.getYear())+"-"+
			   String.format("%02d",ObjTime56.getMonth())+"-"+
			   String.format("%02d",ObjTime56.getDayOfMonth())+" "+
			   String.format("%02d",ObjTime56.getHour())+":"+
			   String.format("%02d",ObjTime56.getMinute())+":"+
			   String.format("%02d",ObjTime56.getSecond())+"."+
               String.format("%02d",ObjTime56.getMillisecond());
	}
	
	// Decodifica classe quality
	public static int decodeQuality(IeQuality ObjQuality) {
		return (ObjQuality.isOverflow()?(0x01):(0))|(ObjQuality.isBlocked()?(0x10):(0))|(ObjQuality.isSubstituted()?(0x20):(0))|(ObjQuality.isNotTopical()?(0x40):(0))|(ObjQuality.isInvalid()?(0x80):(0)); 
	}
	
	// Decodifica classe quality da un singlepointwithquality
	public static int decodeQualityFromSinglePoint(IeSinglePointWithQuality ObjSPQ) {
		return (ObjSPQ.isBlocked()?(0x10):(0))|(ObjSPQ.isSubstituted()?(0x20):(0))|(ObjSPQ.isNotTopical()?(0x40):(0))|(ObjSPQ.isInvalid()?(0x80):(0)); 
	}
	
	// Decodifica classe quality da un doublepointwithquality
	public static int decodeQualityFromDoublePoint(IeDoublePointWithQuality ObjDPQ) {
		return (ObjDPQ.isBlocked()?(0x10):(0))|(ObjDPQ.isSubstituted()?(0x20):(0))|(ObjDPQ.isNotTopical()?(0x40):(0))|(ObjDPQ.isInvalid()?(0x80):(0)); 
	}
}

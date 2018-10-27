
// ================================================================================
//  $Revision: 245 $ 
//  $Author: a130506 $ 
//  $Date: 2018-08-09 10:46:38 +0200 (Thu, 09 Aug 2018) $
// ================================================================================

package org.falpi.kura;

import java.util.ArrayList;

import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlBeans;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.SchemaTypeLoader;

public class XmlUtils {

	// #############################################################################################
    // Esegue la validazione di un payload rispetto ad un wsdl
    // ##############################################################################################

	public static ArrayList<XmlError> validate(XmlObject ObjPayload, XmlObject[] ArrSchemaDocuments) throws XmlException {    
	 
		// Prepara variabili locali
		
		ArrayList<XmlError> ObjErrors = 
			new ArrayList<XmlError>();

		// Prepara il loader per la validazione del payload

		SchemaTypeLoader ObjLoader = 
			XmlBeans.compileXsd(ArrSchemaDocuments, null, 
				   new XmlOptions().setErrorListener(null).setCompileDownloadUrls().setCompileNoPvrRule());

		// Prepara il loader per il caricamento del payload

		XmlObject ObjDocument = 
			ObjLoader.parse(ObjPayload.xmlText(new XmlOptions().setSaveOuter()), null, 
				   new XmlOptions().setLoadLineNumbers(XmlOptions.LOAD_LINE_NUMBERS_END_ELEMENT));
			    
		// Esegue la validazione del documento sulla base dei schema forniti
		
	    ObjDocument.validate(new XmlOptions().setErrorListener(ObjErrors)); 
	    
	    // Restituisce return-code
	    
	    return ObjErrors;
	 }
	
    // ##############################################################################################
}
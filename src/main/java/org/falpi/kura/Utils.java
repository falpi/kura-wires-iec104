package org.falpi.kura;

import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.osgi.framework.Bundle;

public class Utils {

	// Acquisisce una risorsa di tipo testo dal bundle
	public static String getTextResource(Bundle ObjBundle,String StrResourcePath) throws Exception {
		
		// Aquisice indirizzo risorsa
		URL ObjURL = ObjBundle.getResource(StrResourcePath);

		// Se il file non esiste genera eccezione
		if (ObjURL==null) {
			throw new Exception("File not found ("+StrResourcePath+")");
		}
		
		// Legge la risorsa bufferizzata
		BufferedReader ObjBuffer = new BufferedReader(new InputStreamReader(ObjURL.openConnection().getInputStream()));
		
		String StrResource = "";
		
		while (ObjBuffer.ready()){
			StrResource += ObjBuffer.readLine();
		}
		
		ObjBuffer.close();	
		
		// Restituisce return-code
		return StrResource;
	}

}

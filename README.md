# Eclipse Kura™ Wire component for IEC 60870-5-104
An Eclipse Kura Wires component to subscribe with an IEC 60870-5-104 Server.  
It uses J60870 library for IEC protocol stack implementation.  
Tested with Eclipse Kura 3.2.0.

## Supported ASDU types
For the moment the following ASDU types are decoded:  

ASDU TypeID     | Description
--------------- | ----------------------------------------------------------  
**M_SP_NA_1** (1) | Single-point information without time tag  
**M_SP_TA_1** (2) | Single-point information with time tag  
**M_DP_NA_1** (3) | Double-point information without time tag  
**M_DP_TA_1** (4) | Double-point information with time tag  
**M_ME_NA_1** (9) | Measured value, normalized value  
**M_ME_TD_1** (34) | Measured value, normalized value with time tag CP56Time2a  
**M_EI_NA_1** (70) | End of initialization  
**C_IC_NA_1** (100) | Interrogation command  
 
Send me an e-mail to add other types or join to development.  

## Build sources
The only requisite to build from sources is an already  
installed Eclipse Kura User Workspace.  
 
## Binary package
The binary package ready for the installation can be
found in folder "**resources/dp**".

# Eclipse Kura Wire component for IEC 60870-5-104
An Eclipse Kura Wires component to subscribe with an IEC 60870-5-104 Server.  
It uses J60870 library for IEC protocol stack implementation.  
Tested with Eclipse Kura 4.0.0.

## Supported ASDU types
For the moment the following ASDU types are decoded:  

ASDU TypeID         | Description
------------------- | ----------------------------------------------------------  
**M_SP_NA_1** (1)   | Single-point information without time tag  
**M_SP_TA_1** (2)   | Single-point information with time tag  
**M_DP_NA_1** (3)   | Double-point information without time tag  
**M_DP_TA_1** (4)   | Double-point information with time tag  
**M_ME_NA_1** (9)   | Measured value, normalized value  
**M_ME_TD_1** (34)  | Measured value, normalized value with time tag CP56Time2a  
**M_EI_NA_1** (70)  | End of initialization  
**C_IC_NA_1** (100) | Interrogation command  

## Metrics mappings
Actual metrics mappings for wire record messages generated.  

The following metrics are common to all records (except "message").

Metric      | Description
----------- | ----------------------------------------------------------  
**id**      | Device Id (from static configuration)
**host**    | IEC-104 Server Host (from static configuration)
**port**    | IEC-104 Server Port (from static configuration)
**event**   | Event Type (DATA,CONNECT,DISCONNECT,WARNING,ERROR) 
**eventId** | Event Id (0=DATA,1=CONNECT,2=DISCONNECT,3=WARNING,4=ERROR) 
**message** | Event Message (only for alert events, where event<>DATA)

The following additional metric are specific for data messages.

Metric      | Description
----------- | ----------------------------------------------------------  
**type**    | ASDU Type 
**test**    | Test Frame (y/n)
**cot**     | Cause Of Transmission
**oa**      | Originator Address
**ca**      | Common Address
**ioa**     | Information Object Address
**val**     | Value (depend on ASDU type)
**qual**    | Quality (depend on ASDU type)
**time**    | Time (depend on ASDU Type)

## Build sources
The only requisite to build from sources is an already  
installed Eclipse Kura User Workspace.  
  
## Binary package
The binary package ready for the installation can be
found in folder "**resources/dp**".

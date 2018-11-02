# Changelog
All notable changes to this project will be documented in this file.

## [1.3.0] - 2018-11-2
### Added
- Additional metric "eventId" with numeric identifier of event type is  
  added to every message. The decoding is described on README.  
  
- Generate ERROR alert if enrichment configuration is invalid.  
  This not prevent subscribing and publishing of data but could lead to  
  invalid messages and/or data losses if filtering is enabled or some  
  some logic is implemented on additional enriched metrics.  
  Validation error are registered in logs.  

## [1.2.0] - 2018-10-31
### Added
- Enrichment filter parameter.  
  Now it's possible to enable filtering based on "matching" items in enrichment rules.  
  In this case all data messages with not matching IOA are discarded,
  event if "default" rule item is present.

### Changed
- More flexibility in enrichment configuration. 
  Now it's possible to configure enrichment for alert messages and a global default rule  
  for enrichment of data messages that doesn't match any specified IOA. See the default XML  
  for enrichment configuration of component for syntax and example configuration.  

- Reorganize some metrics in messages.   
  Metric "event" is shared by all messages and define event type (DATA,CONNECT,DISCONNECT,ERROR).  
  Metric "message" is used only for alert events (<> DATA) and contains optional message.  
  Metric "type" is used only for data events (=DATA) and contains ASDU Type.  

## [1.1.0] - 2018-10-27
### Added
- Metric enrichment of ASDU messages via simple XML configuration.

## [1.0.0] - 2018-10-19
### Added
- First release
- Limited number of supported ASDU types.
- Support for informational and errors alerting.

# Changelog
All notable changes to this project will be documented in this file.

## [1.2.0] - under development
### Changed
- Enhanced enrichment.  
  Now it's possible to configure enrichment for alert messages and a global default rule  
  for enrichment of data messages that doesn't match any specified IOA. See the default XML  
  for enrichment configuration of component for syntax and example configuration.  

- Reorganize some metrics.   
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

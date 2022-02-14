# Workflow Engine

## About
Consists of the following elements:
*  Camel bridge: For routing packet to different stages based on [registration flows](../../README.md#registration-flows).
*  Workflow manager service:  Enables certain workflow functions via exposed APIs:
    *  Pause: Pause processing of packets based on rules
    *  Resume: Resume proessing of packets  
    *  Request for additional information from user (like a new document) and continue registration as CORRECTION packet.
* Reprocessor

The work flow can be controlled with Admin portal

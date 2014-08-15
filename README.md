freeloader
==========

A Clickatell (internal - staff only) Hackathon entry for 2013

This application attempts to simulate the service Clickatell provides in it's HTTP API for sending SMSes and receiving delivery receipts.  It also supports simulating sending MO (Mobile originated) callbacks as a function of MT (mobile terminated) messages sent to it.

The general idea was that it could be used to load test the SMS sending and receiving capabilities of your application locally without having to connect to Clickatell.  It was also intended to eventually support all of Clickatell's services (SMS, Airtime and USSD) and all APIs.

Tested on Java 1.6 and all libs included (apologies).


Usage:
 To run: 
	FreeLoader.java
	
 To simulate receiving MO callbacks run:
	TestMOReceiver.java
	
 To simulate receiving MT delivery receipts run:
	TestMTCallbackReceiver.java
	
 To simulate sending 1000s of MT messages run:
	TestMTGenerator.java

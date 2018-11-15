# VidAS  

The VidAS system provides a secure, scalable video archiving and processing system. The VidAS system has been designed according to the Security-by-Design, Privacy-by-Design and Security-by-Default and Privacy-by-Default principles.  
Security/Privacy-by-Design means that the system has been designed taking into account the security and confidential data management needs, so the security and privacy (i.e., proper management of confidential data) is an essential element of the system.   Security/Privacy-by-Default means that the security design and default configuration of the system already assure a minimum level (which means an acceptable level which can be increased only) of security and privacy (i.e., confidential data management) that:  
* cannot be lowered;  
* is configured by default.  

Indeed, one of the most relevant features of the VidAS system is the end-to-end protection of confidential data (e.g., aircraft videos) using new cryptographic techniques like CP-ABE (Ciphertext Policy Attribute Based Encryption).  
From the security point of view, in addition to the CP-ABE  encryption techniques, the VidAS system makes use of:  
* W3C Web Crypto API  
* Elliptic Curve Cryptography (ECC)  
* Elliptic Curve Diffie–Hellman (ECDH)  
* JWT .  

To increase system’s security every operation requires a specific Authorization Token that is dynamically generated based on the user’s profile and requested operation.

## Important note
We fixed some issues in the way the encryption is managed between the ABE-Proxy and the data source or data consumer components (specifically in the CPABE and Device-Entity libraries). To this end we have produced a new version of these components that are available on [this](https://github.com/FINCONS-IBD/MQTT-SeDEM) GitHub Repository.

N.B.: using the new versions requires an update of the client or server side modules.

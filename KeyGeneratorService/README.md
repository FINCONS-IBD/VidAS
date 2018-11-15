[![DepShield Badge](https://depshield.sonatype.org/badges/FINCONS-IBD/VidAS/depshield.svg)](https://depshield.github.io)

# KeyGeneratorService  

The KeyGeneratorService is a critical component required to support the encryption of information (e.g., video files) using the CP-ABE encryption technique.  
In CP-ABE, a user provided access policy governs the encryption process. The policy, therefore, can be considered as actually encoded in, and an integral part of, the encrypted information. Only decryption keys having the characteristics requested by the access policy will succeed in decrypting the protected information.  
The KeyGeneratorService at start-up generates two keys: the "public key" used to encrypt files, in conjunction with a given policy, and a "master key" used, in combination with the user’s profile retrieved from the LDAP service, to generate the user’s private key necessary to decrypt a video file.  
KeyGeneratorService is configurable by editing the configuration file "config.properties" stored inside "resources/config" folder.

## Usage 

KeyGeneratorService project is a Maven Project. The final product of a Maven Build operation is a .war file that has to be deployed on a Web Server like Apache Tomcat.  

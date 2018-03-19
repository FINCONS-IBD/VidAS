# Token_Service  

Token_Service is devoted to manage VidAS authorization providing specific access tickets.  
Being the system distributed, there is the need to ensure that a specific request submitted by the user (or potentially by other components) is among the ones the user is authorized to perform (e.g., upload a video). Instead of having ACLs or other access control mechanisms on each service components  the VidAS system envisages a specific service that, based on the user’s profile stored in the LDAP Service, grants an access ticket available for a specific service.  
Token_Service is configurable by editing the configuration file "config.properties" stored inside "resources/config" folder. 

## Usage 

Token_Service project is a Maven Project. The final product of a Maven Build operation is a .war file that has to be deployed on a Web Server like Apache Tomcat. 
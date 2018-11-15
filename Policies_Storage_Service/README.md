[![DepShield Badge](https://depshield.sonatype.org/badges/FINCONS-IBD/VidAS/depshield.svg)](https://depshield.github.io)

# Policies_Storage_Service  

Policies_Storage_Service is a web service which cares of managing Policies and Directories stored in a database. It embraces the functionalities exposed by Storage_Service_DBManagement.  

The connection between the Policies_Storage_Service and the DB is available only after setting the configuration parameters inside "config.properties" file. It is stored inside "resources/config" folder.  

## Usage 

Policies_Storage_Service project is a Maven Project. The final product of a Maven Build operation is a .war file that has to be deployed on a Web Server like Apache Tomcat.  

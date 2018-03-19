# Video_Storage_Service  

Video_Storage_Service is a web service which cares of managing Video and Video Directories stored in a database. It embraces the functionalities exposed by Storage_Service_DBManagement.  

The connection between the Video_Storage_Service and the DB is available only after setting the configuration parameters inside "config.properties" file. It is stored inside "resources/config" folder.  

## Usage 

Video_Storage_Service project is a Maven Project. The final product of a Maven Build operation is a .war file that has to be deployed on a Web Server like Apache Tomcat.  
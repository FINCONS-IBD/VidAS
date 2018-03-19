/**
 * 
 */

/**
 * @param url
 * @param object to send in Post request
 */
function ajaxCallPost(url_prefix, service, object) {
	var url=url_prefix+"/"+service;
//    console.log('start', object);
	   console.log('start',url);
	var xmlhttp = new XMLHttpRequest();
	xmlhttp.open("POST", url, true);
	xmlhttp.setRequestHeader("Content-type", "application/json");
	xmlhttp.setRequestHeader("Accept", "application/json");
	sendJSONObject(xmlhttp, url, object);
//	switch (service) {
//		case "generate_shared_secret":
//			sendGenerateSharedSecret(xmlhttp, url, object);
//			break;
//		case "cpabe_information":
//			sendJSONObject(xmlhttp, url, object);
//			break;
//		case "get_shared_secret":
//			sendJSONObject(xmlhttp, url, object);
//			break;
//		default:
//		  	console.error("invalid Service");
//			return;
//		}
};

function sendJSONObject(xmlhttp, url, encryptCpabeInfo){
//	console.log(encryptCpabeInfo);
//    console.log(JSON.stringify(encryptCpabeInfo));
    xmlhttp.send(JSON.stringify(encryptCpabeInfo));
    xmlhttp.onreadystatechange = function() {
//    	console.log(xmlhttp.readyState);
//    	console.log(xmlhttp);
        if (xmlhttp.readyState == XMLHttpRequest.DONE ) {
           var response=null;
           if (xmlhttp.status == 200) {
//	            console.log("Success", xmlhttp.responseText);
        	   console.log("success in request: "+url);
	            response=JSON.parse(xmlhttp.responseText);       
	            
//		        postMessage(JSON.stringify(response));
           }
           else if (xmlhttp.status == 400) {
              console.log('There was an error 400', xmlhttp);
              response={status:"Error", codeStatus:xmlhttp.status,  message:""};
         
           }
           else {
        	   console.log('something else other ', xmlhttp.status);
        	   response={status:"Error", codeStatus:xmlhttp.status ,message:""};
//               postMessage(response);
           }
           postMessage(JSON.stringify(response));
        }
        
    }
};

//function sendGenerateSharedSecret(xmlhttp, url, object){
//	console.log(object);
////    delete object.epk.key_ops;
////    delete object.epk.ext;
//    console.log(JSON.stringify(object));
//    xmlhttp.send(JSON.stringify(object));
//    xmlhttp.onreadystatechange = function() {
//    	console.log(xmlhttp.readyState);
//    	console.log(xmlhttp);
//        if (xmlhttp.readyState == XMLHttpRequest.DONE ) {
//        	var response=null;
//        	if (xmlhttp.status == 200) {
//	            console.log("Success", xmlhttp.responseText);
////	            self.importScripts('../jsrsasign/base64x-1.1.js');
////	            self.importScripts('../jsrsasign/ext/base64.js');
//	            response=JSON.parse(xmlhttp.responseText);
//
////	            json_response.epk.x=b64tob64u(json_response.epk.x);
////	            json_response.epk.y=b64tob64u(json_response.epk.y);
//	            
////		        postMessage(JSON.stringify(json_response));
//           }
//           else if (xmlhttp.status == 400) {
//              console.log('There was an error 400');
//              response={status:"Error", codeStatus:xmlhttp.status, message:""}
////              postMessage(response);
//           }
//           else {
//        	   console.log('something else other ', xmlhttp.status);
//        	   response={status:"Error", codeStatus:xmlhttp.status, message:""};
////                  postMessage(response);
//           }
//           postMessage(JSON.stringify(response));
//        }
//    }
//};

self.onmessage = function (msg) {
	if(msg.data.length>2){
//		console.log(msg.data[0]);
//		console.log(msg.data[1]);
//		console.log(msg.data[2]);
		var url_prefix=msg.data[0];
		var service=msg.data[1];
		console.log("url "+ url_prefix+"/"+service);
//		console.log("jwk "+ msg.data[2]);
		ajaxCallPost(url_prefix, service,  msg.data[2]);
	}else{
		console.error("error in the request");
	}
};


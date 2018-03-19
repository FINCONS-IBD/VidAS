/**
 * Send the token for the service to the Token Service that 
 * verify if the user is authorized and send a response with another token
 * @param nameService
 * @param sJWT
 * @param event
 * @returns
 */
function requestToken(nameService, sJWT, event){	
	var token=$.ajax({
    url: sessionStorage.getItem('proxyGenerateTokenServicePath'),//'${proxyGenerateTokenServicePath}', //YOU_URL_TO_WHICH_DATA_SEND
    type: "GET",
    async: false,
    beforeSend: function(xhr){xhr.setRequestHeader('Authorization', sJWT);},
    success: function(data) {
			var json = data;
			if(json['jwt']!=null){
				if(nameService!=null){
					sessionStorage.setItem(nameService+"Token", json['jwt']);	
				}
			}else{
				console.error("Jwt isn't Generated user unauthorized ");
			}				
    },
    error: function(xhr, status, error) {
//     	alert("codice errore:"+ xhr.status);
    	if(xhr.status==401 || xhr.status==403 || xhr.status==404 ){
    		logoutUser();  
    	}
			      
		}
	}).responseText;
	try {
		var response= JSON.parse(token);
		return response['jwt'];
    } catch (e) {
		if(event)
			event.stopPropagation(); 
        return;
    }
	
	
}

/**
 * Generate an external token to execute the service requested 
 * @param sJWT
 * @param deferedRequest
 * @param input
 * @returns
 */
function executeService(sJWT, deferedRequest, input){
	var result=null;
		var payloadObj = KJUR.jws.JWS.readSafeJSONString(b64utoutf8(sJWT.split(".")[1]));
		var deferredDecrypt= $.Deferred();
		decrypt(sessionStorage.getItem("userSecret"), payloadObj['secureBrow'], deferredDecrypt);
		$.when(deferredDecrypt).done(function (secureCode) {
			if(secureCode!=null && secureCode!=""){
				var oNewPayload = {};
				var tNewTokenEnd = KJUR.jws.IntDate.get('now') + 3600;
				oNewPayload.sub = sessionStorage.getItem('user');//'${username}';
				oNewPayload.exp = tNewTokenEnd;
				oNewPayload.token=sJWT;
				var sNewPayload = JSON.stringify(oNewPayload);
				var newsJWT=KJUR.jws.JWS.sign("HS256", sHeader, sNewPayload, secureCode);
				var actionAutorizhed=payloadObj['action'];
				var proxyFilterPath = sessionStorage.getItem('proxyFilterPath');
				if(JSON.stringify(actionAutorizhed) == JSON.stringify({})){
					console.log("unAuthorized");
					$("#home").click();
				}else{
				var result= $.ajax({
			         url: proxyFilterPath,//'${proxyFilterServicePath}',		// url, //YOU_URL_TO_WHICH_DATA_SEND
			         type: "POST",			// method,
			         dataType: 'json',
			         data: input,
			         beforeSend: function(xhr){
			        	 xhr.setRequestHeader("Content-Type","application/json");
			        	 xhr.setRequestHeader('Authorization', newsJWT);
			        	},
			         success: function(data) {
			        	 	deferedRequest.resolve(data);
							},
			        error: function(xhr, error, message) {
			        	if(xhr.status==401){
							logoutUser();
			        	}else{
			        		deferedRequest.reject();
				        }
					}
			     });
				
				}
			}
		});
	}

/*
 * Method that create a token for request a service
 */
var oHeader = {alg: 'HS256', typ: 'JWT'};
var sHeader = JSON.stringify(oHeader);
var deferredOperationUnauthorized = $.Deferred();
/**
 * Generate a token for a single service defined by @param service 
 * @param method
 * @param service
 * @param url
 * @param username
 * @param userSecret
 * @returns JWT Token
 */

function generateRequestToken(method, service, url, username, userSecret){
		var validityTime = sessionStorage.getItem("validityTime");
		var userSecret = sessionStorage.getItem("userSecret");
		var challengeID = sessionStorage.getItem("challengeID");
		
//		url=url.split(" ").join("_");

		var oPayload = {};
		var tNow = KJUR.jws.IntDate.get('now');
		var tEnd = tNow + 3600;
		oPayload.sub = username;
		oPayload.exp = tEnd;
		oPayload.action={ 
						 "method": method,
						 "service":service,
						 "url":url
						};
	
		var sPayload = JSON.stringify(oPayload);
		var sJWT = KJUR.jws.JWS.sign("HS256", sHeader, sPayload, userSecret);
		return sJWT
}

/**
 * Converts a buffer array to a hex string
 * @param a
 * @returns
 */
function BAtohex(a) {
    var s = "";
    for (var i = 0; i < a.length; i++) {
	var hex1 = a[i].toString(16);
	if (hex1.length == 1) hex1 = "0" + hex1;
	s = s + hex1;
    }
    return s;
}


function parseHexString(str) { 
    var result = [];
    while (str.length >= 2) { 
        result.push(parseInt(str.substring(0, 2), 16));
        str = str.substring(2, str.length);
    }

    return result;
}

/**
 * Verify the validity of the token
 * @param token
 * @returns
 */
function validateTimeToken(token){
	var payloadObj = KJUR.jws.JWS.readSafeJSONString(b64utoutf8(token.split(".")[1]));
	var deadline=payloadObj['exp'];
	var tNow = KJUR.jws.IntDate.get('now');
	if(deadline<tNow+60){
		return false;
	}else{
		return true;
	}
}

/**
 * Decrypt the random number used to execute a single service
 * @param secret
 * @param encrypted
 * @param myDeffered
 * @returns
 */
function decrypt(secret, encrypted, myDeffered){

	var hex=b64utohex(secret);
	var hexData=b64utohex(encrypted);
	
	var deferredDigest = $.Deferred();
	var iv = getDigest(hex, deferredDigest);
	$.when(deferredDigest).done(function(receivedDigest){
		iv = receivedDigest.substring(0, 32);
		var BAiv = new Uint8Array(parseHexString(iv)).buffer;
		
		var BAsecret = new Uint8Array(parseHexString(receivedDigest)).buffer;
		var BAencrypted =new Uint8Array(parseHexString(hexData)).buffer;
		window.crypto.subtle.importKey("raw",BAsecret,{
				name: "AES-CBC",
		  		length: 256
	   		},true, ["encrypt", "decrypt"])
			.then(function(keydata){
				window.crypto.subtle.decrypt({
						 name: "AES-CBC",
				    	 iv:BAiv 
		            	}, keydata, BAencrypted)
						.then(function(data){
							var s=BAtohex(new Uint8Array (data))
							var hb64u= hextob64u(s);
							myDeffered.resolve(hb64u);
						}).catch(function(e){
							console.log("Error in decryption:"+e);  
						});
			}).catch(function(e){
				console.log("Error in import  Key :"+e); 	
			});	
	});
}

/**
 * Makes a digest to the @param hex string using WebCrypto library
 * @param hexString
 * @param deferredDig
 * @returns
 */
function getDigest(hexString, deferredDig){
	var buff = new Uint8Array(parseHexString(hexString)).buffer;
	var digested =  crypto.subtle.digest('SHA-256', buff);
	var myDigest = '';
	digested.then((dig) => {
		myDigest = arrayBufferToHexString(dig);
		deferredDig.resolve(myDigest);
	});
}

/**
 * Converts an array buffer to hex string
 * @param arrayBuffer
 * @returns
 */
function arrayBufferToHexString(arrayBuffer) {
    var byteArray = new Uint8Array(arrayBuffer);
    var hexString = "";
    var nextHexByte;

    for (var i=0; i<byteArray.byteLength; i++) {
        nextHexByte = byteArray[i].toString(16);
        if (nextHexByte.length < 2) {
            nextHexByte = "0" + nextHexByte;
        }
        hexString += nextHexByte;
    }
    return hexString;
}
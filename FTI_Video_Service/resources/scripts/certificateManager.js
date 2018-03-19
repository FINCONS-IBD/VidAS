/**
 * 
 */

function encrypt(secret, dataToEncrypt, deferredEncr, salt){

	var deferredDeriveIV = $.Deferred();
	var iv 	= deriveIV(salt, deferredDeriveIV)
	var hexToEncrypt = b64utohex(dataToEncrypt);
	var BAToEncrypt = new Uint8Array(parseHexString(hexToEncrypt)).buffer;
	$.when(deferredDeriveIV).done(function(iv){
		iv = iv.slice(0, 16);
		window.crypto.subtle.importKey(
			    "raw", //only "raw" is allowed
			    stringToArrayBuffer(secret), //your password
			    {
			        "name": "PBKDF2",
			    },
			    false, //whether the key is extractable (i.e. can be used in exportKey)
			    ["deriveKey", "deriveBits"] //can be any combination of "deriveKey" and "deriveBits"
			)
			.then(function(importedKey){
				return window.crypto.subtle.deriveKey(
					    {
					        "name": "PBKDF2",
					        "salt": new Uint8Array(parseHexString(salt)).buffer,
					        "iterations": 1000,
					        "hash": {"name": "SHA-256"}, //can be "SHA-1", "SHA-256", "SHA-384", or "SHA-512"
					    },
					    importedKey, //your key from generateKey or importKey
					    { //the key type you want to create based on the derived bits
					    	"name": "AES-CBC", //can be any AES algorithm ("AES-CTR", "AES-CBC", "AES-CMAC", "AES-GCM", "AES-CFB", "AES-KW", "ECDH", "DH", or "HMAC")
					        //the generateKey parameters for that type of algorithm
					    	"length": 256, //can be  128, 192, or 256
				    	},
					    false, //whether the derived key is extractable (i.e. can be used in exportKey)
					    ["encrypt", "decrypt"] //limited to the options in that algorithm's importKey
					)
					.then(function(derivedKey){
						return window.crypto.subtle.encrypt(
								{
							        name: "AES-CBC",
							        //Don't re-use initialization vectors!
							        //Always generate a new iv every time your encrypt!
							        iv: iv,
							    },
							    derivedKey, //from generateKey or importKey above
							    BAToEncrypt //ArrayBuffer of data you want to encrypt
							)
							.then(function(encrypted){
								encrypted = BAtohex(new Uint8Array(encrypted));
								encrypted = hextob64u(encrypted);
								deferredEncr.resolve(encrypted);
							})
							.catch(function(err){
							    console.error("decrypt method " + err);
							});
					})
					.catch(function(err){
					    console.error("derive key method " + err);
					});
			})
			.catch(function(err){
			    console.error("import key method " + err);
			});
	});
}
/**
 * 
 * @param secret
 * @param encrypted
 * @param deferred
 * @param salt
 * @returns
 */
function decryptPrivateKey(secret, encrypted, deferred, salt){
	var deferredDeriveIV = $.Deferred();
	var iv 	= deriveIV(salt, deferredDeriveIV)
	var hexEncrypted = b64utohex(encrypted);
	encryptedAB = new Uint8Array(parseHexString(hexEncrypted)).buffer;
	$.when(deferredDeriveIV).done(function(iv){
		iv = iv.slice(0, 16);
		window.crypto.subtle.importKey(
			    "raw", //only "raw" is allowed
			    stringToArrayBuffer(secret), //your password
			    {
			        "name": "PBKDF2",
			    },
			    false, //whether the key is extractable (i.e. can be used in exportKey)
			    ["deriveKey", "deriveBits"] //can be any combination of "deriveKey" and "deriveBits"
			)
			.then(function(importedKey){
				return window.crypto.subtle.deriveKey(
					    {
					        "name": "PBKDF2",
					        "salt":  new Uint8Array(parseHexString(salt)).buffer,
					        "iterations": 1000,
					        "hash": {"name": "SHA-256"}, //can be "SHA-1", "SHA-256", "SHA-384", or "SHA-512"
					    },
					    importedKey, //your key from generateKey or importKey
					    { //the key type you want to create based on the derived bits
					        "name": "AES-CBC", //can be any AES algorithm ("AES-CTR", "AES-CBC", "AES-CMAC", "AES-GCM", "AES-CFB", "AES-KW", "ECDH", "DH", or "HMAC")
					        //the generateKey parameters for that type of algorithm
					        "length": 256, //can be  128, 192, or 256
					    },
					    false, //whether the derived key is extractable (i.e. can be used in exportKey)
					    ["encrypt", "decrypt"] //limited to the options in that algorithm's importKey
					)
					.then(function(derivedKey){
						return window.crypto.subtle.decrypt(
							    {
							        "name": "AES-CBC",
							        "iv": iv, //The initialization vector you used to encrypt
							    },
							    derivedKey, //from generateKey or importKey above
							    encryptedAB //ArrayBuffer of the data
							)
							.then(function(decrypted){
							    deferred.resolve(decrypted);
							})
							.catch(function(err){
							    console.error("decrypt method " + err);
							    deferred.reject();
							});
					})
					.catch(function(err){
					    console.error("derive key method " + err);
					    deferred.reject();
					});
			})
			.catch(function(err){
			    console.error("import key method " + err);
			    deferred.reject();
			});
	});
		
	
}

function generateJsonCertificate(username, secret, deferred){	
	var encryptedPrivateKey;
	var json_keyPair;

	
	window.crypto.subtle.generateKey(
	    {
	        name: "ECDH",
	        namedCurve: "P-256", 
	    },
	    true, // can extract it later if we want
	    ["deriveKey", "deriveBits"]
	).then(function(keyPair){
	        
	    window.crypto.subtle.exportKey(
			'jwk',  //can be "jwk" (public or private), "raw" (public only), "spki" (public only), or "pkcs8" (private only)
			keyPair.privateKey //can be a publicKey or privateKey, as long as extractable was true
		).then(function(json_private) {
			

			var base64encodedUrl = utf8tob64u(JSON.stringify(json_private));
			
			
			var deferredEncrypt = $.Deferred();
			var salt  = getRandomSalt();
			encrypt(secret, base64encodedUrl, deferredEncrypt, salt);

			$.when(deferredEncrypt).done(function(encryptedPrivateKey){			

			//N.B: da usare lato login per fare il decrypt del certificato che mi ritorna il DS							
			//var decryptedBase64Url = decrypt("ciaoBello", encryptedPrivateKey.toString());	
			//console.log("DECRYPTED_PRIVATE_KEY", decryptedBase64Url);						    	    	
			//var utf8privateKey = b64utoutf8(decryptedBase64Url.toString());		    	    	
			//console.log('UTF8', utf8privateKey);
				window.crypto.subtle.exportKey(
						'jwk',  //can be "jwk" (public or private), "raw" (public only), "spki" (public only), or "pkcs8" (private only)
						keyPair.publicKey //can be a publicKey or privateKey, as long as extractable was true
					).then(function(json_public) {
				    	
						
						json_keyPair ='{"username":"'+username+'", "certificate":{"privateKey":"'+encryptedPrivateKey+'", "publicKey":' + JSON.stringify(json_public)+ ', "salt":"' + salt + '"}}';
						
						deferred.resolve(json_keyPair);
									 
					}, function(reason) {
				    	console.log('Couldnt export publicKey', reason);
				    	deferred.reject();
					}).catch(function(e){
						console.log("Couldnt export publicKey :"+e);
					 	deferred.reject();
					});
			}).fail(function(){
				console.log('error in encrypting privatekey');
				deferred.reject();
			});
			
		}, function(reason) {
	    	console.log('Couldnt export privateKey', reason);
	     	deferred.reject();
		}).catch(function(e){
			console.log("Couldnt export privateKey "+e); 
		 	deferred.reject();
		});
		
		
	
	}, function(reason){
	    console.log('could not generate key', reason);
	    deferred.reject();
	});			
}

function getRandomSalt(){
	var array = new Uint32Array(32);
	var hexSalt = ''; 
	window.crypto.getRandomValues(array);

	hexSalt = arrayBufferToHexString(array);
	
	return hexSalt;
}

function deriveIV(hexStringSalt, deferredIV){
	var buffSalt = new Uint8Array(parseHexString(hexStringSalt)).buffer;//stringToArrayBuffer(hexStringSalt);
	var hashIV =  crypto.subtle.digest('SHA-256', buffSalt);
	var IV = '';
	hashIV.then((message) => {
//		IV = arrayBufferToHexString(message);
		deferredIV.resolve(message);
	});
}

// UTILITY
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

function arrayBufferToString(buffer){
	var decoder = new TextDecoder("utf-8");
	return decoder.decode(buffer);
}

function stringToArrayBuffer(string) {
    var encoder = new TextEncoder("utf-8");
    return encoder.encode(string);
}


function parseHexString(str) { 
    var result = [];
    while (str.length >= 2) { 
        result.push(parseInt(str.substring(0, 2), 16));
        str = str.substring(2, str.length);
    }

    return result;
}
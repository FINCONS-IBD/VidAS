/**
 * FileProtector CPABE-AES javascript,
 * @author Diego Pedone (Fincons Group)
 * 
 * @required <script type="text/javascript" src="scripts/abeClient/AbeProxyInterface.js"></script>
 * This class javascript used AbeProxyInterface.js to encrypt and decrypt a File with CPABE-AES
 */


/**
 * @costructor 
 * @params abeProxyInterface initialized with his configuration
 *
 */	
var FileProtector=function(abeProxyInterface){
	console.log("%c1.2 Init FileProtector", "color:black");
	/**
	 * @params  used for create different userId for encryption and decryption
	 */
	var operation="";
	
	/**
	 * Public Method generate a SharedSymmetricKey (AES-Key), shared with AbeProxy that encrypt the key with CP-ABE algorithm by policy.
	 * 
	 * @param policy: is a textual policy used for encryption of the Symmetric key
	 * @param deferred: is a $.Deferred Used to manage error or success.
	 * @return a json include encrypted AES key, key in Web Crypto format, a random IV used with this key to Encrypt 
				 {
    				"keyEncrypted":AES key encrypted with CP-ABE Base64Url Encoded,
    				"sharedSymKeyWC":sharedSymmetricKey in Web Crypto format,
    				"iv":arrayIV,
				  }
	 */
	this.generateKey=function(policy, deferred){
		var deferredGSS= $.Deferred();
		var deferredEncryption= $.Deferred();
		// Step 1 Generate SharedSymmetricKey, a key shared with ABE-Proxy
		console.log("%c 1.1 Start getSharedSecret ", "color:black");
		abeProxyInterface.getSharedSecret(deferredGSS);
		$.when(deferredGSS).done(function (sharedSymKey) {
			console.log("%c 1.2 End getSharedSecret ", "color:black");
			// Step 2 create HMACKey used to sign the policy when is send to ABE-Proxy,
			// 		the HMACKey is create from the SHA256-Hash of SharedSymmericKey
			var deferredKewHmacWC= $.Deferred();
			ACUtils.importHMACKey(sharedSymKey, deferredKewHmacWC);
			$.when(deferredKewHmacWC).done(function(keyHmacWC){	
				// Step 3 import the SharedSymmetricKey in WebCrypto Format
				var deferredImportSSK= $.Deferred();
				ACUtils.importAESCBCWebcryptoKey(sharedSymKey.buffer, deferredImportSSK);
				$.when(deferredImportSSK).done(function(sharedSymKeyWC){
					// Step 4 CP-ABE Encryption of sharedSymmetricKey by the policy,
					// the message sended to AbeProxy are signed with HMACKey,
					// this step return the encrypted Key info, a random
					// ByteArray of 16 element (the IV)
					// and the sharedSymKeyWebCrypto in Web Crypto format
					var deferredESSK=$.Deferred();
					abeProxyInterface.encSharedSymmetricKey(policy, sharedSymKeyWC, keyHmacWC, deferredESSK);
				    $.when(deferredESSK).done(function (jsonResponse) {
					    	var arrayIV = new Uint8Array(16);
					    	window.crypto.getRandomValues(arrayIV);
				    		var responseKey={
				    				"keyEncrypted":jsonResponse,
				    				"sharedSymKeyWC":sharedSymKeyWC,
				    				"iv":arrayIV,
				    		}
				    		deferred.resolve(responseKey);
					    }).fail(function (){
							console.error("error in SharedSymmetricKey encryption");
							deferred.reject("error in SharedSymmetricKey encryption");
						}); 
					}).fail(function (){
						console.error("error in SharedSymmetricKey Import");
						deferred.reject("error in SharedSymmetricKey Import");
					}); 
				}).fail(function (){
					console.error("error in Sha256  SharedSymmetricKey used for Sign");
					deferred.reject("error in SharedSymmetricKey Sha256");
				})
			}).fail(function (){
				console.error("error in getSharedSymmetricKey");
				deferred.reject("error in SharedSymmetricKey request");
			});
	};
	
	/**
	 * Public Method that encrypt a slice whit AES and return a json 
	 * 			{
	    			key_info:{
	    				enc_sym_key_id:encryptionKey.keyEncrypted.enc_sym_key_id,
	    				storage:{"storage_type": "embedded", "storage_parameters":[[{"name":"fieldName", "value":"fieldValue"}]},
		    			metadata:[{"name":"fieldName", "value":"fieldValue"}].
	    			},
	    			encfile:encryptedSliceBase64UrlEncoded,
	    			iv:Base64urlIv,
		    	}
	    		N.B. in case of storage_type embedded one of the storage_parameters must be the encrypted_key
	 * 
	 * @param currentSliceBA: is a Slice of bufferArray of File, uploaded from the user.
	 * @param encryptionKey: is a json contain the key used top encrypt returned from generateKey method ie: 
	 * 			 {
    				"keyEncrypted":AES key encrypted with CP-ABE Base64Url Encoded,
    				"sharedSymKeyWC":sharedSymmetricKey in Web Crypto format,
    				"iv":arrayIV,
				  }
	 * @param deferred: is a $.Deferred Used to manage error or success.
	 * @return a json with encrypted slice with deferred Resolve or return the error with deferred reject.
	 */
	this.encryptSliceFile=function(currentSliceBA, encryptionKey, deferred){
		var arrayIV= encryptionKey.iv;
		var hexIv=BAtohex(arrayIV);
		var B64uIv=hextob64u(hexIv);
		var deferredEncFile=$.Deferred();
	 	ACUtils.encrypt(currentSliceBA, encryptionKey.sharedSymKeyWC, arrayIV, deferredEncFile);
	 	$.when(deferredEncFile).done(function (encryptedFile) {
	 		var myjson={
	 				key_info:{
	 					enc_sym_key_id:encryptionKey.keyEncrypted.enc_sym_key_id,
	 					storage:encryptionKey.keyEncrypted.storage,
	 					metadata:encryptionKey.keyEncrypted.metadata,
	    			},
	    			encfile:encryptedFile,
	    			iv:B64uIv,
		    };
	 		deferred.resolve(myjson);
	 	}).fail(function (){
	 		console.error("error in file Encryption");
	 		deferred.reject("error in file encryption");
		});
	}
	 
	/**
	 * Public Method that decrypt a file whit AES(CP-ABE) and return the decrypted Slice.
	 * 
	 * @param encSlice: is a slice of a File that the user would like to download
	 * @param iv in Base64URL Encoding is the initialization vector used to encrypt and decrypt the slice
	 * @param sharedSymmetricKeyWC: is the AES key shared with Abe proxy used to encrypt and decrypt the slice 
	 * @return the Buffer Array of decrypted Slice.
	 */
	this.decryptSlice=function(encSlice, iv, sharedSymmetricKeyWC, deferred){
		   var deferredDecryptData=$.Deferred();
		    var encSliceBA=ACUtils.base64urlToArrayBuffer(encSlice);
		    // Step 1 Decrypt Slice with SharedSymmetricKey
		    var ivSlice = ACUtils.base64urlToArrayBuffer(iv);
			ACUtils.decrypt(encSliceBA, sharedSymmetricKeyWC, ivSlice, deferredDecryptData);
			$.when(deferredDecryptData).done(function (decryptSliceB64) {
			    // Decoding and Convert the decrypted slice from Base 64 Url to buffer Array;
				var decSliceBA=ACUtils.base64urlToArrayBuffer(decryptSliceB64);
		        deferred.resolve(decSliceBA);
			}).fail(function (){
				console.error("error in Slice Decryption ");
				deferred.reject("error in Slice Decryption ");
			});
	 };
	 
	/**
	 * Public Method that decrypt a file whit AES(CP-ABE) and return the file decrypted.
	 * 
	 * @param json_key_info is a json that contain all information about the key, this is a information got by metadata file, 
	 * 		so is returned from Video_Storage_Service with the encryptedSlice the structure of this json is:
	 *			key_info:{
	    				enc_sym_key_id:encryptionKey.keyEncrypted.enc_sym_key_id,
	    				storage:{"storage_type": "embedded", "storage_parameters":[{"name":"fieldName", "value":"fieldValue"}]},
		    			metadata:[{"name":"fieldName", "value":"fieldValue"}]
	    		},
	    		N.B. in case of storage_type embedded one of the storage_parameters must be the encrypted_key
	 * @param personalKey: is a textual personalKey in base64url used for decrypted the Symmetric key
	 * @param deferred: is a $.Deferred Used to manage error or success.
	 * @return SharedSymmetricKeyWC the decrypted AES key in Web Crypto format,
	 */
	 this.decryptKey=function(json_key_info, personalKey, deferred){
		 try{
			console.log("start decryption");
			operation="decryption";
			abeProxyInterface.operation=operation;
			var deferredSKP= $.Deferred();
			// Step 1 Generate SharedSymmetricKeyProtector, a key shared with ABE-Proxy used to improved the security,
			//		this key is used to decrypt with AES the SharedSymmetricKey (AES key) used to decrypt the file.
			abeProxyInterface.getSharedSecret(deferredSKP);
			$.when(deferredSKP).done(function (symmetricKeyProtector) {
				// Step 2 create HMACKey used to sign the policy when is send to ABE-Proxy,
				// 		the HMACKey is create from the SHA256-Hash of SharedSymmetricKeyProtector
				var deferredKewHmacWC= $.Deferred();
				ACUtils.importHMACKey(symmetricKeyProtector, deferredKewHmacWC);
				$.when(deferredKewHmacWC).done(function(kewHmacWC){	
					// Step 3 import the SharedSymmetricKeyProtector in WebCrypto Format
					var deferredImportSKP= $.Deferred();
					ACUtils.importAESCBCWebcryptoKey(symmetricKeyProtector.buffer, deferredImportSKP);
					$.when(deferredImportSKP).done(function(symmetricKeyProtectorWC){
					    // Step 5 Get sharedSymmetricKey from	ABE-Proxy, for this operation are used:	
						//	- the CP-ABE personalKey ( a key in Base654Url Encoding) used to decrypt with CP-ABE Algorithm the SharedSymmetricKey;
						//	- enc_sym_key_id the id of the key stored with ABE-Proxy;
						//  - storage the json that contain all the inforamtion about the storage used 
						//		N.B. in this case the type of storage is embedded and one of the storage parameter are the EncryptedKey;
						// 	- symmetricKeyProtectorWC the key used to protect(with AES) the personalKey in the request and to protect(with AES) the CP-ABE decrypted SharedSymmetricKey in response;
						//  - kewHmacWC the key used to sign the message send to ABE-Proxy
						// Return a jsonResponse with the information about SharedSymmetricKey encrypted with symmetricKeyProtectorWC using AES algorithm
						var deferredGSSKP=$.Deferred();
						abeProxyInterface.getSharedSymmetricKeyProtect(personalKey, json_key_info.enc_sym_key_id, json_key_info.storage ,symmetricKeyProtectorWC, kewHmacWC, deferredGSSKP);
						$.when(deferredGSSKP).done(function (jsonResponse) {
							// Step 6 Verify the signature return from the  ABE-Proxy response
					    	var tagBA= ACUtils.base64urlToArrayBuffer(jsonResponse.tag);
					    	var dataBA= new TextEncoder("utf-8").encode(JSON.stringify(jsonResponse.json_encripted_key));
					    	window.crypto.subtle.verify(
				    		    {
				    		        name: "HMAC",
				    		    },
				    		    kewHmacWC, // from generateKey or importKey
											// above
				    		    tagBA, // ArrayBuffer of the signature
				    		    dataBA // ArrayBuffer of the data
				    		).then(function(isvalid){
				    			if(!isvalid){
				    				  deferred.reject("error in verify sign");
				    			}
				    		    console.log("verified signature?", isvalid);
				    		}).catch(function(err){
				    		    console.error(err);    
				    		    deferred.reject("error in verify sign");
						    });
					    	// Step 6.1 Convert iv from Base64 Url Encoded in iv Buffer Array
					    	var ivb64u=jsonResponse.json_encripted_key.iv;
					    	var iv=ACUtils.base64urlToArrayBuffer(ivb64u);
					    	// Step 6.2 Convert sharedSymmetricKey from Base64 Url Encoded in sharedSymmetricKey Buffer Array
					    	var sharedSymKeyEnc=jsonResponse.json_encripted_key.shader_sym_key_protect;
						    var sharedSymKeyEncBA=ACUtils.base64urlToArrayBuffer(sharedSymKeyEnc);
						   	var deferredDSSK=$.Deferred(); 
						   	// Step 7 Decrypted sharedSymmetricKey (bufferArray) with symmetricKeyProtectorWC using AES Algorithm
					    	ACUtils.decrypt(sharedSymKeyEncBA, symmetricKeyProtectorWC, iv,deferredDSSK);
					    	$.when(deferredDSSK).done(function (decryptedSharedSymKey) {
					    		// Step 7.1 Convert sharedSymmetricKey decrypted from Base64 Url Encoded in sharedSymmetricKey decrypted Buffer Array
					    		var decryptedSharedSymKeyBA=ACUtils.base64urlToArrayBuffer(decryptedSharedSymKey);
					    		// Step 8 import the SharedSymmetricKey Decrypted in WebCrypto Format
					    		var deferredImportSSK= $.Deferred();
								ACUtils.importAESCBCWebcryptoKey(decryptedSharedSymKeyBA, deferredImportSSK);
								$.when(deferredImportSSK).done(function(sharedSymmetricKeyWC){
									// END Decrypting Key and return with deferred the sharedSymmetricKey in web Crypto Format
									deferred.resolve(sharedSymmetricKeyWC);
								}).fail(function (){
									console.error("error in SymmetricKey Import");
									deferred.reject("error in SymmetricKey Import")	
								}); 
							}).fail(function (){
								console.error("error in SymmetricKey DecryptionShared");
								deferred.reject("error in SymmetricKey Decryption")	
							}); 
				    	}).fail(function (){
							console.error("error in Shared Symmetric Key Encrypted");
							deferred.reject("error in Shared Symmetric Key Encrypted");
						}); 
					}).fail(function (){
						console.error("error in SharedSymmetricKeyProtector Import");
						deferred.reject("error in SharedSymmetricKeyProtector Import");
					}); 
				}).fail(function (){
					console.error("error in Sha256  SharedSymmetricKeyProtector used for Sign");
					deferred.reject("error in SharedSymmetricKeyProtector Sha256");
				}); 
			}).fail(function (){
				console.error("error in getSharedSymmetricKeyProtect");
				deferred.reject("error in getSharedSymmetricKeyProtect");
			}); 
		  }catch(err){
			console.error("generic error in decryption",err);
		    deferred.reject("generic error in decryption",err );
		  }
	};
};

/**
 * AbeProxyInterface javascript,
 * @author Diego Pedone (Fincons Group)
 *	---- javascript class used for emcryption and decryption in particularly for concatKdf ---- 
 * @required <script type="text/javascript" src="crypto/components/core.js"></script> 
 * @required <script type="text/javascript" src="crypto/components/sha256.js"></script> 
 * @required <script type="text/javascript" src="jsrsasign/crypto-1.1.js"></script>
 *	---- javascript class used for conversion ---- 
 * @required <script type="text/javascript" src="scripts/jsrsasign/base64x-1.1.js"></script>
 * @required <script type="text/javascript" src="scripts/jsrsasign/ext/base64.js"></script>
 * 	---- worker use to communicate with abe proxy ---- 
 * @required "scripts/abeClient/workerAjaxAbeClient.js", imported like worker
 * @version 1.0 (problem with big File).
 */


/**
 * @costructor 
 * @params config must have this structure:
 * 
 *  { 
 *  	worker: is the workerAjaxAbeClient path (used for connect to AbeProxy,
 *  	key_alg: the algorithm use to create the couple of key, 
 *  	key_crv: the curve use from the key , 
 *  	key_kty: identified the key type, key_enc: the algorithm which will be used to encrypt the data, 
 *  	url_abeProxy: the abeProxy's url,
 * 		username: username , key_storage_type:The key_Storage type es. database,
 * 		key_storage_ip: the KeyStorageService's ip address or host ,
 * 		key_storage_port:the KeyStorageService's port , 
 * 		key_db_database:the database name used in keyStorageService, 
 * 		key_db_table:the table name used in keyStorageService, 
 * }
 * First of all is needed the initialization by the costructor passing the config like parameter.
 * @Encryption: instruction to use the library for the encryption
 * - 1 set the operation, this operation allow to have a different username for encryption and decryption, 
 * 		needed for a correct configuration of shared symmetric key.
 * - 2 invoke getSharedSecret for the generation of a shared Symmetic Key
 * - 3 create a new key used for the sign the message with importHMACKey method
 * - 4 import the sharedSymmetricKey in webcrypto Format with importAESCBCWebcryptoKey method
 * - 5 use the method encSharedSymmetricKey to encrypt the policy, sign the encrypted policy 
 * 		and send it  to ABE-Proxy. the Abe-Proxy return the id of SharedSymmetricKey.
 * - 6 encrypt the data with sharedSymmericKey.
 * - 6.1 generate a random IV
 * - 6.2 encrypt the data and return the encrypted file.
 * 
 * @Decryption: instruction to use the library for the decryption
 * - 1 set the operation, this operation allow to have a different username for encryption and decryption, 
 * 		needed for a correct configuration of shared symmetric key.
 * - 2 invoke getSharedSecret for generate SSKP , a key used to Protect the Shared Symmetric Key used for the data encryption
 * - 3 create a new key used for the sign the message with importHMACKey method
 * - 4 import the SSKP in webcrypto Format with importAESCBCWebcryptoKey method
 * - 5 use the method getSharedSymmetricKeyProtect to send to AbeProxy a request to get the ShaderSymmetricKey by Id,
 * 		the SSK(shared Symmetric Key) is protect by the SSKP ( shared Symmetric Key protector, a key used to encrypt the SSK).
 * - 5.1 verify the sign of the response  
 * - 5.2 decrypt the sharedSymmericKey with SSKP.
 * - 5.3 import the sharedSymmetricKey in webcrypto Format with importAESCBCWebcryptoKey method
 * - 6 decrypt the data with sharedSymmericKey.
 * - 6.1 get the iv from protected file
 * - 6.2 decrypt the data and return the decrypted file.
 */	
var AbeProxyInterface=function(config){
	console.log("%c1.1 Init AbeProxyInterface", "color:grey");
	var worker=config.pathWorker;
	var key_alg=config.key_alg;
	var key_crv=config.key_crv;
	var key_kty=config.key_kty;
	var key_enc=config.key_enc;
	var url_abeProxy=config.url_abeProxy;
	var username=config.username;
	var keyStorageConfig=config.key_storage_config;
	 
	this.operation="";
	
	 /**
	 * Public Method Get the Shared Symmetric Key Encrypted with AES from AbeProxy 
	 * used in decryption for get the sharedSymmetricKey decrypted wit CPAbe and protect with AES
	 * 5.0 Create persona Info json
	 * 5.1 Encrypt personal_Info with the symmetricKeyProtector 
	 * 5.2 Send the personal_Info_Encrypted to Abe_Proxy 
	 * 5.7 the shared Symmetric key encrypted with symmetricKeyProtector
	 * @param personalKey, is a textual personalKey in base64url used for decrypted the Symmetric key create by KeyGeneratorService, is based on the user's attributes
	 * @param idSharedSymmetricKey, is Id of the key with which the data was encrypted.
	 * @param storageInfo, a json that indicate what and where is the KeyStorageService.
	 * @param symmetricKeyProtectorWC, key used for encrypted the request. 
	 * @param kewHmacWC, key use tyo sign the request.
	 * @param deferredGSSKP deferred use to return the function result.
	 * @return the json returned from get_shared_secret, a AbeProxy service. 
	 * 			Is Returned by the deferred Resolve o in case of error return by deferred reject. 
	 */
	this.getSharedSymmetricKeyProtect= function(personalKey, idSharedSymmetricKey, storageInfo, symmetricKeyProtectorWC, keyHmacWC, deferredGSSKP){
		console.log("start getSharedSymmetricKeyProtect");
		var  w = new Worker(worker);
		// 5.0 create Personal_info
		var operation= this.operation;
		var personalInfo={
				timestamp:new Date().toJSON().toString(),
				device_Id:"ABE-Service_"+username+this.operation,
				idSharedSymmetricKey:idSharedSymmetricKey,
				personalKey:{
					key: personalKey,
					metadata:[],
				},
				encrypted_symmetric_key_storage:storageInfo,				 			
		};
		console.log("personalInfo", personalInfo);
		var personalInfoBA=new TextEncoder("utf-8").encode(JSON.stringify(personalInfo));
		// 5.1 encryption personalInfo
		var arrayIV=  new Uint8Array(16)
		window.crypto.getRandomValues(arrayIV);
		var hexIv=BAtohex(arrayIV);
		var B64uIv=hextob64u(hexIv);
		var deferredEncr=$.Deferred();
		ACUtils.encrypt(personalInfoBA.buffer, symmetricKeyProtectorWC, arrayIV, deferredEncr);
		$.when(deferredEncr).done(function (encryptedPersonalInfoB64){
			// 5.2.1 POST create encryptedPersonalInfo json
			var deferredHMAC=$.Deferred();
			var enc_info= {"iv":B64uIv, "enc_personal":encryptedPersonalInfoB64};
//			console.log("enc_info", JSON.stringify(enc_info));
			var enc_infoBA=new TextEncoder("utf-8").encode(JSON.stringify(enc_info));
//			console.log("enc_infoBA", enc_infoBA);
			ACUtils.calculateHmac(enc_infoBA, keyHmacWC, deferredHMAC);
			$.when(deferredHMAC).done(function (hmac){
				var encPersonalInfo={
					"enc_personal_info":enc_info,
					"metadata":[
						{
							name: "encryption-date",
							value: new Date().toJSON().toString(),
						},
						{
							name:"encryptor", 
							value:"ABE-Service_"+username+operation,
						},
					],
					"tag":hmac
				};
				// 5.2 POST encryptedPersonalInfo
				w.postMessage([url_abeProxy, "get_shared_secret" ,encPersonalInfo], []);	
				w.onmessage = function(event) {
					var jsonResponseString=event.data;
					var jsonResponse=JSON.parse(jsonResponseString);
					if(jsonResponse.status=="Error"){
						deferredGSSKP.reject("Error in response code: "+ jsonResponse.codeStatus+" message:"+ jsonResponse.message);
					}
					deferredGSSKP.resolve(jsonResponse);
					console.log("success in Encryption");
				}
			}).fail(function (){
				console.error("error in calculation HMAC cpabeInfo");
				deferredESSK.reject("error in calculation HMAC cpabeInfo");
			}); 
		}).fail(function (){
			console.error("error in Encryption cpabeInfo");
			deferredGSSKP.reject("error in Encryption cpabeInfo");
		}); 
	 };	 
			 
	 /**
	 * Private Method that Encrypt the cpabe information with Shared Symmetric Key (AES) and send the info to AbeProxy,
	 * 			 that provides to encrypt with CP-ABE the Shared Symmetric Key and store it in a KeyStorageService.
	 * Encrypt the Shared Symmetric Key with AbeProxy 
	 * 2.1 Encrypt the shared Symmetric key 
	 * 2.2 Send the encrypted shared Symmetric key
	 * 2.7 the encrypted shared Symmetric key ID
	 * @param policy,is t
	 * @param symmetricKeyProtectorWC, key used for encrypted the request. 
	 * @param kewHmacWC, key use tyo sign the request.
	 * @param keyStorageConfig is a jsonConfig like this example: 
	 * 	{
					storage_type:key_storage_type,
					storage_parameters:[
						{ name:"db_ip", value:key_storage_ip},
						{ name:"db_port",value:key_storage_port},
						{ name:"db_database",value:key_db_database},
						{ name: "db_table", value:key_db_table},
					],
				
		}
	 * @param deferredESSK deferred use to return the function result.
	 */
	 this.encSharedSymmetricKey= function(policy, sharedSymKeyWC, kewHmacWC, deferredESSK){
		console.log("start encSharedSymmetricKey");
		var  w = new Worker(worker);
		 // 2.0 create Cpabe_info
		var operation= this.operation;
	 	var cpabeInfo={
			timestamp:new Date().toJSON().toString(),
			device_Id:"ABE-Service_"+username+operation,
			policy:{
				specs: policy,
				metadata:[
					{
	 					name:"creation_time",
						value:new Date().toJSON().toString(),
					},
				],
			},
			encrypted_symmetric_key_storage: keyStorageConfig,
//			{
//				storage_type:key_storage_type,
//				storage_parameters:[
//					{ name:"db_ip", value:key_storage_ip},
//					{ name:"db_port",value:key_storage_port},
//					{ name:"db_database",value:key_db_database},
//					{ name: "db_table", value:key_db_table},
//				],
//			
//			},
					
	 	};
	
	 	var arrayIV=  new Uint8Array(16)
		window.crypto.getRandomValues(arrayIV);
		var hexIv=BAtohex(arrayIV);
		var B64uIv=hextob64u(hexIv);
	    var cpabeInfoBA=new TextEncoder("utf-8").encode(JSON.stringify(cpabeInfo));
		 // 2.1 encryption Cpabe_info
	 	var deferredEncr=$.Deferred();
	 	ACUtils.encrypt(cpabeInfoBA.buffer, sharedSymKeyWC, arrayIV, deferredEncr); 
	 	$.when(deferredEncr).done(function (encryptedCpabeInfoB64){
	 		 // 2.2.1 POST create encryptedCpabeInfo json
	 	 	var deferredHMAC=$.Deferred();
	 	 	var enc_info= {"iv":B64uIv, "enc_cpabe":encryptedCpabeInfoB64};
		    var enc_infoBA=new TextEncoder("utf-8").encode(JSON.stringify(enc_info))
		    // var enc_infoBA=ACUtils.base64ToArrayBuffer(enc_info);
 			ACUtils.calculateHmac(enc_infoBA, kewHmacWC, deferredHMAC);
		 	$.when(deferredHMAC).done(function (hmac){
		 		var encCpabeInfo={
		 			"enc_cpabe_info":enc_info,
		 			"metadata":[
		 				{
		 					name: "encryption-date",
		 					value: new Date().toJSON().toString(),
		 				},
		 				{
		 					name:"encryptor", 
		 					value:"ABE-Service_"+username+operation,
		 				},
		 			],
		 			"tag":hmac,		
		 		};
		 		// 2.2 POST encryptedCpabeInfo
		 		w.postMessage([url_abeProxy, "cpabe_information" ,encCpabeInfo], []);	
				w.onmessage = function(event) {
					var jsonResponseString=event.data;
					var jsonResponse=JSON.parse(jsonResponseString);
					if(jsonResponse.status=="Error"){
						deferredESSK.reject("Error in response code: "+ jsonResponse.codeStatus+" message:"+ jsonResponse.message);
					}
					deferredESSK.resolve(jsonResponse);
				}
				console.log("success in Encryption");
		 	}).fail(function (){
				console.error("error in calculation HMAC cpabeInfo");
				deferredESSK.reject("error in calculation HMAC cpabeInfo");
			}); 
		}).fail(function (){
			console.error("error in Encryption cpabeInfo");
			deferredESSK.reject("error in Encryption cpabeInfo");
		}); 
	 };
		 
	/**
	 * Private Method Generate a Shared Symmetric Key with AbeProxy 1 create a shared key
	 * @param deferredGSS deferred use to return the function result.
	 * @return a sharedSymmetricKey
	 */
	 this.getSharedSecret= function(deferredGSS){
		console.log("%c 2.2 Start getSharedSecret", "color:grey");
		// *prepare element to 1.1
		var  w = new Worker(worker);
		var deferred= $.Deferred();
		var operation= this.operation;
		// **1.1a create client ephimeral key
		ACUtils.generateJsonCertificate(key_alg, key_crv, deferred);
		$.when(deferred).done(function (response) {
			console.log("%c 2.3 End generateJsonCertificate", "color:grey");
			// **1.1b create dsource_epk__info
			//	console.log("done" ,response);
			var usernameB64u= b64tob64u(btoa("ABE-Service_"+username+operation));
			var urlAbeProxyB64u= b64tob64u(btoa(url_abeProxy));
			json_response=JSON.parse(response);
			//prepare jwk to send to abe proxy
			var jwk_4_abe_proxy={
					"alg": key_alg,
					"enc": key_enc,
					"apu": usernameB64u,
					"apv": urlAbeProxyB64u,
					"epk": json_response.certificate.publicKey
			}
			console.log("%c 2.3 generate jwk 4 abe proxy"+jwk_4_abe_proxy, "color:grey");
			// **1.1c post generate_shared_secret
			console.log("%c 2.4 invoke webWorker generate_shared_secret", "color:grey");
			w.postMessage([url_abeProxy, "generate_shared_secret" ,jwk_4_abe_proxy], []);			
			w.onmessage = function(event) {			
				console.log("%c 2.4 response webWorker", "color:grey");
				 // *1.4 if enter in this function the post
					// generate_shared_secret is ok
				var jsonResponse=JSON.parse(event.data);
				if(jsonResponse.status=="Error"){
					deferredGSS.reject("Error in response code: "+ jsonResponse.codeStatus+" message:"+ jsonResponse.message);
				}
				// *1.5 Diffie-Hellman Client side
				console.log("%c 2.5 start Diffie-Hellman", "color:grey");
				var deferredDH= $.Deferred();
				var deferredConcatKdf= $.Deferred();
				ACUtils.deriveKeyDH(json_response.certificate.privateKey, jsonResponse.epk, key_alg, key_crv, deferredDH);
				$.when(deferredDH).done(function (responseSharedSecret) {
					console.log("%c 2.5 End Diffie-Hellman", "color:grey");
					console.log("%c 2.6.1 Preparate Contact KDF", "color:grey");
					// * 1.6.1 prepare parameter for concatKDF
					var algID = ACUtils.toArrayBufferWithLenghtPrefix(key_enc);
				    var pUInfo= ACUtils.toArrayBufferWithLenghtPrefix(jsonResponse.apu);
				    var pVInfo= ACUtils.toArrayBufferWithLenghtPrefix(jsonResponse.apv);
				    // remove the no digit character
				    var keyLength= parseInt(key_enc.replace( /^\D+/g, ''));
				    var suppPubInfo=ACUtils.intToFourBytes(keyLength);
				    var suppPrivInfo=null;
				    // *1.6 Concat-KDF obtain shared_sym_key
					console.log("%c 2.6 Start Contact KDF", "color:grey");
					var shared_Sym_key=ACUtils.concatKdf(responseSharedSecret, algID, pUInfo, pVInfo, keyLength, suppPubInfo, suppPrivInfo);
					if(shared_Sym_key==null){
						console.error("%c 2.6 End Contact KDF Error", "color:red");
						deferredGSS.reject("Error in concatKdf");
					}
					// return sharedSymmetricKey in deferred with resolve
					console.error("%c 2.6 End Contact KDF", "color:green");
					deferredGSS.resolve(shared_Sym_key);
				
				}).fail(function (){
					console.error("fail diffieHellman");
					deferredGSS.reject("fail diffieHellman");
				});
				w.terminate();
		
				
			};	
		}).fail(function (error){
			console.error(error);
			deferredGSS.reject(error);
		}); 
	};

		

};

/**
 * Contains methods for: 
 * @method - encryption
 * @method- decryption
 * @method- calculateHmac
 * @method- generateJsonCertificate
 * @method- deriveKeyDH
 * @method- concatKdf
 * @method- importAESCBCWebcryptoKey
 * @method- importHMACKey
 * @method- intToFourBytes
 * @method- toArrayBufferWithLenghtPrefix
 * @method- concat2arrayBuffer
 * @method- arrayBufferToHexString
 * @method- parseHexString
 * @method- base64ToArrayBuffer:
 */
var ACUtils={
		
	/**
	 * Method used for AES-CBC Encryption 
	 * @param BAToEncrypt ArrayBuffer to encrypt
	 * @param key in webCrypto format use to encrypt
	 * @param iv used to encrypt
	 * @param deferredEncr used to return the result with promise
	 * @return encryptedB64 BAToEncrypt encrypted and encoded in Base64
	 */
	encrypt:function(BAToEncrypt, key, iv, deferredEncr){
//		console.log("%c ACUtils.encrypt Start", "color:orange");
		window.crypto.subtle.encrypt(
			{
				name: "AES-CBC",
			    // Don't re-use initialization vectors!
			    // Always generate a new iv every time your encrypt!
			    iv: iv,
			},
			key, // from generateKey or importKey above
			BAToEncrypt // ArrayBuffer of data you want to encrypt
		).then(function(encrypted){
			afterEncryption(encrypted);
		}).catch(function(err){
			console.error("encrypt method " + err);
			deferredEncr.reject();
		});
		function afterEncryption(encrypted){
//			console.debug("%c ACUtils.encrypt Success", "color:green");
			var encryptedArray=new Uint8Array(encrypted);
//			console.log(encrypted.byteLength);
//			console.log(new Uint8Array(encrypted));
			encrypted=null;
			delete encrypted;
			var encryptedHex=BAtohex(encryptedArray);
			encryptedArray=null;
			delete encryptedArray;
			var encryptedB64u=hextob64u(encryptedHex);
			encryptedHex=null;
			delete encryptedHex;
			deferredEncr.resolve(encryptedB64u);
		};
	},
		
	/**
	 * Method used for AES-CBC Decryption 
	 * @param BAToDecrypt ArrayBuffer to decrypt
	 * @param key in webCrypto format use to decrypt
	 * @param iv used to decrypt
	 * @param deferredDecr used to return the result with promise
	 * @return decryptedB64u BAToDecrypt decrypted and encoded in Base64url
	 */
	decrypt:function(BAToDecrypt, key, iv, deferredDecr){
		console.log("ACUtils.decrypt Start");
		window.crypto.subtle.decrypt(
			{
				name: "AES-CBC",
				// Don't re-use initialization vectors!
				// Always generate a new iv every time your encrypt!
				iv: iv,
			},
			key, // from generateKey or importKey above
			BAToDecrypt // ArrayBuffer of data you want to decrypt
		).then(function(decrypted){
			afterDecryption(decrypted);
		}).catch(function(err){
			console.error("decrypt method " + err);
			deferredDecr.reject();
		});
		function afterDecryption(decrypted){
			console.log("ACUtils.decrypt Success");
			var decryptedArray=new Uint8Array(decrypted);
			decrypted=null;
			delete decrypted;
			var decryptedHex=BAtohex(decryptedArray);
			decrypteddArray=null;
			delete decrypteddArray;
			var decryptedB64u=hextob64u(decryptedHex);
			decryptedHex=null;
			delete decryptedHex;
			deferredDecr.resolve(decryptedB64u);
		};
	},
		
	/**
	 * @param data BAToSign ArrayBuffer to sign 
	 * @param key in webCrypto format use to sign 
	 * @param deferredHMAC used to return the result with promise 
	 * @return encryptedB64 BAToEncrypt encrypted and encoded in Base64
	 */
	calculateHmac:function(data, key, deferredHMAC){
		window.crypto.subtle.sign(
			{name: "HMAC",},
			key, // from generateKey or importKey above
			data // ArrayBuffer of data you want to sign
		).then(function(signature){
			var signatureArray=new Uint8Array(signature);
			var signatureHex=BAtohex(signatureArray);
			var signatureB64u=hextob64u(signatureHex);
			deferredHMAC.resolve(signatureB64u);
		}).catch(function(err){
			console.error("decrypt method " + err);
			deferredHMAC.reject();
		});
	},
	
	/**
	 * generate a couple of key with algorithm and crv passed with parameter
	 * and return the result with a deferred resolution
	 * 
	 * @param alg
	 * @param crv
	 * @param deferred
	 * @returns
	 */
	generateJsonCertificate:function(alg, crv, deferred){
		console.log("%c2.3 generateJsonCertificate", "color:grey");
		// var encryptedPrivateKey;
		var json_keyPair;
		console.log("generate");
		window.crypto.subtle.generateKey(
		    {
		        name: alg,
		        namedCurve: crv, 
		    },
			true, // can extract it later if we want
			["deriveKey", "deriveBits"]
		).then(function(keyPair){
			console.log("%c 2.3 key generated ", "color:grey");
			window.crypto.subtle.exportKey(
				'jwk',  // can be "jwk" (public or private), "raw" (public only), "spki" (public only), or "pkcs8" (private only)
				keyPair.privateKey // can be a publicKey or privateKey, as long as extractable was true
			).then(function(json_private) {
				console.log("%c 2.3 export Private Key ", "color:grey");
				window.crypto.subtle.exportKey(
					'jwk', // can be "jwk" (public or private), "raw" (public only), "spki" (public only), or "pkcs8" (private only)
					keyPair.publicKey // can be a publicKey or privateKey, as long as extractable was true
				).then(function(json_public) {	
						console.log("%c 2.3 export Public Key ", "color:grey");
						json_keyPair ='{"certificate":{"privateKey":'+JSON.stringify(json_private)+', "publicKey":' + JSON.stringify(json_public)+ '}}';
						console.log("%c 2.3 certificate created ", "color:grey");
						deferred.resolve(json_keyPair);
					}, function(reason) {
						console.error('Couldnt export publicKey', reason);
						deferred.reject('Couldnt export publicKey');
					}).catch(function(e){
						console.error("Couldnt export publicKey :"+e);	
						deferred.reject('Couldnt export publicKey');
					});
			}, function(reason) {
				console.error('Couldnt export privateKey', reason);
		    	deferred.reject("Couldnt export privateKey");
			}).catch(function(e){
				console.error("Couldnt export privateKey :"+e);
				deferred.reject("Couldnt export privateKey");
			});
		}, function(reason) {
			console.error('Couldnt generate key', reason);
	     	deferred.reject("Couldnt generate key ");
		}).catch(function(e){
			console.error("Couldnt generate key "+e); 
		 	deferred.reject("Couldnt generate key ");
		});
				
	},
	/**
	 * Calculate the Diffie-Hellman from the private and public key and
	 * return the shared secret
	 * 
	 * @param myPrivateKey 
	 * @param myPublicKey
	 * @param key_alg
	 * @param key_crv
	 * @param deferred
	 * @returns with deferred resolved o reject
	 */
	deriveKeyDH:function (myPrivateKey, myPublicKey, key_alg, key_crv, deferred){
		console.log("%c 2.5.1 start deriveKeyDH", "color:grey");
		var alg_crv={
			name: key_alg,
			namedCurve:  key_crv,
		};
		myPublicKey.ext=true;
		myPublicKey.key_opt=[];
		window.crypto.subtle.importKey("jwk", myPrivateKey, alg_crv,  true, ["deriveKey", "deriveBits"])
			 .then(function(privK){
				console.log("%c 2.5.2 imported PrivateKey", "color:grey");
				window.crypto.subtle.importKey("jwk", myPublicKey, alg_crv, true, [])
					.then(function(pubK){	
						console.log("%c 2.5.3 imported Public ket", "color:grey");
						var p={
								"name": key_alg,
					            "namedCurve": key_crv,
					            "public": pubK,
					    };
						window.crypto.subtle.deriveKey(p, privK,
					        {
					        	name: "AES-CBC", 
					            length: 256,
					        }, true, ["encrypt", "decrypt"])
					        .then(function (data) {
					        	console.log("%c 2.5.4 derived Key", "color:grey");
					        	window.crypto.subtle.exportKey("raw",data)
				            		.then(function(shared_secret){
				            			console.log("%c 2.5.5 Export derived Key", "color:grey");
				            			var sharedSecretB64u=b64tob64u(btoa(String.fromCharCode.apply(null, new Uint8Array(shared_secret))));
				            			console.log("sharedSecret Calculated" );// , data);
				            			deferred.resolve(shared_secret);
				            		}).catch(function(e){
				            			console.error("Error export derive key: "+e);
				            			deferred.reject("Error export derive key: ");
				            		});
						}).catch(function(e){
							console.error("Error in deriveKey:"+e);  
							deferred.reject("Error in deriveKey:");
						});
				}).catch(function(e){
					console.error("Error in import Public Key :"+e); 	
					deferred.reject("Error in import Public Key :");
				});
			}) .catch(function(e){
				console.error("Error in import Private Key :"+e);
				deferred.reject("Error in import Private Key :");
			});
	},

	/**
	 * Create shared symmetric key with concatKdf
	 * 
	 * @required <script type="text/javascript" src="crypto/components/core.js"></script>
	 * @required <script type="text/javascript" src="crypto/components/sha256.js"></script>
	 * @required <script type="text/javascript" src="jsrsasign/crypto-1.1.js"></script>
	 * @param sharedSecret
	 * @param algID
	 * @param pUInfo
	 * @param pVInfo
	 * @param keyLength
	 * @param suppPubInfo
	 * @param suppPrivInfo
	 * @returns Shared sym Key
	 */
	concatKdf:function (sharedSecret, algID, pUInfo, pVInfo, keyLength, suppPubInfo, suppPrivInfo){
		console.log("Calculate sharedSymKey concatKdf");
		try{
			var ab= new ArrayBuffer();
			ab=ACUtils.concat2arrayBuffer(ab, algID);
			ab=ACUtils.concat2arrayBuffer(ab, pUInfo);
			ab=ACUtils.concat2arrayBuffer(ab, pVInfo);
			if(suppPubInfo!=null){
				ab=ACUtils.concat2arrayBuffer(ab, suppPubInfo);
			}
			if(suppPrivInfo!=null){
				ab=ACUtils.concat2arrayBuffer(ab, suppPrivInfo);
			}
			keyLength = keyLength / 8;
		    var keyBA=new ArrayBuffer(keyLength);
		    var reps= parseInt(keyLength/32);
		    var md = new KJUR.crypto.MessageDigest({"alg": "sha256", "prov": "CriptoJS"});
		 // update data
		    var key=new Int8Array(keyLength);
		    for (var i=0; i<=reps; i++){   	
		    	 md.updateHex(ACUtils.arrayBufferToHexString(ACUtils.intToFourBytes(i+1)));
		    	 md.updateHex(ACUtils.arrayBufferToHexString(sharedSecret));
		    	 md.updateHex(ACUtils.arrayBufferToHexString(ab));
		    	 var hashHex = md.digest();
		    	 var hashA=new Int8Array(parseHexString(hashHex));
		    	 if (i < reps){
		    		 key.set(hashA.slice(0, hashA.length),hashA.length*i);
		    	 }else{
		    		 key.set(hashA.slice(0, (keyLength % hashA.length)),hashA.length*i);
		    	 }
			}
		   console.log("shared key secret calculate");
		   return key;
		}catch(e){
			console.log("Error in concatKDF:"+e);  
			return null;
		};
	},
	
	/**
	 * Method use to import a raw key for AES, key in array buffer format, and obtain a WebCryptoKey
	 * @param keyBA, key in array buffer Format,
	 * @param deferredImport used to retrun error or result.
	 */
	importAESCBCWebcryptoKey:function(keyBA, deferredImport){
		console.log("start import AESkey");
		window.crypto.subtle.importKey(
		    "raw",
		    keyBA,
		    {name: "AES-CBC",},
		    true, 
		    ["encrypt", "decrypt"] 
		).then(function(keyWC){
			deferredImport.resolve(keyWC);
		}).catch(function(err){
		    console.error("import method " + err);
		    deferredImport.reject();
		});
	},

	/**
	 * Method use to import a raw key, in this case the raw key is the SHA-256 hash of the keyBa pass with argument, 
	 * the key is in array buffer format, and obtain a WebCryptoKey.
	 * @param keyBA, key in array buffer Format,
	 * @param deferredImport used to retrun error or result.
	 */
	importHMACKey:function(keyBA, defferedSha256){
		console.log("start import HMACkey");
		window.crypto.subtle.digest("SHA-256", keyBA).then(function (hash) {
			window.crypto.subtle.importKey(
				"raw",
				hash,
				{
					name: "HMAC",
				    hash: {name: "SHA-256"},
				},true,["sign", "verify"] 
			).then(function(keyWC){
				defferedSha256.resolve(keyWC);
			}).catch(function(err){
				    console.error("import method " + err);
				    defferedSha256.reject();
			});
		}).catch(function(err){
			console.error("digest Sha256 " + err);
			defferedSha256.reject();
		});
	},
	// ** UTILS METHOD **//
	
	/**
	 * Convert a int in a ArrayBuffer with length 4
	 * @param num
	 * @return 
	 */
	intToFourBytes:function (num){
		var arr = new ArrayBuffer(4); // an Int32 takes 4 bytes
		var view = new DataView(arr);
		view.setUint32(0, num, false); // byteOffset = 0; litteEndian = false
		// var array = new Int8Array(arr);
		// console.log(num+"->"+array);
		return arr;
	},
		
		
	/**
	 * Utils create a ArrayBuffer concat the string lenght converted in int
	 * to 4 byte and the byte array for the string
	 * 
	 * @param string
	 * @returns ArrayBuffer
	 */
	toArrayBufferWithLenghtPrefix: function (string){
	    var encoder = new TextEncoder("utf-8");
	    var I8Alength=new Int8Array(ACUtils.intToFourBytes(string.length));
	    var I8Avalue=encoder.encode(string);
	    return ACUtils.concat2arrayBuffer(I8Alength.buffer, I8Avalue.buffer)
	},
	
	/**
	 * Concat 2 ArrayBuffer
	 * 
	 * @param b1
	 * @param b2
	 * @returns arraybuffer [b1, b2]
	 */
	concat2arrayBuffer: function (b1, b2){
		var I8Afull= new Int8Array(b1.byteLength +b2.byteLength);
		I8Afull.set(new Int8Array(b1),0);
		I8Afull.set(new Int8Array(b2),b1.byteLength);
		var arrayBuffer=I8Afull.buffer;
		return arrayBuffer;
	},
	
	/**
	 * Convert a array buffer in a hex String
	 * 
	 * @param arrayBuffer
	 * @returns
	 */
	arrayBufferToHexString : function (arrayBuffer) {
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
	},
	
	
	parseHexString : function (str) { 
	    var result = [];
	    while (str.length >= 2) { 
	        result.push(parseInt(str.substring(0, 2), 16));
	        str = str.substring(2, str.length);
	    }

	    return result;
	},
	
	base64urlToArrayBuffer: function (base64u) {
		var b64= b64utob64(base64u);
		return ACUtils.base64ToArrayBuffer(b64);
	},
	
	base64ToArrayBuffer: function (base64) {
	    var binary_string =  window.atob(base64);
	    var len = binary_string.length;
	    var bytes = new Uint8Array( len );
	    for (var i = 0; i < len; i++)        {
	        bytes[i] = binary_string.charCodeAt(i);
	    }
	    return bytes.buffer;
	}
	// ** UTILS METHOD **//
};

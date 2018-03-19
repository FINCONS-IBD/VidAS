/**
 * AbeProxyInterface javascript,
 * @author Diego Pedone (Fincons Group)
 *	---- javascript class used fto write in appending a File in file system and download it ---- 
 * @version 1.0 
 */

function FileUtils(nameFile, sizeFile, type) {
	var instance=null;
	
	if(instance==null){
		if(isChrome()){
			instance=new FileUtilsChrome(nameFile, sizeFile, type);
		}else{
			instance=new FileUtilsGenericBrowser(nameFile, sizeFile, type);
		}
	}
	
	//Interface used to operate on the file
	this.writeNewFile = function(slice, deferred) {
		instance.writeNewFile(slice, deferred);
	}
	
	this.appendFile = function(slice, deferred) {
		instance.appendFile(slice, deferred);
	}
	
	this.download = function() {
		instance.download();
	}
	
	function isChrome(){
		return navigator.userAgent.toLowerCase().indexOf('chrome') > -1;
	}
}

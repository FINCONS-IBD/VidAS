/**
 * AbeProxyInterface javascript,
 * @author Diego Pedone (Fincons Group)
 *	---- javascript class used fto write in appending a File in file system and download it ---- 
 * @version 1.0 
 */


function FileUtilsChrome(nameFile, sizeFile, type)
{
	var currentSlice=null;
	var currentDeferred=null;
	
	window.requestFileSystem = window.requestFileSystem || window.webkitRequestFileSystem;
	//Interface used to operate on the file
	this.writeNewFile= function(slice, deferred){
		currentSlice=slice;
		currentDeferred=deferred;
		window.requestFileSystem(window.TEMPORARY, sizeFile, writeFile, errorHandler);
	}
	this.appendFile= function(slice, deferred){
		currentSlice=slice;
		currentDeferred=deferred;
		window.requestFileSystem(window.TEMPORARY, sizeFile, appendFile, errorHandler);
	}
	this.download= function(){
		window.requestFileSystem(window.TEMPORARY, sizeFile, download, errorHandler);
	}
	
	function errorHandler(e) {
	  var msg = '';
	
	  switch (e.message) {
	    case FileError.QUOTA_EXCEEDED_ERR:
	      msg = 'QUOTA_EXCEEDED_ERR';
	      break;
	    case FileError.NOT_FOUND_ERR:
	      msg = 'NOT_FOUND_ERR';
	      break;
	    case FileError.SECURITY_ERR:
	      msg = 'SECURITY_ERR';
	      break;
	    case FileError.INVALID_MODIFICATION_ERR:
	      msg = 'INVALID_MODIFICATION_ERR';
	      break;
	    case FileError.INVALID_STATE_ERR:
	      msg = 'INVALID_STATE_ERR';
	      break;
	    default:
	      msg = 'Unknown Error';
	      break;
	  };
	
	  console.log('Error: ' + msg);
	}

	function writeFile(fs) {
	  fs.root.getFile(nameFile, {
	    create: true
	  }, function(fileEntry) {
	    // Create a FileWriter object for our FileEntry 
	    fileEntry.createWriter(function(fileWriter) {
	      fileWriter.onwriteend = function(e) {
	    	if(fileWriter.length===0){
	    		// Create a new Blob and write it to nameFile
	    	    var blob = new Blob([new Uint8Array(currentSlice)], {type: type});
	    	    fileWriter.write(blob);
	    	}else{
	    		currentDeferred.resolve();
		        console.log('Write completed.');
	    	}
	      };
	
	      fileWriter.onerror = function(e) {
	    	currentDeferred.reject();
	        console.log('Write failed: ' + e.toString());
	      };
	      //truncate the old file with same nameFile
	      fileWriter.truncate(0);
	    }, errorHandler);
	  }, errorHandler);
	}
	
	function appendFile(fs) {
	
	  fs.root.getFile(nameFile, {
	    create: false
	  }, function(fileEntry) {
	
	    // Create a FileWriter object for our FileEntry (log.txt).
		  fileEntry.createWriter(function(fileWriter) {
	    
	      fileWriter.onwriteend = function(e) {
	    	    currentDeferred.resolve();
		        console.log('Appended completed.');
		      };
	      fileWriter.onerror = function(e) {
	    	currentDeferred.reject();
	        console.log('Write failed: ' + e.toString());
	      };
	
	      fileWriter.seek(fileWriter.length); 
	      // Create a new Blob and write it to log.txt.
	      var blob = new Blob([new Uint8Array(currentSlice)], {
	        type: type
	      });
	
	      fileWriter.write(blob);
	
	    }, errorHandler);
	
	  }, errorHandler);
	
	}

	function download(fs) {
	
	  fs.root.getFile(nameFile, {}, function(fileEntry) {
	
	    // Get a File object representing the file,
	    // then use FileReader to read its contents.
	    fileEntry.file(function(file) {
	    var url = window.URL.createObjectURL(file);
					    	var a = document.createElement('a');
					    	document.body.appendChild(a);
					        a.href = url;
					        a.download =nameFile;
					        a.click();
					        document.body.removeChild(a);
	    }, errorHandler);
	
	  }, errorHandler);
	
	}
}

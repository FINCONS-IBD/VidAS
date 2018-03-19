/**
 * FileUtilsGenericBrowser javascript,
 * 
 * @author Diego Pedone (Fincons Group) ---- javascript class used fto write in
 *         appending a File in file system and download it ----
 * @version 1.0
 */
function FileUtilsGenericBrowser(nameFile, sizeFile, type) {
	var currentSlice = null;
	var currentDeferred = null;
	var myblob = null;
	// Interface used to operate on the file
	this.writeNewFile = function(slice, deferred) {
		debugger;
		currentDeferred = deferred;
		try{
			myblob = new MyBlobBuilder();
			myblob.append(slice)
			currentDeferred.resolve();
		}catch(err){
			console.err(err);
			currentDeferred.reject();
		}
	}

	this.appendFile = function(slice, deferred) {
		try{
			currentDeferred = deferred;
			myblob.append(slice);
			currentDeferred.resolve();
		}catch(err){
			console.err(err);
			currentDeferred.reject();
		}
	}

	this.download = function() {
		try{
			var url = window.URL.createObjectURL(myblob.getBlob(type));
			var a = document.createElement('a');
			document.body.appendChild(a);
			a.href = url;
			a.download = nameFile;
			a.click();
			document.body.removeChild(a);
		}catch(err){
			console.err(err);
			currentDeferred.reject();
		}
	}
}

var MyBlobBuilder = function() {
	this.parts = [];
}

MyBlobBuilder.prototype.append = function(part) {
	this.parts.push(part);
	this.blob = undefined; // Invalidate the blob
};

MyBlobBuilder.prototype.getBlob = function(blobtype) {
	if (!this.blob) {
		this.blob = new Blob(this.parts, {
			type : blobtype
		});
	}
	return this.blob;
};


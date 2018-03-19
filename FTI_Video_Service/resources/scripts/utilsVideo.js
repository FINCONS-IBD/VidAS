var manageVideoFlag;
var user;
/**
 * Show a single video into the tree
 * 
 * @param url
 * @param viewer
 * @returns
 */
function showVideo(url, viewer) {
// url = url.split(".")[0];
	$(viewer).val(url);
	
// var sJWT = generateRequestToken("GET", "PSY_VideoSService", url, username);
// tokenGetVideo = requestToken(null, sJWT, null);
// var deferedRequest = $.Deferred();
// executeService(tokenGetVideo, deferedRequest, null);
// $.when(deferedRequest).done(function(response) {
// alert("OK la risposta"+ response)
// });
}

/**
 * Show a directory into the tree
 * 
 * @param url
 * @param idDiv
 * @param tree
 * @param viewer
 * @param username
 * @param service
 * @returns
 */
function showDirVideo(url, idDiv, tree, viewer, username, service) {
	$(viewer).val("");
	var sJWT = generateRequestToken("GET", service, url, username);
	tokenGetVideo = requestToken(null, sJWT, null);
	var sonList = $(tree).jstree().get_children_dom($("#" + idDiv));
	if(sonList!=false){
		$.each(sonList, function(i, item) {
			$(tree).jstree().delete_node(item);
		});
	}
	var deferedRequest = $.Deferred();
	executeService(tokenGetVideo, deferedRequest, null);
	$.when(deferedRequest).done(function(response) {
		if (response.code == 200) {
			var listDir = response['listVideo'];
			$.each(listDir, function(i, item) {
				addVideoOrDir(item, idDiv, tree, viewer, username, service);
			});
		}
	});
}

/**
 * Add a directory or a single video into the tree
 * 
 * @param item
 * @param parent
 * @param tree
 * @param viewer
 * @param username
 * @param service
 * @returns
 */
function addVideoOrDir(item, parent, tree, viewer, username, service) {
	var method = "";
	var id = "";
	var type = "";
	var text = "";
	var timestampFile = "";
	if (item.URLDir != null) {
		method = "showDirVideo('" + item.URLDir + "', '" + stob64u(item.URLDir) + "', '"
				+ tree + "', '" + viewer + "', '" + username + "', '" + service+ "', '" + manageVideoFlag +  "');";
		id = stob64u(item.URLDir);
		type = "folder";
		text = item.nameVideo;
	} else {
		method = "showVideo('" + item.URLVideo + "', '"+ viewer + "');";
		id = stob64u(item.URLVideo);
		type = "video";
		timestampFile = item.nameVideo.slice(0,item.nameVideo.indexOf("_"));
		text = item.nameVideo.slice(item.nameVideo.indexOf("_")+1, item.nameVideo.length) + '<label class="subLabel"> - (' + timestampFile.split("_").join("") + ")</label>";
		if(manageVideoFlag == true){
			text = text + "<i class=\"fa fa-trash  tblue\" onClick=\"deleteVideoConfirm('"
			+ stob64u(item.URLVideo) + "')\">  </i>";
		}
	}
	if (parent != null) {
		if (manageVideoFlag == true) {
			text = text;
		}
		parent = "#" + parent;
	}
	var node = {
		id : id, // required
		parent : parent,// required
		text : text,// node text
		type: type,
		state : {
			opened : true
		},
		a_attr : {
			"onclick" : method
		}
	};

	if (parent == null) {
		$(tree).jstree("create_node", null, node, "last", false);
	} else {
		$(tree).jstree("create_node", $(parent), node, "last", false);
	}
}

/**
 * Dispose the tree of the directories that contains the videos or the single
 * videos
 * 
 * @param treeContainer
 * @param tree
 * @param viewer
 * @param username
 * @param service
 * @param tab
 * @returns
 */
function prepareTreeVideo(treeContainer, tree, viewer, username, service, tab, manageVideo) {
	$("#listEmpty").remove();
	manageVideoFlag = manageVideo;
	user = username;
	var tokenGetVideo= sessionStorage.getItem(tab+"Token");
	if (tokenGetVideo == null || !validateTimeToken(tokenGetVideo)) {
		sJWT = generateRequestToken("GET", service, "",username);
		tokenGetVideo = requestToken(tab, sJWT, null);
	}
	$(tree).remove();
	var div = $('<div id="' + tree.replace('#', '') + '" ></div>');
	div.appendTo(treeContainer);
	var deferedRequest = $.Deferred();
	executeService(tokenGetVideo, deferedRequest, null);
	$.when(deferedRequest).done(function(response) {
		if (response.code == 200) {
			var listDir = response['listVideo'];
			$(tree).jstree({
				"core" : {
					"check_callback" : true,
				},"types" : {
					"folder" : {
						"icon" : "tblue fa fa-folder"
					},
					"video" : {
						"icon" : "tblue fa fa-video-camera"
					},
					"default" : {}
				},
				"plugins" : [ "unique", "themes", "types" ]
			});
			$.each(listDir, function(i, item) {
				addVideoOrDir(item, null, tree, viewer, username, service, manageVideoFlag);
			});
			var json = $(tree).jstree('get_json');
			if(JSON.stringify(json) == JSON.stringify([])){
				$("<label id='listEmpty' class='error fieldLabel'>List Empty</label>").appendTo(treeContainer);
			}
		}else{
			$(viewer).val("Error");
		}
		$(tree).change();
	}).fail(function(){
		$(viewer).val("Error");
		$(tree).change();
	});

}

function getJsonFromFile(file, deferred){
 	var reader = new FileReader();
 	reader.onload = function(){
// alert(JSON.parse(reader.result));
 		try{
 		console.log(JSON.parse(reader.result));
 	 	 deferred.resolve(JSON.parse(reader.result));
 		}catch(err){
 			deferred.reject("Error in parsing. JSON Invalid");
 		}
    };
    reader.readAsText(file); 
}

function checkJsonFile(json, fileList){
	var flagcheck= false;
	var listField=["filename", "date", "firstTimestamp", "lastTimestamp", "flightNumber", "tailNumber", "cameraName", "port", "cameraType"];
	try{
	json.forEach(function(elem) {
       	console.log(elem);
       	if(!elem.hasOwnProperty("CameraInfo")){
       		console.log("checkJsonFile False1");
       		return false;
       	}
       	var camInfo=elem.CameraInfo;
      		for (var key of listField) {
      		    if (!camInfo.hasOwnProperty(key)) {
           		console.log("checkJsonFile False2");
      		        return false;
      		    }
      		}
      		fileName=camInfo.filename;
      		console.log(fileName+" "+fileName==fileList[0].name+" "+ fileList[0].name);
        for (file of fileList){
        	console.log(file.name);
        	if(fileName==file.name){
        		console.log("checkJsonFile true");
        		flagcheck = true;
        		return flagcheck;
        	}
        }
	});
	console.log("checkJsonFile End");
	}catch(err){
		console.log("error in jsonFile reading");
		return false;
	}
    return flagcheck;
 }

function getMetadataFromJsonFile(json, nameFile){
	var myMetadata={};
	try{
		json.forEach(function(elem) {
			if(elem.CameraInfo.filename==nameFile){
				myMetadata=elem.CameraInfo;
				console.log("find");
				return;
			}
		});
		console.log("getMetadataFromJsonFile START", myMetadata);
		var metadata=[{
			"name":"path_file",
	  		"value": myMetadata.date+"/"+myMetadata.flightNumber+"/"+myMetadata.cameraType+"/"+myMetadata.cameraName
		},];
		console.log("getMetadataFromJsonFile" ,metadata);
		for (var key of Object.keys(myMetadata)) {
			var obj= {"name": key, "value":myMetadata[key]};
			metadata.push(obj);
		}
	}catch(err){
		console.log("error in jsonFile reading");
		return null;
	}
	console.log("getMetadataFromJsonFile END " ,metadata);
 	return metadata;
}

function deleteVideoConfirm(id) {
	var idNode = id;
	var dialog;
	
	function deleteItem() {
		if (idNode != null && idNode != "") {
			$(".error").hide();
			$(".success").hide();
			var sJWT = generateRequestToken("DELETE", videoService, b64utos(idNode), user);
			requestToken("deleteElement", sJWT, null);
			var tokenDeleteElem = sessionStorage.getItem("deleteElementToken");
			var deferedRequest = $.Deferred();
			if (tokenDeleteElem == null) {
				$("#errorDeleteVideoToken").show();
				return;
			}
			executeService(tokenDeleteElem, deferedRequest, null);
			$.when(deferedRequest).done(function(response) {
				if (response.code == 200) {
					$("#successDeleteVideo").show();
					$.unblockUI();
				} else {
					$("#failDeleteVideo").show();
					$.unblockUI();
				}
				dialog.dialog("close");
			}).fail(function() {
				$("#failDeleteVideo").show();
				$.unblockUI();
				dialog.dialog("close");
			});
		}
	}
	
	dialog = $("#dialog-confirm").dialog({
		resizable : false,
		height : "auto",
		width : 400,
		modal : true,
		buttons : {
			"Delete item" : deleteItem,
			Cancel : function() {
				dialog.dialog("close");
			}
		}
	});
	dialog.dialog("open");
	var evt = window.event || arguments.callee.caller.arguments[0];
	evt.stopPropagation();
}

// function deleteVideoConfirm(id){
// dialog = $( "#dialog-confirm" ).dialog({
// resizable: false,
// height: "auto",
// width: 400,
// modal: true,
// buttons: {
// "Delete all items": deleteVideo,
// Cancel: function() {
// $( this ).dialog( "close" );
// }
// }
// });
//	
// function deleteVideo(){
// var sJWT = generateRequestToken("GET", videoService, b64utos(id),username);
// var tokenDeleteVideo = requestToken(null, sJWT, null);
// if (tokenDeleteVideo == null) {
// $("#errorDeleteVideoToken").show();
// return;
// }
// var deleteVideoDeff = $.Deferred();
// executeService(tokenDeleteVideo, deleteVideoDeff, null);
// $.when
//		
// }
// }

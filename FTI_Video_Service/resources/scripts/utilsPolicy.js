	var UtilsPolicy= function(config){
	
	UtilsPolicy.instance=this;
	var treeContainer= config.treeContainer;
	var tree= config.tree;
	var treeSpinner = config.treeSpinner;
	var workspace= config.workspace;
	var viewer= config.view;
	var username= config.username;
	var service= config.service;
	var modifyTreeFlag= config.modifyTreeFlag;
	var namePolicyText = config.namePolicyText;
	var confirmButton = config.confirmButton;
	
/**
* 
* 
* Show the value of the policy into the viewer
 * 
 * @param url
 *            Path of the policy file
 * @param viewer
 *            The text-box where appears the policy selected
 * @param username
 *            The current logged user
 * @param service
 *            The name of the service used to generate the request token
 */
	this.showPolicy=function (url, idDiv) {
		$("#errorFolderEmpty").hide();
		$("#successPolicy").hide();
		var sJWT = generateRequestToken("GET", service, url, username);
		tokenGetPolicies = requestToken(null, sJWT, null);
		var deferedRequest = $.Deferred();
		var input = {
			type : "policy",
			id : idDiv.split("_").join(":")
		};
		executeService(tokenGetPolicies, deferedRequest, null);
		$.when(deferedRequest).done(function(response) {
			if (response.code == 200) {
	//			$(viewer).val(response.policy); USE THE WORKSPACE TO SEE THE POLICY VALUE
				var genericWS = Blockly.Workspace.getById(workspace);
				genericWS.clear();
				var xml = Blockly.Xml.textToDom(response.xmlPolicy);
				Blockly.Xml.domToWorkspace(xml, genericWS);
				$(viewer).val(response.policy);
				xml_policy = response.xmlPolicy;
			} else {
				$(viewer).val("Error retrieving policy");
			}
		});
	}

/**
 * Expand the directory showing its content
 * 
 * @param url
 *            ID of the directory in Database
 * @param idDiv
 *            Same value of @param url
 * @param tree
 *            The tree defined in the HTML code
 * @param viewer
 *            The text-box where appears the policy selected
 * @param username
 *            The current logged user
 * @param service
 *            The name of the service used to generate the request token
 */
this.showDirPolicy=function(url, idDiv) {//, tree, workspace, viewer, username, service) {
//	$(viewer).val("");
	$("#successPolicy").hide();
	$("#errorFolderEmpty").hide();
	$("#successFolder").hide();
	var sJWT = generateRequestToken("GET", service, url, username);
	tokenGetPolicies = requestToken(null, sJWT, null);
	var sonList = $(tree).jstree().get_children_dom($("#" + idDiv));
	if (sonList != false) {
		$.each(sonList, function(i, item) {
			$(tree).jstree().delete_node(item);
		});
	}
	var deferedRequest = $.Deferred();
	var input = {
		type : "directory",
	};

	executeService(tokenGetPolicies, deferedRequest, null);
	$.when(deferedRequest).done(
			function(response) {
				if (response.code == 200) {
					var listDir = response['listPolicy'];
					if(listDir.length == 0){
						$("#errorFolderEmpty").show();
					} else{
					$.each(listDir, function(i, item) {
						$("#errorFolderEmpty").hide();
						UtilsPolicy.instance.addPolicyOrDir(item, idDiv);
					});}
				}
			});
}

/**
 * Add a directory or a single policy to the tree
 * 
 * @param item
 *            The element of the tree
 * @param parent
 *            The parent node of the @param item
 * @param tree
 *            The tree defined in the HTML code
 * @param viewer
 *            The text-box where appears the policy selected
 * @param username
 *            The current logged user
 * @param service
 *            The name of the service used to generate the request token
 * @param modifyTreeFlag
 *            A boolean flag. If it's "true" there are some button icons in
 *            every node of the tree ("+" for a directory, "pencil" for a
 *            policy) used to manage the tree structure. You can create new
 *            directories or update a policy.
 */
 this.addPolicyOrDir= function(item, parent) {
	$("#errorFolderSelected").hide();
	var method = "";
	var id = "";
	var type = "";
	var text = "";
	if (item.URLDir != null) {
		method = "UtilsPolicy.instance."+"showDirPolicy('" + item.URLDir + "', '" + stob64u(item.URLDir)
				//+ "', '" + config 
				+ "');";
		id = stob64u(item.URLDir);
		type = "folder";
		text = item.namePolicy;
		if (modifyTreeFlag == true) {
			text = text
					+ "<i class=\"fa fa-plus  tblue\" onClick=\"UtilsPolicy.instance.createFolderDialog('"
					+ stob64u(item.URLDir) + "')\">  </i>";// node
			// text
		}
	} else {
		method = "UtilsPolicy.instance."+"showPolicy('" + item.URLPolicy + "', '" + stob64u(item.URLPolicy)
			//	+ "', '" + config
				+ "');";
		id = stob64u(item.URLPolicy);
		type = "policy";
		text = item.namePolicy;
		if (modifyTreeFlag == true) {
			text = text
					+ "<i class='fa fa-pencil  tblue' onClick=\"UtilsPolicy.instance.updatePolicy('"
					+ item.URLPolicy + "', '" + stob64u(item.URLPolicy) + "', '"
					//+ config + "', '" 
					+ item.namePolicy + "')\">  </i>";// node
			xml_policy = item.xmlPolicy;
		}
	}
	if (parent != null) {
		if (modifyTreeFlag == true) {
			text = text;
		}
		parent = "#" + parent;
	}
	if (id != "") {
		var node = {
			id : id, // required
			parent : parent,// required
			text : text,
			type : type,
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
}


/**
 * Dispose the tree of the directories that contains the policies or the single
 * policies
 * 
 * @param treeContainer
 * @param tree
 *            The tree defined in the html code
 * @param viewer
 *            The text-box where appears the policy selected
 * @param username
 *            The current logged user
 * @param service
 *            The name of the service used to generate the request token
 */
 this.prepareTreePolicy=function() {
	$("#listEmpty").remove();
	var tokenGetPolicies = sessionStorage.getItem("viewPoliciesToken");
	if (tokenGetPolicies == null || !validateTimeToken(tokenGetPolicies)) {
		sJWT = generateRequestToken("GET", service, "", username);
		tokenGetPolicies = requestToken("viewPolicies", sJWT, null);
	}
	$(tree).remove();
	var div = $('<div id="' + tree.replace('#', '') + '" ></div>');
	div.appendTo(treeContainer);
	var deferedRequest = $.Deferred();
	$(tree).jstree({
		"core" : {
			"check_callback" : true,
		},
		"types" : {
			"folder" : {
				"icon" : "tblue fa fa-folder"
			},
			"policy" : {
				"icon" : "tblue fa fa-file"
			},
			"default" : {}
		},
		"plugins" : [ "unique", "themes", "types" ]
	}).bind("deselect_all.jstree", function(evt, data) {
//        var nodeText = data.rslt.obj.find("a:first").text();
//        $("#myAlert").append("Deselected: "+nodeText+"<br>");
    });
	var payloadObj = KJUR.jws.JWS.readSafeJSONString(b64utoutf8(tokenGetPolicies.split(".")[1]));
	if(payloadObj['action'].url != ""){
		var nodeOrganization = {
				"namePolicy": payloadObj['action'].url,
				"URLDir": payloadObj['action'].url
		};
		UtilsPolicy.instance.addPolicyOrDir(nodeOrganization, null);

		 $(treeSpinner).hide();
	} else{
	executeService(tokenGetPolicies, deferedRequest, null);
	$
			.when(deferedRequest)
			.done(
					function(response) {
						if (response.code == 200) {
							var listDir = response['listPolicy'];
							if(listDir.length == 0){
								$("#errorFolderEmpty").show();
							} else{
								$.each(listDir, function(i, item) {
									$("#errorFolderEmpty").hide();
									UtilsPolicy.instance.addPolicyOrDir(item, null);

								});
							}
							
							var json = $(tree).jstree('get_json');
							if (JSON.stringify(json) == JSON.stringify([])) {
								$(
										"<label id='listEmpty' class='error fieldLabel'>List Empty</label>")
										.appendTo(treeContainer);
							}
						}else{
							$(viewer).val("Error");
						}
						 $(treeSpinner).hide();
					}).fail(function(){
						$(viewer).val("Error");
						$(treeSpinner).hide();
					});
	}
}

/**
 * dialog window with a text-box used to create a new directory.
 * 
 * @param idDiv
 *            ID of the directory that will contain the new directory
 * @returns
 */
this.createFolderDialog=function(idNode) {
	var dialog, form,

	name = $("#nameFolder"), allFields = $([]).add(name), tips = $(".validateTips");
	
	/*
	 * Updates the error section of the createFolderDialog window
	 */
	function updateTips(t) {
		tips.text(t)
	}
	
	/**
	 * Checks the length of the string parameter
	 * 
	 * @param o
	 *            The value to check
	 * @param n
	 *            The type of the value (What does the value refer to)
	 * @param min
	 * 		The minimum length of @param o
	 * @param max
	 * 		The maximum length of @param o
	 * @return boolean 
	 * 		true = length of @param o is less than max and greater than minimum, false = otherwise 
	 */
	function checkLength(o, n, min, max) {
		if (o.val().length > max || o.val().length < min) {
			o.addClass("ui-state-error");
			updateTips("Length of " + n + " must be between " + min + " and "
					+ max + ".");
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Checks the value of the string parameter
	 * @param o 
	 * 		The value to check
	 * @param regexp
	 * 		The regular expression to respect
	 * @param n
	 * 		The type of the value (What does the value refer to)
	 * @return boolean 
	 * 		true = the value of @param o respect the @param regexp, false = otherwise 
	 */
	function checkRegexp(o, regexp, n) {
		if (!(regexp.test(o.val()))) {
			o.addClass("ui-state-error");
			updateTips(n);
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Starts the validation. After a correct validation, calls the function
	 * that creates a new folder.
	 */
	function validateAndCreate() {
		var valid = true;
		allFields.removeClass("ui-state-error");

		valid = valid && checkLength(name, "username", 3, 16);

		valid = valid
				&& checkRegexp(
						name,
						/^[a-z]([a-z0-9\s_-])+$/i,
						"Fodler name may consist of a-z, 0-9, underscores, hyphens, spaces and must begin with a letter.");

		if (valid) {
			UtilsPolicy.instance.createNewFolder(name.val(), idNode);
			dialog.dialog("close");
		}
	}

	dialog = $("#dialog-form").dialog({
		autoOpen : false,
		height : 400,
		width : 350,
		modal : true,
		buttons : {
			"Create folder" : validateAndCreate,
			Cancel : function() {
				dialog.dialog("close");
			}
		},
		close : function() {
			form[0].reset();
			allFields.removeClass("ui-state-error");
		}
	});
	$("#successFolder").hide();
	$("#errorFolderEmpty").hide();
	dialog.dialog("open");
	var evt = window.event || arguments.callee.caller.arguments[0];
	evt.stopPropagation();
	form = dialog.find("form").on("submit", function(event) {
		event.preventDefault();
		validateAndCreate();
	});

}

/**
 * Creates a new folder, calling the Policy Storage Service
 * 
 * @param nameFolder
 * 			The name of the folder
 * @param fatherNode
 * 			The ID of the father node
 */
 this.createNewFolder = function(nameFolder, fatherNode) {
	$("#failPolicy").hide();
	if (fatherNode != null && fatherNode != "" && nameFolder != null
			&& nameFolder != "") {
		$("#errorCreatePolicyToken").hide();
		$("#successPolicy").hide();
		var sJWT = generateRequestToken("POST", policyService, b64utos(fatherNode) + "/"
				+ nameFolder, username);
		requestToken("createPolicies", sJWT, null);
		var tokenGetPolicies = sessionStorage.getItem("createPoliciesToken");
		var deferedRequest = $.Deferred();
		if (tokenGetPolicies == null) {
			$("#errorCreatePolicyToken").show();
			return;
		}
		executeService(tokenGetPolicies, deferedRequest, JSON.stringify({
			'username' : username,
			'type' : 'Directory'
		}));
		$.when(deferedRequest).done(function(response) {
			if (response.code == 201) {
				$("#successFolder").show();
				$.unblockUI();
			} else {
				$("#failPolicy").show();
				$.unblockUI();
			}
		}).fail(function() {
			$("#failPolicy").show();
			$.unblockUI();
		});
	}
}



/**
 * Updates the value of a policy.
 * 
 * @param url
 *            The ID of the policy in the Database
 * @param idDiv
 *            The same value of
 * @param url
 * @param viewer
 *            The text-box where the policy value appears
 * @param username
 *            The current logged user
 * @param service
 *            The name of the service used to create the request token
 * @param namePolicy
 *            The name of the policy to update
 */
this.updatePolicy=function(url, idDiv, namePolicy) {
	$(tree).jstree(true).deselect_all(true);
	$(tree).jstree('select_node', idDiv);
	var itemClicked = $(tree).jstree("get_selected", true);
	$(tree).data("isSelected", "true");
	var genericWS = Blockly.Workspace.getById(workspace);
	genericWS.clear();
	UtilsPolicy.instance.showPolicy(url, idDiv);
	$(namePolicyText).val(namePolicy);
	var evt = window.event || arguments.callee.caller.arguments[0];
	evt.stopPropagation();
	$(confirmButton).val("Update Policy");
}

 
// function deleteDialog(idDiv, tree) {
// var itemClicked = $(tree).jstree().get_node("#" + idDiv);
// var idNode = itemClicked.id;
// var dialog;
//
// function deleteItem() {
// if (idNode != null && idNode != "") {
// $("#errorCreatePolicyToken").hide();
// var sJWT = generateRequestToken("DELETE", policyService, idNode, username);
// requestToken("deleteElement", sJWT, null);
// var tokenDeleteElem = sessionStorage
// .getItem("deleteElementToken");
// var deferedRequest = $.Deferred();
// if (tokenGetPolicies == null) {
// $("#errorCreatePolicyToken").show();
// return;
// }
// executeService(tokenDeleteElem, deferedRequest, null);
// $.when(deferedRequest).done(function(response) {
// if (response.code == 200) {
// $("#deletedFolder").show();
// $.unblockUI();
// } else {
// $("#failPolicy").show();
// $.unblockUI();
// }
// dialog.dialog("close");
// }).fail(function() {
// $("#failPolicy").show();
// $.unblockUI();
// });
// }
// }
//
// dialog = $("#dialog-confirm").dialog({
// resizable : false,
// height : "auto",
// width : 400,
// modal : true,
// buttons : {
// "Delete item" : deleteItem,
// Cancel : function() {
// dialog.dialog("close");
// }
// }
// });
// }
}
UtilsPolicy.instance=null;
UtilsPolicy.initBlockly=function(toolbox){
	for(var[key, value] of blocklyMap) {
		
		Blockly.Blocks[key] = {
			name: key,
			init: function() {
				var object = blocklyMap.get(this.name);
				if(object.options.length > 0){
					var obj = [];
					for(var elem of object.options){
						var toInsert = elem;
						obj.push([toInsert, toInsert]);
					}
					this.appendDummyInput().appendField(this.name).appendField(new Blockly.FieldDropdown(obj), "Value");
					
				}
				else {
					this.appendDummyInput().appendField(this.name).appendField(new Blockly.FieldTextInput("valueAttribute"), "Value");
				}
				
				this.setPreviousStatement(true, "");
				this.setNextStatement(true, "");
				this.setColour("#0055B2");
				this.setTooltip('');
			}
		};
		
		Blockly.JavaScript[key] = function(block) {
			console.log(block);
			var obj_value= blocklyMap.get(block.name);
			var text_value = block.getFieldValue('Value');
			var code = obj_value.ldapName + ":" + text_value.split(" ").join("_") + " *$*";
			console.log("code", code);
			return code;
		};
	}
	
	
	
	
	Blockly.Blocks['and_condition'] = {
		init: function() {
			this.appendDummyInput()
			    .appendField("AND");
			this.appendStatementInput("CONDITION")
			    .setCheck("");
			this.setPreviousStatement(true, "");
			this.setNextStatement(true, "");
			this.setColour("#009900");
			this.setTooltip('');
		}
	};
	
	Blockly.Blocks['or_condition'] = {
		init: function() {
			this.appendDummyInput()
			   .appendField("OR");
			this.appendStatementInput("CONDITION")
			    .setCheck("");
			this.setPreviousStatement(true, "");
			this.setNextStatement(true, "");
			this.setColour("#E69829");
			this.setTooltip('');
		}
	};
	Blockly.JavaScript['or_condition'] = function(block) {
		var statements_name = Blockly.JavaScript.statementToCode(block, 'CONDITION');
		var code = statements_name;
		code= code.split("*$*").join("OR ");
		code= "("+code.substring(0, code.length-3).trim() +") *$*";
		return code;
	};
	Blockly.JavaScript['and_condition'] = function(block) {
		var statements_name = Blockly.JavaScript.statementToCode(block, 'CONDITION');
		var code = statements_name;
		code= code.split("*$*").join("AND ");
		code= "("+code.substring(0, code.length-4).trim() +") *$*";
		return code;
	};
	
		for(var[key, value] of blocklyMap){
		$(toolbox).show();
		$(toolbox).append('<block type="' + key + '"></block>');
	};
	
}
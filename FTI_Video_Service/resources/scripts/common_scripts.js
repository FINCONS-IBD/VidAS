/**
 * Logout from the system
 * 
 * @returns
 */
function logoutUser(){
	sessionStorage.clear();
	sessionStorage.setItem('sessionTimeoutError', true);
	window.location.href = 'document';
}

/**
 * Object with a list of services in Upload video tab
 */
var Util = {
	servicesList : [],
	abeConfig : {},
	blocklyDefaultConfig : {
		collapse : true,
		disable : false,
		readOnly : false,
		sounds : false,
		toolbox : null,
		zoom : {
			controls : true,
			wheel : true,
			startScale : 0.7,
			maxScale : 1.5,
			minScale : 0.3,
			scaleSpeed : 1.2
		}
	}
};

/**
 * Map object that contains pairs [key, value]. Key: the name of the attribute
 * in Blockly Value: an object with a list of options and a code that represents
 * the LDAPname of the attrubute
 */
var blocklyMap = new Map();

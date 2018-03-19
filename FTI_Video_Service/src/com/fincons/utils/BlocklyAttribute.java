package com.fincons.utils;

import java.util.List;

public class BlocklyAttribute {
	
	private String blocklyName;
	private String ldapName;
	private List<String> options;
	
	public BlocklyAttribute(){}
	
	public BlocklyAttribute(String blocklyName, String ldapName) {
		super();
		this.blocklyName = blocklyName;
		this.ldapName = ldapName;
		this.options = null;
	}
	
	public BlocklyAttribute(String blocklyName, String ldapName, List<String> options) {
		super();
		this.blocklyName = blocklyName;
		this.ldapName = ldapName;
		this.options = options;
	}
	
	public String getBlocklyName() {
		return blocklyName;
	}
	public void setBlocklyName(String blocklyName) {
		this.blocklyName = blocklyName;
	}
	public String getLdapName() {
		return ldapName;
	}
	public void setLdapName(String ldapName) {
		this.ldapName = ldapName;
	}
	public List<String> getOptions() {
		return options;
	}
	public void setOptions(List<String> options) {
		this.options = options;
	}
	
	
}

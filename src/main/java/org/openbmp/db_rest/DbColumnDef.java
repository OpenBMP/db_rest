package org.openbmp.db_rest;

import java.math.BigInteger;
import java.sql.Types;

/**
 * Database column definition.  This is used to represent a column result.
 */
public class DbColumnDef {
	private int     type;				// Column type - java.sql.Types
	private String  name;				// Column Name
	private String  tableName;			// Column table name
	private String  value;				// Value in string format
	
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public String getName() {
		return name;
	}
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
}

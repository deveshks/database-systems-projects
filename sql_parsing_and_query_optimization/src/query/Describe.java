package query;

import java.util.HashMap;

import global.Minibase;
import parser.AST_Describe;
import relop.Schema;

/**
 * Execution plan for describing tables.
 */
class Describe implements Plan {

	/**
	 * Optimizes the plan, given the parsed query.
	 * 
	 * @throws QueryException if table doesn't exist
	 */
	protected String table_name;
	protected HashMap <Integer, String> typemap;

	public Describe(AST_Describe tree) throws QueryException {
		table_name = tree.getFileName();
		QueryCheck.tableExists(table_name);
		typemap = new HashMap <Integer, String>();
		typemap.put(11, "INTEGER");
		typemap.put(12, "FLOAT");
		typemap.put(13, "STRING");
	} // public Describe(AST_Describe tree) throws QueryException

	/**
	 * Executes the plan and prints applicable output.
	 */
	public void execute() {
		Schema table_schema = Minibase.SystemCatalog.getSchema(table_name);
		IndexDesc []idesc = Minibase.SystemCatalog.getIndexes(table_name);

		// print description of table:
		System.out.println ("Table Name = " + table_name);
		int nfields = table_schema.getCount();
		System.out.println ("ColumnName --- ColumnType");
		for(int field = 0; field < nfields; field++){
			System.out.println (table_schema.fieldName(field) + "    " + typemap.get(table_schema.fieldType(field)));
		}
		if (idesc.length > 0)
			System.out.println ("Index Name ---  Column Name");

		for (IndexDesc id : idesc){
			System.out.println (id.indexName + "     " + id.columnName);
		}
		System.out.println("# records = "+ Minibase.SystemCatalog.getTableCardinality(table_name));
	
	} // public void execute()

} // class Describe implements Plan
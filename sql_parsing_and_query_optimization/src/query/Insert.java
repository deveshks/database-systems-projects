package query;

import index.HashIndex;
import global.Minibase;
import global.RID;
import global.SearchKey;
import heap.HeapFile;
import parser.AST_Insert;
import relop.Schema;
import relop.Tuple;

/**
 * Execution plan for inserting tuples.
 */
class Insert implements Plan {

	public String tablename;
	public Schema schema;
	public Object[] values;
	
  /**
   * Optimizes the plan, given the parsed query.
   * 
   * @throws QueryException if table doesn't exists or values are invalid
   */
  public Insert(AST_Insert tree) throws QueryException {
	  
	  tablename = tree.getFileName();
	  QueryCheck.tableExists(tablename);
	  schema = Minibase.SystemCatalog.getSchema(tablename);
	  values = tree.getValues();
	  QueryCheck.insertValues(schema, values);

  } 

  /**
   * Executes the plan and prints applicable output.
   */
  public void execute() {
	  
	  HeapFile hfile = new HeapFile(tablename);
	  RID newrid = hfile.insertRecord(new Tuple(schema,values).getData());
	  
	  IndexDesc[] indexes;
	  indexes = Minibase.SystemCatalog.getIndexes(tablename);
	  
	  for(int i = 0; i < indexes.length; i++){
		  HashIndex hashind = new HashIndex(indexes[i].indexName);
		  hashind.insertEntry(new SearchKey(values[schema.fieldNumber(indexes[i].columnName)]), newrid);
	  }
	  
	  Minibase.SystemCatalog.incrementTableCardinality(tablename, 1);

    System.out.println("1 rows inserted");
    

  } 

} 

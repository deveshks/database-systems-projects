package query;

import heap.HeapFile;
import heap.HeapScan;
import index.HashIndex;
import global.Minibase;
import global.RID;
import global.SearchKey;
import parser.AST_CreateIndex;
import relop.Schema;
import relop.Tuple;

/**
 * Execution plan for creating indexes.
 */
class CreateIndex implements Plan {
	
	public String indexname;
	public String tablename;
	public String columnname;
	public Schema schema;
	public HashIndex hashind;
	
  /**
   * Optimizes the plan, given the parsed query.
   * 
   * @throws QueryException if index already exists or table/column invalid
   */
  public CreateIndex(AST_CreateIndex tree) throws QueryException {
	  indexname = tree.getFileName();
	  tablename = tree.getIxTable();
	  schema = Minibase.SystemCatalog.getSchema(tablename);
	  columnname = tree.getIxColumn();
	  
	  QueryCheck.fileNotExists(indexname);
	  QueryCheck.tableExists(tablename);
	  QueryCheck.columnExists(schema, columnname);
	  
	  hashind = new HashIndex(indexname);   // Is empty right now, since it doesn't exist

  }

  /**
   * Executes the plan and prints applicable output.
   */
  public void execute() {
	  
	  Minibase.SystemCatalog.createIndex(indexname, tablename, columnname);
	  HeapScan scan = new HeapFile(tablename).openScan();
	  RID temprid = new RID();
	  
	  while(scan.hasNext()){
		  byte[] b = scan.getNext(temprid);
		  hashind.insertEntry(new SearchKey(new Tuple(schema,b).getField(columnname)), temprid);
	  }
	  
	  scan.close();
	  System.out.println("Index Created for "+tablename+" for Column "+columnname);
	 
  }

}

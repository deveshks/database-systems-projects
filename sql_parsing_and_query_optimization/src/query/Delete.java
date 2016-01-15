package query;

import index.HashIndex;

import javax.management.Query;

import org.omg.CORBA.PUBLIC_MEMBER;

import global.Minibase;
import global.RID;
import global.SearchKey;
import heap.HeapFile;
import heap.HeapScan;
import parser.AST_Delete;
import relop.Predicate;
import relop.Schema;
import relop.Tuple;

/**
 * Execution plan for deleting tuples.
 */
class Delete implements Plan {
	public String tablename;
	public Predicate[][] predicates;
	public Schema schema;

  /**
   * Optimizes the plan, given the parsed query.
   * 
   * @throws QueryException if table doesn't exist or predicates are invalid
   */
  public Delete(AST_Delete tree) throws QueryException {
	  tablename = tree.getFileName();
	  predicates = tree.getPredicates();
	  QueryCheck.tableExists(tablename);
	  schema = Minibase.SystemCatalog.getSchema(tablename);
	  QueryCheck.predicates(schema, predicates);

  } 

  /**
   * Executes the plan and prints applicable output.
   */
  public void execute() {
	  
	  HeapFile hfile = new HeapFile(tablename);
	  HeapScan scan = hfile.openScan();
	  RID temprid = new RID();
	  byte[] data;
	  Tuple temptuple;
	  IndexDesc[] indexes = Minibase.SystemCatalog.getIndexes(tablename);
	  int numberdeleted = 0;
	  
	  while(scan.hasNext()){
		  data = scan.getNext(temprid);
		  temptuple = new Tuple(schema,data);
		  boolean sat = false;
		  if(predicates.length == 0){
			  sat = true;
		  }
		  for(int i = 0; i < predicates.length; i++){
			  for(int j = 0; j < predicates[i].length; j++){
				  if(predicates[i][j].evaluate(temptuple)){
					  sat = true;
					  break;
				  }
			  }
			  if(!sat){
				  break;
			  }
		  }
		  
		  if(sat){
			  numberdeleted++;
			  
			  for(int i = 0; i < indexes.length; i++){
				  HashIndex hashind = new HashIndex(indexes[i].indexName);
				  hashind.deleteEntry(new SearchKey(temptuple.getField(indexes[i].columnName)), temprid);
			  }
			  hfile.deleteRecord(temprid);
			  Minibase.SystemCatalog.incrementTableCardinality(tablename, -1);
		  }
	  }

    System.out.println(numberdeleted+" rows affected.");
    scan.close();
  }
}

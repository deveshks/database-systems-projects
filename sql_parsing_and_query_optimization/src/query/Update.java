package query;

import index.HashIndex;
import global.Minibase;
import global.RID;
import global.SearchKey;
import heap.HeapFile;
import heap.HeapScan;
import parser.AST_Update;
import relop.Predicate;
import relop.Schema;
import relop.Tuple;

/**
 * Execution plan for updating tuples.
 */
class Update implements Plan {
	
	public String tablename;
	public Predicate[][] predicates;
	public String[] columnstoupdate;
	public Object[] newvalues;
	public Schema schema;

  /**
   * Optimizes the plan, given the parsed query.
   * 
   * @throws QueryException if invalid column names, values, or pedicates
   */
  public Update(AST_Update tree) throws QueryException {
	  
	  tablename = tree.getFileName();
	  newvalues = tree.getValues();
	  predicates = tree.getPredicates();
	  columnstoupdate = tree.getColumns();
	  schema = Minibase.SystemCatalog.getSchema(tablename);
	  
	  QueryCheck.tableExists(tablename);
	  for(int i = 0; i < columnstoupdate.length; i++){
		  QueryCheck.columnExists(schema, columnstoupdate[i]);
	  }
	  QueryCheck.predicates(schema, predicates);

  }

  /**
   * Executes the plan and prints applicable output.
   */
  public void execute() {
	  
	  HeapFile hfile = new HeapFile(tablename);
	  HeapScan scan = hfile.openScan();
	  
	  RID temprid = new RID();
	  Tuple temptuple;
	  byte[] data;
	  int rowsaffected = 0;
	  while(scan.hasNext()){
		  data = scan.getNext(temprid);
		  temptuple = new Tuple(schema,data);
		  
		  boolean sat = false; 
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
			  rowsaffected++;
			  Tuple newtuple = new Tuple(schema,data); // updating a copy because we need it for searching indexes
			  for(int i = 0; i < columnstoupdate.length; i++){
				  newtuple.setField(columnstoupdate[i], newvalues[i]);
			  }
			  
			  IndexDesc[] indexes = Minibase.SystemCatalog.getIndexes(tablename);
			  if(indexes.length == 0){
				// continue
			  }
			  for(int i = 0; i < indexes.length; i++){
				  for(int j = 0; j < columnstoupdate.length; j++){
					  if(indexes[i].columnName.equalsIgnoreCase(columnstoupdate[j])){
						  HashIndex hashind = new HashIndex(indexes[i].columnName);
						  hashind.deleteEntry(new SearchKey(temptuple.getField(indexes[i].columnName)), temprid);
						  hashind.insertEntry(new SearchKey(newtuple.getField(indexes[i].columnName)), temprid);
						  break;
					  }
				  }
			  }
			  hfile.updateRecord(temprid, newtuple.getData());
		  }
	  }
	  
    System.out.println(rowsaffected + " Rows affected.");
    scan.close();
  }
}
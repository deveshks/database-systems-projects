package query;

import global.Minibase;
import index.HashIndex;
import parser.AST_DropIndex;

/**
 * Execution plan for dropping indexes.
 */
class DropIndex implements Plan {
	
	public String indexname;

  /**
   * Optimizes the plan, given the parsed query.
   * 
   * @throws QueryException if index doesn't exist
   */
  public DropIndex(AST_DropIndex tree) throws QueryException {
	  indexname = tree.getFileName();
	  QueryCheck.indexExists(indexname);
  }

  /**
   * Executes the plan and prints applicable output.
   */
  public void execute() {
	  
	  HashIndex hashind = new HashIndex(indexname);
	  hashind.deleteFile();
	  Minibase.SystemCatalog.dropIndex(indexname);
	  System.out.println("Dropped Index "+indexname);
  }
}

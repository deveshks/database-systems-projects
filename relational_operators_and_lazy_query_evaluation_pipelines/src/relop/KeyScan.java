package relop;

import global.RID;
import global.SearchKey;
import heap.HeapFile;
import index.HashIndex;
import index.HashScan;

/**
 * Wrapper for hash scan, an index access method.
 */
public class KeyScan extends Iterator {

  private HashScan scan;
  private HeapFile file;
  private HashIndex index;
  private SearchKey key;
  
  /**
   * Constructs an index scan, given the hash index and schema.
   */
  public KeyScan(Schema schema, HashIndex index, SearchKey key, HeapFile file) {
  
	  this.schema = schema;
	  this.file = file;
	  this.index = index;
	  this.key = key;
	  scan = this.index.openScan(key);
  }

  /**
   * Gives a one-line explaination of the iterator, repeats the call on any
   * child iterators, and increases the indent depth along the way.
   */
  public void explain(int depth) {
	  indent(depth);
	  System.out.println("KeyScan");
  }

  /**
   * Restarts the iterator, i.e. as if it were just constructed.
   */
  public void restart() {
	  scan = index.openScan(key);
  }

  /**
   * Returns true if the iterator is open; false otherwise.
   */
  public boolean isOpen() {
	if(isScanNull()){
		return true;
	}
	return false;
  }

  /**
   * Closes the iterator, releasing any resources (i.e. pinned pages).
   */
  public void close() {
	  scan.close();
  }

  /**
   * Returns true if there are more tuples, false otherwise.
   */
  public boolean hasNext() {
	  return scan.hasNext();
  }

  /**
   * Gets the next tuple in the iteration.
   * 
   * @throws IllegalStateException if no more tuples
   */
  public Tuple getNext() {
	  Tuple tuple = null;
      if(isScanNull()){
              RID rid = scan.getNext();
              byte[] array = file.selectRecord(rid);
              tuple = new Tuple(schema, array);
      }
      return tuple;
  }
  
  //TODO: might have to use this in restart,isOpen, close and hasNext
  private boolean isScanNull(){
	  return scan!=null;
  }

} 
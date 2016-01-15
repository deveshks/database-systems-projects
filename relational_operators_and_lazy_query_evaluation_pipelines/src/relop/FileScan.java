package relop;

import global.RID;
import heap.HeapFile;
import heap.HeapScan;

/**
 * Wrapper for heap file scan, the most basic access method. This "iterator"
 * version takes schema into consideration and generates real tuples.
 */
public class FileScan extends Iterator {
	//TODO : Check if scan is null or not at many places
	private HeapScan hs;
	protected HeapFile hf;
	private RID prevrid;

  /**
   * Constructs a file scan, given the schema and heap file.
   */
  public FileScan(Schema schema, HeapFile file) {
	  this.hs = file.openScan();
	  this.hf = file;
	  this.schema = schema;
	  this.prevrid = new RID();
  }

  /**
   * Gives a one-line explaination of the iterator, repeats the call on any
   * child iterators, and increases the indent depth along the way.
   */
  public void explain(int depth) {
	  indent(depth);
	  System.out.println("FileScan");
  }

  /**
   * Restarts the iterator, i.e. as if it were just constructed.
   */
  public void restart() {
	  hs = hf.openScan();
  }

  /**
   * Returns true if the iterator is open; false otherwise.
   */
  public boolean isOpen() {
	  if(hs == null){
		  return false;
	  }
	  else{
		  return true;
	  }
  }

  /**
   * Closes the iterator, releasing any resources (i.e. pinned pages).
   */
  public void close() {
	  hs.close();
  }

  /**
   * Returns true if there are more tuples, false otherwise.
   */
  public boolean hasNext() {
	  if(hs.hasNext() == true){
		  return true;
	  }
	  else{
		  return false;
	  }
  }

  /**
   * Gets the next tuple in the iteration.
   * 
   * @throws IllegalStateException if no more tuples
   */
  public Tuple getNext() {
	  Tuple t = null;
	  byte[] data = hs.getNext(prevrid);
	  t = new Tuple(schema, data);
	  return t;
  }

  /**
   * Gets the RID of the last tuple returned.
   */
  public RID getLastRID() {
	  return prevrid;
  }

} 

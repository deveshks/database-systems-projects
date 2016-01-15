package relop;

/**
 * The projection operator extracts columns from a relation; unlike in
 * relational algebra, this operator does NOT eliminate duplicate tuples.
 */
public class Projection extends Iterator {

	private Iterator scan;
	private Integer[] fields;
	private Tuple tuple;
  /**
   * Constructs a projection, given the underlying iterator and field numbers.
   */
  public Projection(Iterator iter, Integer... fields) {
	  this.scan = iter;
      this.fields = fields;
      
      this.schema = new Schema(fields.length);
      for(int i = 0; i < fields.length; i++) {
              schema.initField(i, iter.schema, fields[i]);
      }
      
      tuple = null;
  }

  /**
   * Gives a one-line explaination of the iterator, repeats the call on any
   * child iterators, and increases the indent depth along the way.
   */
  public void explain(int depth) {
	  indent(depth);
	  System.out.println("Projection");
	  scan.explain(depth+1);
  }

  /**
   * Restarts the iterator, i.e. as if it were just constructed.
   */
  public void restart() {
	  scan.restart();
	  tuple = null;
  }

  /**
   * Returns true if the iterator is open; false otherwise.
   */
  public boolean isOpen() {
	return  scan.isOpen();
  }

  /**
   * Closes the iterator, releasing any resources (i.e. pinned pages).
   */
  public void close() {
	  scan.close();
	  tuple = null;
  }

  /**
   * Returns true if there are more tuples, false otherwise.
   */
  public boolean hasNext() {
	 if(scan.hasNext()) {
          Tuple tmp = scan.getNext();
          
          tuple = new Tuple(schema);
          for(int i = 0; i < fields.length; i++) {
                  tuple.setField(i, tmp.getField(fields[i]));
          }
          
          return true;
  }
  
  tuple = null;
  return false;
  }

  /**
   * Gets the next tuple in the iteration.
   * 
   * @throws IllegalStateException if no more tuples
   */
  public Tuple getNext() {
	  if(tuple == null)
          throw new IllegalStateException("getNext(): no more tuples");
  
	  return tuple;
  }

}
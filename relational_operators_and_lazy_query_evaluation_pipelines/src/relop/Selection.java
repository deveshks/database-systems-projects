package relop;

/**
 * The selection operator specifies which tuples to retain under a condition; in
 * Minibase, this condition is simply a set of independent predicates logically
 * connected by OR operators.
 */
public class Selection extends Iterator {
	private Iterator scan;
	private Predicate[] preds;
	private Tuple t;
	
  /**
   * Constructs a selection, given the underlying iterator and predicates.
   */
  public Selection(Iterator iter, Predicate... preds) {
	  
	  scan = iter;
	  this.schema = iter.schema;	
	  this.preds = preds;
  }

  /**
   * Gives a one-line explaination of the iterator, repeats the call on any
   * child iterators, and increases the indent depth along the way.
   */
  public void explain(int depth) {
	  indent(depth);
	  System.out.println("Selection");
	  scan.explain(depth+1);
  }

  /**
   * Restarts the iterator, i.e. as if it were just constructed.
   */
  public void restart() {
	  scan.restart();
	  t = null;
  }

  /**
   * Returns true if the iterator is open; false otherwise.
   */
  public boolean isOpen() {
	  return scan.isOpen();
  }

  /**
   * Closes the iterator, releasing any resources (i.e. pinned pages).
   */
  public void close() {
	  scan.close();
	  t = null;
	  preds = null;
  }

  /**
   * Returns true if there are more tuples, false otherwise.
   */
  public boolean hasNext() {
	  while(scan.hasNext()) {
          boolean status = false;
          Tuple tmp = scan.getNext();
          for(int i=0;i<preds.length;i++) {
                  if(preds[i].evaluate(tmp)) {
                          status = true;
                          break;
                  }
          }
          
          if(status) {
                  t = tmp;
                  return true;
          }
  }
	  t = null;
	  return false;
  }

  /**
   * Gets the next tuple in the iteration.
   * 
   * @throws IllegalStateException if no more tuples
   */
  public Tuple getNext() {
	  if(t == null){
		  throw new IllegalStateException("No more tuples");
	  }
	  return t;
  }

} 
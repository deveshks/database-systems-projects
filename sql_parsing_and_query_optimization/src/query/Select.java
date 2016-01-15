package query;

import global.Minibase;
import global.RID;
import heap.HeapFile;
import heap.HeapScan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import parser.AST_Select;
import relop.FileScan;
import relop.Iterator;
import relop.Predicate;
import relop.Projection;
import relop.Schema;
import relop.Selection;
import relop.SimpleJoin;
import relop.Tuple;

/**
 * Execution plan for selecting tuples.
 */
class Select implements Plan {
	
	public String[] tablenames;
	public Predicate[][] predicates;
	public String[] projectcolumns;
	public ArrayList<Iterator> tablefscans = new ArrayList<Iterator>();
	public Iterator combinediterator;
	public ArrayList<Integer> finishedpredicates = new ArrayList<Integer>();
	public Schema combinedschema;
	public boolean isexplain;
	
	public FileScan getfilescan(String tablename){
		HeapFile hfile = new HeapFile(tablename);
		Schema schema = Minibase.SystemCatalog.getSchema(tablename);
		FileScan fscan = new FileScan(schema, hfile);
		return fscan;
	}

	public static boolean isNumeric(String str)
	{
	  return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
	}
  /**
   * Optimizes the plan, given the parsed query.
   * 
   * @throws QueryException if validation fails
   */
  public Select(AST_Select tree) throws QueryException {
	  tablenames = tree.getTables();
	  predicates = tree.getPredicates();
	  projectcolumns = tree.getColumns();
	  isexplain = tree.isExplain;
	  Integer[] projectcolids = null;
	  
	  if (tablenames == null)
			throw new QueryException("Tables array invalid in SELECT");

	  if (tablenames.length == 0)
			throw new QueryException("At least one table is required in SELECT");
		
	  if (projectcolumns == null)
			throw new QueryException("Columns array invalid in SELECT");
	  
 	  boolean[] exists = new boolean[projectcolumns.length];
	  for(int i=0;i<projectcolumns.length;i++){
		  exists[i] = false;
		  for(int j=0;j<tablenames.length;j++){
			  Schema s = Minibase.SystemCatalog.getSchema(tablenames[j]);
			  for(int k=0;k<s.getCount();k++){
				  if(projectcolumns[i].equalsIgnoreCase(s.fieldName(k))){
					  exists[i] = true;
					  break;
				  }
			  }
			  if(exists[i]){
				  break;
			  }
		  }
	  }
	  for(int i=0;i<exists.length;i++){
		  if(!exists[i]){
			  QueryCheck.columnExists(Minibase.SystemCatalog.getSchema(tablenames[0]), projectcolumns[i]);
		  }
	  }
	  Schema tempcombined;
	  tempcombined = Minibase.SystemCatalog.getSchema(tablenames[0]);
	  for(int i = 1; i < tablenames.length; i++){
		 tempcombined = Schema.join(tempcombined, Minibase.SystemCatalog.getSchema(tablenames[i]));
	  }
	  QueryCheck.predicates(tempcombined, predicates);
		  
	  for(int i = 0; i < tablenames.length; i++){
		  tablefscans.add(getfilescan(tablenames[i]));
	  }
	    
	  for(int i = 0; i < predicates.length; i++){
		  ArrayList<String> attrlist = new ArrayList<String>();
		  
		  for(int j = 0; j < predicates[i].length; j++){
			  if(!isNumeric(predicates[i][j].getLeft().toString())){
				  attrlist.add(predicates[i][j].getLeft().toString());
			  }
			  if(!isNumeric(predicates[i][j].getRight().toString())){
				  attrlist.add(predicates[i][j].getRight().toString());
			  }
		  }
		  
		  for(int j = 0; j < tablenames.length; j++){
			  int flag = 1;
			  for(int k = 0; k < attrlist.size(); k++){
				  Schema tmpschema = Minibase.SystemCatalog.getSchema(tablenames[j]);
				  if(tmpschema.fieldNumber(attrlist.get(k)) == -1){
					  flag = 0;
					  break;
				  }
			  }
			  if(flag == 1){
				  tablefscans.set(j, new Selection(tablefscans.get(j), predicates[i]));
				  break;
			  }
		  }
	  }
	  
	  ArrayList<Integer> sizes = new ArrayList<Integer>();
	  for(int i = 0; i < tablenames.length; i++){
		  sizes.add(Minibase.SystemCatalog.getTableCardinality(tablenames[i]));
	  }
	  
	  ArrayList<Pair> pairArr = new ArrayList();
	  for (int i = 0;i<sizes.size();i++){
		  pairArr.add(new Pair(i, sizes.get(i)));
	  }
	 Collections.sort(pairArr);
	
	  ArrayList<Integer> incorder = new ArrayList<Integer>();
	  for (int i = pairArr.size()-1; i >= 0 ; i--){
		  System.out.println(pairArr.get(i).value);
	      incorder.add(pairArr.get(i).index);
	  } 
	  
	  combinediterator = tablefscans.get(incorder.get(0));
	  combinedschema = Minibase.SystemCatalog.getSchema(tablenames[incorder.get(0)]);
	  for(int i = 1; i < incorder.size(); i++){
		  combinediterator = new SimpleJoin(combinediterator,tablefscans.get(incorder.get(i)));
		  combinedschema = Schema.join(combinedschema, Minibase.SystemCatalog.getSchema(tablenames[incorder.get(i)]));
	  }
	  
	  for(int i = 0; i < predicates.length; i++){
		  if(finishedpredicates.contains(i)){
			  continue;
		  }
		  else{
			  combinediterator = new Selection(combinediterator, predicates[i]);
		  }
	  }
	  
	  ArrayList<Integer> projectcolumnsindexes = new ArrayList<Integer>();
	  for(int i = 0; i < projectcolumns.length; i++){
		  projectcolumnsindexes.add(combinedschema.fieldNumber(projectcolumns[i]));
	  }
	  
	  Integer[] temparr;
	  if(projectcolumns.length == 0){
		  temparr = new Integer[combinedschema.getCount()];
		  for(int i = 0; i < combinedschema.getCount(); i++){
			  temparr[i] = i;
		  }
	  }
	  else{
		  temparr = projectcolumnsindexes.toArray(new Integer[projectcolumnsindexes.size()]);
	  }
	  combinediterator = new Projection(combinediterator, temparr);
	  Schema finalschema = new Schema(projectcolumnsindexes.size());
	  for(int i = 0; i < projectcolumnsindexes.size(); i++){
		  finalschema.initField(i, combinedschema, projectcolumnsindexes.get(i));
	  }
	  if(!(projectcolumns.length == 0)){
		  combinedschema = finalschema;
	  }
  }

  /**
   * Executes the plan and prints applicable output.
   */
  public void execute() {
	  if(isexplain){
		  combinediterator.explain(1);
	  }
	  int outcount = 0;
	  combinedschema.print();
	  while(combinediterator.hasNext()){
		  Tuple t = combinediterator.getNext();
		  t.print();
		  outcount++;
	  }
	  combinediterator.close();
	  for(int i = 0; i < tablefscans.size(); i++){
		  tablefscans.get(i).close();
	  }
    System.out.println(outcount+" rows affected.");

  }

} 

class Pair implements Comparable<Pair> {
    public final int index;
    public final int value;

    public Pair(int index, int value) {
        this.index = index;
        this.value = value;
    }

    public int compareTo(Pair other) {
        return -1 * Integer.valueOf(this.value).compareTo(other.value);
    }
}

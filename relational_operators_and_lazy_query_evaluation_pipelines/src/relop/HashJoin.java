package relop;

import global.SearchKey;
import heap.HeapFile;
import index.HashIndex;

public class HashJoin extends Iterator {
	
	int left_field_number;
	int right_field_number;
	IndexScan leftscan;
	IndexScan rightscan;
	HashTableDup inMemoryHash = new HashTableDup();
	Tuple nextTuple = null;
	Tuple[] matchedList = null;
	int currenthashval = -1;
	int nextindexinmatchedlist = 0;
	Tuple currentrightTuple = null;
	
	public HashJoin(Iterator left, Iterator right, Integer lcol, Integer rcol){
		schema = Schema.join(left.schema, right.schema);
		this.left_field_number = lcol;
		this.right_field_number = rcol;
		
		if(left instanceof IndexScan){
			this.leftscan = (IndexScan) left;
		}
		if(left instanceof FileScan){
			this.leftscan = getIndexScanFromFileScan((FileScan) left, lcol, ((FileScan) left).hf);
		}
		else{
			this.leftscan = getIndexScan(left, lcol);
		}
		
		if(right instanceof IndexScan){
			this.rightscan = (IndexScan) right;
		}
		if(right instanceof FileScan){
			this.rightscan = getIndexScanFromFileScan((FileScan) right, rcol, ((FileScan) right).hf);
		}
		else{
			this.rightscan = getIndexScan(right, rcol);
		}
	}
	
	public IndexScan getIndexScanFromFileScan(FileScan fs, Integer field, HeapFile hpfile){
		HashIndex hi = new HashIndex(null);
		while(fs.hasNext()){
			Tuple t = fs.getNext();
			hi.insertEntry(new SearchKey(t.getField(field).toString()), fs.getLastRID());
		}
		IndexScan is = new IndexScan(fs.getSchema(),hi,hpfile);
		return is;
	}
	
	public IndexScan getIndexScan(Iterator it, Integer field){
		HeapFile hf = new HeapFile(null);
		while(it.hasNext()){
			hf.insertRecord(it.getNext().data);
		}
		FileScan temp = new FileScan(it.schema, hf);
		return getIndexScanFromFileScan(temp, field, hf);
	}
	
	@Override
	public void explain(int depth) {
		 indent(depth);
		 System.out.println("HashJoin");
		 leftscan.explain(depth+1);
		 rightscan.explain(depth+1);
	}

	@Override
	public void restart() {
		// TODO Auto-generated method stub
		inMemoryHash = null;
		leftscan.restart();
		rightscan.restart();
		nextTuple = null;
		matchedList = null;
		currenthashval = (Integer) null;
		currentrightTuple = null;
	}

	@Override
	public boolean isOpen() {
		// TODO Auto-generated method stub
		if(leftscan.isOpen()){
			if(rightscan.isOpen()){
				return true;
			}
		}
		return false;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		leftscan.close();
		rightscan.close();
		matchedList = null;
		nextindexinmatchedlist = 0;
		currentrightTuple = null;
		nextTuple = null;
		inMemoryHash = null;
	}

	public void constructInMemoryHash(int hashval){
		// find the bucket corresponding to the hashval
		leftscan.restart();
		inMemoryHash.clear();
		while(leftscan.hasNext()){
			int temp = leftscan.getNextHash();
			if(temp == hashval){
				Tuple leftTuple = leftscan.getNext();
				inMemoryHash.add(new SearchKey(leftTuple.getField(left_field_number).toString()), leftTuple);
			}
			else{
				leftscan.getNext();
			}
		}
	}
	
	@Override
	public boolean hasNext() {
		// TODO Auto-generated method stub
		
		if(matchedList == null){
			if(rightscan.hasNext()){
				int nexthashval = rightscan.getNextHash();
				currentrightTuple = rightscan.getNext();
				if(nexthashval == currenthashval){
					matchedList = inMemoryHash.getAll(new SearchKey(currentrightTuple.getField(right_field_number).toString()));
					if(matchedList == null){
						nextindexinmatchedlist = 0;
						return hasNext();
					}
					nextindexinmatchedlist = 0;
					while(nextindexinmatchedlist <= matchedList.length-1){
						if(currentrightTuple.getField(right_field_number).equals(matchedList[nextindexinmatchedlist].getField(left_field_number))){
							nextTuple = Tuple.join(matchedList[nextindexinmatchedlist], currentrightTuple, schema);
							nextindexinmatchedlist++;
							return true;
						}
						else{
							nextindexinmatchedlist++;
						}
					}
					nextindexinmatchedlist = 0;
					matchedList = null;
					return hasNext();
				}
				else{
					currenthashval = nexthashval;
					constructInMemoryHash(currenthashval);
					matchedList = inMemoryHash.getAll(new SearchKey(currentrightTuple.getField(right_field_number).toString()));
					if(matchedList == null){
						nextindexinmatchedlist = 0;
						return hasNext();
					}
					nextindexinmatchedlist = 0;
					while(nextindexinmatchedlist <= matchedList.length-1){
						if(currentrightTuple.getField(right_field_number).equals(matchedList[nextindexinmatchedlist].getField(left_field_number))){
							nextTuple = Tuple.join(matchedList[nextindexinmatchedlist], currentrightTuple, schema);
							nextindexinmatchedlist++;
							return true;
						}
						else{
							nextindexinmatchedlist++;
						}
					}
					nextindexinmatchedlist = 0;
					matchedList = null;
					return hasNext();
				}
			}
			else{
				return false;
			}
		}
		
		else{
			if(nextindexinmatchedlist == matchedList.length){
				matchedList = null;
				nextindexinmatchedlist = 0;
				return hasNext();
			}
			else{
				while(nextindexinmatchedlist <= matchedList.length-1){
					if(currentrightTuple.getField(right_field_number).equals(matchedList[nextindexinmatchedlist].getField(left_field_number))){
						nextTuple = Tuple.join(matchedList[nextindexinmatchedlist], currentrightTuple, schema);
						nextindexinmatchedlist++;
						return true;
					}
					else{
						nextindexinmatchedlist++;
					}
				}
				nextindexinmatchedlist = 0;
				matchedList = null;
				return hasNext();
			}
		}
	}

	@Override
	public Tuple getNext() {
		// TODO Auto-generated method stub
		if(nextTuple == null){
			// throw Exception
			throw new IllegalStateException("No Tuples Left");
		}
		else{
			return nextTuple;
		}
	}
}

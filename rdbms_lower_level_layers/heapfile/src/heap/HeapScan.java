package heap;

import chainexception.ChainException;
import global.Minibase;
import global.PageId;
import global.RID;

public class HeapScan {
	int seen = 0;
	HeapFile hfile;
	RID currentrecord;
	RID specialrecord;
	RID temp;
	int hasnextflag = -1;
	int scannedcount = 0;
	public HeapScan(){
		hfile = new HeapFile(" ");
		PageId pg = new PageId(0);
		// probably not in 0,0 : find where the first entry actually is
		currentrecord = new RID(pg,0);
		specialrecord = null;
	}
	
	public boolean hasNext(){
		if(hfile.pages.get(hfile.pids.indexOf(currentrecord.pageno.pid)).hasNext(currentrecord)){
			return true;
		}
		else{
			// last page and no more records
			if(hfile.pids.indexOf(currentrecord.pageno.pid) == hfile.pages.size()-1){
				return false;
			}
			// not last page but no record in this
			else{
				//check all the next pages
				for(int i = hfile.pids.indexOf(currentrecord.pageno.pid)+1; i < hfile.pages.size(); i++){
					if(hfile.pages.get(i).firstRecord() != null){
						specialrecord = hfile.pages.get(i).firstRecord();
						return true;
					}
				}
				return false;
			}
		}
	}
	
	public Tuple getNext(RID rid){
		scannedcount += 1;
		//System.out.println(scannedcount);
		//System.out.println(currentrecord == null);
		if(currentrecord == null){
			Minibase.BufferManager.unpinPage(temp.pageno, false);
			rid = null;
			return null;
		}
		
		if(this.hasNext()){
			Minibase.BufferManager.unpinPage(new PageId(currentrecord.pageno.pid), false);
			temp = currentrecord;
			
			if(hfile.pages.get(hfile.pids.indexOf(currentrecord.pageno.pid)).nextRecord(currentrecord) != null){
				currentrecord = hfile.pages.get(hfile.pids.indexOf(currentrecord.pageno.pid)).nextRecord(currentrecord);
			}
			else{
				currentrecord = specialrecord;
				specialrecord = null;
			}
			Minibase.BufferManager.pinPage(new PageId(currentrecord.pageno.pid), hfile.pages.get(hfile.pids.indexOf(currentrecord.pageno.pid)),false);
			seen += 1;
			rid.pageno = temp.pageno;
			rid.slotno = temp.slotno;
			return new Tuple(hfile.pages.get(hfile.pids.indexOf(temp.pageno.pid)).selectRecord(temp));
		}
		
		else{
			Minibase.BufferManager.unpinPage(new PageId(currentrecord.pageno.pid), false);
			temp = currentrecord;
			currentrecord = null;
			Minibase.BufferManager.pinPage(new PageId(temp.pageno.pid),hfile.pages.get(hfile.pids.indexOf(temp.pageno.pid)), false);
			rid.pageno = temp.pageno;
			rid.slotno = temp.slotno;
			return new Tuple(hfile.pages.get(hfile.pids.indexOf(temp.pageno.pid)).selectRecord(temp));
		}
		
//		if(hasnextflag == 1){
//			hasnextflag = -1;
//			Minibase.BufferManager.unpinPage(new PageId(currentrecord.pageno.pid), false);
//			RID temp = currentrecord;
//			currentrecord = nextrecord;
//			Minibase.BufferManager.pinPage(new PageId(currentrecord.pageno.pid),hfile.pages.get(hfile.pids.indexOf(currentrecord.pageno.pid)), false);
//			seen += 1;
//			return new Tuple(hfile.pages.get(hfile.pids.indexOf(temp.pageno.pid)).selectRecord(temp));
//		}
//		if(hasnextflag == -1){
//			if(this.hasNext()){
//				hasnextflag = -1;
//				Minibase.BufferManager.unpinPage(new PageId(currentrecord.pageno.pid), false);
//				RID temp = currentrecord;
//				currentrecord = nextrecord;
//				Minibase.BufferManager.pinPage(new PageId(currentrecord.pageno.pid), hfile.pages.get(hfile.pids.indexOf(currentrecord.pageno.pid)),false);
//				seen += 1;
//				return new Tuple(hfile.pages.get(hfile.pids.indexOf(temp.pageno.pid)).selectRecord(temp));
//			}
//			else{
//				hasnextflag = -1;
//				//Minibase.BufferManager.unpinPage(new PageId(currentrecord.pageno.pid), false);
//				return new Tuple(hfile.pages.get(hfile.pids.indexOf(currentrecord.pageno.pid)).selectRecord(currentrecord));
//			}
//		}
//		else{
//			//Minibase.BufferManager.unpinPage(new PageId(currentrecord.pageno.pid), false);
//			hasnextflag = -1;
//			return new Tuple(hfile.pages.get(hfile.pids.indexOf(currentrecord.pageno.pid)).selectRecord(currentrecord));
//		}
	}
	
	public void close() throws ChainException{
		//Minibase.BufferManager.unpinPage(temp.pageno, false);
		finalize();
	}
	
	public void finalize(){
		//hfile = new HeapFile("");
		int y = hfile.pids.get(1);
		//currentrecord = hfile.pages.get(1).firstRecord();
		currentrecord = new RID();
		currentrecord.pageno.pid = hfile.pids.get(1);
		currentrecord.slotno = 0;
		//nextrecord = new RID();
	}
}
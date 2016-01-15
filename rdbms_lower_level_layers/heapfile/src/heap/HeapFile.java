package heap;
import java.util.ArrayList;
import global.*;
import java.util.List;

import chainexception.ChainException;
import heap.SpaceNotAvailableException;
import bufmgr.BufMgr;
import global.Page;
import global.PageId;
import global.RID;
import heap.HFPage;
import global.Minibase;
import heap.InvalidUpdateException;
import heap.SpaceNotAvailableException;

public class HeapFile {
	String fname;
	int cntr;
	int npages = Minibase.BufferManager.getNumBuffers();
	List<Integer> countFree;
	List<Integer> pids;
	int totalrecordcount = 0;
	List<HFPage> pages;
	
	public HeapFile(String x){
		// if already present
		countFree = new ArrayList<Integer>(npages);
		pids = new ArrayList<Integer>(npages);
		pages = new ArrayList<HFPage>(npages);
		if(Minibase.DiskManager.get_file_entry(x) != null){
			PageId hpid = Minibase.DiskManager.get_file_entry(x);
			HFPage hp = new HFPage();
			Minibase.BufferManager.pinPage(hpid, hp, false);
			Minibase.BufferManager.unpinPage(hpid, false);
			pages.add(0,hp);
			pids.add(0,hpid.pid);
			countFree.add(0,(int) hp.getFreeSpace());
			fname = x;
			HFPage hp2 = hp;
			int i = 1;
			while(true){
				PageId hpid2 = hp2.getNextPage();
				//HFPage hp2 = new HFPage();
				if(hpid2.pid == -1){
					break;
				}
				Minibase.BufferManager.pinPage(hpid2, hp2, false);
				Minibase.BufferManager.unpinPage(hpid2, false);
				HFPage hp3 = new HFPage(new Page(hp2.getData()));
				pages.add(i,hp3);
				pids.add(i,hpid2.pid);
				countFree.add(i,(int) hp2.getFreeSpace());
				fname = x;
				i++;
			}
		}
		
		else{
			if(x == null){
				fname = "tempheapfile";
			}
			else{
				fname = x;
			
				//comment under to get test 1 working
				Page p = new Page();
				PageId pid = Minibase.BufferManager.newPage(p, 1);
				Minibase.DiskManager.add_file_entry(x, pid);
				Minibase.BufferManager.unpinPage(pid, true);
				pages.add(0, new HFPage(p));
				pids.add(0,pid.pid);
			}
		}
	}
	
	public RID insertRecord(byte[] record) throws Exception{
		
		// TODO : Send HFPage directly instead of sending a Page 
		
		int size = record.length;
		//System.out.println(size);
		if(pids.size() == 0){
			//Page p = new Page();
			//PageId pId = new PageId();
			//pId = Minibase.BufferManager.newPage(p, 1);
			//Minibase.BufferManager.unpinPage(pId, false);
			//pids.add(0,pId.pid);
			//HFPage hp = new HFPage(p);
			//countFree.add(0,(int) hp.getFreeSpace());
			//Minibase.BufferManager.pinPage(pId, p, false);
			//hp.setCurPage(pId);
			HFPage hp = new HFPage();
			PageId hpid = new PageId();
			hpid = Minibase.BufferManager.newPage(hp, 1);
			Minibase.BufferManager.unpinPage(hpid, false);
			pids.add(0,hpid.pid);
			countFree.add(0,(int) hp.getFreeSpace());
			Minibase.BufferManager.pinPage(hpid, hp, false);
			hp.setCurPage(hpid);
			
			//System.out.println(pages.get(0).getFreeSpace());
			RID r;
//			System.out.println(hp.getFreeSpace());
			try{
				r = hp.insertRecord(record);
			}
			catch(Exception e){
				throw new SpaceNotAvailableException("Error: Space not available for record");
			}
			pages.add(0,hp);
//			System.out.println(hp.getFreeSpace());
//			System.out.println("\n");
			//System.out.println(pages.get(0).getFreeSpace());
			countFree.set(0, (int) hp.getFreeSpace());
			totalrecordcount++;
			//countRecords.set(i, countRecords.get(i)+1);
			Minibase.BufferManager.unpinPage(hpid,true);
			//System.out.println(Mini);
			return r;
		}
		else{
			// there are some pages in buffer pool
			int i;
			for(i = 0; i < pids.size(); i++){
				
				HFPage hp = pages.get(i);
				PageId hpid = new PageId(pids.get(i));
				Minibase.BufferManager.pinPage(hpid, hp, false);
				RID r = hp.insertRecord(record);
				if(r == null){
					Minibase.BufferManager.unpinPage(hpid,false);
					continue;
				}
				else {
					pages.set(i, hp);
					cntr+=1;
					totalrecordcount++;
					Minibase.BufferManager.unpinPage(hpid,true);
					return r;
				}
			}
			
			// 2 cases : pids size = npages and no space found, so, no chance to add
			// or pids size less than npages and no space found, else, make a new page and add into that
			
			if(pids.size() == npages){
				return null;
			}
			else{
				
				HFPage hp = new HFPage();
				PageId hpid = new PageId();
				int occupancy = pids.size();
				hpid = Minibase.BufferManager.newPage(hp, 1);
				Minibase.BufferManager.unpinPage(hpid, false);
				pids.add(occupancy,hpid.pid);
				//countFree.add(occupancy,(int) hp.getFreeSpace());
				Minibase.BufferManager.pinPage(hpid, hp, false);
				RID r;
				try{
					r = hp.insertRecord(record);
				}
				catch(Exception e){
					throw new SpaceNotAvailableException("Error: Space not available for record");
				}
				if(r == null){
					throw new SpaceNotAvailableException("Error: Space not available for record");
				}
				hp.setCurPage(hpid);
				hp.setPrevPage(new PageId(pids.get(pids.size()-2)));
				pages.add(occupancy, hp);
				pages.get(occupancy-1).setNextPage(new PageId(pids.get(occupancy)));
				//countFree.add(occupancy, (int) hp.getFreeSpace());
				totalrecordcount++;
				Minibase.BufferManager.unpinPage(hpid,true);
				return r;
			}
		}
	}
	
	
	public Tuple getRecord(RID rid) throws ChainException{
//		// Only return if it is in the bufferpool
//		int frnum = -1;
//		for(int i = 0; i < pids.size(); i++){
//			if(pids.get(i) == rid.pageno.pid){
//				frnum = i;
//				break;
//			}
//		}
//		if(frnum == -1){
//			// The page is not in the bufferpool
//			return null;
//		}
//		HFPage hPage = new HFPage();
//		Minibase.BufferManager.pinPage(new PageId(pids.get(frnum)), hPage, false);
//		Tuple t = new Tuple(hPage.selectRecord(rid));
//		Minibase.BufferManager.unpinPage(new PageId(pids.get(frnum)), false);
//		return t;
		
		HFPage hPage = new HFPage();
		Minibase.BufferManager.pinPage(rid.pageno, hPage, false);
		Tuple t = new Tuple(hPage.selectRecord(rid));
		Minibase.BufferManager.unpinPage(rid.pageno, false);
		return t;
		
	}
	
	public boolean updateRecord(RID rid, Tuple newRecord) throws ChainException{
		int frnum = -1;
		for(int i = 0; i < pids.size(); i++){
			if(pids.get(i) == rid.pageno.pid){
				frnum = i;
				break;
			}
		}
		if(frnum == -1){
			//TODO can we do this, or should we check in all disk pages
			// The page is not in the bufferpool
			//return false;
		}
		HFPage hPage = new HFPage();
		Minibase.BufferManager.pinPage(rid.pageno, hPage, false);
		try{
			hPage.updateRecord(rid, newRecord);
		}
		catch(Exception e){
			throw new InvalidUpdateException();
		}
		pages.set(frnum, hPage);
		Minibase.BufferManager.unpinPage(rid.pageno, true);
		return true;
	}
	
	public boolean deleteRecord(RID rid) throws ChainException{
		int frnum = -1;
		for(int i = 0; i < pids.size(); i++){
			if(pids.get(i) == rid.pageno.pid){
				frnum = i;
				break;
			}
		}
		if(frnum == -1){
			//TODO can we do this, or should we check in all disk pages
			// The page is not in the bufferpool
			return false;
		}
		
		HFPage hPage = new HFPage();
		Minibase.BufferManager.pinPage(new PageId(pids.get(frnum)), hPage, false);
		hPage.deleteRecord(rid);
		pages.set(frnum, hPage);
		Minibase.BufferManager.unpinPage(new PageId(pids.get(frnum)), true);
		countFree.set(frnum, (int) hPage.getFreeSpace());
		return true;
	}
	
	public int getRecCnt() throws ChainException{
		// Total Heap File
		return totalrecordcount;
	}
	
	public HeapScan openScan() throws ChainException{
//		HeapScan x = new HeapScan();
//		x.hfile = this;
//		for(int i = 0; i < x.hfile.pages.size(); i++){
//			if(x.hfile.pages.get(i).firstRecord() == null){
//				continue;
//			}
//			else{
//				x.currentrecord = x.hfile.pages.get(i).firstRecord();
//				break;
//			}
//		}
//		Minibase.BufferManager.pinPage(x.currentrecord.pageno, new Page(), false);
//		return x;
		HeapScan x = new HeapScan();
		x.hfile = this;
		//x.currentrecord = x.hfile.pages.get(1).firstRecord();
		x.currentrecord.pageno.pid = pids.get(1);
		x.currentrecord.slotno = 0;
		Minibase.BufferManager.pinPage(x.currentrecord.pageno, new Page(), false);
		//System.out.println(pids.size());
		//System.out.println(pages.size());
		return x;
	}
}

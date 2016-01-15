package bufmgr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import chainexception.ChainException;
import global.PageId;
import global.GlobalConst;
import global.Minibase;
import global.Page;
import diskmgr.*;

public class BufMgr {
	int entrysize; 
	int bufcount;
	List<byte[]> data;
	public HashTable hash = null;
	public List<FrameDescriptor> fd;
	int currenttime = 0;

	public BufMgr(int numbufs, int lookAheadSize, String replacementPolicy) {
		data = new ArrayList<byte[]>(numbufs);
		fd = new ArrayList<FrameDescriptor>(numbufs);
		byte[] pageByteArr = new byte[GlobalConst.PAGE_SIZE];
		for (int i = 0; i < numbufs; i++) {
			data.add(i, pageByteArr);
			fd.add(i, new FrameDescriptor(i));
		}
		hash = new HashTable(101);
	}

	public double findlrfuval(FrameDescriptor f, int currenttime) {
		double lrfuval = 0;
		for (int i = 0; i < f.accesstimes.size(); i++) {
			lrfuval += 1 / (currenttime - f.accesstimes.get(i) + 1);
		}
		return lrfuval;

	}

	public void pinPage(PageId pageno, Page page, boolean emptyPage) throws ChainException {
		int framenumber = hash.getframenumber(pageno);
		currenttime++;
		if (framenumber == -1) {
			FrameDescriptor temp = new FrameDescriptor(-1);
			temp.framenumber = -1;
			double minval = Integer.MAX_VALUE;
			for (Iterator<FrameDescriptor> iterator = fd.iterator(); iterator.hasNext();) {
				FrameDescriptor f = iterator.next();
				if (f.pincount == 0) {
					double lrfuval = findlrfuval(f, currenttime);
					if (lrfuval <= minval) {
						minval = lrfuval;
						temp = f;
					}
				}
			}

			if (temp.framenumber == -1) {
				throw new BufferPoolExceededException(null, "Error: Cannot find unpinned frames");
			}

			int oldpagenumber = temp.pagenumber;
			if (oldpagenumber >= 0 & oldpagenumber < Minibase.DiskManager.db_num_pages()) {
				PageId oldpage = new PageId();
				oldpage.pid = oldpagenumber;
				if (temp.dirty == true) {
					flushPage(oldpage);
				}
				hash.remove(oldpage);
			}

			hash.insert(pageno, temp.framenumber);
			page.setpage(data.get(temp.framenumber));
			try {
				Minibase.DiskManager.read_page(pageno, page);
			} catch (Exception e) {
				throw new ChainException(e, "Error: Cannot read page");
			}
			data.set(temp.framenumber, page.getData());
			temp.pagenumber = pageno.pid;
			temp.dirty = false;
			temp.pincount = 1;
			temp.accesstimes.add(currenttime);
			fd.set(temp.framenumber, temp);

		} else {
			if (fd.get(framenumber).pincount == 0) {
				fd.get(framenumber).accesstimes.clear();
			}
			fd.get(framenumber).pincount += 1;
			fd.get(framenumber).accesstimes.add(currenttime);
			page.setpage(data.get(framenumber));
		}
	}

	public void unpinPage(PageId pageno, boolean dirty) throws ChainException {
		int framenumber = hash.getframenumber(pageno);

		if (framenumber == -1) {
			throw new HashEntryNotFoundException(null, "Error: Page not in buffer");
		}

		if (fd.get(framenumber).pincount == 0) {
			throw new PageUnpinnedException(null, "Error: Page already unpinned");
		} else {
			fd.get(framenumber).pincount -= 1;
			if (dirty == true) {
				fd.get(framenumber).dirty = true;
			}
		}
	}

	public PageId newPage(Page firstpage, int howmany) throws ChainException, IOException {
		PageId pgid = new PageId();
		try {
			Minibase.DiskManager.allocate_page(pgid, howmany);
		} catch (ChainException e) {
			throw new ChainException(e, "Error: Allocation of " + howmany + "pages failed");
		}
		try {
			pinPage(pgid, firstpage, true);
		} catch (Exception e) {
			try {
				Minibase.DiskManager.deallocate_page(pgid, howmany);
			} catch (ChainException e2) {
				throw new ChainException(e2, "Error: Deallocation of " + howmany + "pages failed");
			}
			return null;
		}
		return pgid;
	}

	public void freePage(PageId globalPageId) throws ChainException {

		int framenumber = hash.getframenumber(globalPageId);
		if (framenumber != -1) {

			if (fd.get(framenumber).pincount > 1) {
				throw new PagePinnedException(null, "Error: Trying to free a pinned page");
			}

			if (fd.get(framenumber).pincount == 1) {
				unpinPage(globalPageId, false);
			} else {

			}
		}
		try {
			Minibase.DiskManager.deallocate_page(globalPageId);
		} catch (ChainException e) {
			throw new ChainException(e, "Error: Cannot deallocating page");
		}
		if (framenumber != -1) {
			fd.set(framenumber, new FrameDescriptor(framenumber));
			hash.remove(globalPageId);
		}

	}

	public void flushPage(PageId pageid) throws ChainException {
		for (Iterator<FrameDescriptor> iterator = fd.iterator(); iterator.hasNext();) {
			FrameDescriptor f = iterator.next();
			if (f.pagenumber == pageid.pid) {
				if (f.dirty == true) {
					Page apage = new Page();
					apage.setpage(data.get(f.framenumber));
					try {
						Minibase.DiskManager.write_page(pageid, apage);
					} catch (Exception e) {
						throw new ChainException(e, "Error: Unable to write page to disk");
					}
				}
			}
		}
	}

	public void flushAllPages() throws ChainException {
		for (Iterator<FrameDescriptor> iterator = fd.iterator(); iterator.hasNext();) {
			FrameDescriptor f = iterator.next();
			PageId pgId = new PageId(f.pagenumber);
			flushPage(pgId);
			/*
			 * if(f.dirty == true){ Page apage = new Page();
			 * apage.setpage(data.get(f.framenumber)); PageId pgid = new
			 * PageId(f.pagenumber); try{ Minibase.DiskManager.write_page(pgid,
			 * apage); } catch(Exception e){ throw new ChainException(e,
			 * "Error while writing page to disk"); } }
			 */
		}
	}

	public int getNumBuffers() {
		return fd.size();
	}

	public int getNumUnpinned() {
		int count = 0;
		for (Iterator<FrameDescriptor> iterator = fd.iterator(); iterator.hasNext();) {
			FrameDescriptor f = iterator.next();
			if (f.pincount == 0) {
				count++;
			}
		}
		return count;
	}

}
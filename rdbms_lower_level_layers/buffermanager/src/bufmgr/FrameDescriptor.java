package bufmgr;

import java.util.ArrayList;
import java.util.List;

public class FrameDescriptor{
    int pincount;
    boolean dirty;
    int pagenumber;
    double lrfuval;
    int framenumber;
    List<Integer> accesstimes;
    
    public FrameDescriptor(int x){
        pincount = 0;
        dirty = false;
        pagenumber = -1;
        lrfuval = 0;
        framenumber = x;
        accesstimes = new ArrayList<Integer>();
    }

	public int getPincount() {
		return pincount;
	}

	public void setPincount(int pincount) {
		this.pincount = pincount;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public int getPagenumber() {
		return pagenumber;
	}

	public void setPagenumber(int pagenumber) {
		this.pagenumber = pagenumber;
	}

	public double getLrfuval() {
		return lrfuval;
	}

	public void setLrfuval(double lrfuval) {
		this.lrfuval = lrfuval;
	}

	public int getFramenumber() {
		return framenumber;
	}

	public void setFramenumber(int framenumber) {
		this.framenumber = framenumber;
	}

	public List<Integer> getAccesstimes() {
		return accesstimes;
	}

	public void setAccesstimes(List<Integer> accesstimes) {
		this.accesstimes = accesstimes;
	}
    
    
}
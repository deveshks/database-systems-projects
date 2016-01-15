package bufmgr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import global.PageId;


class HashEntry{
    public int PageNumber;
    public int FrameNumber;
}

class HashTable{
    public int hsize = 101;
    public List<List<HashEntry>> hashtable;
    
    public HashTable(){
        hashtable = new ArrayList<List<HashEntry>>(hsize);
    }
    
    public HashTable(int size){
        hsize = size;
        hashtable = new ArrayList<List<HashEntry>>(hsize);
        for(int i = 0; i < hsize; i++){
            List<HashEntry> l = new ArrayList<HashEntry>();
            hashtable.add(l);
        }
        
    }
    // TODO : change code to get 2d : List of List of hash entries
    public void insert(PageId pageno, int frameno){
        int bucketnumber = findhashval(pageno);
        HashEntry he = new HashEntry();
        he.PageNumber = pageno.pid;
        he.FrameNumber = frameno;
        hashtable.get(bucketnumber).add(he);
    }
    
    public int getframenumber(PageId pageno){
        int bucketnumber = findhashval(pageno);
        for (HashEntry entry : hashtable.get(bucketnumber)){
            if(entry.PageNumber == pageno.pid){
                return entry.FrameNumber;
            }
        }
        return -1;
    }
    
    public void remove(PageId pageno){
        int bucketnumber = findhashval(pageno);
        for (Iterator<HashEntry> iterator = hashtable.get(bucketnumber).iterator(); iterator.hasNext();) {
            HashEntry entry = iterator.next();
            if (entry.PageNumber == pageno.pid) {
                iterator.remove();
            }
        }
    }
    
    public int findhashval(PageId pageno){
        return (10*pageno.pid+5)%hsize;
    }
}

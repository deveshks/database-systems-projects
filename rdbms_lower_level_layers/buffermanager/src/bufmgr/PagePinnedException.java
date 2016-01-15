package bufmgr;

import chainexception.ChainException;

public class PagePinnedException extends ChainException {
	public PagePinnedException(Exception arg0, String arg1) {
        super(arg0, arg1);     
    }
}

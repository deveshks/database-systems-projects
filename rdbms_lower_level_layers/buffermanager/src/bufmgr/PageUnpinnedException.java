package bufmgr;

import chainexception.ChainException;

public class PageUnpinnedException extends ChainException {
	public PageUnpinnedException(Exception arg0, String arg1) {
        super(arg0, arg1);     
    }
}

package bufmgr;

import chainexception.ChainException;

public class HashEntryNotFoundException extends ChainException {
	public HashEntryNotFoundException(Exception arg0, String arg1) {
        super(arg0, arg1);     
    }
}

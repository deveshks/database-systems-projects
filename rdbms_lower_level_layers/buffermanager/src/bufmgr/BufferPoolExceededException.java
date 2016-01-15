package bufmgr;

import chainexception.ChainException;

public class BufferPoolExceededException extends ChainException {
	public BufferPoolExceededException(Exception arg0, String arg1) {
        super(arg0, arg1);     
    }
}

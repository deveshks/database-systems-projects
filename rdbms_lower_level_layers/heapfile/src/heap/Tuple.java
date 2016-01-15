package heap;

public class Tuple {
	int length;
	byte[] data;
	
	public Tuple(){
		
	}
	
	public Tuple(byte[] val) {
		// TODO Auto-generated constructor stub
		data = val;
		length = data.length;
	}
	
	public Tuple(byte[] val, int a, int b){
		data = val;
		length = b;
	}

	public int getLength() {
		// TODO Auto-generated method stub
		return length;
	}

	public byte[] getTupleByteArray() {
		// TODO Auto-generated method stub
		return data;
	}

}

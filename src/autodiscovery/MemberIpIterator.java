package autodiscovery;

public interface MemberIpIterator {

	MemberDescriptor next();
	
	boolean hasNext();
	
}

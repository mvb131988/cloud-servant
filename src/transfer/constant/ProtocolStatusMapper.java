package transfer.constant;

public class ProtocolStatusMapper {

	public MasterTransferThreadStatus map(MasterSlaveCommunicationStatus in) {
		MasterTransferThreadStatus out = null;

		switch (in) {
		case BUSY:
			out = MasterTransferThreadStatus.BUSY;
			break;
		case READY:
			out = MasterTransferThreadStatus.READY;
			break;
		default:
			break;
		}

		return out;
	}

}

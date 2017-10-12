package main;

public enum LifecycleCommand {

	START("start"),
	STOP("stop");
	
	private String command;
	
	private LifecycleCommand(String command){
		this.command = command;
	}
	
	public static LifecycleCommand to(String s){
		for(LifecycleCommand lc: LifecycleCommand.values()) {
			if(lc.getCommand().equals(s)) {
				return lc;
			}
		}
		return null;
	}

	public String getCommand() {
		return command;
	}
	
}

package main;

public class Main {

	@SuppressWarnings("resource")
	public static void main(String[] args) {
		System.out.println("Run");
		AppContext context = new AppContext();
		context.start();
	}

}

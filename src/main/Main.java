package main;

import java.io.IOException;

import file.FileSender;

public class Main {

	@SuppressWarnings("resource")
	public static void main(String[] args) {
		System.out.println("Run");
		AppContext context = new AppContext();
		context.start();
	}

}

package model;

import java.io.FileWriter;
import java.io.IOException;

public class Csv {
	private FileWriter writer;

	public Csv(String fileName) {
		try {
			writer = new FileWriter(fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void appendLine(String line) {
		try {
			writer.append(line + '\n');
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void saveToDisk() {
		try {
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}

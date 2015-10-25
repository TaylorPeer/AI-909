package experiments;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

import midi.MidiUtilities;
import model.MidiSequence;

public class GenerateSequencesFromMidi {

	private static final String MIDI_PATH = "src/main/resources/midi";

	private static final String OUTPUT_PATH = "src/main/resources/raw-training-data.tsv";

	public static void main(String[] args) {

		// Output all available training material
		List<MidiSequence> drumSequences = MidiUtilities.getDrumSequencesInDirectory(MIDI_PATH);

		PrintWriter writer;
		try {
			writer = new PrintWriter(OUTPUT_PATH, "UTF-8");

			for (MidiSequence drumSequence : drumSequences) {
				StringBuilder sb = new StringBuilder();
				sb.append(drumSequence.getFileName() + "\t");
				sb.append(drumSequence.getBpm() + "\t");
				sb.append(drumSequence.getHitVector().substring(0, 16));
				System.out.println(sb.toString());
				writer.println(sb.toString());
			}

			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

	}

}

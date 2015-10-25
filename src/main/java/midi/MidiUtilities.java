package midi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import model.MidiSequence;

public class MidiUtilities {

	private static final int DEFAULT_BPM = 175;

	private static final int DRUM_CHANNEL_INDEX = 9;

	private static final byte MIDI_SET_TEMPO = 0x51;

	private static final int MICROSECONDS_PER_MINUTE = 60000000;

	private static final int NOTE_ON = 0x90;

	/**
	 * Loads all drum sequences found in a given directory.
	 * 
	 * @param dirPath
	 * @return
	 */
	public static List<MidiSequence> getDrumSequencesInDirectory(String dirPath) {
		File folder = new File(dirPath);
		File[] listOfFiles = folder.listFiles();
		List<MidiSequence> drumSequences = new ArrayList<MidiSequence>();

		for (File file : listOfFiles) {
			String path = file.getAbsolutePath();
			Sequence sequence = MidiUtilities.loadSequence(path);
			if (sequence != null) {
				MidiSequence drumSequence = new MidiSequence();
				drumSequence.setFileName(file.getName());
				drumSequence.setDirectoryPath(dirPath);
				drumSequence.setMidiSequence(sequence);
				drumSequences.add(drumSequence);
			}
		}
		return drumSequences;
	}

	/**
	 * Loads a drum sequence from a MIDI file in a given location.
	 * 
	 * @param path
	 * @return
	 */
	public static Sequence loadSequence(String path) {
		Sequence sequence = null;
		try {
			sequence = MidiSystem.getSequence(new File(path));
		} catch (InvalidMidiDataException | IOException e) {
			e.printStackTrace();
		}
		return sequence;
	}

	/**
	 * Calculates the BPM given a number of microseconds per quarter note.
	 * 
	 * @param microsecondsPerQuarterNote
	 * @return
	 */
	public static float getBaseBpm(Sequence sequence) {
		int microsecondsPerQuarterNote = getMicrosecondsPerQuarterNote(sequence);
		if (microsecondsPerQuarterNote == 0) {
			return DEFAULT_BPM;
		}
		return MICROSECONDS_PER_MINUTE / microsecondsPerQuarterNote;
	}

	/**
	 * Gets the number of microseconds per quarter note for a sequence, used to
	 * determine its BPM.
	 * 
	 * @param sequence
	 * @return
	 */
	private static int getMicrosecondsPerQuarterNote(Sequence sequence) {

		// Check all MIDI tracks for MIDI_SET_TEMPO message
		for (Track track : sequence.getTracks()) {
			for (int i = 0; i < track.size(); i++) {
				MidiEvent event = track.get(i);
				MidiMessage message = event.getMessage();
				if (message instanceof MetaMessage) {
					MetaMessage m = (MetaMessage) message;
					byte[] data = m.getData();
					int type = m.getType();
					if (type == MIDI_SET_TEMPO) {
						return ((data[0] & 0xff) << 16) | ((data[1] & 0xff) << 8) | (data[2] & 0xff);
					}
				}
			}
		}
		return 0;
	}

	public static Map<Integer, ArrayList<Long>> getDrumHits(Sequence sequence) {
		Map<Integer, ArrayList<Long>> drumHits = new HashMap<Integer, ArrayList<Long>>();
		for (Track track : sequence.getTracks()) {
			for (int i = 0; i < track.size(); i++) {
				MidiEvent event = track.get(i);
				Long tick = event.getTick();
				MidiMessage message = event.getMessage();
				if (message instanceof ShortMessage) {
					ShortMessage sm = (ShortMessage) message;
					int channel = sm.getChannel();
					if (channel == DRUM_CHANNEL_INDEX) {
						if (sm.getCommand() == NOTE_ON) {
							int key = sm.getData1();
							if (drumHits.containsKey(key)) {
								ArrayList<Long> hitListForDrum = drumHits.get(key);
								hitListForDrum.add(tick);
							} else {
								ArrayList<Long> hitListForDrum = new ArrayList<Long>();
								hitListForDrum.add(tick);
								drumHits.put(key, hitListForDrum);
							}
						}
					}
				}
			}
		}

		return drumHits;
	}
}

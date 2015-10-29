package service;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import generator.HTMSequenceGenerator;

@RestController
public class SequenceController {

	private static final String TRAINING_FILE = "src/main/resources/training-sequences.tsv";

	private static final String DELIMINATOR = "\t";

	private List<Sequence> trainingSequences;

	private HTMSequenceGenerator generator;

	private String lastSequenceReturned = "";

	@RequestMapping("/learn")
	public void learn(@RequestParam(value = "sequence", required = true) String sequenceString,
			@RequestParam(value = "bpm") float bpm, @RequestParam(value = "memoryBank") int memoryBank) {

		StringBuilder sb = new StringBuilder();
		sb.append(memoryBank + "\t");
		sb.append("user_sequence" + "\t");
		sb.append(bpm + "\t");
		sb.append(sequenceString);

		try {
			Files.write(Paths.get(TRAINING_FILE), ("\n" + sb.toString()).getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@RequestMapping("/getSequence")
	public Sequence getSequence(@RequestParam(value = "seedSequence", required = false) String seedSequenceString,
			@RequestParam(value = "bpm") float bpm, @RequestParam(value = "memoryBank") int memoryBank) {

		trainingSequences = loadTrainingData(memoryBank);

		// If the bank is empty, nothing can be trained
		if (trainingSequences.size() == 0) {
			return new Sequence("0000000000000000", bpm);
		}

		String sequenceString = new String();
		boolean validSequence = false;
		boolean seedSequenceWasSet = (seedSequenceString != null);
		while (!validSequence) {

			// Reset the HTM
			generator = new HTMSequenceGenerator();

			List<String> trainingSubset = new ArrayList<String>();
			Map<Sequence, Float> sequenceScores = new HashMap<Sequence, Float>();
			Sequence seedSequence = new Sequence(seedSequenceString, bpm);

			// Select a seed sequence if none was specified
			if (!seedSequenceWasSet) {
				Random randomizer = new Random();
				int randomIndex = randomizer.nextInt(trainingSequences.size());
				seedSequence = trainingSequences.get(randomIndex);
				seedSequenceString = seedSequence.getSequence();
			}

			trainingSubset.add(seedSequenceString);
			trainingSequences.remove(seedSequenceString);
			
			System.out.println("Using  " + seedSequenceString + " as seed.");
			
			// Score all other sequences by their similarity to the selected seed
			for (Sequence sequence : trainingSequences) {
				sequenceScores.put(sequence, seedSequence.getSimilarity(sequence));
			}

			// Select top scoring training sequences
			sequenceScores = MapUtilities.sortByValue(sequenceScores);
			for (Sequence key : sequenceScores.keySet()) {
				int random = ThreadLocalRandom.current().nextInt(0, 10 + 1);
				if (trainingSubset.size() < 2 && random > 6 && !seedSequenceString.equalsIgnoreCase(key.getSequence())) {
					System.out.println(
							"Adding " + key.getSequence() + " as training sequence. Score: " + sequenceScores.get(key));
					trainingSubset.add(key.getSequence());
				}
			}

			// TODO ensure training subset is correct size

			// Select first hit at random from trainingSubset
			int random = ThreadLocalRandom.current().nextInt(0, trainingSubset.size());
			Double firstHit = Double.valueOf(trainingSubset.get(random).substring(0, 1));

			// Train HTM, create and return new sequence
			sequenceString = generator.generate(trainingSubset, firstHit);

			// Ensure the sequence is not too sparse or uniform
			int emptyBeats = StringUtils.countMatches(sequenceString, "0");
			List<String> digitsSeen = new ArrayList<String>();
			for (String digit : new String[] { "0", "1", "2", "3", "4", "5", "6", "7" }) {
				int count = StringUtils.countMatches(sequenceString, digit);
				if (count > 1) {
					digitsSeen.add(digit);
				}
			}
			int sequenceSparsityThreshold = (sequenceString.length() / 2) + 1;
			if (emptyBeats <= sequenceSparsityThreshold && digitsSeen.size() > 2
					&& !lastSequenceReturned.equalsIgnoreCase(sequenceString)) {
				validSequence = true;
			} else {
				if (emptyBeats > sequenceSparsityThreshold) {
					System.out.println("Sequence was too sparse: " + sequenceString + " (contained " + emptyBeats
							+ " empty beats)");
				} else if (digitsSeen.size() < 2) {
					System.out.println("Sequence did not contain enough drum hits: " + sequenceString + " (contained "
							+ digitsSeen.size() + " unique drums)");
				} else if (lastSequenceReturned.equalsIgnoreCase(sequenceString)) {
					System.out.println("Sequence was equal to the last generated sequence: " + sequenceString);
				}

			}
		}

		lastSequenceReturned = sequenceString;

		return new Sequence(sequenceString, bpm);
	}

	private static List<Sequence> loadTrainingData(int bank) {

		BufferedReader reader = null;
		String line = "";
		List<Sequence> sequences = new ArrayList<Sequence>();

		try {
			reader = new BufferedReader(new FileReader(TRAINING_FILE));
			while ((line = reader.readLine()) != null) {
				String[] entry = line.split(DELIMINATOR);
				Sequence sequence = new Sequence(entry[3], Float.parseFloat(entry[2]));
				if (Integer.valueOf(entry[0]) == bank) {
					sequences.add(sequence);
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// Shuffle the list to ensure a random ordering
		long seed = System.nanoTime();
		Collections.shuffle(sequences, new Random(seed));

		return sequences;
	}

}

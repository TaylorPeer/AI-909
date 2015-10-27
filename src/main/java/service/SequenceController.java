package service;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

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

		// Reset the HTM on every request
		generator = new HTMSequenceGenerator();

		List<String> trainingSubset = new ArrayList<String>();
		Map<Sequence, Float> sequenceScores = new HashMap<Sequence, Float>();

		// Score training sequences by request parameters
		if (seedSequenceString != null) {
			// If a seed sequence was received, order all training data by
			// similarity to it
			Sequence seedSequence = new Sequence(seedSequenceString, bpm);
			trainingSubset.add(seedSequenceString);
			for (Sequence sequence : trainingSequences) {
				sequenceScores.put(sequence, seedSequence.getSimilarity(sequence));
			}
		} else {

			// Order all training data by similarity score to received BPM
			for (Sequence sequence : trainingSequences) {
				float bpmScore = Math.abs(bpm - sequence.getBpm());
				sequenceScores.put(sequence, bpmScore);
			}

			// Select a seed sequence
			sequenceScores = MapUtilities.sortByValue(sequenceScores);
			Sequence seedSequence = null;
			for (Sequence key : sequenceScores.keySet()) {
				seedSequence = key;
				break;
			}
			trainingSubset.add(seedSequence.getSequence());
			// Order all training data by similarity to selected seed
			sequenceScores.clear();
			for (Sequence sequence : trainingSequences) {
				sequenceScores.put(sequence, seedSequence.getSimilarity(sequence));
			}
		}

		// Select top scoring training sequences
		sequenceScores = MapUtilities.sortByValue(sequenceScores);
		for (Sequence key : sequenceScores.keySet()) {
			int random = ThreadLocalRandom.current().nextInt(0, 10 + 1);
			if (trainingSubset.size() < 4 && random > 3) {
				System.out.println(
						"Adding " + key.getSequence() + " as training sequence. Score: " + sequenceScores.get(key));
				trainingSubset.add(key.getSequence());
			}
		}

		// TODO ensure training subset is correct size
		System.out.println(trainingSubset.size());

		// Select first hit at random from trainingSubset
		int random = ThreadLocalRandom.current().nextInt(0, trainingSubset.size());
		Double firstHit = Double.valueOf(trainingSubset.get(random).substring(0, 1));

		// Train HTM, create and return new sequence
		String sequence = generator.generate(trainingSubset, firstHit);

		return new Sequence(sequence, bpm);
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

		return sequences;
	}

}

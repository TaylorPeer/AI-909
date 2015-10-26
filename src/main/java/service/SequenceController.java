package service;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	public SequenceController() {
		trainingSequences = loadTrainingData();
		generator = new HTMSequenceGenerator();
	}

	@RequestMapping("/getSequence")
	public Sequence sequence(@RequestParam(value = "seedSequence", required = false) String seedSequenceString,
			@RequestParam(value = "bpm") float bpm, @RequestParam(value = "intensity") float intensity) {

		List<String> trainingSubset = new ArrayList<String>();
		Map<Sequence, Float> sequenceScores = new HashMap<Sequence, Float>();

		// Score training sequences by request parameters (most similar to
		// request parameters)
		if (seedSequenceString != null) {
			Sequence seedSequence = new Sequence(seedSequenceString, bpm);
			for (Sequence sequence : trainingSequences) {
				sequenceScores.put(sequence, seedSequence.getSimilarity(sequence));
			}
		} else {
			for (Sequence sequence : trainingSequences) {
				float bpmScore = Math.abs(bpm - sequence.getBpm());
				float intensityScore = Math.abs(intensity - sequence.getIntensity());
				Float totalScore = (bpmScore + intensityScore) / 2;
				sequenceScores.put(sequence, totalScore);
			}
		}

		// Select top scoring training sequences
		sequenceScores = MapUtilities.sortByValue(sequenceScores);
		for (Sequence key : sequenceScores.keySet()) {
			if (trainingSubset.size() < 3) {
				System.out.println(
						"Adding " + key.getSequence() + " as training sequence. Score: " + sequenceScores.get(key));
				trainingSubset.add(key.getSequence());
			}
		}

		// TODO select first hit at random from trainingSubset
		// TODO pass first hit to generator

		// Train HTM, create and return new sequence
		String sequence = generator.generate(trainingSubset);

		return new Sequence(sequence, bpm);
	}

	private static List<Sequence> loadTrainingData() {

		BufferedReader reader = null;
		String line = "";
		List<Sequence> sequences = new ArrayList<Sequence>();

		try {
			reader = new BufferedReader(new FileReader(TRAINING_FILE));
			while ((line = reader.readLine()) != null) {
				String[] entry = line.split(DELIMINATOR);
				Sequence sequence = new Sequence(entry[2], Float.parseFloat(entry[1]));
				sequences.add(sequence);
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

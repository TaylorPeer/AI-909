package service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import generator.HTMSequenceGenerator;

/**
 * Controller for REST endpoints.
 * 
 * @author taylorpeer
 */
@RestController
public class SequenceController {

	private static final String TRAINING_FILE = "src/main/resources/training-sequences.tsv";

	private static final String DELIMINATOR = "\t";

	private static final int MAX_SEQUENCE_MEMORY = 3;

	private static final int MAX_PRECOMPUTED_SEQUENCES = 10;

	private static final int TRAINING_SEQUENCE_COUNT = 2;

	private HTMSequenceGenerator generator;

	private List<String> lastSequencesReturned = new LinkedList<String>();

	private Multimap<Integer, String> precomputedSequences = HashMultimap.create();

	static Logger log = Logger.getLogger(SequenceController.class.getName());

	/**
	 * Creates a new {@link SequenceController}.
	 */
	public SequenceController() {

		// Begin precomputing new sequences for all available memory banks
		int memoryBankCount = getMemoryBankCount();
		for (int bankIndex = 1; bankIndex <= memoryBankCount; bankIndex++) {
			precomputeSequences(bankIndex);
		}
	}

	/**
	 * Begins precomputing new sequences for a given memory bank in a new thread.
	 * 
	 * @param bankIndex
	 */
	private void precomputeSequences(int bankIndex) {
		PrecomputionThread precomputionThread = new PrecomputionThread(bankIndex);
		precomputionThread.start();
	}

	/**
	 * REST endpoint for retrieving a list of sequences stored in a given memory bank location.
	 * 
	 * @param memoryBank
	 * @return
	 */
	@RequestMapping("/getSequencesForMemoryBank")
	public List<Sequence> getSequencesForMemoryBank(@RequestParam(value = "memoryBank") int memoryBank) {
		return loadTrainingData(memoryBank);
	}

	/**
	 * REST endpoint for adding a new sequence to a memory bank.
	 * 
	 * @param sequenceString
	 * @param bpm
	 * @param memoryBank
	 */
	@RequestMapping("/learn")
	public void learn(@RequestParam(value = "sequence", required = true) String sequenceString,
			@RequestParam(value = "bpm") float bpm, @RequestParam(value = "memoryBank") int memoryBank) {

		log.info("Received request to learn a new sequence");

		// Don't learn silence...
		if (sequenceString.equals("0000000000000000")) {
			log.info("Invalid sequence: " + sequenceString);
			return;
		}

		// Check if sequence already exists in memory bank
		List<Sequence> trainingData = loadTrainingData(memoryBank);
		for (Sequence trainingSequence : trainingData) {
			if (trainingSequence.getSequence().equals(sequenceString)) {
				log.info("Sequence already exists in memory bank: " + sequenceString);
				return;
			}
		}

		log.info("Sequence to learn: " + sequenceString);
		log.info("Storing to memory bank: " + memoryBank);

		StringBuilder sb = new StringBuilder();
		sb.append(memoryBank + "\t");
		sb.append("user_sequence" + "\t");
		sb.append(bpm + "\t");
		sb.append(sequenceString);

		try {
			Files.write(Paths.get(TRAINING_FILE), ("\n" + sb.toString()).getBytes(), StandardOpenOption.APPEND);
			log.info("Sequence stored");
		} catch (IOException e) {
			log.error("Error storing sequence " + sequenceString + " to memory bank " + memoryBank);
			e.printStackTrace();
		}
	}

	/**
	 * REST endpoint for generating new sequences.
	 * 
	 * @param seedSequenceString
	 * @param bpm
	 * @param memoryBank
	 * @return
	 */
	@RequestMapping("/getSequence")
	public Sequence getSequence(@RequestParam(value = "seedSequence", required = false) String seedSequenceString,
			@RequestParam(value = "bpm") float bpm, @RequestParam(value = "memoryBank") int memoryBank) {

		log.info("Received request to generate a new sequence");

		// Refresh training sequences
		List<Sequence> trainingSequences = loadTrainingData(memoryBank);

		// If the memory bank is empty, nothing can be trained
		if (trainingSequences.size() == 0) {
			return new Sequence("0000000000000000", bpm);
		}

		// Check precomputed sequences if a seed sequence was not specified
		if (seedSequenceString == null) {
			Collection<String> sequences = precomputedSequences.get(memoryBank);
			for (String precomputedSequence : sequences) {
				if (!wasSequenceRecentlyReturned(precomputedSequence)) {
					lastSequencesReturned.add(precomputedSequence);
					precomputedSequences.get(memoryBank).remove(precomputedSequence);
					precomputeSequences(memoryBank);
					recordReturnedSequence(precomputedSequence);
					return new Sequence(precomputedSequence, bpm);
				}
			}
		}

		String sequenceString = generateSequence(seedSequenceString, bpm, memoryBank);
		recordReturnedSequence(sequenceString);
		return new Sequence(sequenceString, bpm);
	}

	/**
	 * Records sequence in memory to prevent returning it again in the near future.
	 * 
	 * @param sequenceString
	 */
	private void recordReturnedSequence(String sequenceString) {
		lastSequencesReturned.add(sequenceString);
		while (lastSequencesReturned.size() > MAX_SEQUENCE_MEMORY) {
			lastSequencesReturned.remove(0);
		}
	}

	/**
	 * Generates a new sequence.
	 * 
	 * @param seedSequenceString
	 * @param bpm
	 * @param memoryBank
	 * @return
	 */
	private String generateSequence(String seedSequenceString, float bpm, int memoryBank) {

		// Refresh training sequences
		List<Sequence> trainingSequences = loadTrainingData(memoryBank);

		// If the memory bank is empty, nothing can be trained
		if (trainingSequences.size() == 0) {
			return "0000000000000000";
		}

		String sequenceString = new String();
		boolean isSequenceValid = false;
		boolean seedSequenceWasSet = (seedSequenceString != null);
		while (!isSequenceValid) {

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

			// Score all other sequences by their similarity to the selected seed
			for (Sequence sequence : trainingSequences) {
				sequenceScores.put(sequence, seedSequence.getSimilarity(sequence));
			}

			// Select top scoring training sequences
			sequenceScores = MapUtilities.sortByValue(sequenceScores);
			for (Sequence key : sequenceScores.keySet()) {
				int random = ThreadLocalRandom.current().nextInt(0, 10 + 1);
				int minRandom = 6;
				if (trainingSubset.size() < TRAINING_SEQUENCE_COUNT && random > minRandom
						&& !seedSequenceString.equalsIgnoreCase(key.getSequence())) {
					trainingSubset.add(key.getSequence());
				}
			}

			// Ensure training subset is correct size
			if (trainingSubset.size() != TRAINING_SEQUENCE_COUNT) {
				isSequenceValid = false;
				continue;
			}

			// Select first hit at random from trainingSubset
			int random = ThreadLocalRandom.current().nextInt(0, trainingSubset.size());
			Double firstHit = Double.valueOf(trainingSubset.get(random).substring(0, 1));

			// Train HTM, create and return new sequence
			sequenceString = generator.generate(trainingSubset, firstHit);

			// Validate sequence
			isSequenceValid = isSequenceValid(sequenceString);
		}

		return sequenceString;
	}

	/**
	 * Checks if a sequence string was recently returned to the user.
	 * 
	 * @return
	 */
	private boolean wasSequenceRecentlyReturned(String sequenceString) {
		return lastSequencesReturned.contains(sequenceString);
	}

	/**
	 * Checks if a given sequence string is a valid drum beat.
	 * 
	 * @param sequenceString
	 * @return
	 */
	private boolean isSequenceValid(String sequenceString) {

		String[] drums = { "0", "1", "2", "3", "4", "5", "6", "7" };

		// TODO base sparsity on training sequences!
		// Check sparsity
		int emptyBeats = StringUtils.countMatches(sequenceString, "0");
		int sequenceSparsityThreshold = (sequenceString.length() / 2);
		if (emptyBeats > sequenceSparsityThreshold) {
			log.error("Sequence was too sparse: " + sequenceString + " (contained " + emptyBeats + " empty beats)");
			return false;
		}

		// Check variation
		List<String> digitsSeen = new ArrayList<String>();
		for (String digit : drums) {
			int count = StringUtils.countMatches(sequenceString, digit);
			// Only count if drum hit occured more than once
			if (count > 1) {
				digitsSeen.add(digit);
			}
		}
		if (digitsSeen.size() < 3) {
			log.error("Sequence was not varied enough: " + sequenceString);
			return false;
		}

		// Check for entire measures of silence
		if (sequenceString.contains("0000")) {
			log.error("Sequence contained an entire measure of silence: " + sequenceString);
			return false;
		}

		// Check for long "rolls" of the same drums
		for (String digit : drums) {
			String roll = digit + digit + digit + digit + digit;
			if (sequenceString.contains(roll)) {
				log.error("Sequence contained too long of a drum roll: " + sequenceString);
				return false;
			}
		}

		return true;
	}

	/**
	 * Returns the number of memory banks containing data.
	 * 
	 * @return
	 */
	private static int getMemoryBankCount() {

		BufferedReader reader = null;
		String line = "";
		List<Integer> banks = new ArrayList<Integer>();

		try {
			reader = new BufferedReader(new FileReader(TRAINING_FILE));
			while ((line = reader.readLine()) != null) {
				String[] entry = line.split(DELIMINATOR);
				Integer bankIndex = Integer.valueOf(entry[0]);
				if (!banks.contains(bankIndex)) {
					banks.add(bankIndex);
				}
			}
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
		return banks.size();
	}

	/**
	 * Returns training sequences from the memory bank with the given index.
	 * 
	 * @param bank
	 * @return
	 */
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

	/**
	 * Precomputes new sequences in a separate thread.
	 * 
	 * @author taylorpeer
	 */
	private class PrecomputionThread extends Thread {

		private int bankIndex;

		private List<Sequence> trainingData;

		/**
		 * Creates a {@PrecomputionThread}.
		 * 
		 * @param bankIndex
		 */
		public PrecomputionThread(int bankIndex) {
			this.bankIndex = bankIndex;
			trainingData = loadTrainingData(bankIndex);
		}

		/**
		 * Checks size of precomputed sequence store for this thread's memory bank and generates new sequences if
		 * needed.
		 */
		public void run() {
			int bankSize = precomputedSequences.get(bankIndex).size();
			while (bankSize < MAX_PRECOMPUTED_SEQUENCES) {
				log.info("Memory bank " + bankIndex + " contains " + bankSize + " precomputed sequences.");
				log.info("Precomputing sequences for memory bank " + bankIndex);
				String newSequence = generateSequence(null, trainingData.get(0).getBpm(), bankIndex);
				if (precomputedSequences.get(bankIndex).contains(newSequence)) {
					log.error("Geenrated sequence " + newSequence + " was already precomputed.");
					continue;
				}
				precomputedSequences.put(bankIndex, newSequence);
				bankSize = precomputedSequences.get(bankIndex).size();
			}
			log.info("Finished precomputing sequences for memory bank " + bankIndex);
		}
	}

}

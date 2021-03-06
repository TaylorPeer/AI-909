package generator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.numenta.nupic.ComputeCycle;
import org.numenta.nupic.Connections;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.SDR;
import org.numenta.nupic.algorithms.CLAClassifier;
import org.numenta.nupic.algorithms.ClassifierResult;
import org.numenta.nupic.algorithms.SpatialPooler;
import org.numenta.nupic.algorithms.TemporalMemory;
//import org.numenta.nupic.algorithms.ClassifierResult;
import org.numenta.nupic.encoders.ScalarEncoder;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.util.ArrayUtils;

import gnu.trove.list.array.TIntArrayList;

/**
 * Wrapper class for the HTM. Generates new sequences after being given sample sequences to learn from.
 * 
 * @author taylorpeer
 */
public class HTMSequenceGenerator {

	private Connections memory = new Connections();

	private int columnCount;

	private ScalarEncoder encoder;

	private SpatialPooler spatialPooler;

	private TemporalMemory temporalMemory;

	private CLAClassifier classifier;

	public HTMSequenceGenerator() {

		Parameters params = getParameters();

		// Layer components
		ScalarEncoder.Builder builder = ScalarEncoder.builder().n(150).w(21).radius(1.0).minVal(0).maxVal(8)
				.periodic(false).forced(true).resolution(1);
		encoder = builder.build();
		spatialPooler = new SpatialPooler();
		temporalMemory = new TemporalMemory();
		classifier = new CLAClassifier(new TIntArrayList(new int[] { 1 }), 0.3, 0.05, 0);

		params.apply(memory);
		spatialPooler.init(memory);
		temporalMemory.init(memory);

		columnCount = memory.getPotentialPools().getMaxIndex() + 1;
	}

	/**
	 * Generates a new sequence, given a list of sequences to learn from and the first element that the generated
	 * sequence should begin with.
	 * 
	 * @param sequences
	 * @param firstHit
	 * @return
	 */
	public String generate(List<String> sequences, Double firstHit) {

		Map<String, Object> classification = new LinkedHashMap<String, Object>();

		boolean isResetting = true;
		int charIndex = -1;
		int sequenceIndex = 0;
		for (int sequenceNum = 0; sequenceNum < (sequences.get(0).length() * 2000); sequenceNum++) {

			if (charIndex == (sequences.get(0).length() - 1)) {
				sequenceIndex = sequenceIndex + 1;
				if (sequenceIndex > sequences.size() - 1) {
					sequenceIndex = 0;
				}
			}

			charIndex = (charIndex == (sequences.get(0).length() - 1) ? 0 : charIndex + 1);

			if (charIndex == 0 && isResetting) {
				temporalMemory.reset(memory);
			}

			Double value = Double.parseDouble(String.valueOf(sequences.get(sequenceIndex).charAt(charIndex)));

			// Use current character index for recordNum if re-cycling records, otherwise use the sequence number
			int recordNum = isResetting ? charIndex : sequenceNum;

			int[] output = new int[columnCount];

			// Input through encoder
			int[] encoding = encoder.encode(value);
			int bucketIdx = encoder.getBucketIndices(value)[0];

			spatialPooler.compute(memory, encoding, output, true, true);

			// Let the SpatialPooler train independently (warm up) first
			if (sequenceNum > 1000) {

				int[] input = ArrayUtils.where(output, ArrayUtils.WHERE_1);
				ComputeCycle cc = temporalMemory.compute(memory, input, true);

				classification.put("bucketIdx", bucketIdx);
				classification.put("actValue", value);
				
				Set<Cell> activeCells = cc.activeCells;
				int[] activeCellIndices = SDR.asCellIndices(activeCells);
				classifier.compute(recordNum, classification, activeCellIndices, true, true);
			}
		}

		temporalMemory.reset(memory);

		int[] output = new int[columnCount];

		String stringSequence = String.valueOf(sequences.get(0).charAt(0));
		double value = firstHit;

		for (int i = 0; i < (sequences.get(0).length()); i++) {

			int[] encoding = encoder.encode(value);
			int bucketIdx = encoder.getBucketIndices(value)[0];

			spatialPooler.compute(memory, encoding, output, true, false);

			int[] input = ArrayUtils.where(output, ArrayUtils.WHERE_1);
			ComputeCycle cc = temporalMemory.compute(memory, input, false);

			classification.put("bucketIdx", bucketIdx);
			classification.put("actValue", value);
			
			Set<Cell> activeCells = cc.activeCells;
			int[] activeCellIndices = SDR.asCellIndices(activeCells);
			ClassifierResult<Double> result = classifier.compute(i, classification, activeCellIndices, false, true);

			value = Math.round(result.getMostProbableValue(1));
			if (i < (sequences.get(0).length() - 1)) {
				stringSequence += Math.round(value);
			}

		}

		return stringSequence;
	}

	/**
	 * Returns the parameters the HTM should be created with.
	 * 
	 * @return
	 */
	public static Parameters getParameters() {
		Parameters parameters = Parameters.getAllDefaultParameters();
		parameters.setParameterByKey(KEY.INPUT_DIMENSIONS, new int[] { 150 });
		parameters.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 21 });
		parameters.setParameterByKey(KEY.CELLS_PER_COLUMN, 32);

		// SpatialPooler specific
		parameters.setParameterByKey(KEY.POTENTIAL_PCT, 0.8);
		parameters.setParameterByKey(KEY.GLOBAL_INHIBITIONS, true);
		parameters.setParameterByKey(KEY.LOCAL_AREA_DENSITY, -1.0);
		parameters.setParameterByKey(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 5.0);
		parameters.setParameterByKey(KEY.STIMULUS_THRESHOLD, 1.0);
		parameters.setParameterByKey(KEY.SYN_PERM_INACTIVE_DEC, 0.075075);
		parameters.setParameterByKey(KEY.SYN_PERM_ACTIVE_INC, 0.05);
		parameters.setParameterByKey(KEY.SYN_PERM_TRIM_THRESHOLD, 0.05);
		parameters.setParameterByKey(KEY.SYN_PERM_CONNECTED, 0.1);
		parameters.setParameterByKey(KEY.MIN_PCT_OVERLAP_DUTY_CYCLE, 0.1);
		parameters.setParameterByKey(KEY.MIN_PCT_ACTIVE_DUTY_CYCLE, 0.1);
		parameters.setParameterByKey(KEY.DUTY_CYCLE_PERIOD, 10);
		parameters.setParameterByKey(KEY.MAX_BOOST, 2.0);
		parameters.setParameterByKey(KEY.SEED, 42);
		parameters.setParameterByKey(KEY.SP_VERBOSITY, 0);

		// Temporal Memory specific
		parameters.setParameterByKey(KEY.INITIAL_PERMANENCE, 0.2);
		parameters.setParameterByKey(KEY.CONNECTED_PERMANENCE, 0.8);
		parameters.setParameterByKey(KEY.MIN_THRESHOLD, 5);
		parameters.setParameterByKey(KEY.MAX_NEW_SYNAPSE_COUNT, 10);
		parameters.setParameterByKey(KEY.PERMANENCE_INCREMENT, 0.05);
		parameters.setParameterByKey(KEY.PERMANENCE_DECREMENT, 0.05);
		parameters.setParameterByKey(KEY.ACTIVATION_THRESHOLD, 4);

		return parameters;
	}
}
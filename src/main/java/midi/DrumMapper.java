package midi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class DrumMapper {

	private static Map<Integer, ArrayList<Integer>> drumEncodings = new HashMap<Integer, ArrayList<Integer>>();

	static {

		// Ensure property encodings
		List<Integer[]> combinationsToEncode = new LinkedList<Integer[]>();
		combinationsToEncode.add(new Integer[] {});
		combinationsToEncode.add(new Integer[] { 42 });
		combinationsToEncode.add(new Integer[] { 38 });
		combinationsToEncode.add(new Integer[] { 36 });
		combinationsToEncode.add(new Integer[] { 38, 42 });
		combinationsToEncode.add(new Integer[] { 36, 42 });
		combinationsToEncode.add(new Integer[] { 36, 38 });
		combinationsToEncode.add(new Integer[] { 36, 38, 42 });
		for (Integer[] combination : combinationsToEncode) {
			ArrayList<Integer> asArrayList = new ArrayList<Integer>(Arrays.asList(combination));
			DrumMapper.getDrumEncoding(asArrayList);
		}

	}

	public static int getDrumEncoding(ArrayList<Integer> hits) {
		Collections.sort(hits);
		if (drumEncodings.containsValue(hits)) {
			return getKeyByValue(drumEncodings, hits);
		} else {
			return addNewEncoding(hits);
		}
	}

	public static List<Integer> getHitListFromEncoding(Integer encoding) {
		return drumEncodings.get(encoding);
	}

	public static Map<Integer, ArrayList<Integer>> getEncodings() {
		return drumEncodings;
	}

	private static int addNewEncoding(ArrayList<Integer> hits) {
		int highestKey = -1;
		for (Integer key : drumEncodings.keySet()) {
			if (key > highestKey) {
				highestKey = key;
			}
		}
		int newKey = highestKey + 1;
		drumEncodings.put(newKey, hits);
		return newKey;
	}

	private static <T, E> Integer getKeyByValue(Map<Integer, ArrayList<Integer>> drumEncodings, List<Integer> hits) {
		for (Entry<Integer, ArrayList<Integer>> entry : drumEncodings.entrySet()) {
			if (hits.equals(entry.getValue())) {
				return entry.getKey();
			}
		}
		return null;
	}

}

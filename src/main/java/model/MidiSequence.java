package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.sound.midi.Sequence;

import midi.DrumMapper;
import midi.MidiUtilities;

public class MidiSequence {

	private static final int TICKS_PER_QUARTER_NOTE = 24;

	private String directoryPath;

	private String fileName;

	private Float bpm;

	private int measures = 0;

	private Map<Integer, ArrayList<Long>> drumHits;

	private Map<Integer, ArrayList<Boolean>> hitMatrix = new HashMap<Integer, ArrayList<Boolean>>();

	private String hitVector = new String();

	private List<Integer> drums = new ArrayList<Integer>();

	private Sequence midiSequence;

	public float getBpm() {
		if (bpm == null) {
			bpm = MidiUtilities.getBaseBpm(this.midiSequence);
		}
		return bpm;
	}

	public String getDirectoryPath() {
		return directoryPath;
	}

	public void setDirectoryPath(String directoryPath) {
		this.directoryPath = directoryPath;
	}

	public String getFileName() {
		return fileName;
	}

	public String getHitVector() {
		if (drumHits == null) {
			loadHitData();
		}
		return hitVector;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public Sequence getMidiSequence() {
		return midiSequence;
	}

	public void setMidiSequence(Sequence midiSequence) {
		this.midiSequence = midiSequence;
	}

	public void printDrumTab() {
		if (drumHits == null) {
			loadHitData();
		}

		for (Entry<Integer, ArrayList<Boolean>> entry : hitMatrix.entrySet()) {
			Integer drumIndex = entry.getKey();
			ArrayList<Boolean> hitListings = entry.getValue();
			System.out.print(drumIndex + ": ");
			for (Boolean hit : hitListings) {
				if (hit) {
					System.out.print("o");
				} else {
					System.out.print("-");
				}
			}
			System.out.println();
		}

		ArrayList<Boolean> hitListings = hitMatrix.entrySet().iterator().next().getValue();
		System.out.print("    ");
		for (int i = 0; i < hitListings.size(); i++) {
			if ((double) i % 8 == 0) {
				System.out.print("|");
			} else if ((double) i % 2 == 0) {
				System.out.print("-");
			} else {
				System.out.print(" ");
			}
		}
		System.out.println();

		System.out.print("    ");
		System.out.println(hitVector);
	}

	public Boolean getHit(Integer drumIndex, int measure, int beat) {
		if (drumHits == null) {
			loadHitData();
		}
		for (Entry<Integer, ArrayList<Boolean>> entry : hitMatrix.entrySet()) {
			Integer currentDrumIndex = entry.getKey();
			ArrayList<Boolean> hitListings = entry.getValue();
			if (drumIndex == currentDrumIndex) {
				int queryIndex = (((measure - 1) * 8) + beat) - 1;
				if (queryIndex < hitListings.size()) {
					Boolean hitIndicator = hitListings.get(queryIndex);
					return hitIndicator;
				}
			}
		}
		return false;
	}

	private void quantizeHits() {
		for (Entry<Integer, ArrayList<Long>> entry : drumHits.entrySet()) {
			Integer drumIndex = entry.getKey();
			ArrayList<Long> hitList = entry.getValue();
			ArrayList<Long> roundedHitList = new ArrayList<Long>();
			for (Long hit : hitList) {
				roundedHitList.add(roundToNearest(hit, TICKS_PER_QUARTER_NOTE));
			}
			drumHits.put(drumIndex, roundedHitList);
		}
	}

	private void generateHitMatrix() {
		// Get maximum tick
		Long currentMaximum = 0L;
		for (Entry<Integer, ArrayList<Long>> entry : drumHits.entrySet()) {
			ArrayList<Long> hitList = entry.getValue();
			int size = hitList.size();
			Long lastElement = hitList.get(size - 1);
			if (lastElement > currentMaximum) {
				currentMaximum = lastElement;
			}
		}
		// Round up to next full measure
		currentMaximum = roundUpToNearest(currentMaximum + 1, 8 * TICKS_PER_QUARTER_NOTE);

		for (Entry<Integer, ArrayList<Long>> entry : this.drumHits.entrySet()) {
			Integer drumIndex = entry.getKey();
			drums.add(drumIndex);
			ArrayList<Long> hitList = entry.getValue();
			ArrayList<Boolean> hitListings = new ArrayList<Boolean>();
			for (Long index = 0L; index <= currentMaximum; index = index + TICKS_PER_QUARTER_NOTE) {
				if (hitList.contains(index)) {
					hitListings.add(true);
				} else {
					hitListings.add(false);
				}
			}
			hitMatrix.put(drumIndex, hitListings);
		}
	}

	private void loadHitData() {
		drumHits = MidiUtilities.getDrumHits(this.midiSequence);
		quantizeHits();
		generateHitMatrix();

		int beats = hitMatrix.entrySet().iterator().next().getValue().size();
		measures = beats / 8;

		calculateHitVector();
	}

	private void calculateHitVector() {
		for (int measure = 1; measure <= measures; measure++) {
			for (int beat = 1; beat <= 8; beat++) {
				ArrayList<Integer> drumHitsAtTime = new ArrayList<Integer>();
				for (Integer drum : drums) {
					if (getHit(drum, measure, beat)) {
						drumHitsAtTime.add(drum);
					}
				}
				int drumEncoding = DrumMapper.getDrumEncoding(drumHitsAtTime);
				hitVector += String.valueOf(drumEncoding);
			}
		}
	}

	private Long roundToNearest(Long x, int f) {
		double factor = Math.round((double) x / f);
		Long roundedTo = (long) (f * factor);
		return roundedTo;
	}

	private Long roundUpToNearest(Long x, int f) {
		double factor = Math.ceil((double) x / f);
		Long roundedTo = (long) (f * factor);
		return roundedTo;
	}

	// TODO compare with another drum sequence (return % similarity)

}

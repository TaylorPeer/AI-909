package service;

import java.util.List;

import midi.DrumMapper;

public class Sequence {

	private final String sequence;

	private final float bpm;

	private float intensity;

	public String getSequence() {
		return sequence;
	}

	public float getBpm() {
		return bpm;
	}

	public float getIntensity() {
		return intensity;
	}

	public Sequence(String sequence, float bpm) {
		this.sequence = sequence;
		this.bpm = bpm;
	}

	public float getSimilarity(Sequence anotherSequence) {

		int differences = 0;

		for (int index = 0; index < sequence.length(); index++) {

			Integer seq1DrumHits = Integer.valueOf(sequence.substring(index, index + 1));
			Integer seq2DrumHits = Integer.valueOf(anotherSequence.getSequence().substring(index, index + 1));

			List<Integer> seq1HitList = DrumMapper.getHitListFromEncoding(seq1DrumHits);
			List<Integer> seq2HitList = DrumMapper.getHitListFromEncoding(seq2DrumHits);

			for (Integer seq1Hit : seq1HitList) {
				if (!seq2HitList.contains(seq1Hit)) {
					differences++;
				}
			}

			for (Integer seq2Hit : seq2HitList) {
				if (!seq1HitList.contains(seq2Hit)) {
					differences++;
				}
			}
		}

		float distance = (float) differences / (sequence.length() * 3);
		return 1 - distance;
	}

}

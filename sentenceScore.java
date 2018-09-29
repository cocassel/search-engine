
public class sentenceScore implements Comparable<sentenceScore> {

	public String sentence;
	private double sentenceScore;
	
	public sentenceScore(String sentence, double sentenceScore){
		this.sentence=sentence;
		this.sentenceScore=sentenceScore;
	}
	
	public double getSentenceScore(){
		
		return this.sentenceScore;
	}
	
	public String getSentence(){
		
		return this.sentence;
	}
	
	@Override
	public int compareTo(sentenceScore scoreToCompare) {

		double score1 = this.sentenceScore;
		double score2 = scoreToCompare.getSentenceScore();

		int returnVal = Double.compare(score2, score1);
		
		return returnVal; //+ve if score2>score1

	}

}

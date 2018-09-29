
public class BM25pair implements Comparable<BM25pair> {

	private int docID;
	private double BM25score;
	
	public BM25pair(int docID, double BM25score){
		this.docID=docID;
		this.BM25score=BM25score;
	}
	
	public double getBM25score(){
		
		return this.BM25score;
	}
	
	public int getDocID(){
		
		return this.docID;
	}
	
	@Override
	public int compareTo(BM25pair scoreToCompare) {

		double score1 = this.BM25score;
		double score2 = scoreToCompare.getBM25score();

		int returnVal = Double.compare(score2, score1);
		
		return returnVal; //+ve if score2>score1

	}

}

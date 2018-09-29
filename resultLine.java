import java.util.Comparator;

public class resultLine implements Comparable<resultLine>{
	private String docno;
	private float score;
	
	public resultLine(String docno, float score){
		this.docno=docno;
		this.score=score;	
	}
	
	public float getScore(){
		return this.score;
	}
	public String getDocno(){
		return this.docno;
	}

	@Override
	public int compareTo(resultLine resultLineToCompare) {

		float score1 = this.score;
		float score2 = resultLineToCompare.getScore();

		int returnVal = Float.compare(score2, score1);
		
		if(returnVal==0){
			
			returnVal=resultLineToCompare.getDocno().compareTo(this.docno); // >0 if resultLine1 is alphabetically before resultLine2
		}
		return returnVal; //+ve if score2>score1

	}
}

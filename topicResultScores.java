
public class topicResultScores {
	
	private int topicID;
	private float averagePrecision;
	private float precisionAt10;
	private float NDCGat10;
	private float NDCGat1000;
	private float timeBasedGain;
	
	public topicResultScores(int topicID, float avgPrecision, float precisionAt10, float NDCGat10, float NDCGat1000, float timeBasedGain){
		
		this.topicID=topicID;
		this.averagePrecision= avgPrecision;
		this.precisionAt10=precisionAt10;
		this.NDCGat10=NDCGat10;
		this.NDCGat1000=NDCGat1000;
		this.timeBasedGain=timeBasedGain;
		
	}
	
	public int getTopicID(){
		return this.topicID;
	}
	
	public float getAP(){
		return this.averagePrecision;
	}
	
	public float getP10(){
		return this.precisionAt10;
	}
	
	public float getNDCG10(){
		return this.NDCGat10;
	}
	
	public float getNDCG1000(){
		return this.NDCGat1000;
	}
	
	public float getTBG(){
		return this.timeBasedGain;
	}
}

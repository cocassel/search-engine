import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class evaluateRetrieval{

	public static void main(String[] args) throws IOException {
	
		String resultsFileName="cocassel-hw4-bm25-stem.txt";
				
		String qrelFilePath="/Users/Celeste/Documents/2018/3A/MSCI541/MSCI541-Homework/HW3_Files/qrels/LA-only.trec8-401.450.minus416-423-437-444-447.txt";
		String resultsFilePath="/Users/Celeste/Documents/2018/3A/MSCI541/MSCI541-Homework/src/" + resultsFileName;
		String printResultsFilePath="/Users/Celeste/Documents/2018/3A/MSCI541/MSCI541-Homework/" + "scores." + resultsFileName;
		
		String pathToIndex="/Users/Celeste/Documents/2018/3A/MSCI541/MSCI541-Homework/LAtimesStemmed/";
		HashMap<Integer, metadata> metadata =readInMetadataDoc(pathToIndex); //call function to read in metadata
		HashMap<String, Integer> docnoToInternalID=readInDocnoDoc(pathToIndex);//read in saved hashmap
		HashMap<Integer, ArrayList<String>> qrels=readInQrelsFile(qrelFilePath); //read in qrels
		readInResultsFile(resultsFilePath, printResultsFilePath, qrels, metadata, docnoToInternalID);
	}
	
	public static HashMap<Integer, ArrayList<String>> readInQrelsFile(String qrelFilePath) throws IOException{
		FileReader docReader = new FileReader(qrelFilePath);
		BufferedReader lineReader = new BufferedReader(docReader);
		HashMap<Integer, ArrayList<String>> qrels= new HashMap<Integer, ArrayList<String>>(); //key is the topic number, which maps to arraylists storing all docnos of relevant docs
		
		String currentLine = null;
		int currentTopicID=-1;
		String currentDocno="";
		int currentRelevancy=-1;
		
		while((currentLine = lineReader.readLine()) != null) {
			String[] split = currentLine.split(" ");
			currentTopicID=Integer.parseInt(split[0]);
			currentDocno=split[2];
			currentRelevancy=Integer.parseInt(split[3]);
			if(currentRelevancy>0){ //tests if the current document is relevant for current topic
				if(qrels.containsKey(currentTopicID)){
					qrels.get(currentTopicID).add(currentDocno); //adds the docno of the relevant document to the arraylist for the given topic
				}
				else{
					ArrayList<String> docnos= new ArrayList<String>(); //creates an arraylist for relevant docnos for new topic
					docnos.add(currentDocno);
					qrels.put(currentTopicID, docnos); //adds the topic to the dictionary 
				}
			}
		}
		lineReader.close();
		return qrels;
	}
	
	public static void readInResultsFile(String resultsFilePath, String printResultsFilePath, HashMap<Integer, ArrayList<String>> qrels, 
			HashMap<Integer, metadata> metadata, HashMap<String, Integer> docnoToInternalID) throws IOException{
		
		
		FileReader docReader = new FileReader(resultsFilePath); //to read in student results file
		BufferedReader lineReader = new BufferedReader(docReader);
		
		HashMap <Integer, topicResultScores> calculatedScores= new HashMap<Integer, topicResultScores>(); //hashmap to store precision, AP, NDCG, and time based gain
		ArrayList<resultLine> currentTopicResults = new ArrayList<resultLine>(); //arraylist to store each result line (docno and score) for the current topicID
		
		String currentLine = null;
		int currentTopicID=-1;
		String currentDocno="";
		float currentScore=-1;
		
		while((currentLine = lineReader.readLine()) != null) {
			String[] split = currentLine.split(" ");
			if(split.length!=6){
				System.out.println("This results file is formatted incorrectly");
				lineReader.close();
				return; //exits method
			}
			else{
				try{
					int lastTopicID=currentTopicID; //topicID from last line of results
					currentTopicID=Integer.parseInt(split[0]); //topicID from current line of reults
					if(currentTopicID!=lastTopicID && lastTopicID!=-1){ //tests for a change of topic id
	
						Collections.sort(currentTopicResults); //sort arraylist based on scores
						float ap=getAveragePrecision(lastTopicID,currentTopicResults, qrels);
						float p10= getPrecision(lastTopicID, 10, currentTopicResults, qrels);
						float ndcg10 = getNDCG(lastTopicID, 10, currentTopicResults, qrels);
						float ndcg1000= getNDCG(lastTopicID, 1000, currentTopicResults, qrels);
						float tbg=getTimeBasedGain(lastTopicID, currentTopicResults, qrels, metadata, docnoToInternalID);
						calculatedScores.put(lastTopicID, new topicResultScores(lastTopicID, ap, p10, ndcg10, ndcg1000, tbg));
	
						
						currentTopicResults=new ArrayList<resultLine>(); //empties arraylist of result objects for new topicID
					}
					currentDocno=split[2];
					if(currentDocno.length()!=13){
						System.out.println("This results file is formatted incorrectly");
						return;
					}
					currentScore=Float.valueOf(split[4]);
					currentTopicResults.add(new resultLine(currentDocno, currentScore)); //adds docno and score pair to arraylist for current topicID
				}
				catch(Exception e){
					lineReader.close();
					System.out.println("This results file is formatted incorrectly");
					return;
				}
				
			}
		}
		//fence post issue to get include results from last topic!
		Collections.sort(currentTopicResults); //sort arraylist based on scores
		float ap=getAveragePrecision(currentTopicID, currentTopicResults, qrels);
		float p10= getPrecision(currentTopicID, 10, currentTopicResults, qrels);
		float ndcg10 = getNDCG(currentTopicID, 10, currentTopicResults, qrels);
		float ndcg1000= getNDCG(currentTopicID, 1000, currentTopicResults, qrels);
		float tbg=getTimeBasedGain(currentTopicID, currentTopicResults, qrels, metadata, docnoToInternalID);
		calculatedScores.put(currentTopicID, new topicResultScores(currentTopicID, ap, p10, ndcg10, ndcg1000, tbg));
		
		printScores(calculatedScores, printResultsFilePath); //print out all scores to new file
		lineReader.close();
		
	}
	
	public static void printScores(HashMap <Integer, topicResultScores> calculatedScores, String printResultsFilePath) throws FileNotFoundException, UnsupportedEncodingException{
		PrintWriter docWriter = new PrintWriter(printResultsFilePath, "UTF-8"); //creates new text file for the evaluation results
		
		for(int i=401; i<=450; i++){ //prints average precision for each topic
			if(i!=416 && i!=423 && i!=437 && i!=444 && i!=447){ //ignores topics we are not interested in
				if(calculatedScores.containsKey(i)){
					docWriter.print("ap\t\t\t");
					docWriter.print(calculatedScores.get(i).getTopicID() + "\t");
					docWriter.println(calculatedScores.get(i).getAP());
				}
				else{
					docWriter.print("ap\t\t\t");
					docWriter.print(i + "\t");
					docWriter.println("0");
				}
			}
		}
		for(int i=401; i<=450; i++){ //prints average precision for each topic
			if(i!=416 && i!=423 && i!=437 && i!=444 && i!=447){ //ignores topics we are not interested in
				if(calculatedScores.containsKey(i)){
					docWriter.print("p_10\t\t\t");
					docWriter.print(calculatedScores.get(i).getTopicID() + "\t");
					docWriter.println(calculatedScores.get(i).getP10());
				}
				else{
					docWriter.print("p_10\t\t\t");
					docWriter.print(i + "\t");
					docWriter.println("0");
				}
			}
		}
		for(int i=401; i<=450; i++){ //prints average precision for each topic
			if(i!=416 && i!=423 && i!=437 && i!=444 && i!=447){ //ignores topics we are not interested in
				if(calculatedScores.containsKey(i)){
					docWriter.print("NDCG_10\t\t\t");
					docWriter.print(calculatedScores.get(i).getTopicID() + "\t");
					docWriter.println(calculatedScores.get(i).getNDCG10());
				}
				else{
					docWriter.print("NDCG_10\t\t\t");
					docWriter.print(i + "\t");
					docWriter.println("0");
				}
			}
		}
		
		for(int i=401; i<=450; i++){ //prints average precision for each topic
			if(i!=416 && i!=423 && i!=437 && i!=444 && i!=447){ //ignores topics we are not interested in
				if(calculatedScores.containsKey(i)){
					docWriter.print("NDCG_1000\t\t");
					docWriter.print(calculatedScores.get(i).getTopicID() + "\t");
					docWriter.println(calculatedScores.get(i).getNDCG1000());
				}
				else{
					docWriter.print("NDCG_1000\t\t");
					docWriter.print(i + "\t");
					docWriter.println("0");
				}
			}
		}
		
		for(int i=401; i<=450; i++){ //prints average precision for each topic
			if(i!=416 && i!=423 && i!=437 && i!=444 && i!=447){ //ignores topics we are not interested in
				if(calculatedScores.containsKey(i)){
					docWriter.print("TBG\t\t\t");
					docWriter.print(calculatedScores.get(i).getTopicID() + "\t");
					docWriter.println(calculatedScores.get(i).getTBG());
				}
				else{
					docWriter.print("TBG\t\t\t");
					docWriter.print(i + "\t");
					docWriter.println("0");
				}
			}
		}
		
		docWriter.close();
	}
	
	public static float getAveragePrecision(int topicID, ArrayList<resultLine> currentTopicResults, HashMap<Integer, ArrayList<String>> qrels){
		
		ArrayList<String> relevantDocs = qrels.get(topicID);
		
		float currentPrecision=0;
		float relevantDocsUpUntilRank=0;
		float APnumerator=0;
		float APdenominator=relevantDocs.size(); //number of relevant docs in the collection
		
		for(int i=1; i<currentTopicResults.size() +1; i++){
			if(qrels.get(topicID).contains(currentTopicResults.get(i-1).getDocno())){  //if doc student program judged as relevant is actually relevant
				relevantDocsUpUntilRank+=1;
				float currentRank =i;
				currentPrecision=relevantDocsUpUntilRank/currentRank;
				APnumerator+=currentPrecision;
			}
		}
		float averagePrecision=APnumerator/APdenominator;
		return averagePrecision;
	}
	
	public static float getPrecision(int topicID, int rank, ArrayList<resultLine> currentTopicResults, HashMap<Integer, ArrayList<String>> qrels ){
		float precision=0;
		float relevantDocsUpUntilRank=0;
		
		for(int i=0; i<rank && i<currentTopicResults.size(); i++){
			if(qrels.get(topicID).contains(currentTopicResults.get(i).getDocno())){ //if doc student program judged as relevant is actually relevant
				relevantDocsUpUntilRank+=1;
			}
		}
		precision=relevantDocsUpUntilRank/rank;
		return precision; //at rank 10
	}
	
	public static float getNDCG(int topicID, int rank, ArrayList<resultLine> currentTopicResults, HashMap<Integer, ArrayList<String>> qrels){ //at rank 10 and 1000
		float DCG=0;
		float IDCG=0;
		ArrayList<String> relevantDocs = qrels.get(topicID);
		for(int i=1; i<rank+1 && i<currentTopicResults.size(); i++){
			if(qrels.get(topicID).contains(currentTopicResults.get(i-1).getDocno())){  //if doc student program judged as relevant is actually relevant
				double denominator=Math.log(i+1)/Math.log(2); //change to base 2

				DCG+=((1)/(float)(denominator));
			}
		}
	
		for(int i=1; i<=rank && i<=relevantDocs.size(); i++){ //compute IDCG
			double denominator=Math.log(i+1)/Math.log(2); //change to base 2
			IDCG+=((1)/(float)(denominator));
		}
		
		float NDCG=DCG/IDCG;
		
		return NDCG;
	}
	
	public static float getTimeBasedGain(int topicID, ArrayList<resultLine> currentTopicResults, HashMap<Integer, ArrayList<String>> qrels, 
			HashMap<Integer, metadata> metadata, HashMap<String, Integer> docnoToInternalID){
		float timeBasedGain=0;
		float gainForRelevantDoc=(float) (1*(0.64)*(0.77));
		float timeToReachRankK=0;
		float probStillSearchingAtRankK=1;
		float currentDocLength=0;
		
		for(int i=1; i<=currentTopicResults.size(); i++){ //for whole student results list
			if(qrels.get(topicID).contains(currentTopicResults.get(i-1).getDocno())){  //if doc student program judged as relevant is actually relevant
				
				probStillSearchingAtRankK=(float)Math.pow(Math.E, -1*timeToReachRankK*(Math.log(2))*1/224);
				timeBasedGain+=gainForRelevantDoc*probStillSearchingAtRankK;
				
				String currentDocno=currentTopicResults.get(i-1).getDocno();
				int currentInternalID=docnoToInternalID.get(currentDocno);
				currentDocLength=metadata.get(currentInternalID).getDocLength();
				timeToReachRankK+=4.4 + (0.018*currentDocLength +7.8)*0.64;
				
			}
			else{ // document student program judged as relvant is actually not relevant
				String currentDocno=currentTopicResults.get(i-1).getDocno();
				int currentInternalID=docnoToInternalID.get(currentDocno);
				currentDocLength=metadata.get(currentInternalID).getDocLength();
				timeToReachRankK+=4.4 + (0.018*currentDocLength +7.8)*0.39;
			}
		}
		
		return timeBasedGain;
	}

	
	
	public static HashMap<Integer, metadata> readInMetadataDoc(String pathToIndex) throws IOException{ //recreates hashmap of internal IDs to metadata
		
		HashMap<Integer, metadata> IDtoMetadata = new HashMap<Integer, metadata>(); //key is internal doc ID and value is metadata
        
		FileReader docReader = new FileReader(pathToIndex + "internalIDtoMetadata.txt");
		BufferedReader lineReader = new BufferedReader(docReader);
		
		String currentLine = null;
		int internalID= -1;
		String docno= null;
		String date= null;
		String headline= null;
		int docLength=-1;
		
        while((currentLine = lineReader.readLine()) != null) {
        	if(currentLine.contains("{")){ //identifies new document
        		currentLine = lineReader.readLine();//skips to the Internal_ID line
        		String[] split = currentLine.split(" ");
        		internalID=Integer.parseInt(split[1]); //identifies internal ID
        		
        		currentLine = lineReader.readLine();//skips to the docno line
        		split = currentLine.split(" ");
        		docno=split[1]; //identifies internal ID
        		
        		currentLine = lineReader.readLine();//skips to the doc length line
        		split = currentLine.split(" ");
        		docLength=Integer.parseInt(split[1]); //identifies internal ID
        		
        		currentLine = lineReader.readLine();//skips to the date line
        		split = currentLine.split(" ");
        		date=split[1]; //identifies date
        		
        		headline=lineReader.readLine(); //reads the first line of the headline
        		headline=headline.substring(10); //gets rid of "Headline: "
        		while(!(currentLine = lineReader.readLine()).equals("}")){
        			headline= headline + "\n" + currentLine;
        		}
        		if(headline.isEmpty()){
        			headline="None";
        		}
        		
        	}       	
        	IDtoMetadata.put(internalID, new metadata(internalID, docno, date, headline, docLength)); //add internal ID and metadata object to hashmap
        }   

        lineReader.close();    
        
		return IDtoMetadata;
	}
	
	
	public static HashMap<String, Integer> readInDocnoDoc(String pathToDoc) throws IOException{ //recreates hashmap of 
		
		HashMap<String, Integer> docnoToInternalID = new HashMap<String, Integer>(); //key is docno and value is internal doc ID
		
		FileReader docReader = new FileReader(pathToDoc + "docnoToInternalID.txt");
		BufferedReader lineReader = new BufferedReader(docReader);
		
		String currentLine = null;
		String currentDocno = null;
		int currentInternalId=-1;
		
		while((currentLine = lineReader.readLine()) != null) {
			String[] split = currentLine.split(" ");
			currentDocno=split[0];
			currentDocno= currentDocno.substring(0, currentDocno.length()-1);//removes colon
			currentInternalId=Integer.parseInt(split[1]);
			docnoToInternalID.put(currentDocno, currentInternalId);
        }   

        lineReader.close(); 
		
		return docnoToInternalID;
		
	}
}

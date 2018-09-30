import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;

public class getQueryResults {

	public static void main(String[] args) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
		
		System.out.println("Please enter the path to the location of the index file");
		String pathToIndex=scanner.next();
		
		System.out.println("Please enter the name of the queries file");
		String queriesFileName=scanner.next();
		queriesFileName="queries.txt";
		
		System.out.println("Please enter your prefered name for the results file");
		String resultsFileName=scanner.next();
		resultsFileName="cocassel-hw4-bm25-baseline.txt";
		
		HashMap<Integer, ArrayList<Integer>> invIndex = readInIndex(pathToIndex); //calls the function to read in the index from text file
		HashMap<String, Integer> lexicon1 = readInLexicon(pathToIndex);	//call function to read in lexicon
		HashMap<Integer, metadata> metadata =readInMetadataDoc(pathToIndex); //call function to read in metadata
		
		getResults(invIndex, lexicon1, metadata, queriesFileName, resultsFileName); //calls function that performs search for given query file and prints results to a text file
		
		scanner.close();
	}
	
	public static void getResults(HashMap<Integer, ArrayList<Integer>> invIndex, HashMap<String, Integer> lexicon1, HashMap<Integer, metadata> metadata, String queriesFileName, String resultsFileName) throws IOException{
		
		String filePath= "/Users/Celeste/Documents/2018/3A/MSCI541/MSCI541-Homework/src/";
		FileReader docReader = new FileReader(filePath + queriesFileName); //queries text file
		BufferedReader lineReader = new BufferedReader(docReader);
		
		PrintWriter resultsWriter = new PrintWriter(filePath + resultsFileName, "UTF-8"); //creates new text file to print results to 
		
		double avgLength=getAverageDocLength(metadata)[0];
		double totalDocCount=getAverageDocLength(metadata)[1];

		
		String currentLine = null;
		int topicID=-1;
		String query=null;
		
		while((currentLine = lineReader.readLine()) != null) { //goes through each query
			topicID=Integer.parseInt(currentLine);
			currentLine = lineReader.readLine(); //skips to the line with the query
			query=currentLine; //get query string
			processQuery(avgLength, totalDocCount, invIndex,lexicon1, metadata, query, topicID, resultsWriter);//send query to function that will process it (i.e. documents are searched for query's terms)
		}
		
		lineReader.close(); 
		resultsWriter.close();
	}
	
	public static void processQuery(double avgDocLength, double totalDocCount, HashMap<Integer, ArrayList<Integer>> invIndex, HashMap<String, Integer> lexicon1, HashMap<Integer, metadata> metadata, String query, int topicID, PrintWriter resultsWriter){
		
		ArrayList<String> queryTokens = new ArrayList<String>();
		tokenizeQuery(query, queryTokens); //call function to tokenize query
		
		HashMap<Integer, Double> BM25scores = new HashMap<Integer, Double>(); //key is doc id, which maps to BM25 for that document for the given query

		
		double k1=1.2;
		double k2=7;
		double b=0.75;
		
		for(int i=0; i<queryTokens.size(); i++){//for each term in query
			
			String currentQueryTerm= queryTokens.get(i);
			
			if(lexicon1.containsKey(currentQueryTerm)){ //tests if the query term is in the lexicon --if it's not we know the collection doesn't contain that term resulting in a partial score of 0
				
				int termId= lexicon1.get(currentQueryTerm); //uses lexicon to get termID for given query term string
				ArrayList<Integer> postings= invIndex.get(termId); //get postings list for query term
				
				
				double numberOfDocsWithQueryTerm=postings.size()/2; //size of postings list divided by 2, which is the number of docs in the collection that contain that word
				double frequencyOfQueryTermInQuery=0; //count of current query term in query
				for(int g=0; g<queryTokens.size(); g++){
					if(queryTokens.get(g).equals(currentQueryTerm)){
						frequencyOfQueryTermInQuery++;
					}
					
				}
						
				for(int j=0; j<postings.size(); j+=2){ //for each docID in postings list
					int docID=postings.get(j); //gets the docID from postings list
					
					double docLength=metadata.get(docID).getDocLength(); //gets current doc length
					double K = k1*((1-b) + b*docLength/avgDocLength); //computes K for BM25 equation
					
					double frequencyOfQueryTermInDoc=postings.get(j+1); //gets count of word in document
					
					double A= (k1 +1)*frequencyOfQueryTermInDoc/(K+frequencyOfQueryTermInDoc); //first multiplicative term in BM25 equation
					double B= (k2 +1)*frequencyOfQueryTermInQuery/(k2 + frequencyOfQueryTermInQuery); //second multiplicative term in BM25 equation
					double C= Math.log(( totalDocCount - numberOfDocsWithQueryTerm +0.5)/(numberOfDocsWithQueryTerm +0.5)); //third multiplicative term in BM25 equation
					
					double partialScoreForCurrentQuery=A*B*C; //partial score for current query term for given doc
					
					if(BM25scores.containsKey(docID)){ //if docID exists in BM25 scores dictionary
						BM25scores.replace(docID, BM25scores.get(docID)+partialScoreForCurrentQuery); //adds new partial score to the old partial score 
					}
					else{ //if docID does not exist in BM25 scores dictionary
						BM25scores.put(docID, partialScoreForCurrentQuery); //adds the docID to the dictionary and initiates the score with partial score
					}
				}
					
			}
			
		}
		
		ArrayList<BM25pair> resultSet= new ArrayList<BM25pair>(); //creates a list for the BM25 scores in order to sort them and keep top 1000
		
		for (Integer docID : BM25scores.keySet()) { //for reach docID in BM25 scores for given query term
			resultSet.add(new BM25pair(docID, BM25scores.get(docID))); //put all results in array
		}
			
		Collections.sort(resultSet);//sort array by BM25score
		
		String Q0="Q0"; //same for all docs
		String runTag="cocasselBM25"; //same for all docs
		
		
		for(int k=0; k<resultSet.size() && k<1000; k++ ){ //for each document in the result set but keeping the top 1000 if more than 1000 results
			int docID=resultSet.get(k).getDocID();
			String docno = metadata.get(docID).getDocno(); //retrieves the docno for the given document ID
			int rank=k +1;
			double score = resultSet.get(k).getBM25score();
			
			resultsWriter.println(topicID + " " + Q0 + " " + docno + " " + rank + " " + score + " " + runTag);	
				
			
		}
	}
	
	public static double[] getAverageDocLength(HashMap<Integer, metadata> metadata){ //returns average doc length and total number of docs in collection
		double averageDocLength=0;
		double numberOfDocsInCollection=0;
		
		for (Integer docID : metadata.keySet()) {
			numberOfDocsInCollection+=1;
			averageDocLength+= metadata.get(docID).getDocLength();
		}
		averageDocLength=averageDocLength/numberOfDocsInCollection;
		
		double[] averageDocLengthAndTotalDocCount= new double[2];
		averageDocLengthAndTotalDocCount[0]=averageDocLength;
		averageDocLengthAndTotalDocCount[1]=numberOfDocsInCollection;
		return averageDocLengthAndTotalDocCount;
		
	}

	public static void tokenizeQuery(String query, ArrayList<String> queryTokens){ // tokenizes a document line and adds to doc's token list
		
		int start=0;
		int j=0;
		for(j=0; j<query.length(); j++){ //iterates through each character of string
			char currentChar=query.charAt(j); //gets the character at spot j of string
			if(!(Character.isDigit(currentChar)) && !(Character.isLetter(currentChar))){ //if character is not a number or letter
				if(start!=j){
					String token=query.substring(start, j);
					queryTokens.add(token.toLowerCase());
				}
				start=j+1;
			}
		}
		if(start!=j){
			queryTokens.add(query.substring(start, j).toLowerCase()); //catches last token
		}
		
	}
	
	public static HashMap<Integer, ArrayList<Integer>> readInIndex(String pathToIndex) throws IOException{ //recreates hashmap for index
		
		HashMap<Integer, ArrayList<Integer>> invIndex= new HashMap<Integer, ArrayList<Integer>>(); //creates the index
		
		FileReader docReader = new FileReader(pathToIndex + "invIndex.txt");
		BufferedReader lineReader = new BufferedReader(docReader);
		
		String currentLine = null;
		int currentTerm = -1;
	
		while((currentLine = lineReader.readLine()) != null) {
			currentTerm=Integer.parseInt(currentLine); //gets the term id
			currentLine = lineReader.readLine(); //skips to the next line where the posting list is held for the above term id
			
			String[] split = currentLine.split(" ");
			
			ArrayList<Integer> postings= new ArrayList<Integer>(); //create a postings list
			
			postings.add(Integer.parseInt(split[0].substring(1, split[0].length()-1))); //adds the first number of the line to the postings list (without the square bracket and comma)
			for(int i=1; i<split.length; i++){ //iterates through postings list entries for term at hand
				postings.add(Integer.parseInt(split[i].substring(0, split[i].length()-1))); //adds number without comma (or square bracket for last one) to postings list
			}
			invIndex.put(currentTerm, postings);
			
        }   

        lineReader.close(); 
		
		return invIndex;
		
	}
	
	public static HashMap<String, Integer> readInLexicon(String pathToIndex) throws IOException{ //read in lexicon here
		HashMap<String, Integer> lexicon1 = new HashMap<String, Integer>();//lexicon that maps from terms to term IDs
		
		FileReader lexiconReader = new FileReader(pathToIndex+ "lexicon1.txt");
		BufferedReader lineReader = new BufferedReader(lexiconReader);
		
		String currentLine = null;
		
		while((currentLine = lineReader.readLine()) != null) {
			String[] split = currentLine.split(": ");
			String term= split[0];
			int termID=Integer.parseInt(split[1]);
			lexicon1.put(term, termID);
		}
		lineReader.close();
		return lexicon1;
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

}

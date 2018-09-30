import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class BooleanAND {

	public static void main(String[] args) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
		
		System.out.println("Please enter the path to the location of the index file");
		String pathToIndex=scanner.next();
		pathToIndex="/Users/Celeste/Documents/2018/3A/MSCI541/MSCI541-Homework/LAtimesDocs/";
		//pathToIndex="/Users/Celeste/Documents/2018/3A/MSCI541/MSCI541-Homework/LAtimesSampleDocs/";
		
		System.out.println("Please enter the name of the queries file");
		String queriesFileName=scanner.next();
		queriesFileName="queries.txt";
		//queriesFileName="cocasselQueries.txt";
		
		System.out.println("Please enter your prefered name for the results file");
		String resultsFileName=scanner.next();
		resultsFileName="cocassel-hw2-results.txt";
		//resultsFileName="hw2-results-cocassel-part2a.txt";
		
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
		
		String currentLine = null;
		int topicID=-1;
		String query=null;
		
		while((currentLine = lineReader.readLine()) != null) { //goes through each query
			topicID=Integer.parseInt(currentLine);
			currentLine = lineReader.readLine(); //skips to the line with the query
			query=currentLine; //get query string
			processQuery(invIndex,lexicon1, metadata, query, topicID, resultsWriter);//send query to function that will process it (i.e. documents are searched for query's terms)
		}
		
		lineReader.close(); 
		resultsWriter.close();
	}
	
	public static void processQuery(HashMap<Integer, ArrayList<Integer>> invIndex, HashMap<String, Integer> lexicon1, HashMap<Integer, metadata> metadata, String query, int topicID, PrintWriter resultsWriter){
		
		HashMap<Integer, Integer> docCount = new HashMap<Integer, Integer>(); //key is doc id, which maps to count of query terms in that document
		
		ArrayList<String> queryTokens = new ArrayList<String>();
		tokenizeQuery(query, queryTokens); //call function to tokenize query
		
		for(int i=0; i<queryTokens.size(); i++){//for each term in query
			if(lexicon1.containsKey(queryTokens.get(i))){ //tests if the query term is in the lexicon --if it's not we know the collection doesn't contain that term  and therefore booleanAND returns 0 results
				int termId= lexicon1.get(queryTokens.get(i)); //uses lexicon to get termID for given term string
				ArrayList<Integer> postings= invIndex.get(termId); //get postings list for query term
				for(int j=0; j<postings.size(); j+=2){ //for each docID in postings list
					int docID=postings.get(j); //gets the docID from postings list
					if(docCount.containsKey(docID)){ //if docID exists in docCount dictionary
						docCount.replace(docID, docCount.get(docID)+1); //adds 1 to the count of the term in that doc
					}
					else{ //if docID does not exist in docCount dictionary
						docCount.put(docID, 1); //puts docID in docCounts dictionary with count=1
					}
				}
				ArrayList<Integer> resultSet= new ArrayList<Integer>(); //creates a list for the results (i.e. docs that contain both query terms)
				for (Integer docID : docCount.keySet()) { //for each documentID in docCounts dictionary
					if(docCount.get(docID)==queryTokens.size()){
						resultSet.add(docID);
					}
				}
				
				String Q0="Q0"; //same for all docs
				String runTag="cocasselAND"; //same for all docs
				
				for(int k=0; k<resultSet.size(); k++ ){ //for each document in the result set
					int docID=resultSet.get(k);
					String docno = metadata.get(docID).getDocno(); //retrieves the docno for the given document ID
					int rank=k +1;
					int score=resultSet.size() - rank ; //number of documents returned
					resultsWriter.println(topicID + " " + Q0 + " " + docno + " " + rank + " " + score + " " + runTag);
					
				}
			}
		}
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;

public class searchEngine {

	public static void main(String[] args) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
		
		System.out.println("Please enter the path to the location of the index file");
		String pathToIndex=scanner.nextLine();
		pathToIndex="/Users/Celeste/Documents/2018/3A/MSCI541/MSCI541-Homework/LAtimesDocs/";
		System.out.println("Please wait while the collection information is loaded");
		
		HashMap<Integer, ArrayList<Integer>> invIndex = readInIndex(pathToIndex); //calls the function to read in the index from text file
		HashMap<String, Integer> lexicon1 = readInLexicon(pathToIndex);	//call function to read in lexicon
		HashMap<Integer, metadata> metadata =readInMetadataDoc(pathToIndex); //call function to read in metadata
		System.out.println("Collection information has been loaded \n");
			
		//fence-post starts here
		System.out.println("Please enter your query");
		String query=scanner.nextLine();
		String response;
		
		ArrayList<Integer> top10 = getResults(pathToIndex, invIndex, lexicon1, metadata, query); //calls function that performs search for given query file and prints results to a text file
		if(!top10.isEmpty()){ //if there is at least one result
			System.out.println("If you woud like to view one of these documents, enter the number of the document you would like to view. "
					+ "\nIf you would like to quit, enter \"Q\"."
					+ "If you would like to enter a new query, please enter \"N\"");
			response=scanner.nextLine();
		}
		else{ //if there are no results returned for the query.
			System.out.println("No documents were retrieved for this query. If you would like to enter a new query, enter \"N\". "
			+ "If you would like to quit, enter \"Q\"");
			response=scanner.nextLine();
		}
		
		//end of fence-post
		
		while(!(response.equals("Q"))){ // while the users entry does not equal "Q"
			if(response.equals("N")){ //new query
				System.out.println("Please enter your new query");
				String newQuery=scanner.nextLine();//reads in new query
				top10 = getResults(pathToIndex, invIndex, lexicon1, metadata, newQuery); //calls function that performs search for given query file and prints results to a text file
				if(!top10.isEmpty()){ //if there is at least one result
					System.out.println("If you woud like to view one of these documents, enter the number of the document you would like to view. "
						+ "\nIf you would like to quit, enter \"Q\"."
						+ "If you would like to enter a new query, please enter \"N\"");
				}
				else{ //if there are no results returned for the query.
					System.out.println("No documents were retrieved for this query. If you would like to enter a new query, enter \"N\". "
					+ "\nIf you would like to quit, enter \"Q\"");
				}
			}
		
			else{
				boolean isNumber = response.chars().allMatch(Character::isDigit); //tests if all characters are numerics
				if(isNumber==true && Integer.parseInt(response)>0 && Integer.parseInt(response)<=10){ //tests if the number is between 1 and 10
					
					
					//print the raw document
					int docNum=Integer.parseInt(response); //user enters number out of 10 for the doc they want to see
					int docID=top10.get(docNum-1); //get doc id from arraylist of top 10 results
					System.out.println();
					printDocFromId(docID, pathToIndex, metadata);
					System.out.println();
					
					System.out.println("\nIf you woud like to view another document, enter the number of the document you would like to view. "
							+ "\nIf you would like to quit, enter \"Q\"."
							+ "If you would like to enter a new query, please enter \"N\"");
				}
				else{  //user has entered an invalid option
					System.out.println("That is not a valid option. "
							+ "If you woud like to view one of these documents, enter the number of the document you would like to view. "
							+ "\nIf you would like to quit, enter \"Q\"."
							+ "If you would like to enter a new query, please enter \"N\"");
				}
				
			}
			response=scanner.nextLine();
			
		}
		System.out.println("Thank you for using this search engine! Have a good day!");
		scanner.close();
	}
	
	public static ArrayList<Integer> getResults(String filePath, HashMap<Integer, ArrayList<Integer>> invIndex, HashMap<String, Integer> lexicon1, HashMap<Integer, metadata> metadata, String query) throws IOException{
		
		
		double avgLength=getAverageDocLength(metadata)[0];
		double totalDocCount=getAverageDocLength(metadata)[1];

		ArrayList<Integer> top10 = processQuery(filePath, avgLength, totalDocCount, invIndex,lexicon1, metadata, query);//send query to function that will process it (i.e. documents are searched for query's terms)
		
		return top10;
		
	}
	
	public static ArrayList<Integer> processQuery(String filePath, double avgDocLength, double totalDocCount, HashMap<Integer, ArrayList<Integer>> invIndex, HashMap<String, Integer> lexicon1, HashMap<Integer, metadata> metadata, String query) throws IOException{
		
		long start=System.currentTimeMillis();
		
		
		
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
		
		
		ArrayList<Integer> top10= new ArrayList<Integer>(); //array that stores docids of top 10 docs
		
		for(int k=0; k<resultSet.size() && k<10; k++ ){ //for each document in the result set but keeping the top 10 if more than 10 results
			
			int docID=resultSet.get(k).getDocID();
			top10.add(docID); //adds docID to arraylist
			String docno = metadata.get(docID).getDocno(); //retrieves the docno for the given document ID
			int rank=k +1;
			
			ArrayList<String> rawDoc= getDocFromId(filePath,metadata,docID);//calls function to read in raw document
			
			//tokenize doc and used tokenized doc for headline 
			String snippet =getQueryBiasedSummary(docID, rawDoc, queryTokens); //calls function to get query biased summary strin
			
			//print top 10 results to console 
			System.out.print(rank + ". ");
			String headline = metadata.get(docID).getHeadline();
			
			if(!headline.equals("None")){ //test if headline isn't empty
				System.out.print(headline);
			}
			
			else{
				
				if(snippet.length()==0){
					System.out.print("A document summary is not available");
				}
				else if(snippet.length()<=50){
					System.out.println(snippet);
				}
				else{
					System.out.print(snippet.substring(0, 50));
				}
	
				System.out.print("...");
			}
			
			String date=metadata.get(docID).getDate().replace("-", "/");
			System.out.println(" (" + date.substring(3) + "/19" + date.substring(0, 2) + ")");
			
			
			if(snippet.length()!=0){
				System.out.print(snippet);
			}
			System.out.println(" (" + docno + ")");
			System.out.println();
			
		}
		
		//show time of retrieval
		long end = System.currentTimeMillis();
		long timeOfRetrieval = end-start;
		System.out.println("\nRetrieval took " + (double)timeOfRetrieval/(double)1000 + " seconds");
		
		return top10;
		
	}
	
	public static ArrayList<String> getDocFromId(String filePath, HashMap<Integer, metadata> IDtoMetadata, int internalID) throws IOException{
		Scanner scanner = new Scanner(System.in);
			
		String dateOfDoc=IDtoMetadata.get(internalID).getDate(); //retrieves date of document
		ArrayList<String> rawDoc= new ArrayList<String>(); //arraylist to store raw doc lines
		
		//raw doc
		FileReader docReader = new FileReader(filePath + "/" +dateOfDoc + "/" +Integer.toString(internalID)+ ".txt");
		BufferedReader lineReader = new BufferedReader(docReader);
		String currentLine = null;
		while((currentLine = lineReader.readLine()) != null) {
			rawDoc.add(currentLine);
        }  
		lineReader.close(); 
		scanner.close();
		return rawDoc;
	}
	
	public static void printDocFromId(int internalID, String filePath, HashMap<Integer, metadata> IDtoMetadata) throws IOException{
		
		String dateOfDoc=IDtoMetadata.get(internalID).getDate(); //retrieves date of document
		
		
		FileReader docReader = new FileReader(filePath + "/" +dateOfDoc + "/" +Integer.toString(internalID)+ ".txt");
		BufferedReader lineReader = new BufferedReader(docReader);
		String currentLine = null;
		while((currentLine = lineReader.readLine()) != null) {
			System.out.println(currentLine);
        }  
		lineReader.close(); 
	}

	public static String getQueryBiasedSummary(int docID, ArrayList<String> rawDoc, ArrayList<String> queryTokens){
		
		ArrayList<sentenceScore> sentences = new ArrayList<sentenceScore>(); //arraylist holding sentence objects (stores sentence string and sentence score)

		splitIntoSentences(rawDoc, sentences, queryTokens); //calls function to split the raw doc into sentences
		
		Collections.sort(sentences); //sorts the array by score of sentences
		if(sentences.size()!=0 && sentences.get(0).sentence.substring(0,1).equals(" ")){
			sentences.get(0).sentence=sentences.get(0).sentence.substring(1); //removes first space of first sentence
		}
		
		String snippet ="";
		for(int j=0; j<3 && j<sentences.size(); j++){ //prints out the top 3 scoring sentences or less if the document has less than 3 sentences
			snippet+=(sentences.get(j).getSentence());
		
		}
		return snippet;
	}
	
	public static String getDocString(ArrayList<String> rawDoc){
		String document = "";
		for(int i=0; i<rawDoc.size(); i++){ //concatenates entire document into one string but only includes text in between p tags and only consider p tags in between text tags
			if(rawDoc.get(i).equals("<TEXT>")){ //finds text tag
				i++;
				while(!rawDoc.get(i).equals("</TEXT>")){ //finds end of text tag
					if(rawDoc.get(i).equals("<P>")){
						i=i+1; //advances i passed p tag
						while(!rawDoc.get(i).equals("</P>")){ //finds end of tag
							document += rawDoc.get(i); 
							i++;
						}
						i++; //advances passed end of p tag
					}
					else{
						i++;
					}
				}
			}
			else if(rawDoc.get(i).equals("<GRAPHIC>")){ //finds text tag
				i++;
				while(!rawDoc.get(i).equals("</GRAPHIC>")){ //finds end of text tag
					if(rawDoc.get(i).equals("<P>")){
						i=i+1; //advances i passed p tag
						while(!rawDoc.get(i).equals("</P>")){ //finds end of tag
							document += rawDoc.get(i); 
							i++;
						}
						i++; //advances passed end of p tag
					}
					else{
						i++;
					}
				}
			}
		}
		return document;
	}
	
	public static void splitIntoSentences(ArrayList<String> rawDoc, ArrayList<sentenceScore> sentences, ArrayList<String> queryTokens){
		String document = getDocString(rawDoc);
		int start=0;
		int j;
		for(j=0; j<document.length(); j++){ //iterates through each character of document string
			char currentChar=document.charAt(j); //gets the character at spot j of string
			if(currentChar=='!' || currentChar=='?' || currentChar=='.' ){ //if character is a period, question mark, or exclamation mark
				
				if(start!=j){
					String sentence=document.substring(start, j+1);
					//get sentence score
					double sentenceScore=getSentenceScore(sentence, queryTokens);
					
					sentences.add(new sentenceScore(sentence, sentenceScore));
				}
				start=j+1;
			}
		}
		if(start!=j){
			String sentence=document.substring(start);
			//get sentence score
			double sentenceScore=getSentenceScore(sentence, queryTokens);;
			sentences.add(new sentenceScore(sentence, sentenceScore)); //catches last token
		}
	
		
		
	}
	
	public static double getSentenceScore(String sentence, ArrayList<String> queryTokens){
		ArrayList<String> sentenceTokens = new ArrayList<String>();
		tokenizeString(sentence, sentenceTokens); 
		
		double sentenceScore=0;
		boolean[] queryTermInSentence=new boolean[queryTokens.size()]; //creates an array of booleans to keep track if the query word is in the sentence at least once
		
		for(int j=0; j<queryTokens.size(); j++){ //for each query token
			for(int i=0; i<sentenceTokens.size(); i++){ //for each sentence token
				if(sentenceTokens.get(i).equals(queryTokens.get(j))){ //if sentence token matches query token
					sentenceScore+=1; //increase score by 1
					queryTermInSentence[j]=true; //query term j occurs at least once in sentence
				}
			}
		}
		int distinctQueryWordsInSentence=0;
		for(int l=0; l<queryTermInSentence.length; l++){ //counts the number of true values
			distinctQueryWordsInSentence+=1; 
		}
		
		sentenceScore+=distinctQueryWordsInSentence;
		
		return sentenceScore;
		
		
	}
	
	public static void tokenizeString(String currentLine, ArrayList<String> docTokens){ // tokenizes a document line and adds to doc's token list
		int start=0;
		int j=0;
		for(j=0; j<currentLine.length(); j++){ //iterates through each character of string
			char currentChar=currentLine.charAt(j); //gets the character at spot j of string
			if(!(Character.isDigit(currentChar)) && !(Character.isLetter(currentChar))){ //if character is not a number or letter
				if(start!=j){
					String token=currentLine.substring(start, j);
					docTokens.add(token.toLowerCase());
				}
				start=j+1;
			}
		}
		if(start!=j){
			docTokens.add(currentLine.substring(start, j).toLowerCase()); //catches last token
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

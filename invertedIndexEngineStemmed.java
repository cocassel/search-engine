import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.zip.GZIPInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class invertedIndexEngineStemmed {

	public static void main(String[] args) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
		
		System.out.println("Please enter the path to the latimes.gz file");
		String filePath=scanner.next();
		filePath="/Users/Celeste/Documents/2018/3A/MSCI541/latimes.gz";
		//filePath="/Users/Celeste/Documents/2018/3A/MSCI541/latimesSampleDocs.gz";
		File laTimes=new File(filePath);
		while(!laTimes.exists()){
			System.out.println("The latimse.gz file is not in this path. Please enter a valid path.");
			filePath=scanner.next();
			laTimes=new File(filePath);
		}
		if(filePath.isEmpty()){
			System.out.println("That is not a valid file path. You must enter the filepath to the latimes.gz file.");
		}
		
		System.out.println("Where would you like the documents and metadata to be stored? Please enter a path to a new directory.");
		String pathToStoreDocs=scanner.next();
		//pathToStoreDocs="/Users/Celeste/Documents/2018/3A/MSCI541/MSCI541-Homework/LAtimesDocs";
		pathToStoreDocs="/Users/Celeste/Documents/2018/3A/MSCI541/MSCI541-Homework/LAtimesStemmed";
		if(pathToStoreDocs.isEmpty()){
			System.out.println("That is not a valid file path. Please enter a valid file path. ");
		}
		
		File storeDocs = new File(pathToStoreDocs);
		if(storeDocs.exists()){
			System.out.println("This directory already exists! You must enter a new directory");
		}
		else{
			gzip(filePath, pathToStoreDocs);
		}
		scanner.close();
	}
	
	public static void gzip(String filePath, String pathToStoreDocs) throws FileNotFoundException, IOException{
		GZIPInputStream gzipFile = new GZIPInputStream(new FileInputStream(filePath));
		BufferedReader reader = new BufferedReader(new InputStreamReader(gzipFile));
		
		new File(pathToStoreDocs).mkdir(); //creates the directory where the files will be stored
		HashMap<Integer, metadata> IDtoMetadata = new HashMap<Integer, metadata>();//key is internal doc ID and value is metadata
		HashMap<String, Integer> docnoToInternalID = new HashMap<String, Integer>(); //key is docno and value is internal doc ID
		HashMap<String, Integer> lexicon1 = new HashMap<String, Integer>();//lexicon that maps from terms to term IDs
		HashMap<Integer, String> lexicon2 = new HashMap<Integer, String>();//lexicon that maps from term IDs to terms
		HashMap<Integer, ArrayList<Integer>> invIndex= new HashMap<Integer, ArrayList<Integer>>(); //creates the index
		
		String currentLine=null;
		int internalID=-1;
		String currentDocno=null;
		String currentDate=null;
		String currentHeadline="\n";
		int currentDocLength=-1;
		ArrayList<String> docLines = new ArrayList<String>();
		
		
		
		while((currentLine=reader.readLine())!=null){
			
			if(currentLine.contains("<DOC>")){
				docLines.clear(); //clear the array to start the new document
				internalID+=1;
				currentHeadline="\n";
			}
			docLines.add(currentLine); //adds the raw document line to the arrayList
			
			if(currentLine.contains("<DOCNO>")){
				currentLine=currentLine.replaceAll("<DOCNO> ", "");
				currentLine=currentLine.replaceAll(" </DOCNO>", "");		
				currentDocno=currentLine;
				currentDate=currentDocno.substring(6,8) + "-" + currentDocno.substring(2,4) + "-" + currentDocno.substring(4,6);
				
				String newDateDirectory=pathToStoreDocs +"/" + currentDate;
		
				File newDateDir = new File(newDateDirectory);
				if (!newDateDir.exists()){ //tests if the directory exists
					new File(newDateDirectory).mkdir(); //creates the directory if it doesn't already exist 
				}
				
				docnoToInternalID.put(currentDocno, internalID); //add docno and internal ID to hashmap
			}
			if(currentLine.contains("<HEADLINE>")){
				while(!(currentLine=reader.readLine()).contains("</HEADLINE>")){ //finds end of headline
					docLines.add(currentLine);
					if(currentLine.contains("<P>")){ //finds the <P> tag
						while(!(currentLine=reader.readLine()).contains("</P>")){ //finds end of paragraph
							docLines.add(currentLine);
							if(!currentHeadline.equals("\n")){ //if headline isn't empty
								currentHeadline+=currentLine + " \n";
							}
							else{ //if headline is null
								currentHeadline=currentLine + " \n";
							}
						}
						docLines.add(currentLine); //adds the </P> tag
					}
				}
				docLines.add(currentLine); //adds the </HEADLINE> tag
			}
			if(currentLine.contains("</DOC>")){ //identifies the end of the current document
				
				ArrayList<String> docTokens = tokenizeDoc(docLines); //passes the document to tokenizing function to get token list
				currentDocLength=docTokens.size();
				ArrayList<Integer> docTokenIDs= convertTokensToIDs(docTokens, lexicon1, lexicon2); //convert token terms to token IDs
				HashMap <Integer, Integer> termIdCounts=countDocWords(docTokenIDs); //count doc tokens
				addToPostings(termIdCounts, invIndex, internalID); //adds that document's terms and counts to the index's postings
				
				//System.out.print(internalID + " " + currentDocno);
				//System.out.println(docTokens);
				//System.out.println(docTokenIDs);
				//System.out.println(termIdCounts);
				
				IDtoMetadata.put(internalID, new metadata(internalID, currentDocno, currentDate, currentHeadline, currentDocLength)); //add internal ID and metadata object to hashmap
				
				PrintWriter docWriter = new PrintWriter(pathToStoreDocs +"/" + currentDate + "/" + internalID + ".txt", "UTF-8"); //creates new text file
				for(int i=0; i<docLines.size(); i++){
					docWriter.println(docLines.get(i)); //write raw line i of the document to the text file
				}
				docWriter.close();
			}
		}
		
		PrintWriter metadataWriter = new PrintWriter(pathToStoreDocs + "/internalIDtoMetadata.txt", "UTF-8"); //creates new text file for metadata
		for (Integer internalIdKey : IDtoMetadata.keySet()) {
			   metadataWriter.print(internalIdKey + " {");
			   metadataWriter.print(IDtoMetadata.get(internalIdKey).stringifyMetadata());
			   metadataWriter.println("}");  
		}	
		metadataWriter.close();
		
		PrintWriter docnoWriter = new PrintWriter(pathToStoreDocs + "/docnoToInternalID.txt", "UTF-8"); //creates new text file for docnos and internal ids
		for (String docnoKey : docnoToInternalID.keySet()) {
			   docnoWriter.print(docnoKey + ": ");
			   docnoWriter.println(docnoToInternalID.get(docnoKey));
		}	
		docnoWriter.close();	
		
		PrintWriter lexicon1Writer = new PrintWriter(pathToStoreDocs + "/lexicon1.txt", "UTF-8"); //creates new text file for lexicon 1
		for (String term : lexicon1.keySet()) {
			   lexicon1Writer.print(term + ": ");
			   lexicon1Writer.println(lexicon1.get(term));
		}	
		lexicon1Writer.close();	
		
		PrintWriter lexicon2Writer = new PrintWriter(pathToStoreDocs + "/lexicon2.txt", "UTF-8"); //creates new text file for lexicon 2
		for (int id : lexicon2.keySet()) {
			   lexicon2Writer.print(id + ": ");
			   lexicon2Writer.println(lexicon2.get(id));
		}	
		lexicon2Writer.close();

		PrintWriter indexWriter = new PrintWriter(pathToStoreDocs + "/invIndex.txt", "UTF-8"); //creates new text file for inv index
		for (int termId : invIndex.keySet()) {
			   indexWriter.println(termId);
			   indexWriter.println(invIndex.get(termId));
		}	
		indexWriter.close();
		reader.close();
	}
	
	public static ArrayList<String> tokenizeDoc(ArrayList<String> docLines){ //function to tokenize a document. returns doc's token list
		ArrayList<String> docTokens = new ArrayList<String>();
		int i=0;
		while(i<docLines.size()){ //to iterate through each document line
			String currentLine=docLines.get(i).toLowerCase(); //gets and lowercases current line
			
			if(currentLine.equals("<text>") || currentLine.equals("<headline>") || currentLine.equals("<graphic>")){ //identifies start tag of text we care about
				i=i+1; //increases doc line number to skip tag line
				currentLine=docLines.get(i).toLowerCase(); //gets new line
				while(!currentLine.equals("</text>") && !currentLine.equals("</headline>") && !currentLine.equals("</graphic>")){ //finds end tag
					if(!(currentLine.contains("<") && currentLine.contains(">"))){ //if line isnt a tag
						tokenizeString(currentLine, docTokens);//calls other function to tokenize current doc line
					}
					i=i+1;
					currentLine=docLines.get(i).toLowerCase();	//skips to next line
				}
			}
			else{ //if we dont find text, headline, or graphic start tags
				i=i+1;	//increases doc line number
			}
		}
		return docTokens;
	}
	
	public static void tokenizeString(String currentLine, ArrayList<String> docTokens){ // tokenizes a document line and adds to doc's token list
		int start=0;
		int j=0;
		for(j=0; j<currentLine.length(); j++){ //iterates through each character of string
			char currentChar=currentLine.charAt(j); //gets the character at spot j of string
			if(!(Character.isDigit(currentChar)) && !(Character.isLetter(currentChar))){ //if character is not a number or letter
				if(start!=j){
					String token=currentLine.substring(start, j);
					docTokens.add(PorterStemmer.stem(token));
				}
				start=j+1;
			}
		}
		if(start!=j){
			docTokens.add(PorterStemmer.stem(currentLine.substring(start, j))); //catches last token
		}
		
	}
	
	public static ArrayList<Integer> convertTokensToIDs(ArrayList<String> docTokens, HashMap <String, Integer> lexicon1, HashMap<Integer, String> lexicon2){
		//converts token term list into list of token IDs
		ArrayList<Integer> docTokenIDs = new ArrayList<Integer>();
		for(int i=0; i<docTokens.size(); i++){
			String token=docTokens.get(i); //current token in list
			if(lexicon1.containsKey(token)){ //if lexicon already contains that token as a key
				docTokenIDs.add(lexicon1.get(token)); //adds the associated integer ID for that token
			}
			else{ //if term is not in lexicon
				int newTermID=lexicon1.size();
				lexicon1.put(token, newTermID); //adds term to lexicon 1
				lexicon2.put(newTermID, token); //adds term to lexicon 2
				docTokenIDs.add(newTermID); //adds the term to docTokenIds
			}
		}
		return docTokenIDs;
	}
	
	public static HashMap<Integer, Integer> countDocWords(ArrayList<Integer> docTokenIDs){ //returns dictionary of a document's token IDs and their counts

		HashMap <Integer, Integer> termIdCounts= new HashMap<Integer, Integer>();
		for(int i=0; i<docTokenIDs.size(); i++){
			int currentDocID=docTokenIDs.get(i);
			if(termIdCounts.containsKey(currentDocID)){ //if term ID exists in termIdCounts
				termIdCounts.replace(currentDocID, termIdCounts.get(currentDocID) +1); //adds 1 to the count for that term
			}
			else{ //if term ID doesn't exist in termIdCounts
				termIdCounts.put(currentDocID, 1);
			}
		}
		return termIdCounts;
	}
	
	public static void addToPostings(HashMap <Integer, Integer> termIdCounts, HashMap<Integer, ArrayList<Integer>> invIndex, int internalId ){
		
		for (int termId : termIdCounts.keySet()) {
			int termCount=termIdCounts.get(termId);
			if (!invIndex.containsKey(termId)){ //if the index doesnt contain that term
				ArrayList<Integer> postings= new ArrayList<Integer>(); //create a postings list
				invIndex.put(termId, postings); //add postings to index
			}
			(invIndex.get(termId)).add(internalId); //appends the internal doc id to the posting
			(invIndex.get(termId)).add(termCount); //appends the term count to the posting
		}
	}
}


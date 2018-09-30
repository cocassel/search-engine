import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;
import java.util.HashMap;

public class readDoc {

	public static void main(String[] args) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
		
		System.out.println("Please enter a path to the location of the documents and metadata store for the LA Times");
		String filePath=scanner.next();
		filePath="/Users/Celeste/Documents/2018/3A/MSCI541/MSCI541-Homework/LAtimesDocs/";
		if(!(filePath.substring(filePath.length() - 1)).equals("/")){ //checks if the directory entered ends in a slash
			filePath=filePath + "/";
		}
		
		File filePathOfMetadataStore=new File(filePath + "internalIDtoMetadata.txt");
		while(!filePathOfMetadataStore.exists()){ //tries to find metadata store
			System.out.println("The documents and metadata cannot be found at this file path. Please enter a valid file path.");
			filePath=scanner.nextLine();
			if(!(filePath.substring(filePath.length() - 1)).equals("/")){ //checks if the directory entered ends in a slash
				filePath=filePath + "/";
			}
			filePathOfMetadataStore=new File(filePath + "internalIDtoMetadata.txt");
		}
		
		HashMap<Integer, metadata> IDtoMetadata=readInMetadataDoc(filePath);//read in saved hashmap
		HashMap<String, Integer> docnoToInternalID=readInDocnoDoc(filePath);//read in saved hashmap
		
		System.out.println("Do you want to retrieve a document and its meta data via docno or id?\n"
				+ "If you would like to use docno, enter \"docno\". If you would like to use id, enter \"id\"");
		String idOrDocno=scanner.next();
		
		if(idOrDocno.toLowerCase().equals("docno")){
			getFromDocno(filePath, IDtoMetadata, docnoToInternalID);
		}
		else if(idOrDocno.toLowerCase().equals("id")){
			getFromId(filePath, IDtoMetadata);
		}	
		else{
			System.out.println("Please enter either \"docno\" or \"id\" to specify which would you would like use to retrieve a document");
			while(!(idOrDocno=scanner.next()).equals("docno") && !idOrDocno.equals("id")){
				System.out.println("Please enter either \"docno\" or \"id\" to specify which would you would like use to retrieve a document.");
			}
		}	
		scanner.close();
	}
	
	public static void getFromDocno(String filePath, HashMap<Integer, metadata> IDtoMetadata, HashMap<String, Integer> docnoToInternalID) throws IOException{
		System.out.println("Please enter the docno of the document you would like to retrieve");
		Scanner scanner = new Scanner(System.in);
		String docno=scanner.next();
		
		int internalID=docnoToInternalID.get(docno);
		String dateOfDoc=IDtoMetadata.get(internalID).getDate();
		
		IDtoMetadata.get(internalID).printMetadata();
		
		System.out.println("Raw Document:");
		FileReader docReader = new FileReader(filePath + "/" +dateOfDoc + "/" +Integer.toString(internalID)+ ".txt");
		BufferedReader lineReader = new BufferedReader(docReader);
		String currentLine = null;
		while((currentLine = lineReader.readLine()) != null) {
			System.out.println(currentLine);
        }  
		lineReader.close();
		scanner.close();
	}
	
	public static void getFromId(String filePath, HashMap<Integer, metadata> IDtoMetadata) throws IOException{
		System.out.println("Please enter the internal id of the document you would like to retrieve");
		Scanner scanner = new Scanner(System.in);
			
		int internalID=Integer.parseInt(scanner.next());
		String dateOfDoc=IDtoMetadata.get(internalID).getDate(); //retrieves date of document
		
		IDtoMetadata.get(internalID).printMetadata();
		
		System.out.println("Raw Document:");
		FileReader docReader = new FileReader(filePath + "/" +dateOfDoc + "/" +Integer.toString(internalID)+ ".txt");
		BufferedReader lineReader = new BufferedReader(docReader);
		String currentLine = null;
		while((currentLine = lineReader.readLine()) != null) {
			System.out.println(currentLine);
        }  
		lineReader.close(); 
		scanner.close();
	}
	
	public static HashMap<Integer, metadata> readInMetadataDoc(String pathToDoc) throws IOException{ //recreates hashmap of internal IDs to metadata
		
		HashMap<Integer, metadata> IDtoMetadata = new HashMap<Integer, metadata>(); //key is internal doc ID and value is metadata
        
		FileReader docReader = new FileReader(pathToDoc + "internalIDtoMetadata.txt");
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

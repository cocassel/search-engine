public class metadata {
	private int internalID;
	private String docno;
	private String date;
	private String headline;
	private int documentLength;
	
	public metadata(int internalID, String docno, String date, String headline, int documentLength){
		this.internalID=internalID;
		this.docno=docno;
		this.date=date;
		this.headline=headline;
		this.documentLength=documentLength;
	}
	
	public String stringifyMetadata(){
		String metadataString= "\nInternal_ID: " + internalID + "\nDOCNO: " + docno + "\nDocument_Length: " + documentLength + "\nDate: " + date + "\nHeadline: " + headline;
		return metadataString;
	}
	
	public String getHeadline(){
		return this.headline;
	}
	
	public String getDate(){
		return this.date;
	}
	
	public String getDocno(){
		return this.docno;
	}
	
	public int getDocLength(){
		
		return this.documentLength;
	}
	
	public void printMetadata(){
		String[] dateSplit =this.date.split("-");
		
		String month=dateSplit[1];
		if(month.equals("01")){ month="January"; }
		else if(month.equals("02")){month="February";}
		else if(month.equals("03")){month="March";}
		else if(month.equals("04")){month="April";}
		else if(month.equals("05")){month="May";}
		else if(month.equals("06")){month="June";}
		else if(month.equals("07")){month="July";}
		else if(month.equals("08")){month="August";}
		else if(month.equals("09")){month="September";}
		else if(month.equals("10")){month="October";}
		else if(month.equals("11")){month="November";}
		else if(month.equals("12")){month="December";}
		
		String day=dateSplit[2];
		if(day.substring(0, 1).equals("0")){ //tests if the first digit is a 0
			day=day.substring(1);//gets rid of 0
		}
		date=month + " " + day + ", " + "19" + dateSplit[0]; 
		
		System.out.println("Docno: " + this.docno);
		System.out.println("Internal ID: " + this.internalID);
		System.out.println("Document Length: " + this.documentLength);
		System.out.println("Date: " + this.date);
		System.out.println("Headline: " + this.headline);
	
	}
}


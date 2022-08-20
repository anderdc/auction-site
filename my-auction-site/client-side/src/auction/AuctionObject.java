//******This is the CLIENT AuctionObject file******
package auction;

import com.google.gson.GsonBuilder;

import auction.AuctionObject.SaleStatus;



//******This is the CLIENT AuctionObject file******
public class AuctionObject {
	//class is to hold information such as price, last person bid, and such
	private final int BIDLOGSIZE = 50;
	private String name;
	private int id;
	private Double price;
	private Double buyNowPrice;
	private String lastBidder;
	SaleStatus status;
	private String[] bidLog = new String[BIDLOGSIZE]; //bid log can only be 100 entries large 

    //used for testing
	public static void main(String args[]) {
		AuctionObject test = new AuctionObject("nft", 37.39, 7, 500);
		System.out.println("******returning arbitrary AuctionObject as JSON******");
		System.out.println(test);
	}
	
	public AuctionObject(String name, double startPrice, int id, double buyNow) {
		this.name = name;
		this.buyNowPrice = buyNow;
		lastBidder = "";
		price = startPrice;
		this.id = id;
		status = SaleStatus.ONSALE;
	}
	
	
	
	
	public String getLastBidder() {
		return lastBidder;
	}
	public Double getPrice() {
		return price;
	}
	public String getName() {
		return name;
	}
	public String getStatus() {
		return status.toString();
	}
	public int getId() {
		return id;
	}
	public String[] getbidLog() {
		return bidLog;
	}
	
	//only sets to sold 
	public void setSold() {
		status = SaleStatus.SOLD;
	}
	
	public enum SaleStatus{
		ONSALE{
			@Override
			public String toString() {
				return "On Sale";
			}	
		},
		SOLD{
			@Override
			public String toString() {
				return "Sold";
			}
		};
	}
	
	/**
	 * @param newPrice, in dollars
	 * @return true if bid went through, false otherwise
	 */
	public boolean newBid(Double newPrice, String newBidder) {
		//if item still on sale
		if(this.status == SaleStatus.ONSALE){
			if(newPrice > price) {
				price = newPrice;
				lastBidder = newBidder;
				String s = null;
				for(int i = 0; i < BIDLOGSIZE; i++) { //to find the first location there is no bid
					s = bidLog[i];
					if(s == null) {
						bidLog[i] = newBidder + ": $" + String.format("%.2f", newPrice); //add bid to record
						break;
					}
				}
				if(newPrice > buyNowPrice) setSold();  //bid was greater than buy now so its sold
				return true;
			}
			else return false;
		}
		else return false;
	}

	@Override
	public String toString() {
		return new GsonBuilder().setPrettyPrinting().create().toJson(this);
	}
	
	public boolean isEqual(AuctionObject other) {
		if(this.id == other.id) return true;
		else return false;
	}
}

package server;
import java.io.IOException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
/**
 * used to scrape and clean price data
 * @author ander
 *
 */
public class PriceScraper {
	
	//used to debug
	public static void main(String[] args) {
		String url = "https://coinmarketcap.com/";		
		Document doc = request(url);
		if(doc != null) {
			//<div class="sc-131di3y-0 cLgOOr"><a href="/currencies/solana/markets/" class="cmc-link"><span>$96.92</span></a></div>
			Elements sol = doc.getElementsByClass("sc-131di3y-0 cLgOOr");  //find the class of div with solana price
			Elements price = sol.select("[Href*=/currencies/solana/markets/]"); //find the one which specifically visits solana
			
			String solanaPrice = price.html();
			solanaPrice = solanaPrice.replaceAll("<", "");
			solanaPrice = solanaPrice.replaceAll("span", "");
			solanaPrice = solanaPrice.replaceAll("/", "");
			solanaPrice = solanaPrice.replaceAll(">", "");
			solanaPrice = solanaPrice.substring(1);
			double p = Double.parseDouble(solanaPrice);
			System.out.println("Solana's current price in dollars: "+p);
			
			System.out.println(getSolanaPrice());
		}
	}
	
	
	/**
	 * @return solana's price in dollars
	 * returns 0 if the call to the website did not go through
	 */
	public static double getSolanaPrice() {
		String url = "https://coinmarketcap.com/";		
		Document doc = request(url);
		if(doc != null) {
			//<div class="sc-131di3y-0 cLgOOr"><a href="/currencies/solana/markets/" class="cmc-link"><span>$96.92</span></a></div>
			Elements sol = doc.getElementsByClass("sc-131di3y-0 cLgOOr");  //find the class of div with solana price
			Elements price = sol.select("[Href*=/currencies/solana/markets/]"); //find the one which specifically visits solana
			
			String solanaPrice = price.html();
			solanaPrice = solanaPrice.substring(7, 12);
			double p = Double.parseDouble(solanaPrice);
			return p;
			
		}
		else return 1;
	}
	
	
	/**
	 *  visits a page and returns a Document if the page was visitable
	 * @param url to scrape
	 * @return
	 */
	private static Document request(String url) {
		try {
			Connection con = Jsoup.connect(url);
			Document doc = con.get();
			if(con.response().statusCode() == 200) { // means it is ok to visit this website
				return doc;
			}
			return null;
		} catch(IOException e) {
			System.out.println("could not connect to website coinmarketcap.com");
			return null;
		}
	}
}

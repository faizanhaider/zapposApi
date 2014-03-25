import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 */

/**
 * @author Faizan
 *
 */
public class Products {

	static final String API_KEY = "67d92579a32ecef2694b74abfc00e0f26b10d623";
	static final String ZAPPOS_API = "http://api.zappos.com/Product/";
	static final String DOWNLOADING_IMAGE_FOLDER = "images";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		// file name should always be present in the argument
		if (args.length == 0) {
			displayProgramUsage();
			System.exit(0);
		}

		File downloadFolder = new File(DOWNLOADING_IMAGE_FOLDER);
		if (!downloadFolder.exists()) {
			downloadFolder.mkdir();
		}

		File fileContainingSkus = new File(args[0]);
		ArrayList<String> skusList = new ArrayList<String>();

		if (fileContainingSkus.exists()) {
			try {
				String line;
				BufferedReader br = new BufferedReader(new FileReader(fileContainingSkus));

				// Getting rid of spaces
				while ((line = br.readLine()) != null) {
					skusList.add(line.trim());
				}
				br.close();

			} catch (IOException e) {
				System.err.println(" ERROR: I/O exception");
			}
		} else {
			System.out.println(" ERROR: File " + args[0] + " does not exist.");
		}

		for (int i = 0; i < skusList.size(); i++) {

			String skuNumber = skusList.get(i);
			if(!skuNumber.isEmpty()){
				String URLlist[] = getImageURLs(skuNumber);
				if (URLlist == null) {
					System.err.println(" ERROR: URL for SKU number " + skuNumber + "  not be found.");
					continue;
				}

				for (String URLstr : URLlist) {
					if (URLstr != null) {
						File downloadFile = downloadImageFromURL(URLstr);
						if(downloadFile.exists()){
							System.out.println("Image for SKU number " + skuNumber + " Downloaded.");
						}
					}
				}
			}
		}

	}

	/**
	 * Printing to the console on how to use this program.
	 */
	public static void displayProgramUsage() {
		System.out.println("File name is missing. Pass File Name in the Argument.");
		System.out.println("Usage: Products [FILE]");
		System.out.println("Program would download images for products using SKU numbers contained in FILE.");
		System.out.println("File should only contains a list of SKUs, one per line. ");
		System.out.println();
	}

	public static String[] getImageURLs(String sku) {

		try {
			URL url = new URL(ZAPPOS_API + sku + "?key=" + API_KEY);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setAllowUserInteraction(false);

			if (conn.getResponseCode() != 200) {
				System.err.println("ERROR: API Call for SKU # " + sku + " returned Status Code: " + conn.getResponseCode());
				System.err.println("Message: " + conn.getResponseMessage());
				return null;
			}

			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = rd.readLine()) != null) {
				sb.append(line);
			}
			rd.close();
			conn.disconnect();

			String JSONstring = sb.toString();
			String[] imageURLs = getImageURLsFromJSON(JSONstring, sku);
			return imageURLs;

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	private static String[] getImageURLsFromJSON(String jSONstring, String skuNumber) {
		JSONObject response;
		try {
			response = new JSONObject(jSONstring);
			JSONArray products = response.getJSONArray("product");
			String[] imageURLs = new String[products.length()];
			for (int i = 0; i < products.length(); i++) {
				Object imageURL = ((JSONObject) products.get(i)).get("defaultImageUrl");
				if(imageURL != JSONObject.NULL)
				{
					imageURLs[i] = ((JSONObject) products.get(i)).getString("defaultImageUrl");
				}
				else {
					imageURLs[i] = null;
					System.err.println(" ERROR: URL for SKU number " + skuNumber + "  not be found.");
				}
			}
			return imageURLs;

		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static File downloadImageFromURL(String imageURL) {

		// Extract the file name returned by the API and Save image using that name.
		String[] urlTokens = imageURL.split("/");
		File outputFile = new File(DOWNLOADING_IMAGE_FOLDER + "/" + urlTokens[urlTokens.length - 1]);

		URL downloadURL;
		try {
			downloadURL = new URL(imageURL);

			// Read image into bytes from the image URL.
			ReadableByteChannel imageBytes = Channels.newChannel(downloadURL.openStream());
			FileOutputStream fs = new FileOutputStream(outputFile);
			fs.getChannel().transferFrom(imageBytes, 0, 1 << 24);

			// Closing the File output stream after image has been saved.
			fs.close();
			return outputFile;

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}

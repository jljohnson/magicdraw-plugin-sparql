package regex;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexTest {

	public static void main(String[] args) {
		String sparqlQuery = "SELECT ?sourcePort ( COUNT(?targetValue) AS ?sumTargetValue)"
				+ "WHERE";
		ArrayList<String> queryVariables = getQueryVariables(sparqlQuery);
		for (String string : queryVariables) {
			System.out.println(string);
		}

	}

	
	private static ArrayList<String> getQueryVariables(String sparqlQuery) {
		// get all query variables

		int selectIndex = sparqlQuery.indexOf("SELECT");
		int whereIndex = sparqlQuery.indexOf("WHERE");
		String selectLine = sparqlQuery.substring(selectIndex, whereIndex);
		Pattern pattern = Pattern.compile("(?=\\?).+?(?=\\s|$)");
		Matcher matcher = pattern.matcher(selectLine);
		ArrayList<String> queryVariables = new ArrayList<String>();
		while (matcher.find()) {
			// System.out.print("Start index: " + matcher.start());
			// System.out.print(" End index: " + matcher.end() + " ");
			// System.out.println(matcher.group());
			String match = matcher.group();
			match = match.replace("?", "");
			// quick fix - better aapproach is to write a better regex
			if(!(selectLine.contains(match + " AS") | selectLine.contains(match + "AS")) | selectLine.contains(match + " as") | selectLine.contains(match + "as")){
				match = match.replace(")", "");
				queryVariables.add(match);
			}
			
		}
		return queryVariables;
	}
}

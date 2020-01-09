package analyser;

import java.util.ArrayList;
import java.util.List;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeEntity;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.Node;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.matching.measure.NGramsCalculator;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.matching.measure.StringSimilarityCalculator;

public class StringAnalyser {
	
	private	StringSimilarityCalculator strSimCalc;
	
	public StringAnalyser() {
		this.strSimCalc = new NGramsCalculator(2);
	}
	
	public double similarity(String s1, String s2) {
		return this.strSimCalc.calculateSimilarity(s1, s2);	
	}
	
	public String removeSubString(String str, char begin, char end, boolean keepMarkers) {
		StringBuilder result = new StringBuilder();
		char[] sequence = str.toCharArray();
		
		int count = 0;
		int beginIndex = 0;
		int endIndex = 0;
		for(int i = 0; i<sequence.length; i++) {
			if(sequence[i]==begin) {
				if(count==0) {
					result.append(str.substring(beginIndex, i+(keepMarkers?1:0)));
				}
				count++;
				
			}else if(sequence[i]==end) {
				count--;
				if(count==0) {
					beginIndex=i+(keepMarkers?0:1);
				}
			}
		}
		result.append(str.substring(beginIndex, sequence.length));
		return result.toString();
	}
	
	public List<String> getSubStrings(String str, char begin, char end, boolean keepMarkers) {
		List<String> strings = new ArrayList<String>();
		char[] sequence = str.toCharArray();
		
		int count = 0;
		int beginIndex = 0;
		int endIndex = 0;
		for(int i = 0; i<sequence.length; i++) {
			if(sequence[i]==begin) {
				if(count==0) {
					beginIndex=i+(keepMarkers?0:1);
				}
				count++;
			}else if(sequence[i]==end) {
				count--;
				if(count==0) {
					strings.add(str.substring(beginIndex, i+(keepMarkers?1:0)));
				}
			}
		}

		return strings;
	}
	
}

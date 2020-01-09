package analyser.callerAnalyser;

import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;

public class CallerPattern {
	
	private String shortName;
	private int nParameters;
	private CallerType type;
	
	
	public enum CallerType{
		Field,
		Method,
		Class;
	}
	
	public CallerPattern(String shortName, int nParameters, CallerType type) {
		this.shortName = shortName;
		this.nParameters = nParameters;
		this.type = type;
	}
	
	public CallerPattern(String fullName) {
		setFromFullName(fullName);
	}
	
	public String getShortName() {
		return shortName;
	}
	
	public void setShortName(String shortName) {
		this.shortName = shortName;
	}
	
	public void setShortNameFromFullName(String fullName) {
		int lastIndex = fullName.indexOf("(");		
		if(lastIndex<0)
			this.shortName = fullName.substring(fullName.lastIndexOf('.')+1);
		else	
			fullName = fullName.substring(0, lastIndex);
			this.shortName = fullName.substring(fullName.lastIndexOf('.')+1);
	}
	
	public int getnParameters() {
		return nParameters;
	}
	
	public void setnParameters(int nParameters) {
		this.nParameters = nParameters;
	}
	
	public void setnParametersFromFullName(String fullName) {
		fullName = fullName.replaceAll("\\s", "");
		int beginIndex = fullName.lastIndexOf("(");
		if(beginIndex<0) {
			this.nParameters = -1;
			return;
		}
		int endIndex = fullName.indexOf(")");
		if(endIndex-beginIndex!=1) {
			String str = fullName.substring(beginIndex +1, endIndex);
			int count = str.split(",").length;
			this.nParameters = count;
		}else
			this.nParameters = 0;
	}
	
	public CallerType getType() {
		return type;
	}

	public void setType(CallerType type) {
		this.type = type;
	}

	public void setFromFullName(String fullName) {
		this.setShortNameFromFullName(fullName);
		int lastIndex = fullName.indexOf("(");		
		if(lastIndex<0) {
			this.type = CallerType.Field;
			this.nParameters = -1;
		}
		else {
			this.type = CallerType.Method;
			setnParametersFromFullName(fullName);
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CallerPattern other = (CallerPattern) obj;
        return new EqualsBuilder().append(getType(), other.getType())
                .append(getnParameters(), other.getnParameters())
                .append(getShortName(), other.getShortName())
                .isEquals();
	}
}

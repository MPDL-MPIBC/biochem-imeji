package de.mpg.imeji.rest.to.predefinedMetadataTO;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import de.mpg.imeji.rest.to.MetadataTO;

@XmlRootElement
@XmlType (propOrder = {	 
		"text"
		})
public class TextTO extends MetadataTO{
	private String text;

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}


	
	

}

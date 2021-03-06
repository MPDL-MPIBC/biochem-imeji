package de.mpg.imeji.rest.to;

import java.io.File;

import de.mpg.imeji.logic.vo.Item;

/**
 * A convenience TO to fit the specification for create {@link Item}
 * 
 * @author saquet
 *
 */
public class ItemWithFileTO extends ItemTO {
	private static final long serialVersionUID = 3788266886306040199L;
	private File file;
	private String referenceUrl;
	private String fetchUrl;

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public String getReferenceUrl() {
		return referenceUrl;
	}

	public void setReferenceUrl(String referenceUrl) {
		this.referenceUrl = referenceUrl;
	}

	public String getFetchUrl() {
		return fetchUrl;
	}

	public void setFetchUrl(String fetchUrl) {
		this.fetchUrl = fetchUrl;
	}
}

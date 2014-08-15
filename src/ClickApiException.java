
public class ClickApiException extends Exception {

	private String code;

	public ClickApiException(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

}

import java.text.DecimalFormat;
import java.util.Properties;


public class ClickMessage {
	private String to, from, text, apiId, user, password, timestamp, udh; 
	private MsgType type;
	private EventType eventStatus;
			
	//MT specific
	private String delivAck, delivTime, reqFeat, concat, mo, unicode, msgType, encryption; 

	//MT callback specific
	private String apiMsgId, cliMsgId, status, charge;
	
	//MO callback specific
	private String moMsgid; 

	public String getTo() {
		return to;
	}
	public void setTo(String to) {
		this.to = to;
	}
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getApiId() {
		return apiId;
	}
	public void setApiId(String apiId) {
		this.apiId = apiId;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public MsgType getType() {
		return type;
	}
	public void setType(MsgType type) {
		this.type = type;
	}
	public EventType getEventStatus() {
		return eventStatus;
	}
	public void setEventStatus(EventType status) {
		this.eventStatus = status;
	}
	
	
	/**
	 * mt constructor
	 */
	public ClickMessage(String to, String from, String apiId, String user, String password, String text) {
		super();
		this.to = to;
		this.from = from;
		this.apiId = apiId;
		this.user = user;
		this.password = password;
		this.text = text;

		this.type = MsgType.MT;
		this.eventStatus = null;
		this.timestamp = Utils.getDateTime();
		this.apiMsgId = Utils.genMsgId();
	}
	
	/**
	 * mt callback constructor
	 */
	public ClickMessage(String to, String from, String apiMsgId, String cliMsgId, String status, double charge) {
		super();
		DecimalFormat df = new DecimalFormat("#.##");
		
		this.to = to;
		this.from = from;
		this.apiMsgId = apiMsgId;
		this.cliMsgId = cliMsgId;
		this.status = status;
		this.charge = df.format(charge);
		
		this.type = MsgType.MTCB;
		this.eventStatus = null;
		this.timestamp = Utils.getDateTime();
	}
	
	/**
	 * mo constructor
	 */
	public ClickMessage(String to, String from, String apiId, String moMsgid, String text) {
		super();
		this.to = to;
		this.from = from;
		this.apiId = apiId;
		this.moMsgid = moMsgid;
		this.text = text;
		
		this.type = MsgType.MO;
		this.eventStatus = null;
		this.timestamp = Utils.getDateTime();
	}

	
	public ClickMessage(String url) {
		Properties props = Utils.getPropertiesFromUrl(url, null);
		
		// general
		this.from = props.getProperty("from",null);
		this.to = props.getProperty("to");
		this.cliMsgId = props.getProperty("cliMsgId");
		
		// only for MT
		this.user = props.getProperty("user", null);
		this.password = props.getProperty("password", null);
		this.delivAck = props.getProperty("deliv_ack", null);
		if (this.user != null && this.password != null) {
			this.type = MsgType.MT;
		}
		
		// only for MOs
		this.moMsgid = props.getProperty("moMsgId");
		if (this.moMsgid != null) {
			this.type = MsgType.MO;
		}
		
		// only for MT callbacks
		this.apiMsgId = props.getProperty("apiMsgId");
		this.status = props.getProperty("status");
		this.charge = props.getProperty("charge");
		if (this.status != null) {
			this.type = MsgType.MTCB;
		}
		
		// only if not an MT callback
		this.apiId = props.getProperty("api_id");
		this.text = props.getProperty("text");
		
		if (this.type == MsgType.MT) {
			this.apiMsgId = Utils.genMsgId();
		}

	}
	
//	/** {
	
//	 */
//	public ClickMessage(String to, String from, String text, String apiId,
//			String user, String password, MsgType type, EventType status) {
//		super();
//		this.to = to;
//		this.from = from;
//		this.text = text;
//		this.apiId = apiId;
//		this.user = user;
//		this.password = password;
//		this.type = type;
//		this.eventStatus = status;
//	}
//	
//	/**
//	 */
//	public ClickMessage(String url, MsgType type, EventType event_status) {
//		super();
//		
//		this.type = type;
//		this.eventStatus = event_status;
//	}
	
	public String toUrl(StringBuilder buf, String url) {

		buf.setLength(0);
		buf.append(url).append('?');
		
		// general
		Utils.queryAdd(buf, "from",		this.from);
		Utils.queryAdd(buf, "to", 		this.to);
		Utils.queryAdd(buf, "timestamp",this.timestamp);
		
		// only for MT
		if (this.type == MsgType.MT) {
			Utils.queryAdd(buf, "user",		this.user);
			Utils.queryAdd(buf, "password",	this.password);
			Utils.queryAdd(buf, "deliv_ack",this.delivAck);
		} 
		// only for MOs
		if (this.type == MsgType.MO) {
			Utils.queryAdd(buf, "moMsgId",	this.moMsgid);
			Utils.queryAdd(buf, "udh",		"1");
			Utils.queryAdd(buf, "charset",	"latin1");
		} else {
			Utils.queryAdd(buf, "cliMsgId",	this.cliMsgId);
		}
		
		// only for MT callbacks
		if (this.type == MsgType.MTCB) {
			Utils.queryAdd(buf, "apiMsgId",	this.apiMsgId);
			Utils.queryAdd(buf, "status",	this.status);
			Utils.queryAdd(buf, "charge",	this.charge);
		} 
		// only if not an MT callback
		else {
			Utils.queryAdd(buf, "api_id",	this.apiId);
			Utils.queryAdd(buf, "text", 	this.text);
		}
		
		String url_str = buf.toString();
		buf.setLength(0);

		return url_str;
	}
	public String getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	public String getUdh() {
		return udh;
	}
	public void setUdh(String udh) {
		this.udh = udh;
	}
	public String getDelivAck() {
		return delivAck;
	}
	public void setDelivAck(String delivAck) {
		this.delivAck = delivAck;
	}
	public String getDelivTime() {
		return delivTime;
	}
	public void setDelivTime(String delivTime) {
		this.delivTime = delivTime;
	}
	public String getReqFeat() {
		return reqFeat;
	}
	public void setReqFeat(String reqFeat) {
		this.reqFeat = reqFeat;
	}
	public String getConcat() {
		return concat;
	}
	public void setConcat(String concat) {
		this.concat = concat;
	}
	public String getMo() {
		return mo;
	}
	public void setMo(String mo) {
		this.mo = mo;
	}
	public String getUnicode() {
		return unicode;
	}
	public void setUnicode(String unicode) {
		this.unicode = unicode;
	}
	public String getMsgType() {
		return msgType;
	}
	public void setMsgType(String msgType) {
		this.msgType = msgType;
	}
	public String getEncryption() {
		return encryption;
	}
	public void setEncryption(String encryption) {
		this.encryption = encryption;
	}
	public String getApiMsgId() {
		return apiMsgId;
	}
	public void setApiMsgId(String apiMsgId) {
		this.apiMsgId = apiMsgId;
	}
	public String getCliMsgId() {
		return cliMsgId;
	}
	public void setCliMsgId(String cliMsgId) {
		this.cliMsgId = cliMsgId;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getCharge() {
		return charge;
	}
	public void setCharge(String charge) {
		this.charge = charge;
	}
	public String getMoMsgid() {
		return moMsgid;
	}
	public void setMoMsgid(String moMsgid) {
		this.moMsgid = moMsgid;
	}
	
	
}

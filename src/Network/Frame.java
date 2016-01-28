package Network;
import java.io.Serializable;

@SuppressWarnings("serial")
public class Frame implements Serializable {
	
	public Node server_src;
	public Node server_dst;
	public String frame_type;
	public String frame_message;
	public int frame_timestamp;	
	
	public Frame(Node srcNode, Node destNode, String frameType,int frameTS, String frameMessage) {
		this.server_src 	= srcNode;
		this.server_dst 	= destNode;
		this.frame_type 	= frameType;
		this.frame_message 	= frameMessage;
		this.frame_timestamp= frameTS;
	}

}

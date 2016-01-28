package Network;

import java.io.Serializable;

public class Node implements Serializable{
	
	private String host;
	private int port;
	private int mutexport;
	private String name;
	private String role;
	private String lowlayer;
	
	public Node(String server_name, String server_host,int server_port,String lowlayer, String role) {
		this.host = server_host;
		this.port = server_port;
		this.name = server_name;
		this.lowlayer = lowlayer;
		this.role = role;
	}
	
	public Node(String server_name, String server_host,int server_port,int mutexPort,String lowlayer, String role) {
		this.host = server_host;
		this.port = server_port;
		this.name = server_name;
		this.mutexport = mutexPort;
		this.lowlayer = lowlayer;
		this.role = role;
	}
	
	public String[] getLowlayerNodes() {
		return this.lowlayer.split(",");
	}
	
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getMutexPort() {
		return this.mutexport;
	}

	public void setMutexPort(int port) {
		this.mutexport = port;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	@Override
	public String toString() {
		return "Name:"+this.name+" IP:"+this.host+" Port:"+this.port+" Role:"+this.role+" DistPort:"+this.mutexport;
	}
	
	
}

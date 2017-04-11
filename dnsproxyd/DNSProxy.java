/*
 * dnsproxyd
 * Version 1.0
 * Copyright � 2008 Michael Landi
 *
 * This file is part of dnsproxyd.
 *
 * Dnsproxyd is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Dnsproxyd is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Dnsproxyd.  If not, see <http://www.gnu.org/licenses/>
 */

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class DNSProxy extends Thread {
	/*
	 * Constants.
	 */
	private static final double		VERSION	=	0.94;
	private static String			CONFDIR	=	"/etc/dnsproxyd";
	private static boolean			DISABLED= 	false;
	private static boolean			DEBUG	= 	true;
	
	private enum					FILTERMODE {None, Blacklist, Whitelist};

	/*
	 * Settings either default or loaded from configuration.
	 */
	private int					_intPort;
	private int					_intCacheTime;
	private byte[]				_bUnknownIP;
	private byte[]				_bBlockedIP;
	private String				_strLogPath;
	private String				_strWhitePath;
	private String				_strBlackPath;
	private boolean				_varCache;
	private FILTERMODE			_fmFilter;
	private String       		nameserver;

	/*
	 * Instance variables.
	 */
	private boolean 			_varListen;
	private Set<String>			_strWhitelist;
	private Set<String>			_strBlacklist;
	private EventListener		_elSystem;
	private DNSCache			_dnsCache;
	private static DNSLog		_dnsLog;

	static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss");

	/*
	 * Constructor.
	 */
	public DNSProxy(String config) {
		printBanner();
		loadSettings(config);
		if(! DISABLED) {
			_strBlacklist = loadBlackOrWhitelist(_strBlackPath, "blacklist");
			_strWhitelist = loadBlackOrWhitelist(_strWhitePath, "whitelist");
		}

		if (_varCache) {
			_dnsCache = new DNSCache();
		}

		//_dnsLog = new DNSLog(_strLogPath);
	}

//------------------------------------------------------------------------------

	public void close() {

		_varListen = false;
		_elSystem.close();
		
		try {
			Thread.sleep(500);
		}
		catch (Exception e) {
			printDebug(e);
		}

		//_dnsLog.closeHandle();
		System.exit(0);
	}

//UTILITIES---------------------------------------------------------------------

	public static byte[] IPv4toByteArray(String address) throws Exception {
		byte[] bAddress = new byte[4];
		String[] strSplit = address.split("\\.");

		if (strSplit.length != 4) {
			Exception e = new Exception("Invalid IPv4 address: " + address);
			throw(e);
		}
	
		for (int i = 0; i < 4; i++) {
			int temp = Integer.parseInt(strSplit[i]);
			bAddress[i] = (byte)temp;
		}
	
		return bAddress;
	}

//SERVICE-CALLS-----------------------------------------------------------------

	public void run() {
		_elSystem = new EventListener(this, 4445);
		_elSystem.start();

		try {
			_varListen = true;
			listen();
			_varListen = false;
		}
		catch (Exception e) {
			_varListen = false;
			printDebug(e);
		}
	}

	public void listen() throws Exception {
		byte[] bRequest;
		DatagramPacket dPacket;
		printDebug("Listening for requests on port " + _intPort + ":");
		DatagramSocket dSocket = new DatagramSocket(_intPort);
		

		while (_varListen) {
			bRequest = new byte[96];
			dPacket = new DatagramPacket(bRequest, bRequest.length);
			dSocket.receive(dPacket);

			new ClientThread(dSocket, dPacket.getAddress(), 
					dPacket.getPort(), bRequest).start();
		}

		dSocket.close();
	}

	public class ClientThread extends Thread {
		private int				_intPort;
		private byte[]			_bRequest;
		private InetAddress 	_inaSender;
		private DatagramSocket	_dsPointer;

		public ClientThread(DatagramSocket socket, InetAddress address, 
				int port, byte[] request) {
			_dsPointer = socket;
			_inaSender = address;
			_intPort = port;
			_bRequest = request;
		}

		public void run() {

			DNSResponse dnsResponse = new DNSResponse(_bRequest);
			String hostname = dnsResponse.getDomain();

			byte[] responseIPAdress = getAddress(hostname,  _inaSender);
			
			if (responseIPAdress == null)
				return;
				
			byte[] bResponse = dnsResponse.getResponse(responseIPAdress);
			DatagramPacket dPacket = new DatagramPacket(bResponse, 
				bResponse.length, _inaSender, _intPort);

			try {
	          	_dsPointer.send(dPacket);
			}
			catch (Exception e) {
				printDebug(e);
				return;
			}

			if (_varCache ) {
				_dnsCache.cache(hostname, responseIPAdress);
			}
		}

		// experimental, not in use
		// forwards the original UDP datagramm to the real nameserver
		// and the response datagramm to the original requester
		private void run_passThrough() {
			DNSResponse dnsResponse = new DNSResponse(_bRequest);
			String strDomain = dnsResponse.getDomain();

			printDebug("Got request for: "+strDomain);

			try {
				byte[] bResponse = DNSQuery_passThrough.doLookup(_bRequest);
				DatagramPacket dPacket = new DatagramPacket(bResponse, 
						bResponse.length, _inaSender, _intPort);
				_dsPointer.send(dPacket);
			}
			catch (Exception e) {
				printDebug(e);
			}
		}


		byte[] toByteArray(String str) {
			byte[] ret = new byte[4];
			int nr = 0;
			for (String part: str.split("\\.")) {
				ret[nr] = (byte) Integer.parseInt(part);
				nr++;
			}
			return ret;
		}
		
		/*
		 */
		private byte[] getAddress(String hostname, InetAddress requester) {

			//String from = " from " + requester.getHostAddress(); // should not be logged
			String msgChunk = "Got request for: "+hostname;

			if (isBlocked(hostname)) {
				printDebug(msgChunk + "   BLOCKED");
				return _bBlockedIP;
			}

			if (_varCache) {
				DNSEntry dnsEntry = _dnsCache.isCached(hostname, _intCacheTime);
				if (dnsEntry != null) {
					printDebug(msgChunk + "   CACHED");
					return dnsEntry.getAddress();
				}
			}

			try {
				byte[] responseIPAdress = null;
				if (nameserver != null) {
					// contact the given nameserver with dnsjava library
					String ipString = DNSQuery_dnsjava.doLookup(nameserver, hostname);
					if (ipString == null) return null;
					printDebug(msgChunk + "   -> " + ipString);
					responseIPAdress = toByteArray(ipString);
				}
				else {
					// contact the platform DNS service
					responseIPAdress = DNSQuery.doLookup(hostname);
					printDebug(msgChunk + "   -> " + responseIPAdress);
				}
				
				return responseIPAdress;
			} catch (Exception e1) {
				//e1.printStackTrace();
				printDebug(msgChunk + "   ERROR");
				return null;
			}

		}

		private boolean isBlocked(String domain) {
			switch (_fmFilter) {
				case None:
					return false;
				case Whitelist:
					if (isPresent(domain, _strWhitelist))
						return false;
					else
						return true;
				case Blacklist:
					if (isPresent(domain, _strBlacklist))
						return true;
					else
						return false;
				default:
					return true;
			}
		}

	}

	private boolean isPresent(String domain, Set<String> array) {
		domain = domain.trim().toLowerCase();

		for (String strBuffer : array) {
			if (domain.endsWith(strBuffer.trim().toLowerCase()))
				return true;
		}

		return false;
	}

//INITIALIZATION----------------------------------------------------------------

	private void loadSettings(String config) {
		//Create default objects.
		_strWhitelist = new HashSet<String>();
		_strBlacklist = new HashSet<String>();

		//Set default settings.
		_fmFilter = FILTERMODE.Whitelist;
		_bBlockedIP = new byte[] {(byte)192, (byte)168, 0, 2};
		_bUnknownIP = new byte[] {(byte)192, (byte)168, 0, 2};
		_intPort = 53;
		_intCacheTime = 604800000;
		_strLogPath = "/var/log/dnsproxyd";
		_varCache = true;

		try {
			FileReader fReader = new FileReader(config);
			BufferedReader bReader = new BufferedReader(fReader);

			String strBuffer;

			while ((strBuffer = bReader.readLine()) != null) {
				try {
					parseSetting(strBuffer);
				}
				catch (Exception e) {
					printDebug("Parse: " + e);
				}
           	} 

			bReader.close();
			fReader.close();
		}
		catch (Exception e) {
			System.err.println("Could not read config file "+config);
			System.err.println("Using build-in config.");
		}

		printDebug("Configuration loaded: ");
		printDebug("FilterMode = " + _fmFilter);
		printDebug("BlockedIP = " + getPrintableAddress(_bBlockedIP));
		printDebug("UnknownIP = " + getPrintableAddress(_bUnknownIP));
		printDebug("Listen = " + _intPort);
		printDebug("nameserver = " + nameserver);
		printDebug("Whitelist = " + _strWhitePath);
		printDebug("Blacklist = " + _strBlackPath);
		printDebug("CacheTime = " + _intCacheTime + " Minutes");
		printDebug("Cache = " + _varCache + "\n");
		//printDebug("LogPath = " + _strLogPath);
		
		_intCacheTime = _intCacheTime * 60 * 1000; // millisec
	}
	
	private String getPrintableAddress(byte[] addr) {
		try {
			return InetAddress.getByAddress(addr).getHostAddress();
		} catch (UnknownHostException e) {
			return "Not a valid address";
		}
	}

	private void parseSetting(String setting) throws Exception {
		String strSetting;
		String[] strSplit = setting.split("=");
		
		if (strSplit.length != 2)
			return;
		else if (strSplit[0].trim().equals(""))
			return;
		else if (strSplit[0].trim().startsWith("#"))
			return;

		strSetting = strSplit[0].trim().toLowerCase();

		if (strSetting.equals("filtermode")) {
			int b = Integer.parseInt(strSplit[1]);
			
			switch (b) {
				case 0:
					_fmFilter = FILTERMODE.None;
					break;
				case 1:
					_fmFilter = FILTERMODE.Whitelist;
					break;
				case 2:
					_fmFilter = FILTERMODE.Blacklist;
					break;
			}
		}
		else if (strSetting.equals("blockedip"))
			_bBlockedIP = IPv4toByteArray(strSplit[1]);
		else if (strSetting.equals("unknownip"))
			_bUnknownIP = IPv4toByteArray(strSplit[1]);
		else if (strSetting.equals("listen"))
			_intPort = Integer.parseInt(strSplit[1]);
		else if (strSetting.equals("whitelist"))
			_strWhitePath = strSplit[1];
		else if (strSetting.equals("blacklist"))
			_strBlackPath = strSplit[1];
		else if (strSetting.equals("cachetime")) {
			if (strSplit[1].startsWith("m"))
			_intCacheTime = Integer.parseInt(strSplit[1].substring(1));  // in minutes
			else
			_intCacheTime = Integer.parseInt(strSplit[1]) * 24 * 60 ; // in days
		}
		else if (strSetting.equals("logpath"))
			_strLogPath = strSplit[1];
		else if (strSetting.equals("cache"))
			_varCache = new Boolean(strSplit[1]);
		else if (strSetting.equals("nameserver"))
			nameserver = strSplit[1];
		else if (strSetting.equals("debug"))
			DEBUG = "true".equals(strSplit[1]);
	}

	private Set<String> loadBlackOrWhitelist(String blacklist, String listname) {
		if (blacklist == null)
				return null;
		if (blacklist.trim().length() == 0)
			return null;
			
		printDebug("Loading '"+listname+"' database...");
		
		Set<String> result = new HashSet<String>();
		
		try {
			if (! blacklist.startsWith("/") &&! blacklist.startsWith("\\") 
					&&  ! blacklist.substring(1,2).equals(":")) {
				blacklist = CONFDIR + "/" + blacklist;
			};
			File fWhitelist = new File(blacklist);
			if (!fWhitelist.canRead()) {
				printDebug("The specified file does not exist, or you do " +
					"not have permission to access it.");
			}

			String strBuffer = "";
			Vector<String> vecBuffer = new Vector<String>();
			FileReader frStream = new FileReader(fWhitelist);
			BufferedReader brStream = new BufferedReader(frStream);

			while ((strBuffer = brStream.readLine()) != null) {
				vecBuffer.addElement(strBuffer);
           	} 

			brStream.close();
			frStream.close();

			for (int i = 0; i < vecBuffer.size(); i++) {
				String entry = vecBuffer.get(i).trim();
				if (entry.length() < 1) continue;
				String entryStripped = null;
				if (entry.startsWith("#")) {  // comment line
					continue;
				}
				if (entry.startsWith("=")) {  // use this entry without trimming of the beginning parts
					entryStripped = entry.substring(1).trim();
				}
				else { // trim of the beginning parts but two, so abc.def.xyz.com will become xyz.com
					int idx = entry.lastIndexOf(".");
					if (idx > -1)
						idx = entry.lastIndexOf(".", idx-1);
					if (idx > -1)
						entryStripped = entry.substring(idx +1);
					else
						entryStripped = entry;
				}
				printDebug(entry + "    ->    "+ entryStripped);
				if (! isPresent(entryStripped, result))
					result.add(entryStripped);
			}
		}
		catch (Exception e) {
			printDebug("Error: " + e.getMessage() + "\n");
		}
		
		printDebug(result.size() + " domains added to " + listname+".\n");
		return result;
	}

//LOGGING-----------------------------------------------------------------------

	private static void printBanner() {
		printDebug("==============================================");
		printDebug("DNS Proxy                         Version " + VERSION);
		printDebug("==============================================");
		printDebug("");
	}

	public static void printDebug(String s) {
		String out = df.format(new Date()) + " - " + s;
		if (DEBUG) {
			System.out.println(out);
		}
		//_dnsLog.println(out); checks for file rolling on each write, that is a bit much overhead
	}

	public static void printDebug(Exception e) {
			printDebug(e.getMessage());
	}

//ENTRY-------------------------------------------------------------------------
	
	public static void main(String[] args) {
		int eaten = 0;
		while (args.length > eaten) {
			if ((args.length >= 2) && "--configDir".equals(args[eaten])) {
				CONFDIR = args[eaten+1]; // will also be used for relative blacklist/whitelist files
				eaten += 2;
			}
			
			else if ((args.length >= 1) && "--disabled".equals(args[eaten])) {
				DISABLED = true;
				eaten += 1;
			}
			else {
				eaten += 1; // ignore this arg
			}
			
		}
		
		
		DNSProxy dnspThread;
		dnspThread = new DNSProxy(CONFDIR + "/dnsproxy.conf");
		dnspThread.start();

		try {
			dnspThread.join();
		}
		catch (Exception e) {
			printDebug(e);
			System.exit(-1);
		}

		System.exit(0);
	}
}

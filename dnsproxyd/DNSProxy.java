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
import java.util.*;

public class DNSProxy extends Thread {
	/*
	 * Constants.
	 */
	private static final double		VERSION	=	0.93;
	private static final String		CONFDIR	=	"/etc/dnsproxyd";
	private static final boolean	DEBUG	= 	false;
	
	private enum					FILTERMODE {None, Blacklist, Whitelist};

	/*
	 * Settings either default or loaded from configuration.
	 */
	private int					_intPort;
	private int					_intCacheTime;
	private byte[]				_bUnknownIP;
	private byte[]				_bBlockedIP;
	private String				_strLogPath;
	private String				_strCachePath;
	private String				_strWhitePath;
	private String				_strBlackPath;
	private boolean				_varCache;
	private FILTERMODE			_fmFilter;

	/*
	 * Instance variables.
	 */
	private boolean 			_varListen;
	private String[]			_strWhitelist;
	private String[]			_strBlacklist;
	private EventListener		_elSystem;
	private DNSCache			_dnsCache;
	private static DNSLog		_dnsLog;

	/*
	 * Constructor.
	 */
	public DNSProxy(String config) {
		printBanner();
		loadSettings(config);
		loadWhitelist(_strWhitePath);
		loadBlacklist(_strBlackPath);

		if (_varCache) {
			_dnsCache = new DNSCache(_strCachePath);
			printDebug(_dnsCache.prune(_intCacheTime) + " cached records pruned.\n");
		}

		_dnsLog = new DNSLog(_strLogPath);
	}

//------------------------------------------------------------------------------

	public void close() {
		if (_varCache)
			_dnsCache.writeCache();

		_varListen = false;
		_elSystem.close();
		
		try {
			Thread.currentThread().sleep(500);
		}
		catch (Exception e) {
			printDebug(e);
		}

		_dnsLog.closeHandle();
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
		DatagramSocket dSocket = new DatagramSocket(_intPort);
		
		printDebug("Listening for requests on port " + _intPort + ":");

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
		private String			_strLog;
		private boolean			_varDoCache;
		private InetAddress 	_inaSender;
		private DatagramSocket	_dsPointer;

		public ClientThread(DatagramSocket socket, InetAddress address, 
			int port, byte[] request) {
			_strLog = "";
			_intPort = port;
			_varDoCache = false;
			_bRequest = request;
			_inaSender = address;
			_dsPointer = socket;
		}

		public void run() {
			byte[] bResponse;
			byte[] bBuffer;

			DNSResponse dnsResponse = new DNSResponse(_bRequest);
			String strDomain = dnsResponse.getDomain();

			bBuffer = getAddress(strDomain);

			bResponse = dnsResponse.getResponse(bBuffer);
	          DatagramPacket dPacket = new DatagramPacket(bResponse, 
				bResponse.length, _inaSender, _intPort);

			try {
	          	_dsPointer.send(dPacket);
			}
			catch (Exception e) {
				printDebug(e);
			}

			_dnsLog.println(_strLog + String.format(" %40s %16s", strDomain, 
				_inaSender.getHostAddress()));

			if (_varCache && _varDoCache) {
				_dnsCache.cache(strDomain, bBuffer);
			}
		}

		/*
		 * FIXME: Performance
		 * Optimize this code!  It's slowing down responses.
		 */
		private byte[] getAddress(String domain) {
			if (isBlocked(domain)) {
				_strLog = "-B-";
				return _bBlockedIP;
			}

			DNSEntry dnsEntry = null;
		
			if (_varCache)
				dnsEntry = _dnsCache.isCached(domain);

			//Is there no entry in the cache?
			if (dnsEntry == null) {
				try {
					byte[] bBuffer = DNSQuery.doLookup(domain);
					_strLog = "-L-";
					_varDoCache = true;
					return bBuffer;
				}
				catch (Exception e) {
					_strLog = "-U-";
					_varDoCache = true;
					return _bUnknownIP;
				}
			}
			//Is the record in the cache expired?
			else if ((new Date().getTime() - _intCacheTime) > dnsEntry.getDate()) {
				try {
					byte[] bBuffer = DNSQuery.doLookup(domain);
					_strLog = "-E-";
					dnsEntry.setAddress(bBuffer);
					return bBuffer;
				}
				catch (Exception e) {
					_strLog = "-U-";
					_varDoCache = true;
					return _bUnknownIP;
				}
			}
			//It's a good cached record, return it.
			else {
				_strLog = "-C-";
				return dnsEntry.getAddress();
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

		private boolean isPresent(String domain, String[] array) {
			domain = domain.trim().toLowerCase();

			for (String strBuffer : array) {
				if (domain.endsWith(strBuffer.trim().toLowerCase()))
					return true;
			}

			return false;
		}
	}

//INITIALIZATION----------------------------------------------------------------

	private void loadSettings(String config) {
		//Create default objects.
		_strWhitelist = new String[0];
		_strBlacklist = new String[0];

		//Set default settings.
		_fmFilter = FILTERMODE.Whitelist;
		_bBlockedIP = new byte[] {(byte)192, (byte)168, 0, 2};
		_bUnknownIP = new byte[] {(byte)192, (byte)168, 0, 2};
		_intPort = 53;
		_strWhitePath = "/shared/ccinformer/dnsproxy/whitelist.dat";
		_strBlackPath = "/shared/ccinformer/dnsproxy/blacklist.dat";
		_strCachePath = "/tmp/dnscache.tmp";
		_intCacheTime = 604800000;
		_strLogPath = "/shared/ccinformer/logs";
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
		}

		printDebug("Configuration loaded: ");
		printDebug("FilterMode = " + _fmFilter);
		printDebug("BlockedIP = " + _bBlockedIP.toString());
		printDebug("UnknownIP = " + _bUnknownIP.toString());
		printDebug("Listen = " + _intPort);
		printDebug("Whitelist = " + _strWhitePath);
		printDebug("Blacklist = " + _strBlackPath);
		printDebug("CachePath = " + _strCachePath);
		printDebug("CacheTime = " + _intCacheTime);
		printDebug("LogPath = " + _strLogPath);
		printDebug("Cache = " + _varCache + "\n");
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
		else if (strSetting.equals("cachepath"))
			_strCachePath = strSplit[1];
		else if (strSetting.equals("cachetime"))
			_intCacheTime = Integer.parseInt(strSplit[1]) * 24 * 60 * 60 * 1000;
		else if (strSetting.equals("logpath"))
			_strLogPath = strSplit[1];
		else if (strSetting.equals("cache"))
			_varCache = new Boolean(strSplit[1]);
	}

	private void loadWhitelist(String whitelist) {
		if (whitelist == null)
				return;

		try {
			printDebug("Loading 'whitelist' database...");

			File fWhitelist = new File(whitelist);
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

			_strWhitelist = new String[vecBuffer.size()];
			for (int i = 0; i < _strWhitelist.length; i++) {
				_strWhitelist[i] = vecBuffer.get(i);
			}
				
			printDebug(_strWhitelist.length + " domains added to " +
				"whitelist.\n");
		}
		catch (Exception e) {
			_strWhitelist = new String[0];
			printDebug("Error: " + e.getMessage() + "\n");
		}
	}

	private void loadBlacklist(String blacklist) {
		if (blacklist == null)
				return;

		try {
			printDebug("Loading 'blacklist' database...");

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

			_strBlacklist = new String[vecBuffer.size()];
			for (int i = 0; i < _strBlacklist.length; i++) {
				_strBlacklist[i] = vecBuffer.get(i);
			}
				
			printDebug(_strBlacklist.length + " domains added to " +
				"blacklist.\n");
		}
		catch (Exception e) {
			_strBlacklist = new String[0];
			printDebug("Error: " + e.getMessage() + "\n");
		}
	}

//LOGGING-----------------------------------------------------------------------

	private static void printBanner() {
		printDebug("\n==============================================");
		printDebug("DNS Proxy                         Version " + VERSION);
		printDebug("==============================================");
		printDebug("");
	}

	public static void printDebug(String e) {
		if (DEBUG)
			System.out.println(e);
	}

	public static void printDebug(Exception e) {
		if (DEBUG)
			System.out.println(e.getMessage());
	}

//ENTRY-------------------------------------------------------------------------
	
	public static void main(String[] args) {
		/*
		 * TODO: Debug
		 * Add debugging from command line argument.
		 */
		DNSProxy dnspThread;
		String strConfig = CONFDIR + "/dnsproxy.conf";
		
		if (args.length != 0) {
			strConfig = args[0].trim();
		}
		
		dnspThread = new DNSProxy(strConfig);
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

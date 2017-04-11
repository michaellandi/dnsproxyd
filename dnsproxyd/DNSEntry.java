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

public class DNSEntry implements Serializable {
	private static final long serialVersionUID = 7526472295622877147L;
	private long		_lngUpdate;
	private String		_strDomain;
	private byte[]		_bAddress;

	public DNSEntry(String domain, byte[] ipAddress) {
		_strDomain = domain.trim().toLowerCase();
		_bAddress = ipAddress;
		_lngUpdate = System.currentTimeMillis();
	}

	public String getDomain() {
		return _strDomain;
	}

	public byte[] getAddress() {
		return _bAddress;
	}

	public void setAddress(byte[] address) {
		_bAddress = address;
		_lngUpdate = new Date().getTime();
	}

	public String getAddressString() {
		return String.format("%d.%d.%d.%d", _bAddress[0], _bAddress[1],
			_bAddress[2], _bAddress[3]);
	}

	public long getDate() {
		return _lngUpdate;
	}

	public String toString() {
		return _strDomain + ":" + getAddressString();
	}
}

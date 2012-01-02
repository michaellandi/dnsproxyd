/*
 * dnsproxyd
 * Version 1.0
 * Copyright © 2008 Michael Landi
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
import java.net.*;
import java.util.*;

public class DNSQuery {
	public static byte[] doLookup(String domain) throws Exception {
		InetAddress inaDomain;
		String[] strSplit;
		byte[] bAddress = new byte[4];

		inaDomain = InetAddress.getByName(domain);
		strSplit = inaDomain.getHostAddress().split("\\.");
	
		for (int i = 0; i < 4; i++) {
			int temp = Integer.parseInt(strSplit[i]);
			bAddress[i] = (byte)temp;
		}
	
		return bAddress;
	}

	public static byte[] doFastLookup(String domain) throws Exception {
		/*
		 * TODO: Implementation
		 * Here we generate our own request bytes, or use a system tool.
		 */
		Exception e = new Exception("Not implemented.");
		throw(e);
	}
}

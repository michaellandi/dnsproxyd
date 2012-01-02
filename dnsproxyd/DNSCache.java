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


import java.io.*;
import java.util.*;

public class DNSCache {
	/*
	 * Global variables.
	 */
	private String					_strCache;
	private Vector<DNSEntry>		_vecCache; //Vector which stores each dns entry.

	/*
	 * Default constructor, accepts no arguments.
	 */
	public DNSCache(String cachepath) {
		_strCache = cachepath;
		/*
		 * TODO: Convert Vector to array?
		 * Create a 5000 entry cache, or by file size.
		 */
		_vecCache = new Vector<DNSEntry>();

		loadCache();
		DNSProxy.printDebug(_vecCache.size() + " cached records loaded.");
	}

	public DNSEntry isCached(String strDomain) {
		strDomain = strDomain.trim().toLowerCase();

		for (DNSEntry dnsEntry : _vecCache) {
			if (strDomain.equals(dnsEntry.getDomain()))
				return dnsEntry;
		}

		return null;
	}

	public void cache(String domain, byte[] address) {
		domain = domain.trim().toLowerCase();

		_vecCache.addElement(new DNSEntry(domain, address));
	}

	public int prune(long time) {
		int intCount = 0;

		for (int i = 0; i < _vecCache.size(); i++) {
			if ((new Date().getTime() - time) > _vecCache.get(i).getDate()) {
				_vecCache.remove(i);
				intCount++;
			}
		}

		return intCount;
	}

	public void writeCache() {
		try {
			OutputStream os = new FileOutputStream(_strCache);
			ObjectOutput oo = new ObjectOutputStream(os);
			oo.writeObject(_vecCache);
			oo.close();
		}
		catch (Exception e) {
			DNSProxy.printDebug(e);
		}
	}

	public void loadCache() {
		try {
			InputStream is = new FileInputStream(_strCache);
			ObjectInput oi = new ObjectInputStream(is);
			_vecCache = (Vector)oi.readObject();
			oi.close();
		}
		catch (Exception e) {
			DNSProxy.printDebug(e);
		}
	}

	public int size() {
		return _vecCache.size();
	}
}
